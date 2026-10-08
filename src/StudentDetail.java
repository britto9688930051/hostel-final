import java.util.ArrayList;
import java.util.List;

public class StudentDetail {
    private String id;
    private String name;
    private String roomNum;
    private String attendanceStatus;
    private List<String> gateHistory;
    private List<String> leaveHistory;
    private List<String> complaintsHistory;

    public StudentDetail(String id, String name, String roomNum) {
        this.id = id;
        this.name = name;
        this.roomNum = roomNum != null ? roomNum : "Unassigned";
        this.attendanceStatus = "Present";
        this.gateHistory = new ArrayList<>();
        this.leaveHistory = new ArrayList<>();
        this.complaintsHistory = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoomNum() { return roomNum; }
    public void setRoomNum(String roomNum) { this.roomNum = roomNum != null ? roomNum : "Unassigned"; }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }

    public List<String> getGateHistory() { return gateHistory; }
    public List<String> getLeaveHistory() { return leaveHistory; }
    public List<String> getComplaintsHistory() { return complaintsHistory; }

    public void addGateEntry(String entry) { this.gateHistory.add(entry); }
    public void addLeaveHistory(String leave) { this.leaveHistory.add(leave); }
    public void addComplaint(String complaint) { this.complaintsHistory.add(complaint); }
}
