import java.sql.*;

public class ViewDatabase {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:hostel.db";
        String[] tables = {"rooms", "students", "allocation_queue", "gate_register", "attendance", "leave_requests", "complaints", "visitors"};

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            System.out.println("==================================================");
            System.out.println("          HOSTEL.DB DATABASE CONTENTS             ");
            System.out.println("==================================================");

            for (String table : tables) {
                System.out.println("\n--- TABLE: " + table.toUpperCase() + " ---");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();

                    // Header
                    for (int i = 1; i <= cols; i++) {
                        System.out.printf("%-20s ", md.getColumnName(i));
                    }
                    System.out.println("\n" + "-".repeat(cols * 22));

                    int count = 0;
                    while (rs.next()) {
                        count++;
                        for (int i = 1; i <= cols; i++) {
                            String val = rs.getString(i);
                            System.out.printf("%-20s ", val != null ? val : "NULL");
                        }
                        System.out.println();
                    }
                    if (count == 0) {
                        System.out.println("(Empty table)");
                    }
                } catch (SQLException e) {
                    System.out.println("Error reading table: " + e.getMessage());
                }
            }
            System.out.println("\n==================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
