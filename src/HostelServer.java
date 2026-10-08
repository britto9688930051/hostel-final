import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class HostelServer {
    private static final int PORT = 8080;
    private static final List<Room> roomList = new ArrayList<>();
    private static final HostelDataStructures ds = new HostelDataStructures();
    private static final DatabaseManager dbManager = new DatabaseManager();

    public static void main(String[] args) throws IOException {
        // 1. Initialize SQLite Database & Data Structures (Queue, HashSet, HashMap)
        dbManager.initDatabase(roomList, ds);

        // 2. Start HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Serve Static HTML
        server.createContext("/", new StaticFileHandler());

        // API Endpoints
        server.createContext("/api/state", new StateHandler());
        server.createContext("/api/apply", new ApplyHandler());
        server.createContext("/api/allocate", new AllocateHandler());
        server.createContext("/api/vacate", new VacateHandler());
        server.createContext("/api/student-detail", new DetailHandler());
        server.createContext("/api/deduplicate", new DeduplicateHandler());
        server.createContext("/api/verify-access", new VerifyAccessHandler());
        server.createContext("/api/database-view", new DbViewHandler());
        server.createContext("/api/leave", new LeaveHandler());
        server.createContext("/api/complaint", new ComplaintHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println(" Host Management System Backend Server Running!");
        System.out.println(" Server URL: http://localhost:" + PORT);
        System.out.println(" Data Structures Active: Queue, HashSet, HashMap");
        System.out.println(" Database: SQLite (hostel.db)");
        System.out.println("=================================================");
    }

    // --- HELPER METHODS ---
    private static void sendCorsAndHeaders(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // --- HANDLERS ---

    // Static HTML Handler
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            File htmlFile = new File("hostel.html");
            if (!htmlFile.exists()) {
                htmlFile = new File("../hostel.html");
            }

            if (htmlFile.exists()) {
                byte[] content = Files.readAllBytes(htmlFile.toPath());
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                String err = "<h1>404 Not Found</h1><p>hostel.html not found.</p>";
                exchange.sendResponseHeaders(404, err.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(err.getBytes());
                }
            }
        }
    }

    // GET /api/state
    static class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            StringBuilder json = new StringBuilder("{");

            // Rooms & Occupants
            json.append("\"rooms\":[");
            for (int i = 0; i < roomList.size(); i++) {
                Room r = roomList.get(i);
                json.append("{\"num\":\"").append(escapeJson(r.getRoomNum())).append("\",");
                json.append("\"cap\":").append(r.getCapacity()).append(",");
                json.append("\"occ\":[");
                List<Student> occ = r.getOccupants();
                for (int j = 0; j < occ.size(); j++) {
                    Student s = occ.get(j);
                    json.append("{\"id\":\"").append(escapeJson(s.getId())).append("\",");
                    json.append("\"name\":\"").append(escapeJson(s.getName())).append("\"}");
                    if (j < occ.size() - 1) json.append(",");
                }
                json.append("]}");
                if (i < roomList.size() - 1) json.append(",");
            }
            json.append("],");

            // Queue Data Structure
            json.append("\"queue\":[");
            List<Student> queueList = new ArrayList<>(ds.getAllocationQueue());
            for (int i = 0; i < queueList.size(); i++) {
                Student s = queueList.get(i);
                json.append("{\"id\":\"").append(escapeJson(s.getId())).append("\",");
                json.append("\"name\":\"").append(escapeJson(s.getName())).append("\"}");
                if (i < queueList.size() - 1) json.append(",");
            }
            json.append("],");

            // HashSet Stats
            json.append("\"totalRegistered\":").append(ds.getAllRegisteredIds().size());
            json.append("}");

            sendCorsAndHeaders(exchange, 200, json.toString());
        }
    }

    // POST /api/apply (Uses HashSet for duplicate check, Queue for allocation enqueue)
    static class ApplyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);
            String name = data.get("name");
            String id = data.get("id");

            if (name == null || id == null || name.trim().isEmpty() || id.trim().isEmpty()) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Student Name and ID are required.\"}");
                return;
            }

            name = name.trim();
            id = id.trim();

            // 1. Check HashSet for Duplicate Prevention
            if (ds.containsStudentId(id)) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Duplicate rejected by HashSet! Student ID (" + escapeJson(id) + ") is already registered.\"}");
                return;
            }

            // 2. Enqueue into Allocation Queue
            Student student = new Student(id, name);
            boolean enqueued = ds.enqueueAllocationRequest(student);

            if (!enqueued) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Failed to enqueue room application.\"}");
                return;
            }

            // 3. Persist to SQLite Database
            dbManager.addQueueEntry(id, name);

            String response = "{\"success\":true,\"message\":\"Applied successfully. Added to Allocation Queue using Queue & HashSet.\",\"id\":\"" + escapeJson(id) + "\"}";
            sendCorsAndHeaders(exchange, 200, response);
        }
    }

    // POST /api/allocate (Dequeues from Queue, allocates to Room, updates HashMap & DB)
    static class AllocateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            // Dequeue top student from Allocation Queue
            Student nextStudent = ds.dequeueAllocationRequest();
            if (nextStudent == null) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Allocation Queue is empty. No pending applications.\"}");
                return;
            }

            // Find first available room
            Room availableRoom = null;
            for (Room r : roomList) {
                if (!r.isFull()) {
                    availableRoom = r;
                    break;
                }
            }

            if (availableRoom == null) {
                // Put back in queue if no rooms available
                ds.enqueueAllocationRequest(nextStudent);
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"All rooms are currently full. Student kept in Queue.\"}");
                return;
            }

            // Assign occupant
            availableRoom.addOccupant(nextStudent);

            // Update HashMap for fast O(1) detail lookup
            StudentDetail detail = ds.getDetailById(nextStudent.getId());
            if (detail == null) {
                detail = new StudentDetail(nextStudent.getId(), nextStudent.getName(), availableRoom.getRoomNum());
            } else {
                detail.setRoomNum(availableRoom.getRoomNum());
            }
            ds.putDetail(nextStudent.getId(), detail);

            // Sync Database
            dbManager.removeQueueEntry(nextStudent.getId());
            dbManager.saveStudent(nextStudent);

            String response = "{\"success\":true,\"room\":\"" + escapeJson(availableRoom.getRoomNum()) + "\",\"student\":{\"id\":\"" + escapeJson(nextStudent.getId()) + "\",\"name\":\"" + escapeJson(nextStudent.getName()) + "\"}}";
            sendCorsAndHeaders(exchange, 200, response);
        }
    }

    // POST /api/vacate (Removes student from room, updates HashMap & SQLite)
    static class VacateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);
            String id = data.get("id");

            if (id == null || id.trim().isEmpty()) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Student ID is required.\"}");
                return;
            }

            id = id.trim();
            boolean removed = false;
            for (Room r : roomList) {
                if (r.removeOccupant(id)) {
                    removed = true;
                    break;
                }
            }

            // Remove from HashMap & HashSet
            ds.removeDetail(id);
            dbManager.removeStudent(id);

            if (removed) {
                sendCorsAndHeaders(exchange, 200, "{\"success\":true,\"message\":\"Student vacated and removed from HashMap & Database.\"}");
            } else {
                sendCorsAndHeaders(exchange, 404, "{\"error\":\"Student ID not found in any room.\"}");
            }
        }
    }

    // GET /api/student-detail?id=XYZ (O(1) Instant Lookup using HashMap)
    static class DetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String id = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "id".equalsIgnoreCase(pair[0])) {
                        id = pair[1];
                        break;
                    }
                }
            }

            if (id == null || id.trim().isEmpty()) {
                sendCorsAndHeaders(exchange, 400, "{\"found\":false,\"error\":\"Student ID parameter 'id' is required.\"}");
                return;
            }

            id = id.trim();

            // Access Control Check: ONLY registered hostel students can access data
            if (!ds.isHostelStudent(id)) {
                sendCorsAndHeaders(exchange, 403, "{\"found\":false,\"accessDenied\":true,\"message\":\"ACCESS DENIED: Only registered hostel students are authorized to access data.\"}");
                return;
            }

            long startTime = System.nanoTime();

            // Perform instant O(1) lookup in Java HashMap
            StudentDetail detail = ds.getDetailById(id);

            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1000000.0;

            if (detail == null) {
                sendCorsAndHeaders(exchange, 404, "{\"found\":false,\"accessDenied\":true,\"message\":\"Student ID (" + escapeJson(id) + ") not found in HashMap.\"}");
                return;
            }

            StringBuilder json = new StringBuilder("{");
            json.append("\"found\":true,");
            json.append("\"id\":\"").append(escapeJson(detail.getId())).append("\",");
            json.append("\"name\":\"").append(escapeJson(detail.getName())).append("\",");
            json.append("\"roomNum\":\"").append(escapeJson(detail.getRoomNum())).append("\",");
            json.append("\"attendanceStatus\":\"").append(escapeJson(detail.getAttendanceStatus())).append("\",");
            json.append("\"lookupTime\":\"O(1) Constant Time via HashMap (").append(String.format("%.4f", durationMs)).append(" ms)\",");

            // Gate History
            json.append("\"gateHistory\":[");
            List<String> gate = detail.getGateHistory();
            for (int i = 0; i < gate.size(); i++) {
                json.append("\"").append(escapeJson(gate.get(i))).append("\"");
                if (i < gate.size() - 1) json.append(",");
            }
            json.append("],");

            // Leave History
            json.append("\"leaveHistory\":[");
            List<String> leave = detail.getLeaveHistory();
            for (int i = 0; i < leave.size(); i++) {
                json.append("\"").append(escapeJson(leave.get(i))).append("\"");
                if (i < leave.size() - 1) json.append(",");
            }
            json.append("],");

            // Complaints History
            json.append("\"complaintsHistory\":[");
            List<String> comp = detail.getComplaintsHistory();
            for (int i = 0; i < comp.size(); i++) {
                json.append("\"").append(escapeJson(comp.get(i))).append("\"");
                if (i < comp.size() - 1) json.append(",");
            }
            json.append("]");

            json.append("}");
            sendCorsAndHeaders(exchange, 200, json.toString());
        }
    }

    // POST /api/deduplicate (Scans using HashSet and removes duplicates in database)
    static class DeduplicateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }

            int deletedRows = dbManager.removeDuplicateStudents();

            String response = "{\"success\":true,\"deletedDuplicates\":" + deletedRows +
                    ",\"message\":\"Database deduplication complete using Java HashSet verification.\",\"totalUnique\":" + ds.getAllRegisteredIds().size() + "}";
            sendCorsAndHeaders(exchange, 200, response);
        }
    }

    // POST /api/verify-access (Access Control Check: ONLY registered hostel students can access data)
    static class VerifyAccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);
            String id = data.get("id");
            if (id == null || id.trim().isEmpty()) {
                sendCorsAndHeaders(exchange, 400, "{\"authorized\":false,\"message\":\"Student ID is required.\"}");
                return;
            }
            id = id.trim();
            boolean isAuthorized = ds.isHostelStudent(id);
            if (isAuthorized) {
                sendCorsAndHeaders(exchange, 200, "{\"authorized\":true,\"message\":\"Access Granted: Authorized Registered Hostel Student.\",\"id\":\"" + escapeJson(id) + "\"}");
            } else {
                sendCorsAndHeaders(exchange, 403, "{\"authorized\":false,\"message\":\"Access Denied: Only registered hostel students are authorized to access data.\",\"id\":\"" + escapeJson(id) + "\"}");
            }
        }
    }

    // GET /api/database-view (Dumps all tables from hostel.db SQLite database)
    static class DbViewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            String dbJson = dbManager.getDatabaseTablesJson();
            sendCorsAndHeaders(exchange, 200, dbJson);
        }
    }

    // POST /api/leave (Validates student name against database registry)
    static class LeaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);
            String name = data.get("name");
            String from = data.get("from");
            String to = data.get("to");
            String reason = data.get("reason");

            if (name == null || name.trim().isEmpty()) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Student name is required.\"}");
                return;
            }

            name = name.trim();
            if (!ds.isRegisteredStudentByName(name)) {
                sendCorsAndHeaders(exchange, 403, "{\"success\":false,\"error\":\"Access Denied: Student '" + escapeJson(name) + "' is not registered in the hostel database. Only registered hostel students can request leave.\"}");
                return;
            }

            dbManager.saveLeaveRequest(name, from, to, reason, "pending");
            sendCorsAndHeaders(exchange, 200, "{\"success\":true,\"message\":\"Leave request submitted and saved to database.\"}");
        }
    }

    // POST /api/complaint (Validates student name against database registry)
    static class ComplaintHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsAndHeaders(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseJson(body);
            String name = data.get("name");
            String room = data.get("room");
            String cat = data.get("cat");
            String desc = data.get("desc");

            if (name == null || name.trim().isEmpty()) {
                sendCorsAndHeaders(exchange, 400, "{\"error\":\"Student name is required.\"}");
                return;
            }

            name = name.trim();
            if (!ds.isRegisteredStudentByName(name)) {
                sendCorsAndHeaders(exchange, 403, "{\"success\":false,\"error\":\"Access Denied: Student '" + escapeJson(name) + "' is not registered in the hostel database. Only registered hostel students can raise complaints.\"}");
                return;
            }

            dbManager.saveComplaint(name, room, cat, desc, "open");
            sendCorsAndHeaders(exchange, 200, "{\"success\":true,\"message\":\"Complaint submitted and saved to database.\"}");
        }
    }
}
