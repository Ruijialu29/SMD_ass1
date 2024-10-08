import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Item implements Comparable<Item> {
    protected final int floor;
    protected final int room;
    protected final int arrival;
    protected int weight;

    public static final Map<Integer, List<Item>> waitingToArrive = new HashMap<>();
    public static int deliveredCount = 0;
    public static int deliveredTotalTime = 0;

    public Item(int floor, int room, int arrival, int weight) {
        this.floor = floor;
        this.room = room;
        this.arrival = arrival;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Floor: " + floor + ", Room: " + room + ", Arrival: " + arrival + ", Weight: " + weight;
    }

    @Override
    public int compareTo(Item i) {
        int floorDiff = this.floor - i.floor;  // Don't really need this as only deliver to one floor at a time
        return (floorDiff == 0) ? this.room - i.room : floorDiff;
    }

    public int myFloor() {
        return floor;
    }

    public int myRoom() {
        return room;
    }

    public int myArrival() {
        return arrival;
    }

    public int myWeight() {
        return weight;
    }

    public static void deliver(Item mailItem) {
        System.out.println("Delivered: " + mailItem);
        deliveredCount++;
        deliveredTotalTime += Simulation.now() - mailItem.myArrival();
    }

    public static void addToArrivals(int arrivalTime, Item item) {
        System.out.println(item.toString());
        if (waitingToArrive.containsKey(arrivalTime)) {
            waitingToArrive.get(arrivalTime).add(item);
        } else {
            LinkedList<Item> items = new LinkedList<>();
            items.add(item);
            waitingToArrive.put(arrivalTime, items);
        }
    }

    public static boolean someItems(MailRoom mailroom) {
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!mailroom.waitingForDelivery[i].isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // find the floor according to the earliest arrival time of the item
    public static int floorWithEarliestItem(MailRoom mailroom) {
        int floor = -1;
        int earliest = Simulation.now() + 1;
        for (int i = 0; i < Building.getBuilding().NUMFLOORS; i++) {
            if (!mailroom.waitingForDelivery[i].isEmpty()) {
                int arrival = mailroom.waitingForDelivery[i].getFirst().myArrival();
                if (earliest > arrival) {
                    floor = i;
                    earliest = arrival;
                }
            }
        }
        return floor;
    }

    public static void arrive(MailRoom mailroom, List<Item> items) {
        for (Item item : items) {
            mailroom.waitingForDelivery[item.myFloor() - 1].add(item);
            System.out.printf("Item: Time = %d Floor = %d Room = %d Weight = %d\n",
                    item.myArrival(), item.myFloor(), item.myRoom(), item.myWeight());
        }
    }
}
