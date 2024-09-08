import java.util.ListIterator;

public class ColumnRobot extends Robot{

    public ColumnRobot(MailRoom mailroom, int remainingCapacity) {
        super(mailroom, remainingCapacity);
    }

    public void processColumnRobots() {
        if (!this.items.isEmpty() && this.getFloor() != this.items.get(0).myFloor()) {
            this.move(Building.Direction.UP);
        }
        else if (this.items.isEmpty() && this.getFloor() != 0) {
            this.move(Building.Direction.DOWN);
        }
    }

    public static void processDeactivatingRobots(MailRoom mailroom) {
        ListIterator<Robot> iterator = mailroom.deactivatingRobots.listIterator();
        while (iterator.hasNext()) {
            Robot robot = iterator.next();
            iterator.remove();
            mailroom.activeColumnRobots.remove(robot);
            mailroom.idleRobots.add(robot);
        }
    }
}


