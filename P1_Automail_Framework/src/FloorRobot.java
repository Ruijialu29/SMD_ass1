public class FloorRobot extends Robot{
    private int transferPosition;

    public FloorRobot(MailRoom mailroom, int remainingCapacity) {
        super(mailroom, remainingCapacity);
    }

    public void setTransferPosition(int transferPosition) {
        this.transferPosition = transferPosition;
    }

    public int getTransferPosition() {
        return transferPosition;
    }

}
