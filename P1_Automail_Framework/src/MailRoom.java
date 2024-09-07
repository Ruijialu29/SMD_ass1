import java.util.*;

public class MailRoom {
    public enum Mode {CYCLING, FLOORING}
    public List<Item>[] waitingForDelivery;
    private final int numRobots;
    private final Mode mode;
    private int capacity;
    public int rooms;
    private boolean isInitialized = false;

    Queue<Robot> idleRobots;
    List<FloorRobot> activeFloorRobots;
    List<Robot> deactivatingRobots; // Don't treat a robot as both active and idle by swapping directly
    List<Robot> activeColumnRobots;
    List<Robot> activeRobots;

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
        activeFloorRobots = new ArrayList<>();
        deactivatingRobots = new ArrayList<>();
        activeColumnRobots = new ArrayList<>();
        activeRobots = new ArrayList<>();

        initializeRobots();
        
    }

    public void initializeRobots(){
        if(mode == Mode.CYCLING){
            for (int i = 0; i < numRobots; i++)
                idleRobots.add(new CyclingRobot(MailRoom.this, capacity));  // In mailroom, floor/room is not significant
        }
        else if(mode == Mode.FLOORING){
            idleRobots.add(new ColumnRobot(MailRoom.this, capacity));
            idleRobots.add(new ColumnRobot(MailRoom.this, capacity));
            Building building = Building.getBuilding();
            for (int i = 0; i < building.NUMFLOORS; i++) {
                activeFloorRobots.add(new FloorRobot(MailRoom.this, capacity, i+1, 1, activeFloorRobots));
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

    public void cyclingTick(){
        for (Robot activeRobot : activeFloorRobots) {
            System.out.printf("About to tick: " + activeRobot.toString() + "\n"); activeRobot.tick();
        }
        robotDispatch();  // dispatch a robot if conditions are met
        // These are returning robots who shouldn't be dispatched in the previous step
        ListIterator<Robot> iter = deactivatingRobots.listIterator();
        while (iter.hasNext()) {  // In timestamp order
            Robot robot = iter.next();
            iter.remove();
            activeFloorRobots.remove(robot);
            idleRobots.add(robot);
        }
    }

    public void flooringTick(){

        robotDispatch();

        // Process the flooring robots(empty and loaded)
        for (FloorRobot robot : activeFloorRobots) {
            System.out.printf("About to tick: %s\n", robot);
            if (robot.items.isEmpty()) {
                robot.processEmptyFloorRobot();
            } else {
                robot.processLoadedFloorRobot();
            }
        }

        for (Robot robot : activeColumnRobots){
            robot.processColumnRobots();
        }

        dispatchIdleRobots();

        processDeactivatingRobots();

    }

    public void dispatchIdleRobots() {
        int length = idleRobots.size();
        while (length > 0) {
            System.out.println("Dispatch at time = " + Simulation.now());
            int earliestFloor = floorWithEarliestItem();
            if (earliestFloor >= 0) {
                Robot robot = idleRobots.remove();
                length--;
                robot.loadRobot(earliestFloor, robot);

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
                    robot.loadRobot(fwei, robot);
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
                for (Robot robot : activeFloorRobots) {
                    robot.place(floor, 1);
                    floor += 1;
                }
                isInitialized = true; 
            }
        }
    }

}
