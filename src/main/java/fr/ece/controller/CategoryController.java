package fr.ece.controller;

import fr.ece.dao;

// (ce sera appelé par l'interface graphique plus tard)
public class CategoryController {

    // On réutilise le DAO
    private CategoryDAO categoryDAO = new CategoryDAO();

    // Récupérer toutes les catégories
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    // Créer une nouvelle catégorie à partir d'un nom
    public boolean createCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            // nom vide → on refuse
            return false;
        }

        Category category = new Category(name.trim());
        categoryDAO.insert(category);   // on enregistre en BDD
        return true;
    }

    // Renommer une catégorie
    public boolean renameCategory(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }

        // On récupère la catégorie depuis la BDD
        Category existing = categoryDAO.findById(id);
        if (existing == null) {
            return false; // catégorie inexistante
        }

        existing.setName(newName.trim());
        categoryDAO.update(existing);   // on sauvegarde la modif
        return true;
    }

    // Supprimer une catégorie par son id
    public boolean deleteCategory(int id) {
        // On peut vérifier qu'elle existe
        Category existing = categoryDAO.findById(id);
        if (existing == null) {
            return false;
        }

        categoryDAO.delete(id);
        return true;
    }
}

