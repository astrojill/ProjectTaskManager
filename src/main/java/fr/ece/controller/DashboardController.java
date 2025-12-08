package fr.ece.controller;

import fr.ece.dao.CategoryDAO;
import fr.ece.dao.TaskDAO;
import fr.ece.dao.UserDAO;
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
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, String> ownerColumn;
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
    private final UserDAO userDAO = new UserDAO();
    private ObservableList<Task> taskList = FXCollections.observableArrayList();
    private FilteredList<Task> filteredTaskList;
    private Map<Integer, String> categoryMap;
    private Map<Integer, String> userMap;

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

        // Utiliser categoryName au lieu de category
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        if (dueDateColumn != null) {
            dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        }

        if (ownerColumn != null) {
            // Utiliser userName au lieu de owner
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

            // Charger les catégories
            List<Category> categories = categoryDAO.getAllCategories();
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

            // Charger les utilisateurs
            List<User> users = userDAO.getAllUsers();
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));

            if (categoryFilterCombo != null) {
                categoryFilterCombo.getItems().clear();
                categoryFilterCombo.getItems().add("Toutes");
                categoryFilterCombo.getItems().addAll(
                        categories.stream().map(Category::getName).sorted().collect(Collectors.toList())
                );
                categoryFilterCombo.setValue("Toutes");
            }

            // Enrichir chaque tâche avec le nom de catégorie et le nom d'utilisateur
            fetchedTasks.forEach(task -> {
                // Définir le nom de la catégorie
                if (task.getCategoryId() != null) {
                    task.setCategoryName(categoryMap.getOrDefault(task.getCategoryId(), "N/A"));
                } else {
                    task.setCategoryName("Sans Catégorie");
                }

                // Définir le nom de l'utilisateur
                if (task.getUserId() != null) {
                    task.setUserName(userMap.getOrDefault(task.getUserId(), "N/A"));
                } else {
                    task.setUserName("Non assigné");
                }
            });

            taskList.setAll(fetchedTasks);
            updateTaskStatistics(fetchedTasks);
            applyFilters();

        } catch (SQLException e) {
            showAlert("Erreur de Base de Données", "Échec du chargement des données du tableau de bord.", Alert.AlertType.ERROR);
            e.printStackTrace();
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

            if (searchField != null && !searchField.getText().isEmpty()) {
                String searchText = searchField.getText().toLowerCase();
                boolean matches = task.getTitle().toLowerCase().contains(searchText)
                        || (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchText));
                if (!matches) return false;
            }

            if (statusFilterCombo != null && !"Tous".equals(statusFilterCombo.getValue())) {
                if (!task.getStatus().name().equals(statusFilterCombo.getValue())) return false;
            }

            if (priorityFilterCombo != null && !"Tous".equals(priorityFilterCombo.getValue())) {
                if (!task.getPriority().name().equals(priorityFilterCombo.getValue())) return false;
            }

            if (categoryFilterCombo != null && !"Toutes".equals(categoryFilterCombo.getValue())) {
                // Utiliser categoryName au lieu de getCategory()
                if (task.getCategoryName() == null || !task.getCategoryName().equals(categoryFilterCombo.getValue())) return false;
            }

            if (overdueCheckBox != null && overdueCheckBox.isSelected()) {
                if (task.getDueDate() == null || !task.getDueDate().isBefore(LocalDate.now())) return false;
            }

            return true;
        });

        updateTaskStatistics(filteredTaskList.stream().collect(Collectors.toList()));
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
        Task selected = taskTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showTaskView(selected);
        } else {
            showAlert("Aucune Tâche Sélectionnée", "Veuillez sélectionner une tâche à modifier.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteTask() {
        Task selected = taskTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation de suppression");
            confirm.setHeaderText("Supprimer la tâche");
            confirm.setContentText("Supprimer \"" + selected.getTitle() + "\" ?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        if (taskDAO.deleteTask(selected.getId())) {
                            showAlert("Succès", "Tâche supprimée.", Alert.AlertType.INFORMATION);
                            refreshDashboardData();
                        }
                    } catch (SQLException e) {
                        showAlert("Erreur", "Échec de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });

        } else {
            showAlert("Aucune Tâche", "Veuillez sélectionner une tâche.", Alert.AlertType.WARNING);
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
        confirmAlert.setTitle("Déconnexion");
        confirmAlert.setContentText("Voulez-vous vraiment vous déconnecter ?");

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
            showAlert("Erreur", "Impossible d'afficher la vue de connexion.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showCategoryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(CATEGORY_VIEW_PATH));
            Parent root = loader.load();

            CategoryController controller = loader.getController();
            controller.setPreviousScene(welcomeLabel.getScene());

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

            UserManagementController controller = loader.getController();
            if (controller != null) controller.setCurrentUser(currentUser);

            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Utilisateurs - Task Manager");
            stage.show();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la vue de gestion des utilisateurs.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showTaskView(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(TASK_VIEW_PATH));
            Parent root = loader.load();

            TaskController controller = loader.getController();
            controller.setCurrentUserId(currentUser.getId());
            controller.setTaskToEdit(task);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(task == null ? "Créer une Tâche" : "Modifier la Tâche");
            stage.showAndWait();

            refreshDashboardData();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la vue des tâches.", Alert.AlertType.ERROR);
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
            if (task.getDueDate() == null) return false;
            return !task.getDueDate().isBefore(startDate) && !task.getDueDate().isAfter(endDate);
        });

        updateTaskStatistics(filteredTaskList.stream().collect(Collectors.toList()));
    }

    public void filterByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredTaskList.setPredicate(p -> true);
        } else {
            String lower = keyword.toLowerCase();
            filteredTaskList.setPredicate(task ->
                    task.getTitle().toLowerCase().contains(lower)
                            || (task.getDescription() != null
                            && task.getDescription().toLowerCase().contains(lower))
            );
        }

        updateTaskStatistics(filteredTaskList.stream().collect(Collectors.toList()));
    }

    public List<Task> getDisplayedTasks() {
        return filteredTaskList.stream().collect(Collectors.toList());
    }

    @FXML
    private void exportTasksToCSV() {
        showAlert("Export CSV", "Fonctionnalité d'export à implémenter.", Alert.AlertType.INFORMATION);
    }
}