import java.util.*;

import static java.lang.String.format;

public class MailRoom {
    public enum Mode {CYCLING, FLOORING}
    List<Item>[] waitingForDelivery;
    private final int numRobots;
    private final Mode mode;
    private int capacity;
    private int rooms;
    private boolean isInitialized = false;

    Queue<Robot> idleRobots;
    List<Robot> activeRobots;
    List<Robot> deactivatingRobots; // Don't treat a robot as both active and idle by swapping directly
    List<Robot> activeColumnRobots;

    public boolean someItems() {
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!waitingForDelivery[i].isEmpty()) {
                    return true;
            }
        }
        return false;
    }

    private int floorWithEarliestItem() {
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

    MailRoom(int numFloors, int numRobots, Mode mode, int capacity, int rooms) {
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
                idleRobots.add(new Robot(MailRoom.this, capacity));  // In mailroom, floor/room is not significant
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

    void arrive(List<Item> items) {
        for (Item item : items) {
            waitingForDelivery[item.myFloor()-1].add(item);
            System.out.printf("Item: Time = %d Floor = %d Room = %d Weight = %d\n",
                    item.myArrival(), item.myFloor(), item.myRoom(), 0);
        }
    }

    public int isEarlier(Robot r1, Robot r2){
        List<Item> items1 = r1.items;
        List<Item> items2 = r2.items;

        Collections.sort(items1, Comparator.comparingInt(Item::myArrival));
        Collections.sort(items2, Comparator.comparingInt(Item::myArrival));

        if(items1.get(0).myArrival() < items2.get(0).myArrival()){
            return 0;
        }
        else if(items1.get(0).myArrival() > items2.get(0).myArrival()){
            return 1;
        }
        else if(items1.get(0).myArrival() == items2.get(0).myArrival()){
            return 0;
        }
        return -1;
    }

    public int isWaiting(Robot r){
        int left = 0;
        int right = 0;
        Robot leftRobot = null;
        Robot rightRobot = null;

        for(Robot robot : activeColumnRobots){
            if(robot.getFloor() == r.getFloor() && robot.items.getFirst().myFloor() == r.getFloor() && !robot.items.isEmpty()){
                if(robot.getRoom() == 0){
                    left = 1;
                    leftRobot = robot;
                }
                else if(robot.getRoom() == (rooms + 1)){
                    right = 1;
                    rightRobot = robot; 
                }
            }
        }

        if(left == 1 && right == 0){
            return 0;
        }
        else if(left == 0 && right == 1){
            return 1;
        }
        else if(left == 1 && right == 1){
            if(isEarlier(leftRobot, rightRobot) == 0){
                return 0;
            }
            else if(isEarlier(leftRobot, rightRobot) == 1){
                return 1;
            }
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

        // The behavviours of floor robots
        for(Robot robot : activeRobots){
            System.out.printf("About to tick: " + robot.toString() + "\n");
            // 当robot空载时，要么原地不动，要么检测到有ColumnRobot在等待，就向ColumnRobot移动
            if(robot.items.isEmpty()){
                // 如果此时floor robot和column robot在左侧相邻，就传递item
                if(isWaiting(robot) == 0 && robot.getRoom() == 1){
                    Robot providerRobot = null;
                    for(Robot columnRobot : activeColumnRobots){
                        if(columnRobot.getId().equals("R1")){
                            providerRobot = columnRobot;
                            break;
                        }
                    }

                    if(providerRobot != null){
                        robot.transfer(providerRobot);
                    }
                    ((FloorRobot)robot).setTransferPosition(0);
                }
                // 如果此时floor robot和column robot不相邻，则floor robot向column robot移动(向左移动)
                else if(isWaiting(robot) == 0 && robot.getRoom() != 1){
                    robot.move(Building.Direction.LEFT);
                }
                // 如果此时floor robot和column在右侧相邻，就传递item
                else if(isWaiting(robot) == 1 && robot.getRoom() == rooms){
                    Robot providerRobot = null;
                    for(Robot columnRobot : activeColumnRobots){
                        if(columnRobot.getId().equals("R2")){
                            providerRobot = columnRobot;
                            break;
                        }
                    }

                    if(providerRobot != null){
                        robot.transfer(providerRobot);
                    }
                    ((FloorRobot)robot).setTransferPosition(1);
                }
                // 如果此时floor robot和column robot不相邻，则floor robot向column robot移动（向右移动）
                else if(isWaiting(robot) == 1 && robot.getRoom() != rooms){
                    robot.move(Building.Direction.RIGHT);
                }
            }

            else {   
                if(robot.getFloor() == robot.items.getFirst().myFloor() && robot.getRoom() == robot.items.getFirst().myRoom()){
                    do {
                        Item firsItem = robot.items.get(0);
                        robot.setRemainingCapacity(robot.getRemainingCapacity() + firsItem.myWeight());
                        Simulation.deliver(robot.items.removeFirst());
                    } while(!robot.items.isEmpty() && robot.getRoom() == robot.items.get(0).myRoom());
                }

                else if(((FloorRobot)robot).getTransferPosition() == 0){
                    robot.move(Building.Direction.RIGHT);
                }
                else if(((FloorRobot)robot).getTransferPosition() == 1){
                    robot.move(Building.Direction.LEFT);
                }
            }

        }

        robotDispatch();

        // The behaviours of column robots
        for(Robot robot : activeColumnRobots){
            if(!robot.items.isEmpty() && robot.getFloor() != robot.items.get(0).myFloor()){
                robot.move(Building.Direction.UP);
            }
            else if(!robot.items.isEmpty() && robot.getFloor() == robot.items.get(0).myFloor()){}
            else if(robot.items.isEmpty() && robot.getFloor() != 0){
                robot.move(Building.Direction.DOWN);
            }
        }

        // Dispatch column robots
        int length = idleRobots.size();
        while(length > 0){
            System.out.println("Dispatch at time = " + Simulation.now());
            int fwei = floorWithEarliestItem();
            // Need an idle robot and space to dispatch (could be a traffic jam)
            if (fwei >= 0) {
                // Need an item or items to deliver, starting with earliest
                Robot robot = idleRobots.remove();
                length -= 1;
                loadRobot(fwei, robot);
                // Room order for left to right delivery
                if(robot.getId().equals("R1")){
                    robot.sort();
                }
                else if(robot.getId().equals("R2")){
                    Collections.sort(robot.items, Comparator.reverseOrder());
                }
                activeColumnRobots.add(robot);
                System.out.println("Dispatch @ " + Simulation.now() +
                        " of Robot " + robot.getId() + " with " + robot.numItems() + " item(s)");
                if(robot.getId().equals("R1")){
                    robot.place(0, 0);
                }
                else if(robot.getId().equals("R2")){
                    robot.place(0, rooms+1);
                }
            }
        }

        ListIterator<Robot> iter = deactivatingRobots.listIterator();
        while (iter.hasNext()) {  // In timestamp order
            Robot robot = iter.next();
            iter.remove();
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

    void robotDispatch() {
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

    void robotReturn(Robot robot) {
        Building building = Building.getBuilding();
        int floor = robot.getFloor();
        int room = robot.getRoom();
        assert floor == 0 && room == building.NUMROOMS+1: format("robot returning from wrong place - floor=%d, room ==%d", floor, room);
        assert robot.isEmpty() : "robot has returned still carrying at least one item";
        building.remove(floor, room);
        deactivatingRobots.add(robot);
    }

    void loadRobot(int floor, Robot robot) {
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
