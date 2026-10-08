import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:hostel.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public void initDatabase(List<Room> roomList, HostelDataStructures ds) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "room_num TEXT PRIMARY KEY, capacity INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "id TEXT PRIMARY KEY, name TEXT, room_num TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS allocation_queue (" +
                    "queue_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id TEXT, student_name TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS gate_register (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, student_name TEXT, room_num TEXT, direction TEXT, reason TEXT, entry_time TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS attendance (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, student_name TEXT, room_num TEXT, status TEXT, record_date TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS leave_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, student_name TEXT, from_date TEXT, to_date TEXT, reason TEXT, status TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS complaints (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, student_name TEXT, room_num TEXT, category TEXT, description TEXT, status TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS visitors (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, visitor_name TEXT, student_name TEXT, purpose TEXT, id_ref TEXT, entry_time TEXT)");

            // Check if rooms table is empty
            ResultSet rsRooms = stmt.executeQuery("SELECT COUNT(*) FROM rooms");
            if (rsRooms.next() && rsRooms.getInt(1) == 0) {
                seedDefaultData(conn);
            }

            // Sync from Database into memory structures
            loadFromDatabase(conn, roomList, ds);

            System.out.println("[DatabaseManager] SQLite database initialized and synced with HostelDataStructures.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedDefaultData(Connection conn) throws SQLException {
        try (PreparedStatement pstmtRoom = conn.prepareStatement("INSERT INTO rooms (room_num, capacity) VALUES (?, ?)");
             PreparedStatement pstmtStudent = conn.prepareStatement("INSERT INTO students (id, name, room_num) VALUES (?, ?, ?)");
             PreparedStatement pstmtQueue = conn.prepareStatement("INSERT INTO allocation_queue (student_id, student_name) VALUES (?, ?)");
             PreparedStatement pstmtGate = conn.prepareStatement("INSERT INTO gate_register (student_name, room_num, direction, reason, entry_time) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement pstmtLeave = conn.prepareStatement("INSERT INTO leave_requests (student_name, from_date, to_date, reason, status) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement pstmtComplaint = conn.prepareStatement("INSERT INTO complaints (student_name, room_num, category, description, status) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement pstmtVisitor = conn.prepareStatement("INSERT INTO visitors (visitor_name, student_name, purpose, id_ref, entry_time) VALUES (?, ?, ?, ?, ?)")) {

            // Default Rooms
            Object[][] roomsData = {
                {"A-112", 3}, {"B-204", 2}, {"C-301", 2}, {"C-302", 2}, {"D-410", 3}
            };
            for (Object[] r : roomsData) {
                pstmtRoom.setString(1, (String) r[0]);
                pstmtRoom.setInt(2, (Integer) r[1]);
                pstmtRoom.executeUpdate();
            }

            // Default Occupied Students
            Object[][] studentsData = {
                {"TCE1001", "Divya S", "A-112"},
                {"TCE1002", "Priya N", "A-112"},
                {"TCE1003", "Karthik R", "B-204"},
                {"TCE1004", "Arun K", "B-204"},
                {"TCE1005", "Mohan V", "C-301"},
                {"TCE1006", "Naveen T", "D-410"},
                {"TCE1007", "Suresh M", "D-410"},
                {"TCE1008", "Vikram J", "D-410"}
            };
            for (Object[] s : studentsData) {
                pstmtStudent.setString(1, (String) s[0]);
                pstmtStudent.setString(2, (String) s[1]);
                pstmtStudent.setString(3, (String) s[2]);
                pstmtStudent.executeUpdate();
            }

            // Initial Queue Entries
            Object[][] queueData = {
                {"TCE2001", "Ravi Kumar"},
                {"TCE2002", "Anitha M"}
            };
            for (Object[] q : queueData) {
                pstmtQueue.setString(1, (String) q[0]);
                pstmtQueue.setString(2, (String) q[1]);
                pstmtQueue.executeUpdate();
            }

            // Initial Gate Entries
            Object[][] gateData = {
                {"Karthik R", "B-204", "OUT", "Grocery run", "08:12 AM"},
                {"Divya S", "A-112", "IN", "Returned from class", "09:40 AM"},
                {"Mohan V", "C-301", "OUT", "Doctor visit", "10:05 AM"}
            };
            for (Object[] g : gateData) {
                pstmtGate.setString(1, (String) g[0]);
                pstmtGate.setString(2, (String) g[1]);
                pstmtGate.setString(3, (String) g[2]);
                pstmtGate.setString(4, (String) g[3]);
                pstmtGate.setString(5, (String) g[4]);
                pstmtGate.executeUpdate();
            }

            // Initial Leave Requests
            Object[][] leaveData = {
                {"Priya N", "2026-07-22", "2026-07-24", "Home visit", "pending"},
                {"Arun K", "2026-07-19", "2026-07-20", "Family function", "approved"}
            };
            for (Object[] l : leaveData) {
                pstmtLeave.setString(1, (String) l[0]);
                pstmtLeave.setString(2, (String) l[1]);
                pstmtLeave.setString(3, (String) l[2]);
                pstmtLeave.setString(4, (String) l[3]);
                pstmtLeave.setString(5, (String) l[4]);
                pstmtLeave.executeUpdate();
            }

            // Initial Complaints
            Object[][] complaintData = {
                {"Naveen T", "D-410", "Electrical", "Fan not working", "open"},
                {"Divya S", "A-112", "Wi-Fi", "No signal in room", "progress"}
            };
            for (Object[] c : complaintData) {
                pstmtComplaint.setString(1, (String) c[0]);
                pstmtComplaint.setString(2, (String) c[1]);
                pstmtComplaint.setString(3, (String) c[2]);
                pstmtComplaint.setString(4, (String) c[3]);
                pstmtComplaint.setString(5, (String) c[4]);
                pstmtComplaint.executeUpdate();
            }

            // Initial Visitor
            pstmtVisitor.setString(1, "Mrs. Lakshmi");
            pstmtVisitor.setString(2, "Karthik R");
            pstmtVisitor.setString(3, "Parent visit");
            pstmtVisitor.setString(4, "—");
            pstmtVisitor.setString(5, "11:20 AM");
            pstmtVisitor.executeUpdate();
        }
    }

    public void loadFromDatabase(Connection conn, List<Room> roomList, HostelDataStructures ds) throws SQLException {
        roomList.clear();
        Map<String, Room> roomMap = new HashMap<>();

        // 1. Load Rooms
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT room_num, capacity FROM rooms")) {
            while (rs.next()) {
                String num = rs.getString("room_num");
                int cap = rs.getInt("capacity");
                Room r = new Room(num, cap);
                roomList.add(r);
                roomMap.put(num, r);
            }
        }

        // 2. Load Students -> HashSet (unique IDs) & HashMap (O(1) detail lookup)
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, name, room_num FROM students")) {
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String roomNum = rs.getString("room_num");

                Student s = new Student(id, name, roomNum);

                if (roomNum != null && roomMap.containsKey(roomNum)) {
                    roomMap.get(roomNum).getOccupants().add(s);
                }

                // Add to HashMap and HashSet
                StudentDetail detail = new StudentDetail(id, name, roomNum);
                ds.putDetail(id, detail);
            }
        }

        // Load Gate Register History into StudentDetails
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT student_name, direction, reason, entry_time FROM gate_register")) {
            while (rs.next()) {
                String sName = rs.getString("student_name");
                String dir = rs.getString("direction");
                String reason = rs.getString("reason");
                String time = rs.getString("entry_time");
                String entryStr = dir + " (" + reason + ") at " + time;

                // Match in HashMap
                for (StudentDetail d : ds.getStudentDetailMap().values()) {
                    if (d.getName().equalsIgnoreCase(sName)) {
                        d.addGateEntry(entryStr);
                    }
                }
            }
        }

        // Load Leave History into StudentDetails
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT student_name, from_date, to_date, reason, status FROM leave_requests")) {
            while (rs.next()) {
                String sName = rs.getString("student_name");
                String from = rs.getString("from_date");
                String to = rs.getString("to_date");
                String reason = rs.getString("reason");
                String status = rs.getString("status");
                String leaveStr = from + " to " + to + " (" + reason + ") - Status: " + status;

                for (StudentDetail d : ds.getStudentDetailMap().values()) {
                    if (d.getName().equalsIgnoreCase(sName)) {
                        d.addLeaveHistory(leaveStr);
                    }
                }
            }
        }

        // Load Complaints into StudentDetails
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT student_name, category, description, status FROM complaints")) {
            while (rs.next()) {
                String sName = rs.getString("student_name");
                String cat = rs.getString("category");
                String desc = rs.getString("description");
                String status = rs.getString("status");
                String complaintStr = "[" + cat + "] " + desc + " - Status: " + status;

                for (StudentDetail d : ds.getStudentDetailMap().values()) {
                    if (d.getName().equalsIgnoreCase(sName)) {
                        d.addComplaint(complaintStr);
                    }
                }
            }
        }

        // 3. Load Queue (FIFO order)
        ds.clearQueue();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT student_id, student_name FROM allocation_queue ORDER BY queue_id ASC")) {
            while (rs.next()) {
                String id = rs.getString("student_id");
                String name = rs.getString("student_name");
                Student s = new Student(id, name);
                ds.enqueueAllocationRequest(s);
            }
        }
    }

    public void addQueueEntry(String id, String name) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO allocation_queue (student_id, student_name) VALUES (?, ?)")) {
            pstmt.setString(1, id);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeQueueEntry(String id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM allocation_queue WHERE UPPER(student_id) = UPPER(?)")) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveStudent(Student s) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO students (id, name, room_num) VALUES (?, ?, ?)")) {
            pstmt.setString(1, s.getId());
            pstmt.setString(2, s.getName());
            pstmt.setString(3, s.getRoomNum());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateStudentRoom(String id, String roomNum) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE students SET room_num = ? WHERE UPPER(id) = UPPER(?)")) {
            pstmt.setString(1, roomNum);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeStudent(String id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE UPPER(id) = UPPER(?)")) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int removeDuplicateStudents() {
        String sql = "DELETE FROM students WHERE rowid NOT IN (SELECT MIN(rowid) FROM students GROUP BY UPPER(id))";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getDatabaseTablesJson() {
        StringBuilder json = new StringBuilder("{");
        String[] tables = {"rooms", "students", "allocation_queue", "gate_register", "attendance", "leave_requests", "complaints", "visitors"};

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (int t = 0; t < tables.length; t++) {
                String table = tables[t];
                json.append("\"").append(table).append("\":[");

                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    boolean firstRow = true;

                    while (rs.next()) {
                        if (!firstRow) json.append(",");
                        firstRow = false;

                        json.append("{");
                        for (int i = 1; i <= cols; i++) {
                            String colName = md.getColumnName(i);
                            String val = rs.getString(i);
                            json.append("\"").append(colName).append("\":\"")
                                .append(val != null ? val.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") : "")
                                .append("\"");
                            if (i < cols) json.append(",");
                        }
                        json.append("}");
                    }
                } catch (SQLException e) {
                    // Ignore individual table read error
                }
                json.append("]");
                if (t < tables.length - 1) json.append(",");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        json.append("}");
        return json.toString();
    }

    public void saveLeaveRequest(String studentName, String fromDate, String toDate, String reason, String status) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO leave_requests (student_name, from_date, to_date, reason, status) VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setString(1, studentName);
            pstmt.setString(2, fromDate);
            pstmt.setString(3, toDate);
            pstmt.setString(4, reason);
            pstmt.setString(5, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveComplaint(String studentName, String roomNum, String category, String description, String status) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO complaints (student_name, room_num, category, description, status) VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setString(1, studentName);
            pstmt.setString(2, roomNum);
            pstmt.setString(3, category);
            pstmt.setString(4, description);
            pstmt.setString(5, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
