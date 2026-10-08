import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomNum;
    private int capacity;
    private List<Student> occupants;

    public Room(String roomNum, int capacity) {
        this.roomNum = roomNum;
        this.capacity = capacity;
        this.occupants = new ArrayList<>();
    }

    public String getRoomNum() { return roomNum; }
    public int getCapacity() { return capacity; }
    public List<Student> getOccupants() { return occupants; }

    public boolean isFull() { return occupants.size() >= capacity; }

    public boolean addOccupant(Student s) {
        if (!isFull()) {
            occupants.add(s);
            s.setRoomNum(roomNum);
            return true;
        }
        return false;
    }

    public boolean removeOccupant(String studentId) {
        return occupants.removeIf(s -> s.getId().equalsIgnoreCase(studentId));
    }
}
