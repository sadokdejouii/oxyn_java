package org.example.services;

import org.example.dao.UserDAO;
import org.example.entities.AuthUser;
import org.example.entities.Client;
import org.example.entities.User;
import org.example.utils.MyDataBase;
import org.example.utils.PasswordUtils;
import org.example.windowshello.WindowsHelloBridge;
import org.example.windowshello.WindowsHelloLinkDAO;
import org.example.windowshello.WindowsHelloResult;
import org.example.facerec.FaceEmbeddingCodec;
import org.example.facerec.FaceEmbeddingDAO;
import org.example.facerec.FaceSimilarity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Authentification : voie DAO polymorphe + inscription ; option lecture table {@code users} (Symfony).
 */
public final class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final WindowsHelloLinkDAO windowsHelloLinkDAO = new WindowsHelloLinkDAO();
    private final FaceEmbeddingDAO faceEmbeddingDAO = new FaceEmbeddingDAO();

    public User login(String email, String password) throws SQLException {
        return userDAO.login(email, password);
    }

    /**
     * Connexion via Windows Hello (empreinte/PIN Windows) : pas de mot de passe applicatif.
     * Pré-requis : l'utilisateur a activé Windows Hello depuis son profil, ce qui enregistre son SID Windows.
     *
     * @return utilisateur connecté si Hello est vérifié + SID correspond ; sinon {@code null}
     */
    public User loginWithWindowsHello(String email) throws SQLException {
        WindowsHelloResult r = WindowsHelloBridge.verify("Connexion à OXYN");
        if (r == null || !r.ok() || r.sid() == null || r.sid().isBlank()) {
            return null;
        }
        return windowsHelloLinkDAO
                .findUserEligibleForHelloLogin(email, r.sid())
                .orElse(null);
    }

    /**
     * Connexion par reconnaissance faciale (comparaison locale).
     *
     * @param email email saisi
     * @param probeEmbedding embedding 128D (float[128]) capturé localement
     */
    public User loginWithFaceEmbedding(String email, float[] probeEmbedding) throws SQLException {
        if (email == null || email.isBlank() || probeEmbedding == null || probeEmbedding.length != FaceEmbeddingCodec.DIM) {
            return null;
        }
        User u = userDAO.findByEmail(email.trim());
        if (u == null || !u.isActive()) {
            return null;
        }
        var rec = faceEmbeddingDAO.getByUserId(u.getId()).orElse(null);
        if (rec == null || !rec.enabled() || rec.embedding() == null) {
            return null;
        }
        float[] ref = FaceEmbeddingCodec.fromBytes(rec.embedding());

        // Seuil simple cosine distance ; typiquement ~0.30–0.45 selon modèle / qualité
        double d = FaceSimilarity.cosineDistance(ref, probeEmbedding);
        return d <= 0.12 ? u : null;
    }

    /**
     * Retourne la distance (si un visage est enregistré) pour diagnostic UX.
     */
    public Double faceDistanceForEmail(String email, float[] probeEmbedding) throws SQLException {
        if (email == null || email.isBlank() || probeEmbedding == null || probeEmbedding.length != FaceEmbeddingCodec.DIM) {
            return null;
        }
        User u = userDAO.findByEmail(email.trim());
        if (u == null || !u.isActive()) {
            return null;
        }
        var rec = faceEmbeddingDAO.getByUserId(u.getId()).orElse(null);
        if (rec == null || !rec.enabled() || rec.embedding() == null) {
            return null;
        }
        float[] ref = FaceEmbeddingCodec.fromBytes(rec.embedding());
        return FaceSimilarity.cosineDistance(ref, probeEmbedding);
    }

    /**
     * Authentification complète (mot de passe) sur la table {@code users} (hash bcrypt / PHP {@code $2y$}).
     */
    public Optional<AuthUser> authenticateSymfonyUser(String emailInput, String plainPassword) throws SQLException {
        if (emailInput == null || emailInput.isBlank()) {
            return Optional.empty();
        }
        String email = emailInput.trim();
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id_user, email_user, password_user, roles_user, first_name_user, last_name_user
                FROM users
                WHERE email_user = ? AND is_active_user = 1
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                int id = rs.getInt("id_user");
                String hash = rs.getString("password_user");
                String rolesJson = rs.getString("roles_user");
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                if (plainPassword == null || !PasswordUtils.matches(plainPassword, hash)) {
                    return Optional.empty();
                }
                return Optional.of(new AuthUser(id, email, fn, ln, UserRole.fromSymfonyRolesJson(rolesJson)));
            }
        }
    }

    /**
     * Inscription d'un client (rôle imposé côté entité {@link Client}).
     *
     * @return {@code true} si l'insertion a réussi
     */
    public boolean registerClient(String nom, String prenom, String email, String telephone,
                                  String plainPassword, String confirmPassword) throws SQLException {
        String err = AuthValidation.validateRegistrationForm(nom, prenom, email, telephone, plainPassword, confirmPassword);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        String em = email.trim().toLowerCase();
        if (userDAO.findByEmail(em) != null) {
            return false;
        }
        String hash = PasswordUtils.hash(plainPassword);
        Client client = new Client(0, em, hash, nom.trim(), prenom.trim(), telephone.trim(), true);
        return userDAO.register(client);
    }
}
