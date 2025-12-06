package fr.ece.dao;

import fr.ece.model.User;
import fr.ece.util.DatabaseConnection;
import fr.ece.util.PasswordUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Authentifie un utilisateur avec son nom d'utilisateur et mot de passe
     * @param username Le nom d'utilisateur
     * @param password Le mot de passe en clair
     * @return L'utilisateur si authentifié, null sinon
     */
    public User authenticateUser(String username, String password) throws SQLException {
        // Récupère l'utilisateur depuis la base de données
        User user = getUserByUsername(username);

        // Vérifie si l'utilisateur existe ET si le mot de passe correspond
        if (user != null && PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    /**
     * Crée un nouvel utilisateur dans la base de données
     * @param user L'utilisateur à créer
     * @return true si la création a réussi, false sinon
     */
    public boolean createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

        // Obtenir la connexion à la base de données
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);

        // Remplir les paramètres de la requête
        pstmt.setString(1, user.getUsername());
        pstmt.setString(2, user.getPasswordHash());
        pstmt.setString(3, user.getRole().name()); // Convertit l'enum en String

        // Exécuter la requête
        int result = pstmt.executeUpdate();

        // Fermer les ressources
        pstmt.close();
        conn.close();

        return result > 0; // Retourne true si au moins 1 ligne a été ajoutée
    }

    /**
     * Récupère un utilisateur par son ID
     * @param id L'ID de l'utilisateur
     * @return L'utilisateur trouvé, null si aucun utilisateur trouvé
     */
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);

        // Exécuter la requête et récupérer le résultat
        ResultSet rs = pstmt.executeQuery();
        User user = null;

        // Si un résultat est trouvé, créer l'objet User
        if (rs.next()) {
            user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setRole(Role.valueOf(rs.getString("role"))); // Convertit String en enum

            // Gérer le timestamp created_at s'il existe
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }
        }

        // Fermer les ressources
        rs.close();
        pstmt.close();
        conn.close();

        return user;
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur
     * @param username Le nom d'utilisateur
     * @return L'utilisateur trouvé, null si aucun utilisateur trouvé
     */
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        Connection conn = Database.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);

        ResultSet rs = pstmt.executeQuery();
        User user = null;

        if (rs.next()) {
            user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setRole(Role.valueOf(rs.getString("role")));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }
        }

        rs.close();
        pstmt.close();
        conn.close();

        return user;
    }

    /**
     * Récupère tous les utilisateurs de la base de données
     * @return Une liste de tous les utilisateurs
     */
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        Connection conn = Database.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        // Parcourir tous les résultats
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setRole(Role.valueOf(rs.getString("role")));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }

            users.add(user); // Ajouter l'utilisateur à la liste
        }

        rs.close();
        stmt.close();
        conn.close();

        return users;
    }

    /**
     * Met à jour un utilisateur existant
     * @param user L'utilisateur avec les nouvelles informations
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, role = ? WHERE id = ?";

        Connection conn = Database.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);

        pstmt.setString(1, user.getUsername());
        pstmt.setString(2, user.getRole().name());
        pstmt.setInt(3, user.getId());

        int result = pstmt.executeUpdate();

        pstmt.close();
        conn.close();

        return result > 0; // Retourne true si au moins 1 ligne a été modifiée
    }

    /**
     * Supprime un utilisateur par son ID
     * @param id L'ID de l'utilisateur à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        Connection conn = Database.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);

        int result = pstmt.executeUpdate();

        pstmt.close();
        conn.close();

        return result > 0; // Retourne true si au moins 1 ligne a été supprimée
    }
}
