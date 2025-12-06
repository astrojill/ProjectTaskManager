package fr.ece.controller;

import fr.ece.dao.TaskDAO;
import fr.ece.dao.CategoryDAO;
import fr.ece.model.Task;
import fr.ece.model.Task.Status;
import fr.ece.model.Task.Priority;
import fr.ece.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskController {
    
    @FXML private TableView<Task> tasksTable;
    @FXML private TableColumn<Task, Integer> idColumn;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, Status> statusColumn;
    @FXML private TableColumn<Task, Priority> priorityColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, String> categoryColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button statusButton;
    @FXML private Label countLabel;

    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<Status> statusComboBox;
    @FXML private ComboBox<Priority> priorityComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label messageLabel;
    @FXML private Label statusLabel;

    private TaskDAO taskDAO = new TaskDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();
    private ObservableList<Task> tasksList = FXCollections.observableArrayList();
    private ObservableList<Task> filteredList = FXCollections.observableArrayList();
    private Task selectedTask = null;
    private boolean isEditMode = false;
    private int currentUserId = 1; // À remplacer par l'utilisateur connecté

    /**
     * Méthode d'initialisation appelée automatiquement après le chargement du FXML
     */
    @FXML
    public void initialize() {
        // Configuration des colonnes du tableau
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        // Formatter pour la date
        dueDateColumn.setCellFactory(column -> new TableCell<Task, LocalDate>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        // Initialiser les filtres
        initializeFilters();

        // Initialiser les ComboBox du formulaire
        initializeFormComboBoxes();

        // Charger les données
        loadTasks();

        // Listener pour la sélection dans le tableau
        tasksTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        onTaskSelected(newSelection);
                    }
                }
        );

        // Listener pour activer/désactiver le bouton Enregistrer
        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateSaveButtonState();
        });

        statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSaveButtonState();
        });

        priorityComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSaveButtonState();
        });

        updateStatusBar();
    }

    /**
     * Initialise les filtres de statut et priorité
     */
    private void initializeFilters() {
        // Filtre de statut
        statusFilter.getItems().addAll("Tous", "TODO", "IN_PROGRESS", "DONE");
        statusFilter.setValue("Tous");

        // Filtre de priorité
        priorityFilter.getItems().addAll("Tous", "LOW", "MEDIUM", "HIGH");
        priorityFilter.setValue("Tous");
    }

    /**
     * Initialise les ComboBox du formulaire
     */
    private void initializeFormComboBoxes() {
        // Status ComboBox
        statusComboBox.getItems().setAll(Status.values());
        statusComboBox.setConverter(new StringConverter<Status>() {
            @Override
            public String toString(Status status) {
                if (status == null) return "";
                switch (status) {
                    case TODO: return "À faire";
                    case IN_PROGRESS: return "En cours";
                    case DONE: return "Terminé";
                    default: return status.toString();
                }
            }

            @Override
            public Status fromString(String string) {
                return null; // Non utilisé
            }
        });

        // Priority ComboBox
        priorityComboBox.getItems().setAll(Priority.values());
        priorityComboBox.setConverter(new StringConverter<Priority>() {
            @Override
            public String toString(Priority priority) {
                if (priority == null) return "";
                switch (priority) {
                    case LOW: return "Basse";
                    case MEDIUM: return "Moyenne";
                    case HIGH: return "Haute";
                    default: return priority.toString();
                }
            }

            @Override
            public Priority fromString(String string) {
                return null; // Non utilisé
            }
        });

        // Category ComboBox
        loadCategories();
        categoryComboBox.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null; // Non utilisé
            }
        });
    }

    /**
     * Charge les catégories dans le ComboBox
     */
    private void loadCategories() {
        List<Category> categories = categoryDAO.getAllCategories();
        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().add(null); // Option "Aucune catégorie"
        categoryComboBox.getItems().addAll(categories);
    }

    /**
     * Charge toutes les tâches de l'utilisateur
     */
    private void loadTasks() {
        tasksList.clear();
        try {
            List<Task> tasks = taskDAO.getTasksByUserId(currentUserId);
            tasksList.addAll(tasks);
            applyFilters();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des tâches : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Applique les filtres de recherche, statut et priorité
     */
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String statusValue = statusFilter.getValue();
        String priorityValue = priorityFilter.getValue();

        filteredList.clear();

        for (Task task : tasksList) {
            // Filtre de recherche
            boolean matchesSearch = searchText.isEmpty() ||
                    task.getTitle().toLowerCase().contains(searchText) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchText));

            // Filtre de statut
            boolean matchesStatus = statusValue.equals("Tous") ||
                    task.getStatus().toString().equals(statusValue);

            // Filtre de priorité
            boolean matchesPriority = priorityValue.equals("Tous") ||
                    task.getPriority().toString().equals(priorityValue);

            if (matchesSearch && matchesStatus && matchesPriority) {
                filteredList.add(task);
            }
        }

        tasksTable.setItems(filteredList);
        updateCountLabel();
    }

    /**
     * Gère la sélection d'une tâche dans le tableau
     */
    private void onTaskSelected(Task task) {
        selectedTask = task;
        editButton.setDisable(false);
        deleteButton.setDisable(false);
        statusButton.setDisable(false);

        // Afficher les détails dans le formulaire (lecture seule)
        displayTaskDetails(task);
    }

    /**
     * Affiche les détails d'une tâche dans le formulaire
     */
    private void displayTaskDetails(Task task) {
        if (!isEditMode) {
            formTitle.setText("Détails de la tâche");
            titleField.setText(task.getTitle());
            descriptionArea.setText(task.getDescription());
            dueDatePicker.setValue(task.getDueDate());
            statusComboBox.setValue(task.getStatus());
            priorityComboBox.setValue(task.getPriority());

            // Trouver la catégorie correspondante
            if (task.getCategoryId() != null) {
                categoryComboBox.setValue(categoryComboBox.getItems().stream()
                        .filter(c -> c != null && c.getId() == task.getCategoryId())
                        .findFirst()
                        .orElse(null));
            } else {
                categoryComboBox.setValue(null);
            }

            // Désactiver les champs en mode lecture
            setFormFieldsDisabled(true);
        }
    }

    /**
     * Active ou désactive les champs du formulaire
     */
    private void setFormFieldsDisabled(boolean disabled) {
        titleField.setDisable(disabled);
        descriptionArea.setDisable(disabled);
        dueDatePicker.setDisable(disabled);
        statusComboBox.setDisable(disabled);
        priorityComboBox.setDisable(disabled);
        categoryComboBox.setDisable(disabled);
        saveButton.setDisable(disabled);
        cancelButton.setDisable(disabled);
    }

    /**
     * Met à jour l'état du bouton Enregistrer
     */
    private void updateSaveButtonState() {
        if (isEditMode) {
            boolean isValid = titleField.getText() != null && !titleField.getText().trim().isEmpty()
                    && statusComboBox.getValue() != null
                    && priorityComboBox.getValue() != null;
            saveButton.setDisable(!isValid);
        }
    }

    /**
     * Recherche des tâches
     */
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    /**
     * Changement de filtre
     */
    @FXML
    private void handleFilterChange() {
        applyFilters();
    }

    /**
     * Créer une nouvelle tâche
     */
    @FXML
    private void handleNew() {
        isEditMode = true;
        selectedTask = null;

        // Réinitialiser le formulaire
        formTitle.setText("Nouvelle tâche");
        titleField.clear();
        descriptionArea.clear();
        dueDatePicker.setValue(null);
        statusComboBox.setValue(Status.TODO);
        priorityComboBox.setValue(Priority.MEDIUM);
        categoryComboBox.setValue(null);

        // Activer les champs
        setFormFieldsDisabled(false);

        // Désélectionner dans le tableau
        tasksTable.getSelectionModel().clearSelection();
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        statusButton.setDisable(true);

        hideMessage();
        updateStatusBar("Création d'une nouvelle tâche...");
        titleField.requestFocus();
    }

    /**
     * Modifier une tâche existante
     */
    @FXML
    private void handleEdit() {
        if (selectedTask == null) {
            showError("Veuillez sélectionner une tâche à modifier");
            return;
        }

        isEditMode = true;
        formTitle.setText("Modifier la tâche");
        setFormFieldsDisabled(false);
        updateStatusBar("Modification de la tâche...");
    }

    /**
     * Enregistrer une tâche (création ou modification)
     */
    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        LocalDate dueDate = dueDatePicker.getValue();
        Status status = statusComboBox.getValue();
        Priority priority = priorityComboBox.getValue();
        Category category = categoryComboBox.getValue();
        Integer categoryId = (category != null) ? category.getId() : null;

        // Validation
        if (title.isEmpty()) {
            showError("Le titre de la tâche est obligatoire");
            return;
        }

        if (status == null || priority == null) {
            showError("Le statut et la priorité sont obligatoires");
            return;
        }

        try {
            if (selectedTask == null) {
                // Création
                Task newTask = new Task();
                newTask.setTitle(title);
                newTask.setDescription(description);
                newTask.setDueDate(dueDate);
                newTask.setStatus(status);
                newTask.setPriority(priority);
                newTask.setCategoryId(categoryId);
                newTask.setUserId(currentUserId);

                boolean success = taskDAO.addTask(newTask);
                if (success) {
                    showSuccess("Tâche créée avec succès");
                } else {
                    showError("Erreur lors de la création de la tâche");
                    return;
                }
            } else {
                // Modification
                selectedTask.setTitle(title);
                selectedTask.setDescription(description);
                selectedTask.setDueDate(dueDate);
                selectedTask.setStatus(status);
                selectedTask.setPriority(priority);
                selectedTask.setCategoryId(categoryId);

                boolean success = taskDAO.updateTask(selectedTask);
                if (success) {
                    showSuccess("Tâche modifiée avec succès");
                } else {
                    showError("Erreur lors de la modification de la tâche");
                    return;
                }
            }

            // Recharger et réinitialiser
            loadTasks();
            handleCancel();

        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Annuler l'édition
     */
    @FXML
    private void handleCancel() {
        isEditMode = false;
        selectedTask = null;

        // Réinitialiser le formulaire
        formTitle.setText("Détails de la tâche");
        titleField.clear();
        descriptionArea.clear();
        dueDatePicker.setValue(null);
        statusComboBox.setValue(null);
        priorityComboBox.setValue(null);
        categoryComboBox.setValue(null);

        // Désactiver les champs
        setFormFieldsDisabled(true);

        // Désélectionner
        tasksTable.getSelectionModel().clearSelection();
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        statusButton.setDisable(true);

        hideMessage();
        updateStatusBar("Prêt");
    }

    /**
     * Supprimer une tâche
     */
    @FXML
    private void handleDelete() {
        if (selectedTask == null) {
            showError("Veuillez sélectionner une tâche à supprimer");
            return;
        }

        // Confirmation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la tâche");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer la tâche \"" +
                selectedTask.getTitle() + "\" ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = taskDAO.deleteTask(selectedTask.getId());
                if (success) {
                    showSuccess("Tâche supprimée avec succès");
                    loadTasks();
                    handleCancel();
                } else {
                    showError("Erreur lors de la suppression de la tâche");
                }
            } catch (SQLException e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Changer le statut d'une tâche
     */
    @FXML
    private void handleChangeStatus() {
        if (selectedTask == null) {
            showError("Veuillez sélectionner une tâche");
            return;
        }

        // Créer un dialogue pour choisir le nouveau statut
        ChoiceDialog<Status> dialog = new ChoiceDialog<>(selectedTask.getStatus(), Status.values());
        dialog.setTitle("Changer le statut");
        dialog.setHeaderText("Changer le statut de la tâche");
        dialog.setContentText("Nouveau statut:");

        Optional<Status> result = dialog.showAndWait();
        result.ifPresent(status -> {
            try {
                selectedTask.setStatus(status);
                boolean success = taskDAO.updateTask(selectedTask);
                if (success) {
                    showSuccess("Statut modifié avec succès");
                    loadTasks();
                } else {
                    showError("Erreur lors du changement de statut");
                }
            } catch (SQLException e) {
                showError("Erreur lors du changement de statut : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Retour à l'écran précédent
     */
    @FXML
    private void handleBack() {
        Stage stage = (Stage) tasksTable.getScene().getWindow();
        stage.close();
    }


    /**
     * Met à jour le label de comptage
     */
    private void updateCountLabel() {
        int count = filteredList.size();
        countLabel.setText(count + " tâche" + (count > 1 ? "s" : ""));
    }

    /**
     * Met à jour la barre de statut
     */
    private void updateStatusBar() {
        updateStatusBar("Prêt");
    }

    private void updateStatusBar(String message) {
        statusLabel.setText(message);
    }

    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    /**
     * Affiche un message de succès
     */
    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-text-fill: #27ae60;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    /**
     * Cache le message
     */
    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    /**
     * Définit l'utilisateur courant
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadTasks();
    }

    /**
     * Récupère toutes les tâches d'un utilisateur
     */
    public List<Task> getTasksByUserId(int userId) {
        try {
            return taskDAO.getTasksByUserId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Récupère une tâche par son ID
     */
    public Task getTaskById(int id) {
        try {
            return taskDAO.getTaskById(id);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Récupère les tâches d'un utilisateur par statut
     */
    public List<Task> getTasksByUserIdAndStatus(int userId, String status) {
        try {
            return taskDAO.getTasksByUserIdAndStatus(userId, status);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Récupère les tâches d'un utilisateur par catégorie
     */
    public List<Task> getTasksByUserIdAndCategory(int userId, int categoryId) {
        try {
            return taskDAO.getTasksByUserIdAndCategory(userId, categoryId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ajoute une nouvelle tâche
     */
    public boolean addTask(String title, String description, LocalDate dueDate,
                           String status, String priority, Integer categoryId, int userId) {

        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDescription(description != null ? description.trim() : "");
        task.setDueDate(dueDate);

        try {
            task.setStatus(Status.valueOf(status));
            task.setPriority(Priority.valueOf(priority));
        } catch (IllegalArgumentException e) {
            return false;
        }

        task.setCategoryId(categoryId);
        task.setUserId(userId);

        try {
            return taskDAO.addTask(task);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Met à jour une tâche existante
     */
    public boolean updateTask(int taskId, String title, String description, LocalDate dueDate,
                              String status, String priority, Integer categoryId, int userId) {

        if (title == null || title.trim().isEmpty()) {
            return false;
        }

        Task existing;
        try {
            existing = taskDAO.getTaskById(taskId);
            if (existing == null) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        existing.setTitle(title.trim());
        existing.setDescription(description != null ? description.trim() : "");
        existing.setDueDate(dueDate);

        try {
            existing.setStatus(Status.valueOf(status));
            existing.setPriority(Priority.valueOf(priority));
        } catch (IllegalArgumentException e) {
            return false;
        }

        existing.setCategoryId(categoryId);
        existing.setUserId(userId);

        try {
            return taskDAO.updateTask(existing);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Change le statut d'une tâche
     */
    public boolean changeTaskStatus(int taskId, String newStatus, int userId) {
        try {
            Task existing = taskDAO.getTaskById(taskId);
            if (existing == null) {
                return false;
            }

            existing.setStatus(Status.valueOf(newStatus));
            return taskDAO.updateTask(existing);

        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime une tâche par son ID
     */
    public boolean deleteTask(int taskId) {
        try {
            Task existing = taskDAO.getTaskById(taskId);
            if (existing == null) {
                return false;
            }

            return taskDAO.deleteTask(taskId);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
