package fr.ece.controller;

import fr.ece.dao.UserDAO;
import fr.ece.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    private final UserDAO userDAO = new UserDAO();

    // éléments fxml
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    // on initialise
    @FXML
    public void initialize() {
        // Au début : le message d'erreur est caché
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // login : bouton + enter
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // on vérifie que les champs ne sont pas vides
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            showError("Veuillez remplir le nom d'utilisateur et le mot de passe.");
            return;
        }

        username = username.trim();

        try {
            // l’authentification de UserDAO
            // avec getUserByUsername + PasswordUtils.verifyPassword
            User user = userDAO.authenticateUser(username, password);

            if (user == null) {
                // soit utilisateur inconnu, soit mot de passe incorrect
                showError("Identifiants incorrects.");
                return;
            }

            // Connexion ok

            // Naviguer vers la page principale
            goToDashboard(event);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL pendant la connexion.");
        }
    }

    // lien inscription
    @FXML
    private void handleRegister(ActionEvent event) {

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir la page d'inscription.");
        }
    }

    //navigation vers dashboard
    private void goToDashboard(ActionEvent event) {
        // on adapte vers le bon fxml
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir le tableau de bord.");
        }
    }

    // affichage d'erreur
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
