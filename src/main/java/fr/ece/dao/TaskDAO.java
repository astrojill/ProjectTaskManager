package fr.ece.dao;

import model.Task;
import model.Task.Status;
import model.Task.Priority;
import util.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TaskDAO {

/**
     * Ajoute une nouvelle tâche dans la base de données
     * @param task La tâche à ajouter
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addTask(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (title, description, due_date, status, priority, category_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = Database.getConnection();
        // RETURN_GENERATED_KEYS permet de récupérer l'ID auto-généré
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        // Remplir les paramètres de la requête
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
        ps.setString(4, task.getStatus().name()); // Convertit l'enum en String
        ps.setString(5, task.getPriority().name()); // Convertit l'enum en String
        ps.setObject(6, task.getCategoryId()); // Peut être null
        ps.setInt(7, task.getUserId());

        int rows = ps.executeUpdate();

        // Si l'insertion a réussi, récupérer l'ID généré
        if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                task.setId(keys.getInt(1)); // Mettre à jour l'objet task avec son nouvel ID
            }
            keys.close();
            ps.close();
            conn.close();
            return true;
        }

        ps.close();
        conn.close();
        return false;
    }

    /**
     * Récupère une tâche par son ID
     * @param id L'ID de la tâche
     * @return La tâche trouvée, null si aucune tâche trouvée
     */
    public Task getTaskById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        Task task = null;

        if (rs.next()) {
            task = mapTask(rs); // Utilise la méthode helper pour créer l'objet Task
        }

        rs.close();
        ps.close();
        conn.close();

        return task;
    }

    /**
     * Récupère toutes les tâches d'un utilisateur
     * @param userId L'ID de l'utilisateur
     * @return Une liste des tâches de l'utilisateur, triées par date et priorité
     */
    public List<Task> getTasksByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY due_date ASC, priority DESC";
        List<Task> tasks = new ArrayList<>();

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        // Parcourir tous les résultats
        while (rs.next()) {
            tasks.add(mapTask(rs));
        }

        rs.close();
        ps.close();
        conn.close();

        return tasks;
    }

    /**
     * Met à jour une tâche existante
     * @param task La tâche avec les nouvelles informations
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean updateTask(Task task) throws SQLException {
        String sql = "UPDATE tasks SET title = ?, description = ?, due_date = ?, status = ?, priority = ?, category_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
        ps.setString(4, task.getStatus().name());
        ps.setString(5, task.getPriority().name());
        ps.setObject(6, task.getCategoryId());
        ps.setInt(7, task.getId());
        ps.setInt(8, task.getUserId()); // Sécurité : on vérifie que la tâche appartient bien à l'utilisateur

        int result = ps.executeUpdate();

        ps.close();
        conn.close();

        return result > 0;
    }

    /**
     * Supprime une tâche par son ID
     * @param id L'ID de la tâche à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        int result = ps.executeUpdate();

        ps.close();
        conn.close();

        return result > 0;
    }

    /**
     * Récupère les tâches d'un utilisateur par statut
     * @param userId L'ID de l'utilisateur
     * @param status Le statut des tâches (TODO, IN_PROGRESS, DONE)
     * @return Une liste des tâches correspondantes
     */
    public List<Task> getTasksByUserIdAndStatus(int userId, String status) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND status = ? ORDER BY due_date ASC";
        List<Task> tasks = new ArrayList<>();

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setString(2, status);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            tasks.add(mapTask(rs));
        }

        rs.close();
        ps.close();
        conn.close();

        return tasks;
    }

    /**
     * Récupère les tâches d'un utilisateur par catégorie
     * @param userId L'ID de l'utilisateur
     * @param categoryId L'ID de la catégorie
     * @return Une liste des tâches de cette catégorie
     */
    public List<Task> getTasksByUserIdAndCategory(int userId, int categoryId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND category_id = ? ORDER BY due_date ASC";
        List<Task> tasks = new ArrayList<>();

        Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, categoryId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            tasks.add(mapTask(rs));
        }

        rs.close();
        ps.close();
        conn.close();

        return tasks;
    }

    /**
     * Méthode helper pour convertir un ResultSet en objet Task
     * @param rs Le ResultSet contenant les données de la tâche
     * @return Un objet Task rempli avec les données du ResultSet
     */
    private Task mapTask(ResultSet rs) throws SQLException {
        Task task = new Task();

        // Récupérer les données de base
        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));

        // Gérer la date d'échéance (peut être null)
        Date dueDate = rs.getDate("due_date");
        task.setDueDate(dueDate != null ? dueDate.toLocalDate() : null);

        // Convertir les String en enum
        task.setStatus(Status.valueOf(rs.getString("status")));
        task.setPriority(Priority.valueOf(rs.getString("priority")));

        // Récupérer les IDs
        task.setCategoryId(rs.getObject("category_id", Integer.class)); // Peut être null
        task.setUserId(rs.getInt("user_id"));

        // Gérer les timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) task.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) task.setUpdatedAt(updatedAt.toLocalDateTime());

        return task;
    }
}
