import java.util.List;

public class FloorRobot extends Robot{

    private int transferPosition = -1;
    
    public FloorRobot(MailRoom mailroom, int remainingCapacity, int floor, int room, List<Robot> activeRobots) {
        super(mailroom, remainingCapacity);
        this.room = room;
        this.floor = floor;
    }

    public void setTransferPosition(int transferPosition) {
        this.transferPosition = transferPosition;
    }

    public int getTransferPosition() {
        return transferPosition;
    }

}
