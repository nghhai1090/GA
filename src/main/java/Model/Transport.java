package Model;

public class Transport {
    private int code;
    private int from;
    private int activeTimeOfPickup;
    private int to;

    private int activeTimeOfDelivery;
    private int amount;

    public Transport(int from, int to, int activeTimeOfPickup, int activeTimeOfDelivery, int amount) {
        this.from = from;
        this.to = to;
        this.activeTimeOfPickup = activeTimeOfPickup;
        this.activeTimeOfDelivery = activeTimeOfDelivery;
        this.amount = amount;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getActiveTimeOfDelivery() {
        return activeTimeOfDelivery;
    }

    public int getActiveTimeOfPickup() {
        return activeTimeOfPickup;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Model.Transport{" +
                "code=" + code +
                ", from=" + from +
                ", activeTimeOfPickup=" + activeTimeOfPickup +
                ", to=" + to +
                ", activeTimeOfDelivery=" + activeTimeOfDelivery +
                ", amount=" + amount +
                '}';
    }
}
