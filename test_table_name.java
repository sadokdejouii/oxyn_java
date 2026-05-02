import java.sql.*;

public class test_table_name {
    public static void main(String[] args) {
        try {
            Connection conn = org.example.utils.MyDataBase.getConnection();
            
            // Tester equipments (avec 's' anglais)
            try {
                ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM equipments");
                if (rs.next()) {
                    System.out.println("✅ Table 'equipments' existe, " + rs.getInt(1) + " enregistrements");
                }
            } catch (SQLException e) {
                System.out.println("❌ Table 'equipments' n'existe pas: " + e.getMessage());
            }
            
            // Tester equipements (avec 's' français)
            try {
                ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM equipements");
                if (rs.next()) {
                    System.out.println("✅ Table 'equipements' existe, " + rs.getInt(1) + " enregistrements");
                }
            } catch (SQLException e) {
                System.out.println("❌ Table 'equipements' n'existe pas: " + e.getMessage());
            }
            
            // Tester equipment (sans 's')
            try {
                ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM equipment");
                if (rs.next()) {
                    System.out.println("✅ Table 'equipment' existe, " + rs.getInt(1) + " enregistrements");
                }
            } catch (SQLException e) {
                System.out.println("❌ Table 'equipment' n'existe pas: " + e.getMessage());
            }
            
            conn.close();
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
}
