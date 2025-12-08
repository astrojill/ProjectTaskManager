package fr.ece.controller;

import fr.ece.dao.CategoryDAO;
import fr.ece.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryController {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    // Catégorie actuellement en édition (null = mode "nouvelle")
    private Category currentCategory = null;

    // tous les elements FXML

    @FXML
    private TableView<Category> categoriesTable;

    @FXML
    private TableColumn<Category, Integer> idColumn;

    @FXML
    private TableColumn<Category, String> nameColumn;

    @FXML
    private TableColumn<Category, String> descriptionColumn;

    @FXML
    private TableColumn<Category, Integer> tasksCountColumn;

    @FXML
    private TextField searchField;

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ColorPicker colorPicker;

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
        idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));

        // là on n’a pas description
        descriptionColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(""));
        tasksCountColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(0).asObject());

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
        descriptionArea.clear();
        colorPicker.setValue(Color.WHITE);
        formTitle.setText("Détails de la catégorie");
    }

    // barre de recherche
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

    // bouton nouvelle catégorie
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
    }

    // bouton modifier
    @FXML
    private void handleEdit() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélectionne une catégorie à modifier.");
            return;
        }

        currentCategory = selected;
        setFormEnabled(true);
        saveButton.setDisable(false);
        cancelButton.setDisable(false);
        formTitle.setText("Modifier la catégorie");
        statusLabel.setText("Modification de la catégorie : " + selected.getName());
        hideMessage();
    }

    // bouton supprimer
    @FXML
    private void handleDelete() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélectionne une catégorie à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la catégorie \"" + selected.getName() + "\" ?");
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

    // bouton enregistrer
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
                    showMessage("Catégorie ajoutée.");
                } else {
                    showAlert("Impossible d'ajouter la catégorie.");
                }
            } else {
                // Modif
                currentCategory.setName(name);
                boolean ok = categoryDAO.updateCategory(currentCategory);
                if (ok) {
                    showMessage("Catégorie mise à jour.");
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

    // bouton annuler
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

    @FXML
private void handleBack() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();

        // Récupérer le stage actuel
        Stage stage = (Stage) categoriesTable.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Task Manager - Tableau de bord");
        stage.show();

    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Impossible de retourner au tableau de bord.");
    }
}



    // methodes utilitaires
    private void clearForm() {
        nameField.clear();
        descriptionArea.clear();
        colorPicker.setValue(Color.WHITE);
    }

    private void setFormEnabled(boolean enabled) {
        nameField.setDisable(!enabled);
        descriptionArea.setDisable(!enabled);
        colorPicker.setDisable(!enabled);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        statusLabel.setText(message);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
    }
}
