import java.util.Collections;
import java.util.Comparator;

public interface CompareArrivalTime {
    default int compareArrivalTime(Robot r1, Robot r2){

        Collections.sort(r1.items, Comparator.comparingInt(Item::myArrival));
        Collections.sort(r2.items, Comparator.comparingInt(Item::myArrival));

        int arrivalTime1 = r1.items.get(0).myArrival();
        int arrivalTime2 = r2.items.get(0).myArrival();

        return Integer.compare(arrivalTime1, arrivalTime2);

    }
}
