package fr.ece.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Task {

    // Enum pour le statut (correspond à ENUM('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED'))
    public enum Status {
        TODO,
        IN_PROGRESS,
        DONE,
        CANCELLED
    }

    // Enum pour la priorité (ENUM('LOW','MEDIUM','HIGH'))
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    // ---- Champs liés à la BDD ----
    private int id;                     // id AUTO_INCREMENT
    private String title;               // titre de la tâche
    private String description;         // description détaillée
    private LocalDate dueDate;          // date d'échéance (due_date)
    private Status status;              // TODO / IN_PROGRESS / DONE / CANCELLED
    private Priority priority;          // LOW / MEDIUM / HIGH

    // Relations
    private Category category;          // catégorie liée (peut être null si category_id = null)
    private String categoryName;        // Nom de la catégorie (pour affichage)
    private User user;                  // utilisateur propriétaire (user_id NOT NULL)
    private String userName;            // Nom de l'utilisateur (pour affichage)

    // Timestamps
    private LocalDateTime createdAt;    // created_at
    private LocalDateTime updatedAt;    // updated_at

    public Task() {
    }

    // Constructeur pratique sans id/timestamps
    public Task(String title, String description, LocalDate dueDate,
                Status status, Priority priority,
                Category category, User user) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.user = user;
    }

    // Constructeur complet avec id + timestamps (quand ça vient de la BDD)
    public Task(int id, String title, String description, LocalDate dueDate,
                Status status, Priority priority,
                Category category, User user,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    // getters et setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        // c'est la BDD qui le met (AUTO_INCREMENT)
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusAsString() {
        return (status != null) ? status.name() : null;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getPriorityAsString() {
        return (priority != null) ? priority.name() : null;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Integer getCategoryId() {
        return (category != null) ? category.getId() : null;
    }

    public void setCategoryId(Integer categoryId) {
        if (categoryId != null) {
            if (this.category == null) {
                this.category = new Category();
            }
            this.category.setId(categoryId);
        } else {
            this.category = null;
        }
    }

    public String getCategoryName() {
        // Priorité 1 : utiliser categoryName si défini
        if (categoryName != null) {
            return categoryName;
        }
        // Priorité 2 : utiliser category.getName() si disponible
        if (category != null && category.getName() != null) {
            return category.getName();
        }
        // Par défaut
        return "Sans Catégorie";
    }


    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        // user_id NOT NULL → normalement toujours un user
        this.user = user;
    }

    public Integer getUserId() {
        return (user != null) ? user.getId() : null;
    }

    public void setUserId(int userId) {
        if (this.user == null) {
            this.user = new User();
        }
        this.user.setId(userId);
    }

    /**
     * Retourne le nom de l'utilisateur assigné à la tâche
     * Priorité : userName (si défini) > user.getUsername() > "Non assigné"
     */
    public String getUserName() {
        // Priorité 1 : utiliser userName si défini
        if (userName != null) {
            return userName;
        }
        // Priorité 2 : utiliser user.getUsername() si disponible
        if (user != null && user.getUsername() != null) {
            return user.getUsername();
        }
        // Par défaut
        return "Non assigné";
    }

    /**
     * Définit le nom de l'utilisateur (pour affichage dans les tables)
     */
    public void setUserName(String userName) {
        this.userName = userName;
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

    @Override
    public String toString() {
        return "Task { id=" + id +
                ", title='" + title + "'" +
                ", status=" + status +
                ", priority=" + priority +
                " }";
    }
}
