public class Parcel extends Item {

    Parcel(int floor, int room, int arrival, int weight) {
        super(floor, room, arrival, weight);
    }

    @Override
    public String toString() {
        return "Floor: " + floor + ", Room: " + room + ", Arrival: " + arrival + ", Weight: " + weight;
    }


}
