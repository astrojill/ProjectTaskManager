package fr.ece.dao;

import fr.ece.model.Task;
import fr.ece.model.Task.Status;
import fr.ece.model.Task.Priority;
import fr.ece.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    // ajout d'une nouvelle tache dans la bdd
    public boolean addTask(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (title, description, due_date, status, priority, category_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
        ps.setString(4, task.getStatus().name());
        ps.setString(5, task.getPriority().name());
        ps.setObject(6, task.getCategoryId());
        ps.setInt(7, task.getUserId());

        int rows = ps.executeUpdate();

        if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                task.setId(keys.getInt(1));
            }
            keys.close();
        }

        ps.close();
        conn.close();
        return rows > 0;
    }

 // recupere une tache par id
    public Task getTaskById(int id) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        Task task = null;

        if (rs.next()) {
            task = mapTask(rs);
        }

        rs.close();
        ps.close();
        conn.close();

        return task;
    }

   // admin recupere toutes les taches ici
    public List<Task> getAllTasks() throws SQLException {
        String sql = "SELECT * FROM tasks ORDER BY due_date ASC, priority DESC";

        List<Task> tasks = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            tasks.add(mapTask(rs));
        }

        rs.close();
        ps.close();
        conn.close();
        return tasks;
    }

    // recupere les taches d'un user
    public List<Task> getTasksByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE user_id = ? ORDER BY due_date ASC, priority DESC";
        List<Task> tasks = new ArrayList<>();

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            tasks.add(mapTask(rs));
        }

        rs.close();
        ps.close();
        conn.close();
        return tasks;
    }

 // met a jour une tache
    public boolean updateTask(Task task) throws SQLException {
        String sql = "UPDATE tasks SET title = ?, description = ?, due_date = ?, status = ?, priority = ?, category_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setDate(3, task.getDueDate() != null ? Date.valueOf(task.getDueDate()) : null);
        ps.setString(4, task.getStatus().name());
        ps.setString(5, task.getPriority().name());
        ps.setObject(6, task.getCategoryId());
        ps.setInt(7, task.getId());
        ps.setInt(8, task.getUserId());

        int result = ps.executeUpdate();

        ps.close();
        conn.close();
        return result > 0;
    }

  // supprime une tache
    public boolean deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        int result = ps.executeUpdate();

        ps.close();
        conn.close();
        return result > 0;
    }

    // recupere les taches d'un user par status
    public List<Task> getTasksByUserIdAndStatus(int userId, String status) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND status = ? ORDER BY due_date ASC";

        List<Task> tasks = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
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

    // recupere les taches d'un user par categorie
    public List<Task> getTasksByUserIdAndCategory(int userId, int categoryId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE user_id = ? AND category_id = ? ORDER BY due_date ASC";

        List<Task> tasks = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
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

    // converti un resultset en objettask
    private Task mapTask(ResultSet rs) throws SQLException {
        Task task = new Task();

        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));

        Date due = rs.getDate("due_date");
        task.setDueDate(due != null ? due.toLocalDate() : null);

        task.setStatus(Status.valueOf(rs.getString("status")));
        task.setPriority(Priority.valueOf(rs.getString("priority")));

        task.setCategoryId(rs.getObject("category_id", Integer.class));
        task.setUserId(rs.getInt("user_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) task.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) task.setUpdatedAt(updatedAt.toLocalDateTime());

        return task;
    }

   // nb de taches liées au user
    public int countByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE assigned_to = ? OR created_by = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
