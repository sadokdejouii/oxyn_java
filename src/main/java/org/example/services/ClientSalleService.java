package org.example.services;

import org.example.entities.Salle;
import org.example.entities.SubscriptionOffer;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture salles / offres / sessions côté client. Connexion fraîche par appel ; requêtes compatibles
 * {@code ONLY_FULL_GROUP_BY} (sous-requête agrégée pour les notes).
 */
public class ClientSalleService {

    private static Connection open() throws SQLException {
        Connection c = MyDataBase.requireConnection();
        c.setAutoCommit(true);
        return c;
    }

    /** Salles actives avec moyenne d’avis (sous-requête, pas de {@code GROUP BY g.id} + {@code SELECT g.*}). */
    public List<Salle> getSallesActives() throws SQLException {
        List<Salle> list = new ArrayList<>();
        String sql = "SELECT g.id, g.name, g.description, g.address, g.phone, g.email, g.image_url, "
                + "g.google_maps_url, g.youtube_url, g.rating, g.rating_count, g.is_active, g.created_at, g.updated_at, "
                + "COALESCE(agg.avg_rating, 0) AS avg_rating, "
                + "COALESCE(agg.total_ratings, 0) AS total_ratings "
                + "FROM gymnasia g "
                + "LEFT JOIN ("
                + "  SELECT gymnasium_id, AVG(rating) AS avg_rating, COUNT(id) AS total_ratings "
                + "  FROM gym_ratings "
                + "  GROUP BY gymnasium_id"
                + ") agg ON agg.gymnasium_id = g.id "
                + "WHERE g.is_active = 1 "
                + "ORDER BY g.name";
        try (Connection con = open();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Salle s = new Salle();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setDescription(rs.getString("description"));
                s.setAddress(rs.getString("address"));
                s.setPhone(rs.getString("phone"));
                s.setEmail(rs.getString("email"));
                s.setImageUrl(rs.getString("image_url"));
                s.setGoogleMapsUrl(rs.getString("google_maps_url"));
                s.setYoutubeUrl(rs.getString("youtube_url"));
                s.setRating(rs.getDouble("avg_rating"));
                s.setRatingCount(rs.getInt("total_ratings"));
                s.setActive(rs.getInt("is_active") == 1);
                s.setCreatedAt(rs.getTimestamp("created_at"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(s);
            }
        }
        return list;
    }

    public List<SubscriptionOffer> getOffres(int gymnasiumId) throws SQLException {
        List<SubscriptionOffer> list = new ArrayList<>();
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM gym_subscription_offers WHERE gymnasium_id = ? AND is_active = 1")) {
            ps.setInt(1, gymnasiumId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SubscriptionOffer o = new SubscriptionOffer();
                    o.setId(rs.getInt("id"));
                    o.setName(rs.getString("name"));
                    o.setDurationMonths(rs.getInt("duration_months"));
                    o.setPrice(rs.getDouble("price"));
                    o.setDescription(rs.getString("description"));
                    o.setActive(true);
                    o.setGymnasiumId(gymnasiumId);
                    list.add(o);
                }
            }
        }
        return list;
    }

    public List<org.example.entities.Session> getSessions(int gymnasiumId) throws SQLException {
        List<org.example.entities.Session> list = new ArrayList<>();
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM training_sessions WHERE gymnasium_id = ? AND is_active = 1 AND start_at > NOW() ORDER BY start_at")) {
            ps.setInt(1, gymnasiumId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    org.example.entities.Session s = new org.example.entities.Session();
                    s.setId(rs.getInt("id"));
                    s.setTitle(rs.getString("title"));
                    s.setDescription(rs.getString("description"));
                    Timestamp start = rs.getTimestamp("start_at");
                    if (start != null) {
                        s.setStartAt(start.toLocalDateTime());
                    }
                    Timestamp end = rs.getTimestamp("end_at");
                    if (end != null) {
                        s.setEndAt(end.toLocalDateTime());
                    }
                    s.setCapacity(rs.getInt("capacity"));
                    s.setPrice(rs.getDouble("price"));
                    s.setActive(true);
                    s.setGymnasiumId(gymnasiumId);
                    s.setPlacesRestantes(s.getCapacity());
                    list.add(s);
                }
            }
        }
        return list;
    }

    public Salle getById(int id) throws SQLException {
        String sql = "SELECT g.id, g.name, g.description, g.address, g.phone, g.email, g.image_url, "
                + "g.google_maps_url, g.youtube_url, g.rating, g.rating_count, g.is_active, g.created_at, g.updated_at, "
                + "COALESCE(agg.avg_rating, 0) AS avg_rating, "
                + "COALESCE(agg.total_ratings, 0) AS total_ratings "
                + "FROM gymnasia g "
                + "LEFT JOIN ("
                + "  SELECT gymnasium_id, AVG(rating) AS avg_rating, COUNT(id) AS total_ratings "
                + "  FROM gym_ratings "
                + "  GROUP BY gymnasium_id"
                + ") agg ON agg.gymnasium_id = g.id "
                + "WHERE g.id = ? AND g.is_active = 1";
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Salle s = new Salle();
                    s.setId(rs.getInt("id"));
                    s.setName(rs.getString("name"));
                    s.setDescription(rs.getString("description"));
                    s.setAddress(rs.getString("address"));
                    s.setPhone(rs.getString("phone"));
                    s.setEmail(rs.getString("email"));
                    s.setImageUrl(rs.getString("image_url"));
                    s.setYoutubeUrl(rs.getString("youtube_url"));
                    s.setGoogleMapsUrl(rs.getString("google_maps_url"));
                    s.setRating(rs.getDouble("avg_rating"));
                    s.setRatingCount(rs.getInt("total_ratings"));
                    s.setActive(rs.getInt("is_active") == 1);
                    s.setCreatedAt(rs.getTimestamp("created_at"));
                    s.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return s;
                }
            }
        }
        return null;
    }
}
