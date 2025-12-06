package fr.ece.model;

import java.time.LocalDateTime;

public class Category {

    private int id;                // identifiant unique (AUTO_INCREMENT dans la BDD)
    private String name;           // nom de la catégorie (doit être unique)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Category() {
    }

    // Constructeur pratique quand on crée une nouvelle catégorie
    public Category(String name) {
        this.name = name;
    }

    // Constructeur complet
    public Category(int id, String name,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // getters et setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        // la BDD le génère tout seule (AUTO_INCREMENT), donc on fait juste un set ici
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String toString() {
        return name;
    }
}
