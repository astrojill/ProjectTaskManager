package fr.ece.model;

import java.time.LocalDateTime;

public class User {

    public enum Role {
        USER,
        ADMIN
    }

    private int id;                 // identifiant unique en BDD
    private String username;        // nom d'utilisateur pour la connexion
    private String passwordHash;    // mot de passe déjà hashé avec BCrypt
    private Role role;              // USER ou ADMIN

    // timestamps BDD
    private LocalDateTime createdAt;   // correspond à created_at
    private LocalDateTime updatedAt;   // correspond à updated_at

    public User() {
    }

    public User(int id, String username, String passwordHash, Role role,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // getter et setter

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role; // USER ou ADMIN
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
        return "User { id=" + id +
                ", username='" + username + "'" +
                ", role=" + role +
                " }";
    }
}