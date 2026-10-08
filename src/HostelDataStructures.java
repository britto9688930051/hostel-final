import java.util.*;

/**
 * HostelDataStructures encapsulates key data structures for Hostel Management:
 * 
 * 1. Queue (java.util.Queue): Implements First-Come, First-Served (FCFS) room allocation queue.
 * 2. HashSet (java.util.HashSet): Ensures uniqueness of Student IDs & removes duplicate records across system.
 * 3. HashMap (java.util.HashMap): Enables instant O(1) constant-time detail lookup by Student ID.
 */
public class HostelDataStructures {

    // Data Structure 1: Queue for FIFO Room Allocation
    private Queue<Student> allocationQueue = new LinkedList<>();

    // Data Structure 2: HashSet for unique ID tracking & duplicate removal
    private Set<String> registeredStudentIds = new HashSet<>();

    // Data Structure 3: HashMap for fast O(1) detail lookup by Student ID
    private Map<String, StudentDetail> studentDetailMap = new HashMap<>();

    // --- QUEUE OPERATIONS ---
    public synchronized boolean enqueueAllocationRequest(Student student) {
        String key = student.getId().toUpperCase();
        // Check HashSet to prevent adding duplicates into queue
        if (registeredStudentIds.contains(key)) {
            return false; // Duplicate rejected by HashSet
        }
        allocationQueue.offer(student);
        registeredStudentIds.add(key);

        if (!studentDetailMap.containsKey(key)) {
            studentDetailMap.put(key, new StudentDetail(student.getId(), student.getName(), student.getRoomNum()));
        }
        return true;
    }

    public synchronized Student dequeueAllocationRequest() {
        return allocationQueue.poll();
    }

    public synchronized Queue<Student> getAllocationQueue() {
        return allocationQueue;
    }

    public synchronized void clearQueue() {
        allocationQueue.clear();
    }

    // --- HASHSET OPERATIONS (DUPLICATE REMOVAL & VALIDATION) ---
    public synchronized boolean containsStudentId(String id) {
        if (id == null) return false;
        return registeredStudentIds.contains(id.toUpperCase());
    }

    public synchronized boolean addStudentId(String id) {
        if (id == null) return false;
        return registeredStudentIds.add(id.toUpperCase());
    }

    public synchronized boolean removeStudentId(String id) {
        if (id == null) return false;
        return registeredStudentIds.remove(id.toUpperCase());
    }

    public synchronized Set<String> getAllRegisteredIds() {
        return Collections.unmodifiableSet(registeredStudentIds);
    }

    /**
     * Uses HashSet to deduplicate a list of Student objects.
     */
    public synchronized List<Student> removeDuplicates(List<Student> list) {
        Set<String> seen = new HashSet<>();
        List<Student> uniqueList = new ArrayList<>();
        for (Student s : list) {
            String key = s.getId().toUpperCase();
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueList.add(s);
            }
        }
        return uniqueList;
    }

    // --- HASHMAP OPERATIONS (O(1) DETAIL RETRIEVAL BY ID) ---
    public synchronized void putDetail(String id, StudentDetail detail) {
        if (id == null) return;
        String key = id.toUpperCase();
        studentDetailMap.put(key, detail);
        registeredStudentIds.add(key);
    }

    public synchronized StudentDetail getDetailById(String id) {
        if (id == null) return null;
        return studentDetailMap.get(id.toUpperCase());
    }

    public synchronized StudentDetail removeDetail(String id) {
        if (id == null) return null;
        String key = id.toUpperCase();
        registeredStudentIds.remove(key);
        return studentDetailMap.remove(key);
    }

    public synchronized Map<String, StudentDetail> getStudentDetailMap() {
        return studentDetailMap;
    }

    /**
     * Access Control Check: Verifies if a given Student ID belongs to a registered hostel student.
     * ONLY registered hostel students are authorized to access data.
     */
    public synchronized boolean isHostelStudent(String id) {
        if (id == null) return false;
        String key = id.toUpperCase();
        return registeredStudentIds.contains(key) || studentDetailMap.containsKey(key);
    }

    /**
     * Checks if a student name matches a registered hostel student in the database.
     * Non-database students are blocked from submitting complaints and leave requests.
     */
    public synchronized boolean isRegisteredStudentByName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String cleanName = name.trim().toLowerCase();
        for (StudentDetail d : studentDetailMap.values()) {
            if (d.getName() != null && d.getName().trim().equalsIgnoreCase(cleanName)) {
                return true;
            }
        }
        return false;
    }
}
