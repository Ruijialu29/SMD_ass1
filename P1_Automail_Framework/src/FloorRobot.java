import java.util.List;
import java.util.ListIterator;

public class FloorRobot extends Robot{

    private int transferPosition = -1;
    
    public FloorRobot(MailRoom mailroom, int remainingCapacity, int floor, int room, List<FloorRobot> activeFloorRobots) {
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

    // floor robot check whether there is a column robot waiting on the left or right
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

    // process empty floor robot
    // column robot waiting on the left or right, floor robot is adjacent to it, then transfer items
    // column robot waiting, floor robot is not adjacent to it, then move to column robot
    public void processEmptyFloorRobot(){
        int waitingPosition = checkWaitingPosition(this);
        if (waitingPosition == 0 && this.getRoom() == 1) {
            transferItemToRobot("R1", 0);
        }   
        else if (waitingPosition == 0 && this.getRoom() != 1) {
            this.move(Building.Direction.LEFT);
        }   
        else if (waitingPosition == 1 && this.getRoom() == mailroom.rooms) {
           transferItemToRobot("R2", 1);
        }   
        else if (waitingPosition == 1 && this.getRoom() != mailroom.rooms) {
            this.move(Building.Direction.RIGHT);
        }
    }

    // process loaded floor robot
    // find the right destination, deliver items and update capacity
    public void processLoadedFloorRobot() {
        if (this.getFloor() == this.items.getFirst().myFloor() && this.getRoom() == this.items.getFirst().myRoom()) {
            do {
                Item firstItem = this.items.get(0);
                this.setRemainingCapacity(this.getRemainingCapacity() + firstItem.myWeight());
                Item.deliver(this.items.removeFirst());
            } while (!this.items.isEmpty() && this.getRoom() == this.items.get(0).myRoom());
        } else {
            if (((FloorRobot) this).getTransferPosition() == 0) {
                this.move(Building.Direction.RIGHT);
            }
            else if (((FloorRobot) this).getTransferPosition() == 1) {
                this.move(Building.Direction.LEFT);
            }
        }
    }

    public void transferItemToRobot(String providerId, int transferPosition) {
        Robot providerRobot = mailroom.activeColumnRobots.stream().filter(columnRobot -> columnRobot.getId().equals(providerId)).findFirst().orElse(null);
        if (providerRobot != null) {
            this.transfer(providerRobot);
        }
        ((FloorRobot) this).setTransferPosition(transferPosition);
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
}
