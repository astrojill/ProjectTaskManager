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
    @FXML private Button manageCategoriesButton;
    
    // Éléments de filtrage
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

    private static final String LOGIN_VIEW_PATH = "/fr/ece/view/LoginView.fxml";
    private static final String CATEGORY_VIEW_PATH = "/fr/ece/view/CategoryView.fxml";
    private static final String TASK_VIEW_PATH = "/fr/ece/view/TaskView.fxml";

    @FXML
    public void initialize() {
        // Configuration colonnes table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        
        // Initialisation liste filtrée
        filteredTaskList = new FilteredList<>(taskList, p -> true);
        taskTableView.setItems(filteredTaskList);
        
        // Configuration double-clic sur table
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
        
        // Listener pour la recherche de texte
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
        
        // Listener pour les tâches en retard
        if (overdueCheckBox != null) {
            overdueCheckBox.setOnAction(e -> applyFilters());
        }
    }
  
    public void setUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getUsername() + " !");
        
        // Masquer le bouton de gestion des catégories pour les utilisateurs non-ADMIN
        manageCategoriesButton.setVisible(user.getRole() == Role.ADMIN);
        
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

            taskList.setAll(fetchedTasks);
            
            updateTaskStatistics(fetchedTasks);
            
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
            if (searchField != null && !searchField.getText().isEmpty()) {
                String searchText = searchField.getText().toLowerCase();
                boolean matchesSearch = task.getTitle().toLowerCase().contains(searchText) ||
                    (task.getDescription() != null && task.getDescription().toLowerCase().contains(searchText));
                if (!matchesSearch) {
                    return false;
                }
            }
            
            if (statusFilterCombo != null && !statusFilterCombo.getValue().equals("Tous")) {
                if (!task.getStatus().name().equals(statusFilterCombo.getValue())) {
                    return false;
                }
            }
            
            if (priorityFilterCombo != null && !priorityFilterCombo.getValue().equals("Tous")) {
                if (!task.getPriority().name().equals(priorityFilterCombo.getValue())) {
                    return false;
                }
            }
            
            if (categoryFilterCombo != null && !categoryFilterCombo.getValue().equals("Toutes")) {
                String selectedCategory = categoryFilterCombo.getValue();
                if (task.getCategoryName() == null || !task.getCategoryName().equals(selectedCategory)) {
                    return false;
                }
            }
            
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

    private void showTaskView(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(TASK_VIEW_PATH));
            Parent root = loader.load();
            
            // Passer l'ID utilisateur au contrôleur de tâches
            TaskController controller = loader.getController();
            controller.setCurrentUserId(this.currentUser.getId());
            
            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Tâches");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Vue", "Impossible d'ouvrir la vue des tâches.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * boîte de dialogue d'alerte
     * @param title Le titre de l'alerte
     * @param message Le message à afficher
     * @param type Le type d'alerte
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Méthode de filtrage avancée par date
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

    // Filtre les tâches par mot-clé dans le titre et la description
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

    // Retourne les tâches actuellement affichées (filtrées)
    public List<Task> getDisplayedTasks() {
        return filteredTaskList.stream().collect(Collectors.toList());
    }
