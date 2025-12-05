package fr.ece.util;

import java.security.MessageDigest;

public class PasswordUtils {

    // Méthode qui prend un mot de passe en clair et renvoie un hash
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256"); // on choisit l'algorithme
            byte[] hashBytes = md.digest(password.getBytes());       // on calcule le hash

            // On convertit les bytes en string hexadécimale (genre "a3b4c...")
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString(); // on renvoie le hash final
        } 
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Vérifie si le mot de passe entré correspond au hash stocké
    public static boolean verifyPassword(String password, String storedHash) {
        String hashedInput = hashPassword(password); // on hash ce que l'utilisateur a tapé
        return hashedInput.equals(storedHash);       // on compare avec le hash en BDD
    }
}

