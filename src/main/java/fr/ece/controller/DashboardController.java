package fr.ece.controller;

import fr.ece.dao.CategoryDAO;
import fr.ece.dao.TaskDAO;
import fr.ece.model.Category;
import fr.ece.model.Task;
import fr.ece.model.User;
import fr.ece.model.Task.Status;
import fr.ece.model.User.Role;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
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

    private User currentUser;
    private final TaskDAO taskDAO = new TaskDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private ObservableList<Task> taskList = FXCollections.observableArrayList();

    private static final String LOGIN_VIEW_PATH = "/fr/ece/view/LoginView.fxml";
    private static final String CATEGORY_VIEW_PATH = "/fr/ece/view/CategoryView.fxml";
    private static final String TASK_CRUD_VIEW_PATH = "/fr/ece/view/TaskCrudView.fxml";

    /**
     * Initialisation du contrôleur
     */
    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        
        taskTableView.setItems(taskList);
        
        // double-clic configuration
        taskTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !taskTableView.getSelectionModel().isEmpty()) {
                Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
                showTaskCrudView(selectedTask); 
            }
        });
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
            // tâches utilisateur
            List<Task> fetchedTasks = taskDAO.getTasksByUserId(currentUser.getId()); 
          
            Map<Integer, String> categoryMap = categoryDAO.getAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
            
            fetchedTasks.forEach(task -> {
                if (task.getCategoryId() != null) {
                    task.setCategoryName(categoryMap.getOrDefault(task.getCategoryId(), "N/A"));
                } else {
                    task.setCategoryName("Sans Catégorie"); 
                }
            });

            taskList.setAll(fetchedTasks);
            updateTaskStatistics(fetchedTasks);

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
    private void handleAddTask() {
        showTaskCrudView(null); 
    }
    
    @FXML
    private void handleEditTask() {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
            showTaskCrudView(selectedTask);
        } else {
            showAlert("Aucune Tâche Sélectionnée", "Veuillez sélectionner une tâche à modifier.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteTask() {
        Task selectedTask = taskTableView.getSelectionModel().getSelectedItem();
        if (selectedTask != null) {
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
        } else {
            showAlert("Aucune Tâche Sélectionnée", "Veuillez sélectionner une tâche à supprimer.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleManageCategories() {
        if (currentUser.getRole() == Role.ADMIN) {
            showCategoryView();
        }
    }

    @FXML
    private void handleLogout() {
        showLoginView();
    }
    
    // Logique de Navigation
  
    // @return Le Stage principal.
    
    private Stage getStage() {
        return (Stage) welcomeLabel.getScene().getWindow();
    }

     // Connection view.
     
    private void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_VIEW_PATH));
            Parent root = loader.load();
            
            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Déconnexion", "Impossible de basculer vers la vue de connexion.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Navigue vers la vue de gestion des Catégories.
     */
    private void showCategoryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(CATEGORY_VIEW_PATH));
            Parent root = loader.load();
            
            // >>>>>>>>>>>>>> Assume que CategoryController a une méthode pour se rafraîchir ou n'a pas besoin de données initiales <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Catégories");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Navigation", "Impossible d'ouvrir la vue de gestion des catégories.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showTaskCrudView(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(TASK_CRUD_VIEW_PATH));
            Parent root = loader.load();
            
            // Assume que TaskCrudController existe
            TaskCrudController controller = loader.getController();
            controller.setTaskData(task, this.currentUser, this);
            
            Stage stage = getStage();
            stage.setScene(new Scene(root));
            stage.setTitle(task == null ? "Ajouter une Tâche" : "Modifier la Tâche");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur de Vue", "Impossible d'ouvrir la vue d'édition de la tâche.", Alert.AlertType.ERROR);
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
}
