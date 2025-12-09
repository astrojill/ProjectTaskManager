package fr.ece.controller;

import fr.ece.dao.TaskDAO;
import fr.ece.dao.UserDAO;
import fr.ece.model.User.Role;
import fr.ece.model.User;
import fr.ece.util.PasswordUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour la gestion des utilisateurs (interface admin).
 * Permet de visualiser, filtrer, modifier et supprimer les utilisateurs.
 */
public class UserManagementController {

    // Colonnes du tableau
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, Role> roleColumn;
    @FXML private TableColumn<User, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<User, Integer> tasksCountColumn;

    // Contrôles de recherche et filtrage
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;

    // Boutons d'action
    @FXML private Button changeRoleButton;
    @FXML private Button resetPasswordButton;
    @FXML private Button deleteButton;

    // Labels d'information
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private Label currentUserLabel;

    // Statistiques
    @FXML private Text totalUsersLabel;
    @FXML private Text adminUsersLabel;
    @FXML private Text regularUsersLabel;

    // Données et DAOs
    private UserDAO userDAO;
    private TaskDAO taskDAO;
    private ObservableList<User> allUsers; // Liste observable pour sync auto avec le TableView
    private User currentUser; // Utilisateur connecté (admin)

    /**
     * Initialisation du contrôleur après chargement FXML.
     */
    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        taskDAO = new TaskDAO();

        // Redimensionnement automatique des colonnes
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        setupTableColumns();
        setupFilter();
        loadUsers();

        // Listener sur la sélection : active/désactive les boutons selon le contexte
        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean isSelected = (newSelection != null);
                    boolean canDelete = isSelected && !isCurrentUser(newSelection);

                    changeRoleButton.setDisable(!isSelected);
                    resetPasswordButton.setDisable(!isSelected);
                    deleteButton.setDisable(!canDelete); // Empêche de se supprimer soi-même

                    // Afficher des infos sur l'utilisateur sélectionné
                    if (isSelected) {
                        int taskCount = taskDAO.countByUser(newSelection.getId());
                        statusLabel.setText("Sélectionné: " + newSelection.getUsername() +
                                " (" + newSelection.getRole() + ") - " +
                                taskCount + " tâche(s)");
                    } else {
                        statusLabel.setText("Prêt");
                    }
                }
        );

        updateStatistics();
    }

    /**
     * Définit l'utilisateur connecté (appelé depuis DashboardController).
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUserLabel != null) {
            currentUserLabel.setText("Connecté: " + user.getUsername() + " (ADMIN)");
        }
    }

    /**
     * Configure le rendu personnalisé de chaque colonne du tableau.
     */
    private void setupTableColumns() {
        // Colonne ID avec style gris
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setCellFactory(column -> new TableCell<User, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(id));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
                }
            }
        });

        // Colonne username avec indication visuelle de l'utilisateur connecté
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(username);
                    User user = getTableView().getItems().get(getIndex());
                    if (isCurrentUser(user)) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #2ecc71;");
                        setText("👤 " + username + " (Vous)");
                    } else {
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });

        // Colonne rôle avec couleur selon le type
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleColumn.setCellFactory(column -> new TableCell<User, Role>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (role == Role.ADMIN) {
                        setText("Administrateur");
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    } else {
                        setText("Utilisateur");
                        setStyle("-fx-text-fill: #3498db;");
                    }
                }
            }
        });

        // Colonne date formatée
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<User, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatter.format(date));
                    setStyle("-fx-text-fill: #7f8c8d;");
                }
            }
        });

        // Colonne nombre de tâches avec code couleur selon la charge
        tasksCountColumn.setCellValueFactory(cellData -> {
            int userId = cellData.getValue().getId();
            int taskCount = taskDAO.countByUser(userId);
            return new javafx.beans.property.SimpleObjectProperty<>(taskCount);
        });

        tasksCountColumn.setCellFactory(column -> new TableCell<User, Integer>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                if (empty || count == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(count));
                    // Indicateur visuel de charge
                    if (count == 0) {
                        setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                    } else if (count < 5) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (count < 10) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    /**
     * Configure les options du filtre.
     */
    private void setupFilter() {
        filterComboBox.setItems(FXCollections.observableArrayList(
                "Tous",
                "Administrateurs",
                "Utilisateurs",
                "Avec tâches",
                "Sans tâches"
        ));
        filterComboBox.setValue("Tous");
    }

    /**
     * Charge tous les utilisateurs depuis la base de données.
     */
    private void loadUsers() {
        try {
            List<User> users = userDAO.getAllUsers();
            allUsers = FXCollections.observableArrayList(users);
            usersTable.setItems(allUsers);
            updateCount();
            updateStatistics();
            statusLabel.setText("Utilisateurs chargés avec succès");
        } catch (Exception e) {
            statusLabel.setText("Erreur lors du chargement");
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Met à jour les statistiques affichées (nombre d'admins, users, total).
     */
    private void updateStatistics() {
        if (allUsers == null || allUsers.isEmpty()) {
            if (totalUsersLabel != null) totalUsersLabel.setText("0");
            if (adminUsersLabel != null) adminUsersLabel.setText("0");
            if (regularUsersLabel != null) regularUsersLabel.setText("0");
            return;
        }

        try {
            int total = allUsers.size();
            if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(total));

            // Utilisation des streams Java 8+ pour compter
            long admins = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
            if (adminUsersLabel != null) adminUsersLabel.setText(String.valueOf(admins));

            long regular = allUsers.stream().filter(u -> u.getRole() == Role.USER).count();
            if (regularUsersLabel != null) regularUsersLabel.setText(String.valueOf(regular));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour statistiques: " + e.getMessage());
        }
    }

    /**
     * Retour au dashboard.
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            DashboardController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Task Manager - Dashboard");

        } catch (IOException e) {
            showAlert("Erreur", "Impossible de retourner au dashboard", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Recherche en temps réel dans la liste des utilisateurs.
     */
    @FXML
    private void handleSearch(KeyEvent event) {
        String searchTerm = searchField.getText().toLowerCase().trim();

        if (searchTerm.isEmpty()) {
            usersTable.setItems(allUsers);
            statusLabel.setText("Affichage de tous les utilisateurs");
        } else {
            // Filtrage avec lambda
            ObservableList<User> filtered = allUsers.filtered(user ->
                    user.getUsername().toLowerCase().contains(searchTerm)
            );
            usersTable.setItems(filtered);
            statusLabel.setText("Recherche: \"" + searchTerm + "\" - " + filtered.size() + " résultat(s)");
        }
        updateCount();
    }

    /**
     * Applique un filtre prédéfini sur la liste.
     */
    @FXML
    private void handleFilterChange(ActionEvent event) {
        String filter = filterComboBox.getValue();

        if (filter == null || filter.equals("Tous")) {
            usersTable.setItems(allUsers);
            statusLabel.setText("Affichage de tous les utilisateurs");
            updateCount();
            return;
        }

        ObservableList<User> filtered = allUsers.filtered(user -> {
            switch (filter) {
                case "Administrateurs":
                    return user.getRole() == Role.ADMIN;
                case "Utilisateurs":
                    return user.getRole() == Role.USER;
                case "Avec tâches":
                    return taskDAO.countByUser(user.getId()) > 0;
                case "Sans tâches":
                    return taskDAO.countByUser(user.getId()) == 0;
                default:
                    return true;
            }
        });

        usersTable.setItems(filtered);
        updateCount();
        statusLabel.setText("Filtre: " + filter + " - " + filtered.size() + " résultat(s)");
    }

    /**
     * Ouvre le formulaire d'inscription pour créer un nouvel utilisateur.
     */
    @FXML
    private void handleAddUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(loader.load());

            Stage registerStage = new Stage();
            registerStage.setTitle("Nouvel Utilisateur");
            registerStage.setScene(scene);
            registerStage.showAndWait(); // Bloque jusqu'à fermeture

            // Rafraîchir la liste après création
            loadUsers();
            statusLabel.setText("Liste des utilisateurs mise à jour");

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'inscription", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Change le rôle d'un utilisateur.
     */
    @FXML
    private void handleChangeRole(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un utilisateur", Alert.AlertType.WARNING);
            return;
        }

        String currentRoleStr = selected.getRole().toString();

        // Dialogue de choix avec les rôles disponibles
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentRoleStr, "ADMIN", "USER");
        dialog.setTitle("Changer le rôle");
        dialog.setHeaderText("Modifier le rôle de " + selected.getUsername());
        dialog.setContentText("Nouveau rôle:");

        // Optional : évite les NullPointerException
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newRoleStr -> {
            if (!newRoleStr.equals(currentRoleStr)) {
                try {
                    Role newRole = Role.valueOf(newRoleStr);
                    selected.setRole(newRole);

                    boolean success = userDAO.updateUser(selected);

                    if (success) {
                        usersTable.refresh(); // Force le redessin du tableau
                        updateStatistics();
                        statusLabel.setText("✓ Rôle modifié: " + selected.getUsername() + " → " + newRoleStr);
                        showAlert("Succès", "Le rôle a été modifié avec succès", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Erreur", "Impossible de modifier le rôle", Alert.AlertType.ERROR);
                    }

                } catch (IllegalArgumentException e) {
                    showAlert("Erreur", "Rôle invalide: " + newRoleStr, Alert.AlertType.ERROR);
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur.
     */
    @FXML
    private void handleResetPassword(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un utilisateur", Alert.AlertType.WARNING);
            return;
        }

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Réinitialiser le mot de passe");
        dialog.setHeaderText("Nouveau mot de passe pour " + selected.getUsername());
        dialog.setContentText("Nouveau mot de passe:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newPassword -> {
            if (newPassword.length() < 8) {
                showAlert("Erreur", "Le mot de passe doit contenir au moins 8 caractères", Alert.AlertType.ERROR);
                return;
            }

            try {
                // Hash du nouveau mot de passe
                String hashedPassword = PasswordUtils.hashPassword(newPassword);
                selected.setPasswordHash(hashedPassword);

                boolean success = userDAO.updateUser(selected);
                if (success) {
                    statusLabel.setText("✓ Mot de passe réinitialisé pour " + selected.getUsername());
                    showAlert("Succès", "Le mot de passe a été réinitialisé avec succès", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Erreur", "Impossible de réinitialiser le mot de passe", Alert.AlertType.ERROR);
                }

            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la réinitialisation: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        });
    }

    /**
     * Supprime un utilisateur et toutes ses tâches (cascade).
     */
    @FXML
    private void handleDeleteUser(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner un utilisateur", Alert.AlertType.WARNING);
            return;
        }

        // Empêcher la suppression de son propre compte
        if (isCurrentUser(selected)) {
            showAlert("Erreur", "Vous ne pouvez pas supprimer votre propre compte", Alert.AlertType.ERROR);
            return;
        }

        try {
            int taskCount = taskDAO.countByUser(selected.getId());

            // Confirmation avec avertissement sur les tâches
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation de suppression");
            confirm.setHeaderText("Supprimer l'utilisateur " + selected.getUsername() + " ?");
            confirm.setContentText(
                    "Cet utilisateur possède " + taskCount + " tâche(s).\n" +
                            "Toutes ses tâches seront également supprimées.\n\n" +
                            "⚠ Cette action est irréversible !"
            );

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean success = userDAO.deleteUser(selected.getId());
                if (success) {
                    allUsers.remove(selected); // Suppression de la liste observable
                    updateStatistics();
                    updateCount();
                    statusLabel.setText("✓ Utilisateur supprimé: " + selected.getUsername());
                    showAlert("Succès", "L'utilisateur a été supprimé avec succès", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Erreur", "Impossible de supprimer l'utilisateur", Alert.AlertType.ERROR);
                }
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si l'utilisateur passé en paramètre est l'utilisateur connecté.
     */
    private boolean isCurrentUser(User user) {
        return currentUser != null && user != null &&
                currentUser.getId() == user.getId();
    }

    /**
     * Met à jour le label du nombre d'utilisateurs affichés.
     */
    private void updateCount() {
        int count = usersTable.getItems().size();
        if (countLabel != null) {
            countLabel.setText(count + " utilisateur" + (count > 1 ? "s" : ""));
        }
    }

    /**
     * Affiche une alerte JavaFX.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}