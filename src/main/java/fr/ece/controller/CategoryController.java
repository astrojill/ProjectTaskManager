package fr.ece.controller;

import fr.ece.dao.CategoryDAO;
import fr.ece.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryController {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    // Catégorie actuellement en édition (null = mode "nouvelle")
    private Category currentCategory = null;

    // Scène précédente (le dashboard) - pour le bouton "Retour"
    private Scene previousScene;

    // ----------- reçu depuis le Dashboard -----------
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }

    // Éléments FXML

    @FXML
    private TableView<Category> categoriesTable;

    @FXML
    private TableColumn<Category, Integer> idColumn;

    @FXML
    private TableColumn<Category, String> nameColumn;

    @FXML
    private TextField searchField;

    @FXML
    private TextField nameField;

    @FXML
    private Label countLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Text formTitle;

    @FXML
    public void initialize() {
        // Liaison colonnes / attributs de Category
        idColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));

        // Formulaire désactivé au départ
        setFormEnabled(false);
        saveButton.setDisable(true);
        cancelButton.setDisable(true);
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        messageLabel.setVisible(false);

        // Réaction quand on sélectionne une ligne du tableau
        categoriesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> onCategorySelected(newSel)
        );

        // Charger les catégories au démarrage
        loadCategories();
    }

    // Charger toutes les catégories dans la TableView
    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.getAllCategories();
            ObservableList<Category> data = FXCollections.observableArrayList(categories);
            categoriesTable.setItems(data);

            countLabel.setText(categories.size() + " catégorie(s)");
            statusLabel.setText("Catégories chargées.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur lors du chargement des catégories.");
        }
    }

    // Quand on clique sur une catégorie dans la table
    private void onCategorySelected(Category selected) {
        if (selected == null) {
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        currentCategory = selected;
        editButton.setDisable(false);
        deleteButton.setDisable(false);

        // On remplit le champ nom
        nameField.setText(selected.getName());
        formTitle.setText("Détails de la catégorie");
    }

    // Barre de recherche
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText();

        try {
            List<Category> all = categoryDAO.getAllCategories();

            if (keyword == null || keyword.trim().isEmpty()) {
                categoriesTable.setItems(FXCollections.observableArrayList(all));
                countLabel.setText(all.size() + " catégorie(s)");
                statusLabel.setText("Affichage de toutes les catégories.");
                return;
            }

            String lower = keyword.toLowerCase();
            List<Category> filtered = new ArrayList<>();

            for (Category c : all) {
                if (c.getName() != null &&
                        c.getName().toLowerCase().contains(lower)) {
                    filtered.add(c);
                }
            }

            categoriesTable.setItems(FXCollections.observableArrayList(filtered));
            countLabel.setText(filtered.size() + " catégorie(s) trouvée(s)");
            statusLabel.setText("Filtre : \"" + keyword + "\"");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur lors de la recherche.");
        }
    }

    // Bouton nouvelle catégorie
    @FXML
    private void handleNew() {
        currentCategory = null;
        clearForm();
        setFormEnabled(true);
        saveButton.setDisable(false);
        cancelButton.setDisable(false);
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        formTitle.setText("Nouvelle catégorie");
        statusLabel.setText("Création d'une nouvelle catégorie.");
        hideMessage();

        if (nameField != null) {
            nameField.requestFocus();
        }
    }

    // Bouton modifier
    @FXML
    private void handleEdit() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélectionnez une catégorie à modifier.");
            return;
        }

        currentCategory = selected;
        setFormEnabled(true);
        saveButton.setDisable(false);
        cancelButton.setDisable(false);
        formTitle.setText("Modifier la catégorie");
        statusLabel.setText("Modification de la catégorie : " + selected.getName());
        hideMessage();

        if (nameField != null) {
            nameField.requestFocus();
        }
    }

    // Bouton supprimer
    @FXML
    private void handleDelete() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélectionnez une catégorie à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la catégorie \"" + selected.getName() + "\" ?\n\n" +
                "Attention : Les tâches de cette catégorie ne seront pas supprimées, " +
                "mais n'auront plus de catégorie assignée.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            boolean ok = categoryDAO.deleteCategory(selected.getId());
            if (ok) {
                showMessage("Catégorie supprimée.");
                clearForm();
                setFormEnabled(false);
                saveButton.setDisable(true);
                cancelButton.setDisable(true);
                loadCategories();
            } else {
                showAlert("Suppression impossible.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL lors de la suppression.");
        }
    }

    // Bouton enregistrer
    @FXML
    private void handleSave() {
        String name = nameField.getText();

        if (name == null || name.trim().isEmpty()) {
            showAlert("Le nom de la catégorie ne peut pas être vide.");
            return;
        }

        name = name.trim();

        try {
            if (currentCategory == null) {
                // Création
                Category c = new Category();
                c.setName(name);
                boolean ok = categoryDAO.addCategory(c);
                if (ok) {
                    showMessage("Catégorie ajoutée avec succès.");
                } else {
                    showAlert("Impossible d'ajouter la catégorie.");
                }
            } else {
                // Modification
                currentCategory.setName(name);
                boolean ok = categoryDAO.updateCategory(currentCategory);
                if (ok) {
                    showMessage("Catégorie mise à jour avec succès.");
                } else {
                    showAlert("Impossible de mettre à jour la catégorie.");
                }
            }

            clearForm();
            setFormEnabled(false);
            saveButton.setDisable(true);
            cancelButton.setDisable(true);
            loadCategories();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur SQL lors de l'enregistrement.");
        }
    }

    // Bouton annuler
    @FXML
    private void handleCancel() {
        clearForm();
        setFormEnabled(false);
        saveButton.setDisable(true);
        cancelButton.setDisable(true);
        formTitle.setText("Détails de la catégorie");
        statusLabel.setText("Modification annulée.");
        hideMessage();
    }

    // Bouton "Retour" : revenir à la scène précédente (dashboard)
    @FXML
    private void handleBack() {
        if (previousScene != null) {
            Stage stage = (Stage) categoriesTable.getScene().getWindow();
            stage.setScene(previousScene);
        } else {
            System.out.println("Aucun écran précédent enregistré (previousScene = null)");
        }
    }

    // Méthodes utilitaires
    private void clearForm() {
        if (nameField != null) {
            nameField.clear();
        }
    }

    private void setFormEnabled(boolean enabled) {
        if (nameField != null) {
            nameField.setDisable(!enabled);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText("✓ " + message);
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setVisible(true);
        }
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void hideMessage() {
        if (messageLabel != null) {
            messageLabel.setVisible(false);
        }
    }
}