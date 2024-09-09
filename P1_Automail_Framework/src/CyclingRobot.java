public class CyclingRobot extends Robot{

    public CyclingRobot(MailRoom mailroom, int remainingCapacity) {
        super(mailroom, remainingCapacity);
    }

    public void tick() {
        if (items.isEmpty()) {
            returnToMailRoom();
        } else {
            // Items to deliver
            deliverItems();
        }
    }

    // when cycling robot finished delivery, robot move down or right until reaching the ladder to return to mail room
    public void returnToMailRoom(){
        Building building = Building.getBuilding();
        if (room == building.NUMROOMS + 1) { // in right end column
            move(Building.Direction.DOWN);  //move towards mailroom
        } else {
            move(Building.Direction.RIGHT); // move towards right end column
        }
    }

    // when robot reach the right floor, then deliver items and move to next delivery on the same floor
    // else move up to the right floor
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

}
