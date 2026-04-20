package org.example.services;

import org.example.entities.SubscriptionOffer;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionOfferService {
    private final Connection con;
    public SubscriptionOfferService() { con = MyDataBase.getInstance().getConnection(); }

    public void ajouter(SubscriptionOffer o) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
            "INSERT INTO gym_subscription_offers (gymnasium_id, name, duration_months, price, description, is_active, created_at) VALUES (?,?,?,?,?,1,NOW())",
            Statement.RETURN_GENERATED_KEYS);
        if (o.getGymnasiumId() != null) ps.setInt(1, o.getGymnasiumId()); else ps.setNull(1, Types.INTEGER);
        ps.setString(2, o.getName()); ps.setInt(3, o.getDurationMonths()); ps.setDouble(4, o.getPrice()); ps.setString(5, o.getDescription());
        ps.executeUpdate();
        ResultSet k = ps.getGeneratedKeys(); if (k.next()) o.setId(k.getInt(1));
    }

    public List<SubscriptionOffer> afficher() throws SQLException {
        List<SubscriptionOffer> list = new ArrayList<>();
        ResultSet rs = con.createStatement().executeQuery(
            "SELECT o.*, g.name AS gym_name FROM gym_subscription_offers o LEFT JOIN gymnasia g ON o.gymnasium_id=g.id ORDER BY o.created_at DESC");
        while (rs.next()) {
            SubscriptionOffer o = new SubscriptionOffer();
            o.setId(rs.getInt("id")); int gid = rs.getInt("gymnasium_id"); o.setGymnasiumId(rs.wasNull() ? null : gid);
            o.setName(rs.getString("name")); o.setDurationMonths(rs.getInt("duration_months")); o.setPrice(rs.getDouble("price"));
            o.setDescription(rs.getString("description")); o.setActive(rs.getInt("is_active") == 1);
            o.setCreatedAt(rs.getTimestamp("created_at")); o.setUpdatedAt(rs.getTimestamp("updated_at")); o.setGymnasiumName(rs.getString("gym_name"));
            list.add(o);
        }
        return list;
    }

    public List<SubscriptionOffer> afficherParSalle(int gymnasiumId) throws SQLException {
        List<SubscriptionOffer> list = new ArrayList<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT o.*, g.name AS gym_name FROM gym_subscription_offers o LEFT JOIN gymnasia g ON o.gymnasium_id=g.id " +
                "WHERE o.gymnasium_id=? AND o.is_active=1 ORDER BY o.created_at DESC");
        ps.setInt(1, gymnasiumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            SubscriptionOffer o = new SubscriptionOffer();
            o.setId(rs.getInt("id")); int gid = rs.getInt("gymnasium_id"); o.setGymnasiumId(rs.wasNull() ? null : gid);
            o.setName(rs.getString("name")); o.setDurationMonths(rs.getInt("duration_months")); o.setPrice(rs.getDouble("price"));
            o.setDescription(rs.getString("description")); o.setActive(rs.getInt("is_active") == 1);
            o.setCreatedAt(rs.getTimestamp("created_at")); o.setUpdatedAt(rs.getTimestamp("updated_at")); o.setGymnasiumName(rs.getString("gym_name"));
            list.add(o);
        }
        return list;
    }

    public int countParSalle(int gymnasiumId) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM gym_subscription_offers WHERE gymnasium_id=? AND is_active=1");
        ps.setInt(1, gymnasiumId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public void modifier(SubscriptionOffer o) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
            "UPDATE gym_subscription_offers SET gymnasium_id=?, name=?, duration_months=?, price=?, description=?, updated_at=NOW() WHERE id=?");
        if (o.getGymnasiumId() != null) ps.setInt(1, o.getGymnasiumId()); else ps.setNull(1, Types.INTEGER);
        ps.setString(2, o.getName()); ps.setInt(3, o.getDurationMonths()); ps.setDouble(4, o.getPrice()); ps.setString(5, o.getDescription()); ps.setInt(6, o.getId());
        ps.executeUpdate();
    }

    public void toggleActive(int id, boolean active) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE gym_subscription_offers SET is_active=?, updated_at=NOW() WHERE id=?");
        ps.setInt(1, active ? 1 : 0); ps.setInt(2, id); ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement("DELETE FROM gym_subscription_offers WHERE id=?");
        ps.setInt(1, id); ps.executeUpdate();
    }

    public List<String[]> getOrders(int offerId) throws SQLException {
        List<String[]> list = new ArrayList<>();
        PreparedStatement ps = con.prepareStatement(
            "SELECT id, user_id, quantity, unit_price, total_price, status, created_at FROM gym_subscription_orders WHERE offer_id=? ORDER BY created_at DESC");
        ps.setInt(1, offerId); ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(new String[]{
            String.valueOf(rs.getInt("id")), String.valueOf(rs.getInt("user_id")), String.valueOf(rs.getInt("quantity")),
            String.format("%.2f", rs.getDouble("unit_price")), String.format("%.2f", rs.getDouble("total_price")),
            rs.getString("status"), String.valueOf(rs.getTimestamp("created_at"))});
        return list;
    }
}
