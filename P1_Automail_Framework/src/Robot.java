import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class Robot {
    private static int count = 1;
    final private String id;
    protected int floor;
    protected int room;
    protected int load;
    private int remainingCapacity;

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

    public void transfer(Robot robot) {  // Transfers every item assuming receiving robot has capacity
        ListIterator<Item> iter = robot.items.listIterator();
        while(iter.hasNext()) {
            Item item = iter.next();
            this.add(item); //Hand it over
            this.remainingCapacity -= item.myWeight();
            iter.remove();
            robot.setRemainingCapacity(robot.getRemainingCapacity() + item.myWeight());
        }
    }

    public void tick() {
            if (items.isEmpty()) {
                returnToMailRoom();
            } else {
                // Items to deliver
                deliverItems();
            }
    }

    public void returnToMailRoom(){
        Building building = Building.getBuilding();
        if (room == building.NUMROOMS + 1) { // in right end column
            move(Building.Direction.DOWN);  //move towards mailroom
        } else {
            move(Building.Direction.RIGHT); // move towards right end column
        }
    }

    public void deliverItems(){
        if (floor == items.getFirst().myFloor()) {
            // On the right floor
            if (room == items.getFirst().myRoom()) { //then deliver all relevant items to that room
                do {
                    Item.deliver(items.removeFirst());
                } while (!items.isEmpty() && room == items.getFirst().myRoom());
            } else {
                move(Building.Direction.RIGHT); // move towards next delivery
            }
        } else {
            move(Building.Direction.UP); // move towards floor
        }
    }

    public int numItems () {
        return items.size();
    }

    public void add(Item item) {
        items.add(item);
    }

    public void sort() {
        Collections.sort(items);
    }

    public int compareArrivalTime(Robot r1, Robot r2){

        Collections.sort(r1.items, Comparator.comparingInt(Item::myArrival));
        Collections.sort(r2.items, Comparator.comparingInt(Item::myArrival));

        int arrivalTime1 = r1.items.get(0).myArrival();
        int arrivalTime2 = r2.items.get(0).myArrival();

        return Integer.compare(arrivalTime1, arrivalTime2);

    }

    public int checkWaitingPosition(Robot r){

        Robot leftRobot = null;
        Robot rightRobot = null;

        for(Robot robot : mailroom.activeColumnRobots){
            if(robot.getFloor() == r.getFloor() && !robot.items.isEmpty() && robot.items.get(0).myFloor() == r.getFloor()){
                if(robot.getRoom() == 0){
                    leftRobot = robot;
                }
                else if(robot.getRoom() == (mailroom.rooms + 1)){
                    rightRobot = robot; 
                }
            }
        }

        if (leftRobot != null && rightRobot == null) {
            return 0;
        } else if (leftRobot == null && rightRobot != null) {
            return 1;
        } else if (leftRobot != null && rightRobot != null) {
            return r.compareArrivalTime(leftRobot, rightRobot);
        }
    
        return -1;

    }

    public void processColumnRobots() {
        if (!this.items.isEmpty() && this.getFloor() != this.items.get(0).myFloor()) {
            this.move(Building.Direction.UP);
        } 
        else if (this.items.isEmpty() && this.getFloor() != 0) {
            this.move(Building.Direction.DOWN);
        }
    }

    public void robotReturn(Robot robot) {
        Building building = Building.getBuilding();
        int floor = robot.getFloor();
        int room = robot.getRoom();
        assert floor == 0 && room == building.NUMROOMS+1: format("robot returning from wrong place - floor=%d, room ==%d", floor, room);
        assert robot.isEmpty() : "robot has returned still carrying at least one item";
        building.remove(floor, room);
        mailroom.deactivatingRobots.add(robot);
    }

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
