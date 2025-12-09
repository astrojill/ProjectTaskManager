package fr.ece.controller;

import fr.ece.dao.TaskDAO;
import fr.ece.dao.CategoryDAO;
import fr.ece.dao.UserDAO;
import fr.ece.model.Task;
import fr.ece.model.Task.Status;
import fr.ece.model.Task.Priority;
import fr.ece.model.Category;
import fr.ece.model.User;
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
    @FXML private ComboBox<User> assignedUserComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label messageLabel;
    @FXML private Label statusLabel;

    private TaskDAO taskDAO = new TaskDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();
    private UserDAO userDAO = new UserDAO();
    private ObservableList<Task> tasksList = FXCollections.observableArrayList();
    private ObservableList<Task> filteredList = FXCollections.observableArrayList();
    private Task selectedTask = null;
    private boolean isEditMode = false;

    // utilisateur courant (ex : celui connecté)
    private int currentUserId = 1;

    @FXML
    public void initialize() {

        if (tasksTable != null) {
            if (idColumn != null) {
                idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            }
            if (titleColumn != null) {
                titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
            }
            if (statusColumn != null) {
                statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            }
            if (priorityColumn != null) {
                priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
            }
            if (dueDateColumn != null) {
                dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

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
            }
            if (categoryColumn != null) {
                categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
            }

            tasksTable.setItems(filteredList);

            tasksTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            onTaskSelected(newSelection);
                        }
                    }
            );
        }

        initializeFilters();

        initializeFormComboBoxes();   // inclut maintenant aussi les utilisateurs

        loadTasks();

        if (titleField != null) {
            titleField.textProperty().addListener((obs, oldVal, newVal) -> updateSaveButtonState());
        }
        if (statusComboBox != null) {
            statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSaveButtonState());
        }
        if (priorityComboBox != null) {
            priorityComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSaveButtonState());
        }

        updateStatusBar();
    }

    private void initializeFilters() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll("Tous", "TODO", "IN_PROGRESS", "DONE");
            statusFilter.setValue("Tous");
        }

        if (priorityFilter != null) {
            priorityFilter.getItems().addAll("Tous", "LOW", "MEDIUM", "HIGH");
            priorityFilter.setValue("Tous");
        }
    }


    private void initializeFormComboBoxes() {

        if (statusComboBox != null) {
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
                    return null;
                }
            });
        }

        if (priorityComboBox != null) {
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
                    return null;
                }
            });
        }

        if (categoryComboBox != null) {
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

        initializeAssignedUserComboBox();
    }

    private void initializeAssignedUserComboBox() {
        if (assignedUserComboBox == null) return;

        try {
            List<User> users = userDAO.getAllUsers(); // il faut cette méthode dans UserDAO

            assignedUserComboBox.getItems().clear();
            assignedUserComboBox.getItems().addAll(users);

            assignedUserComboBox.setConverter(new StringConverter<User>() {
                @Override
                public String toString(User user) {
                    if (user == null) return "";
                    return user.getUsername();
                }

                @Override
                public User fromString(String string) {
                    return null;
                }
            });

        } catch (SQLException e) {
            showError("Erreur lors du chargement des utilisateurs : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        if (categoryComboBox == null) return;

        try {
            List<Category> categories = categoryDAO.getAllCategories();
            categoryComboBox.getItems().clear();
            categoryComboBox.getItems().add(null);
            categoryComboBox.getItems().addAll(categories);
        } catch (SQLException e) {
            showError("Erreur lors du chargement des catégories : " + e.getMessage());
            e.printStackTrace();
        }
    }


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


    private void applyFilters() {
        String searchText = "";
        if (searchField != null && searchField.getText() != null) {
            searchText = searchField.getText().toLowerCase().trim();
        }

        String statusValue = "Tous";
        if (statusFilter != null && statusFilter.getValue() != null) {
            statusValue = statusFilter.getValue();
        }

        String priorityValue = "Tous";
        if (priorityFilter != null && priorityFilter.getValue() != null) {
            priorityValue = priorityFilter.getValue();
        }

        filteredList.clear();

        for (Task task : tasksList) {
            boolean matchesSearch = searchText.isEmpty()
                    || task.getTitle().toLowerCase().contains(searchText)
                    || (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchText));

            boolean matchesStatus = statusValue.equals("Tous")
                    || task.getStatus().toString().equals(statusValue);

            boolean matchesPriority = priorityValue.equals("Tous")
                    || task.getPriority().toString().equals(priorityValue);

            if (matchesSearch && matchesStatus && matchesPriority) {
                filteredList.add(task);
            }
        }

        if (tasksTable != null) {
            tasksTable.setItems(filteredList);
        }
        updateCountLabel();
    }


    private void onTaskSelected(Task task) {
        selectedTask = task;

        if (editButton != null) editButton.setDisable(false);
        if (deleteButton != null) deleteButton.setDisable(false);
        if (statusButton != null) statusButton.setDisable(false);

        displayTaskDetails(task);
    }


    private void displayTaskDetails(Task task) {
        if (!isEditMode) {
            if (formTitle != null) formTitle.setText("Détails de la tâche");
            if (titleField != null) titleField.setText(task.getTitle());
            if (descriptionArea != null) descriptionArea.setText(task.getDescription());
            if (dueDatePicker != null) dueDatePicker.setValue(task.getDueDate());
            if (statusComboBox != null) statusComboBox.setValue(task.getStatus());
            if (priorityComboBox != null) priorityComboBox.setValue(task.getPriority());

            if (categoryComboBox != null) {
                if (task.getCategoryId() != null) {
                    categoryComboBox.setValue(
                            categoryComboBox.getItems().stream()
                                    .filter(c -> c != null && c.getId() == task.getCategoryId())
                                    .findFirst()
                                    .orElse(null)
                    );
                } else {
                    categoryComboBox.setValue(null);
                }
            }

            if (assignedUserComboBox != null) {
                Integer taskUserId = task.getUserId();
                if (taskUserId != null) {
                    assignedUserComboBox.setValue(
                            assignedUserComboBox.getItems().stream()
                                    .filter(u -> u != null && u.getId() == taskUserId)
                                    .findFirst()
                                    .orElse(null)
                    );
                } else {
                    assignedUserComboBox.setValue(null);
                }
            }

            setFormFieldsDisabled(true);
        }
    }


    private void setFormFieldsDisabled(boolean disabled) {
        if (titleField != null) titleField.setDisable(disabled);
        if (descriptionArea != null) descriptionArea.setDisable(disabled);
        if (dueDatePicker != null) dueDatePicker.setDisable(disabled);
        if (statusComboBox != null) statusComboBox.setDisable(disabled);
        if (priorityComboBox != null) priorityComboBox.setDisable(disabled);
        if (categoryComboBox != null) categoryComboBox.setDisable(disabled);
        if (assignedUserComboBox != null) assignedUserComboBox.setDisable(disabled);
        if (saveButton != null) saveButton.setDisable(disabled);
        if (cancelButton != null) cancelButton.setDisable(disabled);
    }


    private void updateSaveButtonState() {
        if (!isEditMode || saveButton == null) return;

        boolean isValid =
                titleField != null && titleField.getText() != null && !titleField.getText().trim().isEmpty()
                        && statusComboBox != null && statusComboBox.getValue() != null
                        && priorityComboBox != null && priorityComboBox.getValue() != null;

        saveButton.setDisable(!isValid);
    }


    @FXML
    private void handleSearch() {
        applyFilters();
    }


    @FXML
    private void handleFilterChange() {
        applyFilters();
    }


    @FXML
    private void handleNew() {
        isEditMode = true;
        selectedTask = null;

        if (formTitle != null) formTitle.setText("Nouvelle tâche");
        if (titleField != null) titleField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        if (dueDatePicker != null) dueDatePicker.setValue(null);
        if (statusComboBox != null) statusComboBox.setValue(Status.TODO);
        if (priorityComboBox != null) priorityComboBox.setValue(Priority.MEDIUM);
        if (categoryComboBox != null) categoryComboBox.setValue(null);
        if (assignedUserComboBox != null) {
            // par défaut, on peut sélectionner l’utilisateur courant s’il est dans la liste
            assignedUserComboBox.getItems().stream()
                    .filter(u -> u != null && u.getId() == currentUserId)
                    .findFirst()
                    .ifPresentOrElse(
                            assignedUserComboBox::setValue,
                            () -> assignedUserComboBox.setValue(null)
                    );
        }

        setFormFieldsDisabled(false);

        if (tasksTable != null) {
            tasksTable.getSelectionModel().clearSelection();
        }
        if (editButton != null) editButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (statusButton != null) statusButton.setDisable(true);

        hideMessage();
        updateStatusBar("Création d'une nouvelle tâche...");

        if (titleField != null) titleField.requestFocus();
    }


    @FXML
    private void handleEdit() {
        if (selectedTask == null) {
            showError("Veuillez sélectionner une tâche à modifier");
            return;
        }

        setTaskToEdit(selectedTask);
    }


    @FXML
    private void handleSave() {
        if (titleField == null || statusComboBox == null || priorityComboBox == null) {
            showError("Formulaire incomplet (problème de FXML)");
            return;
        }

        String title = titleField.getText().trim();
        String description = (descriptionArea != null && descriptionArea.getText() != null)
                ? descriptionArea.getText().trim() : "";
        LocalDate dueDate = (dueDatePicker != null) ? dueDatePicker.getValue() : null;
        Status status = statusComboBox.getValue();
        Priority priority = priorityComboBox.getValue();
        Category category = (categoryComboBox != null) ? categoryComboBox.getValue() : null;
        Integer categoryId = (category != null) ? category.getId() : null;

        User assignedUser = (assignedUserComboBox != null) ? assignedUserComboBox.getValue() : null;

        if (title.isEmpty()) {
            showError("Le titre de la tâche est obligatoire");
            return;
        }

        if (status == null || priority == null) {
            showError("Le statut et la priorité sont obligatoires");
            return;
        }

        if (assignedUser == null) {
            showError("Veuillez sélectionner un utilisateur à qui assigner la tâche");
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
                newTask.setUserId(assignedUser.getId());

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
                selectedTask.setUserId(assignedUser.getId());

                boolean success = taskDAO.updateTask(selectedTask);
                if (success) {
                    showSuccess("Tâche modifiée avec succès");
                } else {
                    showError("Erreur lors de la modification de la tâche");
                    return;
                }
            }

            loadTasks();
            handleCancel();

        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleCancel() {
        isEditMode = false;
        selectedTask = null;

        if (formTitle != null) formTitle.setText("Détails de la tâche");
        if (titleField != null) titleField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        if (dueDatePicker != null) dueDatePicker.setValue(null);
        if (statusComboBox != null) statusComboBox.setValue(null);
        if (priorityComboBox != null) priorityComboBox.setValue(null);
        if (categoryComboBox != null) categoryComboBox.setValue(null);
        if (assignedUserComboBox != null) assignedUserComboBox.setValue(null);

        setFormFieldsDisabled(true);

        if (tasksTable != null) {
            tasksTable.getSelectionModel().clearSelection();
        }
        if (editButton != null) editButton.setDisable(true);
        if (deleteButton != null) deleteButton.setDisable(true);
        if (statusButton != null) statusButton.setDisable(true);

        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();

        hideMessage();
        updateStatusBar("Prêt");
    }


    @FXML
    private void handleDelete() {
        if (selectedTask == null) {
            showError("Veuillez sélectionner une tâche à supprimer");
            return;
        }

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


    @FXML
    private void handleChangeStatus() {
        if (selectedTask == null) {
            showError("Veuillez sélectionner une tâche");
            return;
        }

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


    @FXML
    private void handleBack() {
        if (tasksTable != null) {
            Stage stage = (Stage) tasksTable.getScene().getWindow();
            stage.close();
        }
    }


    private void updateCountLabel() {
        if (countLabel == null) return;

        int count = filteredList.size();
        countLabel.setText(count + " tâche" + (count > 1 ? "s" : ""));
    }


    private void updateStatusBar() {
        updateStatusBar("Prêt");
    }

    private void updateStatusBar(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }


    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText("❌ " + message);
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        } else {
            System.err.println("Erreur : " + message);
        }
    }


    private void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText("✓ " + message);
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        } else {
            System.out.println(message);
        }
    }


    private void hideMessage() {
        if (messageLabel != null) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }
    }


    public void setTaskToEdit(Task task) {
        if (task != null) {
            this.selectedTask = task;
            this.isEditMode = true;

            if (formTitle != null) {
                formTitle.setText("Modifier la tâche #" + task.getId());
            }

            if (titleField != null) titleField.setText(task.getTitle());
            if (descriptionArea != null) descriptionArea.setText(task.getDescription());
            if (dueDatePicker != null) dueDatePicker.setValue(task.getDueDate());
            if (statusComboBox != null) statusComboBox.setValue(task.getStatus());
            if (priorityComboBox != null) priorityComboBox.setValue(task.getPriority());

            if (categoryComboBox != null) {
                if (task.getCategoryId() != null) {
                    categoryComboBox.getItems().stream()
                            .filter(c -> c != null && c.getId() == task.getCategoryId())
                            .findFirst()
                            .ifPresent(categoryComboBox::setValue);
                } else {
                    categoryComboBox.setValue(null);
                }
            }

            if (assignedUserComboBox != null) {
                Integer taskUserId = task.getUserId();
                if (taskUserId != null) {
                    assignedUserComboBox.getItems().stream()
                            .filter(u -> u != null && u.getId() == taskUserId)
                            .findFirst()
                            .ifPresentOrElse(
                                    assignedUserComboBox::setValue,
                                    () -> assignedUserComboBox.setValue(null)
                            );
                } else {
                    assignedUserComboBox.setValue(null);
                }
            }

            setFormFieldsDisabled(false);

            if (tasksTable != null) {
                tasksTable.getSelectionModel().select(task);
            }

            updateStatusBar("Modification de la tâche...");

        } else {
            handleNew();
        }
    }


    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadTasks();
    }


    public List<Task> getTasksByUserId(int userId) {
        try {
            return taskDAO.getTasksByUserId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Task getTaskById(int id) {
        try {
            return taskDAO.getTaskById(id);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Task> getTasksByUserIdAndStatus(int userId, String status) {
        try {
            return taskDAO.getTasksByUserIdAndStatus(userId, status);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Task> getTasksByUserIdAndCategory(int userId, int categoryId) {
        try {
            return taskDAO.getTasksByUserIdAndCategory(userId, categoryId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

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