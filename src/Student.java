public class Student {
    private String id;
    private String name;
    private String roomNum;

    public Student(String id, String name) {
        this.id = id;
        this.name = name;
        this.roomNum = null;
    }

    public Student(String id, String name, String roomNum) {
        this.id = id;
        this.name = name;
        this.roomNum = roomNum;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoomNum() { return roomNum; }

    public void setRoomNum(String roomNum) { this.roomNum = roomNum; }
}
