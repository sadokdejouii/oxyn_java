package org.example.dao;

import org.example.entities.Admin;
import org.example.entities.Client;
import org.example.entities.Coach;
import org.example.entities.User;
import org.example.utils.MyDataBase;
import org.example.utils.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Accès aux données utilisateurs (table {@code users}). JDBC + PreparedStatement.
 * La fabrique {@link #createUserFromRolesJson(String, int, String, String, String, String, String, boolean)}
 * instancie le sous-type selon la chaîne JSON {@code roles_user} (détection par {@code contains}).
 */
public class UserDAO {

    private static final String COL_SELECT =
            "id_user, email_user, password_user, roles_user, first_name_user, last_name_user, phone_user, is_active_user";

    private final Connection con;

    public UserDAO() {
        this.con = MyDataBase.getConnection();
    }

    /**
     * Fabrique polymorphe : lit {@code roles_user} comme JSON texte et retourne Admin / Client / Coach.
     * Priorité si plusieurs rôles présents : ADMIN &gt; ENCADRANT &gt; CLIENT.
     */
    public static User createUserFromRolesJson(String rolesJson, int id, String email, String password,
                                               String nom, String prenom, String telephone, boolean isActive)
            throws SQLException {
        String json = rolesJson != null ? rolesJson : "";
        if (json.contains("ROLE_ADMIN")) {
            return new Admin(id, email, password, nom, prenom, telephone, isActive);
        }
        if (json.contains("ROLE_ENCADRANT")) {
            return new Coach(id, email, password, nom, prenom, telephone, isActive);
        }
        if (json.contains("ROLE_CLIENT")) {
            return new Client(id, email, password, nom, prenom, telephone, isActive);
        }
        throw new SQLException("Rôle inconnu dans roles_user : " + json);
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_user");
        String email = rs.getString("email_user");
        String password = rs.getString("password_user");
        String rolesJson = rs.getString("roles_user");
        String nom = rs.getString("first_name_user");
        String prenom = rs.getString("last_name_user");
        String telephone = rs.getString("phone_user");
        boolean active = rs.getBoolean("is_active_user");
        return createUserFromRolesJson(rolesJson, id, email, password, nom, prenom, telephone, active);
    }

    private static void bindUserColumns(PreparedStatement ps, User user, int startIndex) throws SQLException {
        ps.setString(startIndex, user.getEmail());
        ps.setString(startIndex + 1, user.getPassword());
        ps.setString(startIndex + 2, user.getRolesJsonValue());
        ps.setString(startIndex + 3, user.getNom());
        ps.setString(startIndex + 4, user.getPrenom());
        if (user.getTelephone() != null) {
            ps.setString(startIndex + 5, user.getTelephone());
        } else {
            ps.setNull(startIndex + 5, Types.VARCHAR);
        }
        ps.setBoolean(startIndex + 6, user.isActive());
    }

    public void addUser(User user) throws SQLException {
        String sql = """
                INSERT INTO users (email_user, password_user, roles_user, first_name_user, last_name_user, phone_user, is_active_user)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUserColumns(ps, user, 1);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
    }

    public void updateUser(User user) throws SQLException {
        String sql = """
                UPDATE users SET
                    email_user = ?,
                    password_user = ?,
                    roles_user = ?,
                    first_name_user = ?,
                    last_name_user = ?,
                    phone_user = ?,
                    is_active_user = ?
                WHERE id_user = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindUserColumns(ps, user, 1);
            ps.setInt(8, user.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Aucun utilisateur avec id_user = " + user.getId());
            }
        }
    }

    /**
     * Mise à jour partielle du profil : nom, prénom, téléphone uniquement (e-mail, mot de passe, rôle, statut inchangés).
     */
    public void updateProfilePartial(int userId, String nom, String prenom, String telephone) throws SQLException {
        String sql = """
                UPDATE users SET
                    first_name_user = ?,
                    last_name_user = ?,
                    phone_user = ?
                WHERE id_user = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            if (telephone != null) {
                ps.setString(3, telephone);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setInt(4, userId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Aucun utilisateur avec id_user = " + userId);
            }
        }
    }

    /**
     * Supprime les lignes des tables qui référencent {@code users.id_user} (ex. {@code programmes_generes}),
     * en s'appuyant sur {@code information_schema} pour ne pas coder en dur les noms de colonnes.
     * Nécessite {@code SET FOREIGN_KEY_CHECKS=0} le temps de la suppression en chaîne (intégrité assurée par la transaction).
     */
    private void deleteRowsReferencingUser(int userId) throws SQLException {
        String listSql = """
                SELECT DISTINCT TABLE_NAME, COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND REFERENCED_TABLE_SCHEMA = DATABASE()
                  AND REFERENCED_TABLE_NAME = 'users'
                  AND REFERENCED_COLUMN_NAME = 'id_user'
                  AND TABLE_NAME <> 'users'
                """;
        Set<String> seen = new LinkedHashSet<>();
        List<String[]> tablesColumns = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(listSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                String column = rs.getString("COLUMN_NAME");
                String key = table + "\0" + column;
                if (seen.add(key)) {
                    tablesColumns.add(new String[]{table, column});
                }
            }
        }
        try (Statement st = con.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=0");
        }
        try {
            for (String[] tc : tablesColumns) {
                String table = tc[0];
                String column = tc[1];
                String del = "DELETE FROM `" + table.replace("`", "``") + "` WHERE `" + column.replace("`", "``") + "` = ?";
                try (PreparedStatement ps = con.prepareStatement(del)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
            }
        } finally {
            try (Statement st = con.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        }
    }

    public void deleteUser(int id) throws SQLException {
        boolean prevAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);
            deleteRowsReferencingUser(id);
            String sql = "DELETE FROM users WHERE id_user = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Aucun utilisateur avec id_user = " + id);
                }
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(prevAutoCommit);
        }
    }

    public User getUserById(int id) throws SQLException {
        String sql = "SELECT " + COL_SELECT + " FROM users WHERE id_user = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT " + COL_SELECT + " FROM users ORDER BY id_user";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Recherche par e-mail (insensible à la casse).
     */
    public User findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT " + COL_SELECT + " FROM users WHERE LOWER(TRIM(email_user)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Authentification : fabrique polymorphe via {@link #mapRow(ResultSet)} après contrôle du mot de passe BCrypt
     * et du statut actif.
     *
     * @return {@code null} si identifiants invalides, compte inactif ou e-mail inconnu (pas d'exception métier).
     */
    public User login(String email, String plainPassword) throws SQLException {
        if (plainPassword == null || email == null || email.isBlank()) {
            return null;
        }
        User user = findByEmail(email.trim());
        if (user == null) {
            return null;
        }
        if (!user.isActive()) {
            return null;
        }
        if (!PasswordUtils.matches(plainPassword, user.getPassword())) {
            return null;
        }
        return user;
    }

    /**
     * Inscription : uniquement un {@link Client}, mot de passe déjà hashé. Retourne {@code false} si l'e-mail existe déjà.
     */
    public boolean register(User user) throws SQLException {
        if (!(user instanceof Client)) {
            throw new SQLException("L'inscription ne concerne que les comptes client.");
        }
        if (findByEmail(user.getEmail()) != null) {
            return false;
        }
        addUser(user);
        return true;
    }
}
