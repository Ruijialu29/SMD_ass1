import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class Robot implements CompareArrivalTime{
    private static int count = 1;
    final private String id;
    protected int floor;
    protected int room;
    protected int load;
    protected int remainingCapacity;

    final protected MailRoom mailroom;
    final protected List<Item> items = new ArrayList<>();

    public Robot(MailRoom mailroom, int remainingCapacity) {
        this.id = "R" + count++;
        this.mailroom = mailroom;
        this.remainingCapacity = remainingCapacity;
    }

    public String getId() {
        return id;
    }

    public int getFloor() { return floor; }

    public int getRoom() { return room; }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public int getLoad(){
        load = 0;
        for(Item item: items){
            load += item.myWeight();
        }
        return load;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public String toString() {
        return "Id: " + id + " Floor: " + floor + ", Room: " + room + ", #items: " + numItems() + ", Load: " + getLoad() ;
    }

    public boolean isEmpty() { return items.isEmpty(); }

    public int numItems () {
        return items.size();
    }

    public void add(Item item) {
        items.add(item);
    }

    public void sort() {
        Collections.sort(items);
    }


    public void place(int floor, int room) {
        Building building = Building.getBuilding();
        building.place(floor, room, id);
        this.floor = floor;
        this.room = room;
    }

    public void move(Building.Direction direction) {
        Building building = Building.getBuilding();
        int dfloor, droom;
        switch (direction) {
            case UP    -> {dfloor = floor+1; droom = room;}
            case DOWN  -> {dfloor = floor-1; droom = room;}
            case LEFT  -> {dfloor = floor;   droom = room-1;}
            case RIGHT -> {dfloor = floor;   droom = room+1;}
            default -> throw new IllegalArgumentException("Unexpected value: " + direction);
        }
        if (!building.isOccupied(dfloor, droom)) { // If destination is occupied, do nothing
            building.move(floor, room, direction, id);
            floor = dfloor; room = droom;
            if (floor == 0) {
                System.out.printf("About to return: " + this + "\n");
                robotReturn(this);
            }
        }
    }

    // check whether robot return to mail room, and turn active robot into deactivating.
    public void robotReturn(Robot robot) {
        Building building = Building.getBuilding();
        int floor = robot.getFloor();
        int room = robot.getRoom();
        assert floor == 0 && room == building.NUMROOMS+1: format("robot returning from wrong place - floor=%d, room ==%d", floor, room);
        assert robot.isEmpty() : "robot has returned still carrying at least one item";
        building.remove(floor, room);
        mailroom.deactivatingRobots.add(robot);
    }

    // transfer items in the mail room to the robot
    public void loadRobot(int floor, Robot robot) {
        Collections.sort(mailroom.waitingForDelivery[floor], Comparator.comparingInt(Item::myArrival));

        ListIterator<Item> iter = mailroom.waitingForDelivery[floor].listIterator();
        int remainingCapacity = robot.getRemainingCapacity();

        while (iter.hasNext()) {  // In timestamp order
            Item item = iter.next();
            if(item.myWeight() == 0){
                robot.add(item); //Hand it over
                iter.remove();
            }
            else if(item.myWeight() > 0 && item.myWeight() <= remainingCapacity){
                robot.add(item); //Hand it over
                remainingCapacity -= item.myWeight();
                iter.remove();

                robot.setRemainingCapacity(remainingCapacity);
            }
        }
    }
}
