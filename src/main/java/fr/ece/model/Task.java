package fr.ece.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Classe Task
public class Task {

    // Attributs principaux (liés à la BDD)
    private int id;                // id AUTO_INCREMENT
    private String title;          // titre de la tâche
    private String description;    // description détaillée
    private LocalDate dueDate;     // date limite (colonne due_date)

    // status : TODO / IN_PROGRESS / DONE / CANCELLED
    private String status;

    // priority : LOW / MEDIUM / HIGH
    private String priority;

    // Relation avec les autres tables
    private Category category;     // correspond à category_id (peut être null)
    private User user;             // correspond à user_id

    // Champs de suivi (timestamps BDD)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // Constructeur vide
    public Task() {
    }

    // Constructeur complet avec id (pour les objets venant de la BDD)
    public Task(int id,
                String title,
                String description,
                LocalDate dueDate,
                String status,
                String priority,
                Category category,
                User user,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.user = user;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructeur sans id ni timestamps (pour créer une nouvelle tâche)
    public Task(String title,
                String description,
                LocalDate dueDate,
                String status,
                String priority,
                Category category,
                User user) {

        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.user = user;
    }

    // Getters & setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    // on pourra vérifier plus tard que la valeur est bien dans l'ENUM
    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category; // peut être null (si la tâche n’a pas de catégorie)
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user; // en BDD, user_id est NOT NULL
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
        return "Task { id=" + id +
                ", title='" + title + "'" +
                ", status='" + status + "'" +
                ", priority='" + priority + "'" +
                " }";
    }

}
