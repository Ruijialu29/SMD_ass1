import java.util.*;

import static java.lang.String.format;

public class MailRoom {
    public enum Mode {CYCLING, FLOORING}
    public List<Item>[] waitingForDelivery;
    private final int numRobots;
    private final Mode mode;
    private int capacity;
    private int rooms;
    private boolean isInitialized = false;

    Queue<Robot> idleRobots;
    List<Robot> activeRobots;
    List<Robot> deactivatingRobots; // Don't treat a robot as both active and idle by swapping directly
    List<Robot> activeColumnRobots;

    public MailRoom(int numFloors, int numRobots, Mode mode, int capacity, int rooms) {
        this.mode = mode;
        this.capacity = capacity;
        this.rooms = rooms;

        waitingForDelivery = new List[numFloors];
        for (int i = 0; i < numFloors; i++) {
            waitingForDelivery[i] = new LinkedList<>();
        }
        this.numRobots = numRobots;

        idleRobots = new LinkedList<>();
        activeRobots = new ArrayList<>();
        deactivatingRobots = new ArrayList<>();
        activeColumnRobots = new ArrayList<>();

        if(mode == Mode.CYCLING){
            for (int i = 0; i < numRobots; i++)
                idleRobots.add(new CyclingRobot(MailRoom.this, capacity));  // In mailroom, floor/room is not significant
        }
        else if(mode == Mode.FLOORING){
            idleRobots.add(new ColumnRobot(MailRoom.this, capacity));
            idleRobots.add(new ColumnRobot(MailRoom.this, capacity));
            Building building = Building.getBuilding();
            for (int i = 0; i < building.NUMFLOORS; i++) {
                activeRobots.add(new FloorRobot(MailRoom.this, capacity, i+1, 1, activeColumnRobots));
            }
        }
    }

    public boolean someItems() {
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!waitingForDelivery[i].isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public int floorWithEarliestItem() {
        int floor = -1;
        int earliest = Simulation.now() + 1;
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!waitingForDelivery[i].isEmpty()) {
                int arrival = waitingForDelivery[i].getFirst().myArrival();
                if (earliest > arrival) {
                    floor = i;
                    earliest = arrival;
                }
            }
        }
        return floor;
    }

    public void arrive(List<Item> items) {
        for (Item item : items) {
            waitingForDelivery[item.myFloor()-1].add(item);
            System.out.printf("Item: Time = %d Floor = %d Room = %d Weight = %d\n",
                    item.myArrival(), item.myFloor(), item.myRoom(), item.myWeight());
        }
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

        for(Robot robot : activeColumnRobots){
            if(robot.getFloor() == r.getFloor() && !robot.items.isEmpty() && robot.items.get(0).myFloor() == r.getFloor()){
                if(robot.getRoom() == 0){
                    leftRobot = robot;
                }
                else if(robot.getRoom() == (rooms + 1)){
                    rightRobot = robot; 
                }
            }
        }

        if (leftRobot != null && rightRobot == null) {
            return 0;
        } else if (leftRobot == null && rightRobot != null) {
            return 1;
        } else if (leftRobot != null && rightRobot != null) {
            return compareArrivalTime(leftRobot, rightRobot);
        }
    
        return -1;

    }

    public void cyclingTick(){
        for (Robot activeRobot : activeRobots) {
            System.out.printf("About to tick: " + activeRobot.toString() + "\n"); activeRobot.tick();
        }
        robotDispatch();  // dispatch a robot if conditions are met
        // These are returning robots who shouldn't be dispatched in the previous step
        ListIterator<Robot> iter = deactivatingRobots.listIterator();
        while (iter.hasNext()) {  // In timestamp order
            Robot robot = iter.next();
            iter.remove();
            activeRobots.remove(robot);
            idleRobots.add(robot);
        }
    }

    public void flooringTick(){

        robotDispatch();

        // Process the flooring robots(empty and loaded)
        for (Robot robot : activeRobots) {
            System.out.printf("About to tick: %s\n", robot);
            if (robot.items.isEmpty()) {
                processEmptyFloorRobot(robot);
            } else {
                processLoadedFloorRobot(robot);
            }
        }

        processColumnRobots();

        dispatchIdleRobots();

        processDeactivatingRobots();

    }

    public void processEmptyFloorRobot(Robot robot){
        int waitingPosition = checkWaitingPosition(robot);
        if (waitingPosition == 0 && robot.getRoom() == 1) {
            transferItemToRobot(robot, "R1", 0);
        }   
        else if (waitingPosition == 0 && robot.getRoom() != 1) {
            robot.move(Building.Direction.LEFT);
        }   
        else if (waitingPosition == 1 && robot.getRoom() == rooms) {
            transferItemToRobot(robot, "R2", 1);
        }   
        else if (waitingPosition == 1 && robot.getRoom() != rooms) {
            robot.move(Building.Direction.RIGHT);
        }
    }

    public void processLoadedFloorRobot(Robot robot) {
        if (robot.getFloor() == robot.items.getFirst().myFloor() && robot.getRoom() == robot.items.getFirst().myRoom()) {
            do {
                Item firstItem = robot.items.get(0);
                robot.setRemainingCapacity(robot.getRemainingCapacity() + firstItem.myWeight());
                Simulation.deliver(robot.items.removeFirst());
            } while (!robot.items.isEmpty() && robot.getRoom() == robot.items.get(0).myRoom());
        } else {
            if (((FloorRobot) robot).getTransferPosition() == 0) {
                robot.move(Building.Direction.RIGHT);
            }
            else if (((FloorRobot) robot).getTransferPosition() == 1) {
                robot.move(Building.Direction.LEFT);
            }
        }
    }

    public void transferItemToRobot(Robot robot, String providerId, int transferPosition) {
        Robot providerRobot = activeColumnRobots.stream().filter(columnRobot -> columnRobot.getId().equals(providerId)).findFirst().orElse(null);
        if (providerRobot != null) {
            robot.transfer(providerRobot);
        }
        ((FloorRobot) robot).setTransferPosition(transferPosition);
    }

    public void processColumnRobots() {
        for (Robot robot : activeColumnRobots) {
            if (!robot.items.isEmpty() && robot.getFloor() != robot.items.get(0).myFloor()) {
                robot.move(Building.Direction.UP);
            } else if (robot.items.isEmpty() && robot.getFloor() != 0) {
                robot.move(Building.Direction.DOWN);
            }
        }
    }

    public void dispatchIdleRobots() {
        int length = idleRobots.size();
        while (length > 0) {
            System.out.println("Dispatch at time = " + Simulation.now());
            int earliestFloor = floorWithEarliestItem();
            if (earliestFloor >= 0) {
                Robot robot = idleRobots.remove();
                length--;
                loadRobot(earliestFloor, robot);

                if (robot.getId().equals("R1")) {
                    robot.sort();
                } 
                else if (robot.getId().equals("R2")) {
                    robot.items.sort(Comparator.reverseOrder());
                }

                activeColumnRobots.add(robot);
                System.out.printf("Dispatch @ %s of Robot %s with %d item(s)\n", Simulation.now(), robot.getId(), robot.numItems());

                if (robot.getId().equals("R1")) {
                    robot.place(0, 0);
                }           
                else if (robot.getId().equals("R2")) {
                    robot.place(0, rooms + 1);
                }
            }
        }
    }

    public void processDeactivatingRobots() {
        ListIterator<Robot> iterator = deactivatingRobots.listIterator();
        while (iterator.hasNext()) {
            Robot robot = iterator.next();
            iterator.remove();
            activeColumnRobots.remove(robot);
            idleRobots.add(robot);
        }
    }

    public void tick() { 
        // Simulation time unit
        if(this.mode == Mode.CYCLING) {
            cyclingTick();
        } else if(this.mode == Mode.FLOORING) {
            flooringTick();
        }
    }

    public void robotDispatch() {
        if(this.mode == Mode.CYCLING){
            // Can dispatch at most one robot; it needs to move out of the way for the next
            System.out.println("Dispatch at time = " + Simulation.now());
            // Need an idle robot and space to dispatch (could be a traffic jam)
            if (!idleRobots.isEmpty() && !Building.getBuilding().isOccupied(0,0)) {
                int fwei = floorWithEarliestItem();
                if (fwei >= 0) {  // Need an item or items to deliver, starting with earliest
                    Robot robot = idleRobots.remove();
                    loadRobot(fwei, robot);
                    // Room order for left to right delivery
                    robot.sort();
                    activeRobots.add(robot);
                    System.out.println("Dispatch @ " + Simulation.now() +
                            " of Robot " + robot.getId() + " with " + robot.numItems() + " item(s)");
                    robot.place(0, 0);
                }
            }
        }

        else if(mode == Mode.FLOORING){
            // Dispatch floor robots
            if (!isInitialized) {
                int floor = 1;
                for (Robot robot : activeRobots) {
                    robot.place(floor, 1);
                    floor += 1;
                }
                isInitialized = true; 
            }
        }
    }

    public void robotReturn(Robot robot) {
        Building building = Building.getBuilding();
        int floor = robot.getFloor();
        int room = robot.getRoom();
        assert floor == 0 && room == building.NUMROOMS+1: format("robot returning from wrong place - floor=%d, room ==%d", floor, room);
        assert robot.isEmpty() : "robot has returned still carrying at least one item";
        building.remove(floor, room);
        deactivatingRobots.add(robot);
    }

    public void loadRobot(int floor, Robot robot) {
        Collections.sort(waitingForDelivery[floor], Comparator.comparingInt(Item::myArrival));

        ListIterator<Item> iter = waitingForDelivery[floor].listIterator();
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
