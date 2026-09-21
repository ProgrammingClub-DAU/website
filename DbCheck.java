import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cpclub_db", "cpclub_user", "cpclub_password");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, email, is_platform_creator FROM users WHERE lower(name) LIKE '%madhav%'");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | Name: " + rs.getString("name") + " | Email: " + rs.getString("email") + " | is_platform_creator: " + rs.getBoolean("is_platform_creator"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
