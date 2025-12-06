package fr.ece.dao;

import fr.ece.model.Category;
import fr.ece.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    /**
     * Ajoute une nouvelle catégorie dans la base de données
     * @param category La catégorie à ajouter
     * @return true si l'ajout a réussi, false sinon
     */
    public boolean addCategory(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name) VALUES (?)";

        Connection conn = DatabaseConnection.getConnection();
        // RETURN_GENERATED_KEYS permet de récupérer l'ID auto-généré
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, category.getName());
        int rows = ps.executeUpdate();

        // Si l'insertion a réussi, récupérer l'ID généré
        if (rows > 0) {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                category.setId(keys.getInt(1)); // Mettre à jour l'objet category avec son nouvel ID
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
     * Récupère toutes les catégories
     * @return Une liste de toutes les catégories, triées par nom
     */
    public List<Category> getAllCategories() throws SQLException {
        String sql = "SELECT id, name FROM categories ORDER BY name ASC";
        List<Category> categories = new ArrayList<>();

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        // Parcourir tous les résultats
        while (rs.next()) {
            Category category = new Category();
            category.setId(rs.getInt("id"));
            category.setName(rs.getString("name"));
            categories.add(category); // Ajouter la catégorie à la liste
        }

        rs.close();
        ps.close();
        conn.close();

        return categories;
    }

    /**
     * Récupère une catégorie par son ID
     * @param id L'ID de la catégorie
     * @return La catégorie trouvée, null si aucune catégorie trouvée
     */
    public Category getCategoryById(int id) throws SQLException {
        String sql = "SELECT id, name FROM categories WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        Category category = null;

        if (rs.next()) {
            category = new Category();
            category.setId(rs.getInt("id"));
            category.setName(rs.getString("name"));
        }

        rs.close();
        ps.close();
        conn.close();

        return category;
    }

    /**
     * Met à jour une catégorie existante
     * @param category La catégorie avec les nouvelles informations
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean updateCategory(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, category.getName());
        ps.setInt(2, category.getId());

        int result = ps.executeUpdate();

        ps.close();
        conn.close();

        return result > 0; // Retourne true si au moins 1 ligne a été modifiée
    }

    /**
     * Supprime une catégorie par son ID
     * @param id L'ID de la catégorie à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        int result = ps.executeUpdate();

        ps.close();
        conn.close();

        return result > 0; // Retourne true si au moins 1 ligne a été supprimée
    }
}
