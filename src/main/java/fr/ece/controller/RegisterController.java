package fr.ece.controller;

import fr.ece.dao.UserDAO;
import fr.ece.model.User;
import fr.ece.util.PasswordUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur pour la page d'inscription.
 * Gère la création de comptes avec validation et sécurisation des mots de passe.
 */
public class RegisterController {

    // Composants FXML
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label usernameHint;
    @FXML private Label passwordStrength;
    @FXML private RadioButton userRoleRadio;
    @FXML private RadioButton adminRoleRadio;
    @FXML private javafx.scene.layout.VBox roleContainer;

    private UserDAO userDAO;

    /**
     * Initialisation du contrôleur.
     * Appelée automatiquement après le chargement du FXML.
     */
    @FXML
    public void initialize() {
        userDAO = new UserDAO();

        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        // Premier utilisateur = admin automatiquement
        checkFirstUser();

        // Validation en temps réel lors de la saisie
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateUsername(newVal);
        });
    }

    /**
     * Gère le clic sur le bouton d'inscription.
     */
    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Valider les champs
        if (!validateForm(username, password, confirmPassword)) {
            return;
        }

        try {
            // Vérifier si le username existe déjà
            User existingUser = userDAO.getUserByUsername(username);
            if (existingUser != null) {
                showError("Ce nom d'utilisateur est déjà pris");
                usernameField.requestFocus();
                return;
            }

            // Créer l'utilisateur
            User newUser = new User();
            newUser.setUsername(username);

            // Hasher le mot de passe avec BCrypt (sécurité)
            String hashedPassword = PasswordUtils.hashPassword(password);
            newUser.setPasswordHash(hashedPassword);

            // Attribuer le rôle
            if (roleContainer.isVisible() && adminRoleRadio.isSelected()) {
                newUser.setRole(User.Role.ADMIN);
            } else {
                newUser.setRole(User.Role.USER);
            }

            // Enregistrer en base
            boolean success = userDAO.createUser(newUser);

            if (success) {
                showSuccess("Compte créé avec succès ! Redirection...");

                // Pause de 2 secondes avant redirection (meilleure UX)
                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(e -> redirectToLogin());
                pause.play();
            } else {
                showError("Erreur lors de la création du compte");
            }

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retour à l'écran de connexion.
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        redirectToLogin();
    }

    /**
     * Valide le formulaire complet avant soumission.
     */
    private boolean validateForm(String username, String password, String confirmPassword) {

        if (username.isEmpty()) {
            showError("Le nom d'utilisateur est obligatoire");
            usernameField.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères");
            usernameField.requestFocus();
            return false;
        }

        // Regex : seulement lettres, chiffres et underscores
        if (!username.matches("[a-zA-Z0-9_]+")) {
            showError("Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores");
            usernameField.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            showError("Le mot de passe est obligatoire");
            passwordField.requestFocus();
            return false;
        }

        if (password.length() < 8) {
            showError("Le mot de passe doit contenir au moins 8 caractères");
            passwordField.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            confirmPasswordField.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Valide le username en temps réel et affiche un retour visuel.
     */
    private void validateUsername(String username) {
        if (username.isEmpty()) {
            usernameHint.setText("3 caractères minimum, lettres et chiffres uniquement");
            usernameHint.setStyle("-fx-text-fill: #7f8c8d;");
            return;
        }

        if (username.length() < 3) {
            usernameHint.setText("✗ Trop court (minimum 3 caractères)");
            usernameHint.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        if (!username.matches("[a-zA-Z0-9_]+")) {
            usernameHint.setText("✗ Caractères non autorisés");
            usernameHint.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        usernameHint.setText("✓ Nom d'utilisateur valide");
        usernameHint.setStyle("-fx-text-fill: #27ae60;");
    }

    /**
     * Calcule et affiche la force du mot de passe.
     * Score de 0 à 6 basé sur la longueur et la complexité.
     */
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrength.setText("Minimum 8 caractères");
            passwordStrength.setStyle("-fx-text-fill: #7f8c8d;");
            return;
        }

        int strength = 0;

        // Critères de force
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.matches(".*[A-Z].*")) strength++;      // Majuscule
        if (password.matches(".*[a-z].*")) strength++;      // Minuscule
        if (password.matches(".*\\d.*")) strength++;         // Chiffre
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength++; // Caractère spécial

        // Affichage selon le score
        switch (strength) {
            case 0:
            case 1:
                passwordStrength.setText("✗ Mot de passe faible");
                passwordStrength.setStyle("-fx-text-fill: #e74c3c;");
                break;
            case 2:
            case 3:
                passwordStrength.setText("⚠ Mot de passe moyen");
                passwordStrength.setStyle("-fx-text-fill: #f39c12;");
                break;
            case 4:
            case 5:
                passwordStrength.setText("✓ Mot de passe fort");
                passwordStrength.setStyle("-fx-text-fill: #27ae60;");
                break;
            case 6:
                passwordStrength.setText("✓ Mot de passe très fort");
                passwordStrength.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                break;
        }
    }

    /**
     * Vérifie si c'est le premier utilisateur.
     * Si oui, le rôle admin est automatiquement sélectionné et forcé.
     */
    private void checkFirstUser() {
        try {
            int userCount = userDAO.getAllUsers().size();

            if (userCount == 0) {
                // Afficher les options de rôle
                roleContainer.setVisible(true);
                roleContainer.setManaged(true);
                adminRoleRadio.setSelected(true);
                adminRoleRadio.setDisable(true); // Forcer admin pour le premier user
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du premier utilisateur: " + e.getMessage());
        }
    }

    /**
     * Redirige vers la page de connexion.
     */
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Task Manager - Connexion");

        } catch (IOException e) {
            showError("Erreur lors du retour à la connexion");
        }
    }

    /**
     * Affiche un message d'erreur en rouge.
     */
    private void showError(String message) {
        messageLabel.setText("✖ " + message);
        messageLabel.setStyle("-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    /**
     * Affiche un message de succès en vert.
     */
    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-background-color: #d5f4e6; -fx-text-fill: #27ae60;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}