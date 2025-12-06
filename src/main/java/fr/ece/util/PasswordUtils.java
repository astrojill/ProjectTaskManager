package fr.ece.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Génère un hash BCrypt à partir du mot de passe
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Vérifie si le mot de passe tapé correspond au hash stocké
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(password, hashedPassword);
    }
}