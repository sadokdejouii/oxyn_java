package org.example.services;

import org.example.model.planning.admin.AdminPlanningModuleStats;
import org.example.model.planning.admin.AdminPlanningUserRow;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture agrégée pour le dashboard admin du module Planning (compteurs + liste synthétique).
 */
public final class AdminPlanningDashboardService {

    public AdminPlanningModuleStats loadModuleStats() throws SQLException {
        Connection c = conn();
        if (c == null) {
            return new AdminPlanningModuleStats(0, 0, 0, 0, 0);
        }
        int fiches = count(c, "SELECT COUNT(*) FROM fiche_sante");
        int progs = count(c, "SELECT COUNT(*) FROM programmes_generes");
        int taches = count(c, "SELECT COUNT(*) FROM taches_quotidiennes");
        int conv = count(c, "SELECT COUNT(*) FROM conversations");
        int msgs = count(c, "SELECT COUNT(*) FROM messages");
        return new AdminPlanningModuleStats(fiches, progs, taches, conv, msgs);
    }

    /**
     * Utilisateurs disposant d’une fiche santé (données fiche + stats planning).
     */
    public List<AdminPlanningUserRow> listUsersWithPlanningActivity() throws SQLException {
        Connection c = conn();
        if (c == null) {
            return List.of();
        }
        String sql = """
                SELECT u.id_user, u.first_name_user, u.last_name_user, u.email_user, u.roles_user,
                       f.genre, f.age, f.poids, f.objectif, f.niveau_activite,
                       (SELECT COUNT(*) FROM programmes_generes p WHERE p.user_id = u.id_user) AS nb_prog,
                       (SELECT COUNT(*) FROM taches_quotidiennes t WHERE t.user_id = u.id_user) AS nb_tache
                FROM users u
                INNER JOIN fiche_sante f ON f.user_id = u.id_user
                ORDER BY u.last_name_user ASC, u.first_name_user ASC, u.id_user ASC
                LIMIT 500
                """;
        List<AdminPlanningUserRow> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fn = rs.getString("first_name_user");
                String ln = rs.getString("last_name_user");
                String name = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                if (name.isEmpty()) {
                    name = "—";
                }
                String roles = rs.getString("roles_user");
                String roleShort = summarizeRole(roles);
                int np = rs.getInt("nb_prog");
                int nt = rs.getInt("nb_tache");
                Integer age = (Integer) rs.getObject("age");
                Double poids = rs.getObject("poids") != null ? rs.getDouble("poids") : null;
                list.add(new AdminPlanningUserRow(
                        rs.getInt("id_user"),
                        name,
                        rs.getString("email_user") != null ? rs.getString("email_user") : "",
                        roleShort,
                        rs.getString("genre"),
                        age,
                        poids,
                        rs.getString("objectif"),
                        rs.getString("niveau_activite"),
                        nt,
                        np > 0
                ));
            }
        }
        return list;
    }

    private static String summarizeRole(String rolesJson) {
        if (rolesJson == null || rolesJson.isBlank()) {
            return "—";
        }
        if (rolesJson.contains("ROLE_ADMIN")) {
            return "Admin";
        }
        if (rolesJson.contains("ROLE_ENCADRANT")) {
            return "Encadrant";
        }
        if (rolesJson.contains("ROLE_USER") || rolesJson.contains("ROLE_CLIENT")) {
            return "Client";
        }
        return "Utilisateur";
    }

    private static int count(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static Connection conn() throws SQLException {
        Connection c = MyDataBase.getInstance().getConnection();
        if (c == null || c.isClosed()) {
            return null;
        }
        return c;
    }
}
