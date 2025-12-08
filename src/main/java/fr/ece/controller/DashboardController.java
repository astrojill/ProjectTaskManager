package fr.ece.controller;

import fr.ece.dao.CategoryDAO;
import fr.ece.dao.TaskDAO;
import fr.ece.model.Category;
import fr.ece.model.Task;
import fr.ece.model.User;
import fr.ece.model.Task.Status;
import fr.ece.model.Task.Priority;
import fr.ece.model.User.Role;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label todoCountLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label doneCountLabel;
    @FXML private TableView<Task> taskTableView;
    @FXML private TableColumn<Task, Integer> idColumn;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> categoryColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn; // Pour la date limite
    @FXML private TableColumn<Task, String> ownerColumn;      // Pour le propriétaire de la tâche
    @FXML private Button manageCategoriesButton;
    @FXML private Button manageUsersButton;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> priorityFilterCombo;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private Button clearFiltersButton;
    @FXML private CheckBox overdueCheckBox;

    private User currentUser;
    private final TaskDAO taskDAO = new TaskDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private ObservableList<Task> taskList = FXCollections.observableArrayList();
    private FilteredList<Task> filteredTaskList;
    private Map<Integer, String> categoryMap;

    private static final String LOGIN_VIEW_PATH = "/fxml/login.fxml";
    private static final String CATEGORY_VIEW_PATH = "/fxml/category.fxml";
    private static final String TASK_VIEW_PATH = "/fxml/task.fxml";
    private static final String USER_MANAGEMENT_VIEW_PATH = "/fxml/user-management.fxml";

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        if (dueDateColumn != null) {
            dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        }
        
        // La colonne du propriétaire doit apparaître uniquement si elle est définie
        if (ownerColumn != null) {
            ownerColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        }
        
        filteredTaskList = new FilteredList<>(taskList, p -> true);
        taskTableView.setItems(filteredTaskList);

        taskTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !taskTableView.getSelectionModel().isEmpty()) {
                Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
                showTaskView(selectedTask);
            }
        });

        // Initialisation des ComboBox de filtrage (si présents dans la vue FXML)
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("Tous", "TODO", "IN_PROGRESS", "DONE", "CANCELLED");
            statusFilterCombo.setValue("Tous");
            statusFilterCombo.setOnAction(e -> applyFilters());
        }

        if (priorityFilterCombo != null) {
            priorityFilterCombo.getItems().addAll("Tous", "LOW", "MEDIUM", "HIGH");
            priorityFilterCombo.setValue("Tous");
            priorityFilterCombo.setOnAction(e -> applyFilters());
        }

        if (categoryFilterCombo != null) {
            categoryFilterCombo.getItems().add("Toutes");
            categoryFilterCombo.setValue("Toutes");
            categoryFilterCombo.setOnAction(e -> applyFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }

        if (overdueCheckBox != null) {
            overdueCheckBox.setOnAction(e -> applyFilters());
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getUsername() + " !");

        // Masquer les boutons d'administration pour les utilisateurs non-ADMIN
        boolean isAdmin = user.getRole() == Role.ADMIN;
        manageCategoriesButton.setVisible(isAdmin);
        if (manageUsersButton != null) {
            manageUsersButton.setVisible(isAdmin);
        }

        refreshDashboardData();
    }

    public void refreshDashboardData() {
        try {
            List<Task> fetchedTasks = taskDAO.getTasksByUserId(currentUser.getId());

            List<Category> categories = categoryDAO.getAllCategories();
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

            if (categoryFilterCombo != null) {
                categoryFilterCombo.getItems().clear();
                categoryFilterCombo.getItems().add("Toutes");
                categoryFilterCombo.getItems().addAll(categories.stream()
                        .map(Category::getName)
                        .sorted()
                        .collect(Collectors.toList()));
                categoryFilterCombo.setValue("Toutes");
            }

            fetchedTasks.forEach(task -> {
                if (task.getCategoryId() != null) {
                    task.setCategoryName(categoryMap.getOrDefault(task.getCategoryId(), "N/A"));
                } else {
                    task.setCategoryName("Sans Catégorie");
                }
            });

            // Mise à jour de la liste principale
            taskList.setAll(fetchedTasks);

            // Mise à jour des statistiques avec toutes les tâches (non filtrées)
            updateTaskStatistics(fetchedTasks);

            // Réappliquer les filtres existants
            applyFilters();

        } catch (SQLException e) {
            System.err.println("Erreur de base de données lors du rafraîchissement du tableau de bord : " + e.getMessage());
            showAlert("Erreur de Base de Données", "Échec du chargement des données du tableau de bord.", Alert.AlertType.ERROR);
        }
    }

    private void updateTaskStatistics(List<Task> tasks) {
        long total = tasks.size();
        long todo = tasks.stream().filter(t -> t.getStatus() == Status.TODO).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == Status.IN_PROGRESS).count();
        long done = tasks.stream().filter(t -> t.getStatus() == Status.DONE).count();

        totalTasksLabel.setText(String.valueOf(total));
        todoCountLabel.setText(String.valueOf(todo));
        inProgressCountLabel.setText(String.valueOf(inProgress));
        doneCountLabel.setText(String.valueOf(done));
    }

    @FXML
    private void applyFilters() {
        filteredTaskList.setPredicate(task -> {
            // Filtre par recherche de texte
            if (searchField != null && !searchField.getText().isEmpty()) {
                String searchText = searchField.getText().toLowerCase();
                boolean matchesSearch = task.getTitle().toLowerCase().contains(searchText) ||
                        (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchText));
                if (!matchesSearch) {
                    return false;
                }
            }

            // Filtre par statut
            if (statusFilterCombo != null && !statusFilterCombo.getValue().equals("Tous")) {
                if (!task.getStatus().name().equals(statusFilterCombo.getValue())) {
                    return false;
                }
            }

            // Filtre par priorité
            if (priorityFilterCombo != null && !priorityFilterCombo.getValue().equals("Tous")) {
                if (!task.getPriority().name().equals(priorityFilterCombo.getValue())) {
                    return false;
                }
            }

            // Filtre par catégorie
            if (categoryFilterCombo != null && !categoryFilterCombo.getValue().equals("Toutes")) {
                String selectedCategory = categoryFilterCombo.getValue();
                if (task.getCategory() == null || !task.getCategory().equals(selectedCategory)) {
                    return false;
                }
            }

            // Filtre pour les tâches en retard
            if (overdueCheckBox != null && overdueCheckBox.isSelected()) {
                if (task.getDueDate() == null || !task.getDueDate().isBefore(LocalDate.now())) {
                    return false;
                }
            }

            return true;
        });

        List<Task> filteredTasks = filteredTaskList.stream().collect(Collectors.toList());
        updateTaskStatistics(filteredTasks);
    }

    @FXML
    private void clearFilters() {
        if (searchField != null) searchField.clear();
        if (statusFilterCombo != null) statusFilterCombo.setValue("Tous");
        if (priorityFilterCombo != null) priorityFilterCombo.setValue("Tous");
        if (categoryFilterCombo != null) categoryFilterCombo.setValue("Toutes");
        if (overdueCheckBox != null) overdueCheckBox.setSelected(false);

        applyFilters();
    }

    @FXML
    private void handleAddTask() {
        showTaskView(null);
    }

    @FXML
    private void handleEditTask() {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            showTaskView(selectedTask);
        } else {
            showAlert("Aucune Tâche Sélectionnée", "Veuillez sélectionner une tâche à modifier.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteTask() {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            // Confirmation avant suppression
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation de suppression");
            confirmAlert.setHeaderText("Supprimer la tâche");
            confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer la tâche \"" + selectedTask.getTitle() + "\" ?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        if (taskDAO.deleteTask(selectedTask.getId())) {
                            showAlert("Succès", "Tâche supprimée avec succès.", Alert.AlertType.INFORMATION);
                            refreshDashboardData();
                        } else {
                            showAlert("Échec", "Échec de la suppression de la tâche.", Alert.AlertType.ERROR);
                        }
                    } catch (SQLException e) {
                        showAlert("Erreur de Base de Données", "Échec de la suppression de la tâche : " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        } else {
            showAlert("Aucune Tâche Sélectionnée", "Veuillez sélectionner une tâche à supprimer.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleManageCategories() {
        if (currentUser.getRole() == Role.ADMIN) {
            showCategoryView();
        } else {
            showAlert("Accès refusé", "Seuls les administrateurs peuvent gérer les catégories.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleManageUsers() {
        if (currentUser.getRole() == Role.ADMIN) {
            showUserManagementView();
        } else {
            showAlert("Accès refusé", "Seuls les administrateurs peuvent gérer les utilisateurs.", Alert.AlertType.WARNING);
        }
    }
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de déconnexion");
        confirmAlert.setHeaderText("Déconnexion");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir vous déconnecter ?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showLoginView();
            }
        });
    }

    private Stage getStage() {
        return (Stage) welcomeLabel.getScene().getWindow();
    }

    private void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_VIEW_PATH));
            Parent root = loader.load();

            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Task Manager");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Déconnexion", "Impossible de basculer vers la vue de connexion.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showCategoryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(CATEGORY_VIEW_PATH));
            Parent root = loader.load();

            CategoryController controller = loader.getController();
            // Si nécessaire, passer des données au contrôleur de catégories

            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Catégories - Task Manager");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Navigation", "Impossible d'ouvrir la vue de gestion des catégories.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showUserManagementView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(USER_MANAGEMENT_VIEW_PATH));
            Parent root = loader.load();

            // Si le contrôleur a besoin de l'utilisateur connecté (pour éviter de se supprimer lui-même)
            UserManagementController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(this.currentUser);
            }

            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Utilisateurs - Task Manager");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Navigation", "Impossible d'ouvrir la vue de gestion des utilisateurs.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showTaskView(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(TASK_VIEW_PATH));
            Parent root = loader.load();

            TaskController controller = loader.getController();
            controller.setCurrentUserId(this.currentUser.getId());

            // **Modification : Utiliser la nouvelle méthode pour passer la tâche**
            // Si task est null, le contrôleur TaskController appellera handleNew().
            // S'il est non-null, il passera en mode édition de cette tâche.
            controller.setTaskToEdit(task);

            Stage stage = new Stage(); // Créer une nouvelle fenêtre pour la vue Task
            stage.setScene(new Scene(root));
            stage.setTitle(task == null ? "Créer une Tâche" : "Modifier la Tâche");
            stage.showAndWait(); // Utiliser showAndWait() si le Dashboard doit attendre la fermeture

            // Rafraîchir les données du tableau de bord après la fermeture de la fenêtre de la tâche
            refreshDashboardData();

        } catch (IOException e) {
            showAlert("Erreur de Vue", "Impossible d'ouvrir la vue des tâches.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void filterByDateRange(LocalDate startDate, LocalDate endDate) {
        filteredTaskList.setPredicate(task -> {
            if (task.getDueDate() == null) {
                return false;
            }
            return !task.getDueDate().isBefore(startDate) && !task.getDueDate().isAfter(endDate);
        });

        List<Task> filteredTasks = filteredTaskList.stream().collect(Collectors.toList());
        updateTaskStatistics(filteredTasks);
    }

    public void filterByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredTaskList.setPredicate(p -> true);
        } else {
            String lowerCaseKeyword = keyword.toLowerCase();
            filteredTaskList.setPredicate(task -> {
                return task.getTitle().toLowerCase().contains(lowerCaseKeyword) ||
                        (task.getDescription() != null &&
                                task.getDescription().toLowerCase().contains(lowerCaseKeyword));
            });
        }

        List<Task> filteredTasks = filteredTaskList.stream().collect(Collectors.toList());
        updateTaskStatistics(filteredTasks);
    }

    public List<Task> getDisplayedTasks() {
        return filteredTaskList.stream().collect(Collectors.toList());
    }

    @FXML
    private void exportTasksToCSV() {
        // à implémenter
        showAlert("Export CSV", "Fonctionnalité d'export à implémenter", Alert.AlertType.INFORMATION);
    }
}
