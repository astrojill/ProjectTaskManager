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

public class RegisterController {

    // éléments fxml
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox termsCheckbox;
    @FXML private Label messageLabel;
    @FXML private Label usernameHint;
    @FXML private Label passwordStrength;

    @FXML private RadioButton userRoleRadio;
    @FXML private RadioButton adminRoleRadio;
    @FXML private javafx.scene.layout.VBox roleContainer;

    private UserDAO userDAO;

    // initialisation
    @FXML
    public void initialize() {
        userDAO = new UserDAO();

        // Cacher le message
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        // Vérifier si c'est le premier utilisateur
        checkFirstUser();

        // Validation en temps réel du mot de passe
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });

        // Validation du nom d'utilisateur
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateUsername(newVal);
        });
    }

    // méthode handleRegister
    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation globale
        if (!validateForm(username, password, confirmPassword)) {
            return;
        }

        try {
            // Vérifier si le nom d'utilisateur existe déjà
            User existingUser = userDAO.getUserByUsername(username);
            if (existingUser != null) {
                showError("Ce nom d'utilisateur est déjà pris");
                usernameField.requestFocus();
                return;
            }

            // Créer le nouvel utilisateur
            User newUser = new User();
            newUser.setUsername(username);

            // Hasher le mot de passe (BCrypt)
            String hashedPassword = PasswordUtils.hashPassword(password);
            newUser.setPasswordHash(hashedPassword);

            // Déterminer le rôle
            if (roleContainer.isVisible() && adminRoleRadio.isSelected()) {
                newUser.setRole(User.Role.ADMIN);
            } else {
                newUser.setRole(User.Role.USER);
            }

            // Sauvegarder dans la base de données
            boolean success = userDAO.createUser(newUser); // <-- ICI : createUser au lieu de save

            if (success) {
                showSuccess("Compte créé avec succès ! Redirection...");

                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(2)
                        );
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

    // méthode handleBackToLogin
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        redirectToLogin();
    }

    // validation

    private boolean validateForm(String username, String password, String confirmPassword) {
        if (!termsCheckbox.isSelected()) {
            showError("Vous devez accepter les conditions d'utilisation");
            return false;
        }

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

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrength.setText("Minimum 8 caractères");
            passwordStrength.setStyle("-fx-text-fill: #7f8c8d;");
            return;
        }

        int strength = 0;

        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*\\d.*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength++;

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

    private void checkFirstUser() {
        try {
            // si aucun user → premier compte = admin
            int userCount = userDAO.getAllUsers().size();

            if (userCount == 0) {
                roleContainer.setVisible(true);
                roleContainer.setManaged(true);
                adminRoleRadio.setSelected(true);
                adminRoleRadio.setDisable(true); // premier user forcé admin
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du premier utilisateur: " + e.getMessage());
        }
    }

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

    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-background-color: #d5f4e6; -fx-text-fill: #27ae60;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}