public class Item implements Comparable<Item> {
    protected final int floor;
    protected final int room;
    protected final int arrival;
    protected int weight;

    @Override public int compareTo(Item i) {
        int floorDiff = this.floor - i.floor;  // Don't really need this as only deliver to one floor at a time
        return (floorDiff == 0) ? this.room - i.room : floorDiff;
    }

    Item(int floor, int room, int arrival, int weight) {
        this.floor = floor;
        this.room = room;
        this.arrival = arrival;
        this.weight = weight;
    }

    public String toString() {
        return "Floor: " + floor + ", Room: " + room + ", Arrival: " + arrival + ", Weight: " + weight;
    }

    int myFloor() { return floor; }
    int myRoom() { return room; }
    int myArrival() { return arrival; }
    int myWeight() { return weight; }
}
