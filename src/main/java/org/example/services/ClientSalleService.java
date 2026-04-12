package org.example.services;

import org.example.entities.Salle;
import org.example.entities.SubscriptionOffer;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClientSalleService {

    private final Connection con;

    public ClientSalleService() {
        con = MyDataBase.getInstance().getConnection();
    }

    /** All active gymnasia */
    public List<Salle> getSallesActives() throws SQLException {
        List<Salle> list = new ArrayList<>();
        ResultSet rs = con.createStatement().executeQuery(
            "SELECT * FROM gymnasia WHERE is_active = 1 ORDER BY name");
        while (rs.next()) {
            Salle s = new Salle();
            s.setId(rs.getInt("id"));
            s.setName(rs.getString("name"));
            s.setDescription(rs.getString("description"));
            s.setAddress(rs.getString("address"));
            s.setPhone(rs.getString("phone"));
            s.setEmail(rs.getString("email"));
            s.setImageUrl(rs.getString("image_url"));
            s.setRating(rs.getDouble("rating"));
            s.setRatingCount(rs.getInt("rating_count"));
            s.setActive(true);
            list.add(s);
        }
        return list;
    }

    /** Active subscription offers for a gymnasium */
    public List<SubscriptionOffer> getOffres(int gymnasiumId) throws SQLException {
        List<SubscriptionOffer> list = new ArrayList<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT * FROM gym_subscription_offers WHERE gymnasium_id = ? AND is_active = 1");
        ps.setInt(1, gymnasiumId);
        ResultSet rs = ps.executeQuery();
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
        return list;
    }

    /** Upcoming active sessions for a gymnasium */
    public List<org.example.entities.Session> getSessions(int gymnasiumId) throws SQLException {
        List<org.example.entities.Session> list = new ArrayList<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT * FROM training_sessions WHERE gymnasium_id = ? AND is_active = 1 AND start_at > NOW() ORDER BY start_at");
        ps.setInt(1, gymnasiumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            org.example.entities.Session s = new org.example.entities.Session();
            s.setId(rs.getInt("id"));
            s.setTitle(rs.getString("title"));
            s.setDescription(rs.getString("description"));
            Timestamp start = rs.getTimestamp("start_at");
            if (start != null) s.setStartAt(start.toLocalDateTime());
            Timestamp end = rs.getTimestamp("end_at");
            if (end != null) s.setEndAt(end.toLocalDateTime());
            s.setCapacity(rs.getInt("capacity"));
            s.setPrice(rs.getDouble("price"));
            s.setActive(true);
            s.setGymnasiumId(gymnasiumId);
            s.setPlacesRestantes(s.getCapacity());
            list.add(s);
        }
        return list;
    }
}
