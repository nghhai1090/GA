package Model;

public class Vehicle {
    private int code;
    private int depot;
    private int cap;

    private int loadFactor;

    private int fixCost;
    private int speed;

    public Vehicle(int code, int depot, int cap, int fixCost, int speed, int loadFactor) {
        this.depot = depot;
        this.cap = cap;
        this.fixCost = fixCost;
        this.speed = speed;
        this.code = code;
        this.loadFactor = loadFactor;
    }

    public int getDepot() {
        return depot;
    }

    public int getCap() {
        return cap;
    }
    public int getFixCost() {return fixCost;}
    public int getSpeed() {return speed;}
    public int getCode() {return code;}

    public int getLoadFactor() {
        return loadFactor;
    }

    @Override
    public String toString() {
        return "Model.Vehicle{" +
                "depot=" + depot +
                ", cap=" + cap +
                ", fixCost=" + fixCost +
                ", speed=" + speed +
                '}';
    }
}

