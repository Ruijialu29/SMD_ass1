// Check that maxweight (of parcel) is less than or equal to the maxcapacity of robot.

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Simulation {
    private static int time = 0;
    public final int endArrival;
    final public MailRoom mailroom;
    private static int timeout;

    Simulation(Properties properties) {
        int seed = Integer.parseInt(properties.getProperty("seed"));
        Random random = new Random(seed);
        this.endArrival = Integer.parseInt(properties.getProperty("mail.endarrival"));
        int numLetters = Integer.parseInt(properties.getProperty("mail.letters"));
        int numParcels = Integer.parseInt(properties.getProperty("mail.parcels"));
        int maxWeight = Integer.parseInt(properties.getProperty("mail.parcelmaxweight"));
        int numFloors = Integer.parseInt(properties.getProperty("building.floors"));
        int numRooms = Integer.parseInt(properties.getProperty("building.roomsperfloor"));
        int numRobots = Integer.parseInt(properties.getProperty("robot.number"));
        int robotCapacity = Integer.parseInt(properties.getProperty("robot.capacity"));
        timeout = Integer.parseInt(properties.getProperty("timeout"));
        MailRoom.Mode mode = MailRoom.Mode.valueOf(properties.getProperty("mode"));

        Building.initialise(numFloors, numRooms);
        Building building = Building.getBuilding();
        mailroom = new MailRoom(building.NUMFLOORS, numRobots, mode, robotCapacity, numRooms);
        for (int i = 0; i < numLetters; i++) { //Generate letters
            int arrivalTime = random.nextInt(endArrival)+1;
            int floor = random.nextInt(building.NUMFLOORS)+1;
            int room = random.nextInt(building.NUMROOMS)+1;
            Item.addToArrivals(arrivalTime, new Letter(floor, room, arrivalTime, 0));
        }
        for (int i = 0; i < numParcels; i++) { // Generate parcels
            int arrivalTime = random.nextInt(endArrival)+1;
            int floor = random.nextInt(building.NUMFLOORS)+1;
            int room = random.nextInt(building.NUMROOMS)+1;
            int weight = random.nextInt(maxWeight)+1;
            // What am I going to do with all these values?
            Item.addToArrivals(arrivalTime, new Parcel(floor, room, arrivalTime, weight));
        }
    }

    public static int now() { return time; }

    void step() {
        // External events
        if (Item.waitingToArrive.containsKey(time))
            Item.arrive(mailroom, Item.waitingToArrive.get(time));
        // Internal events
        mailroom.tick();
        }

    void run() {
        while (time++ <= endArrival || Item.someItems(mailroom)) {
            step();
            try {
                TimeUnit.MILLISECONDS.sleep(timeout);
            } catch (InterruptedException e) {
                // System.out.printf("Sleep interrupted!\n");
            }
        }
        System.out.printf("Finished: Items delivered = %d; Average time for delivery = %.2f%n",
                Item.deliveredCount, (float) Item.deliveredTotalTime/Item.deliveredCount);
    }

}
