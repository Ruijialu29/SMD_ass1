import java.util.List;

public class FloorRobot extends Robot{
    private int transferPosition = -1;
    private List<Robot> activeRobots;

    public FloorRobot(MailRoom mailroom, int remainingCapacity, int floor, int room, List<Robot> activeRobots) {
        super(mailroom, remainingCapacity);
        this.activeRobots = activeRobots;
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
