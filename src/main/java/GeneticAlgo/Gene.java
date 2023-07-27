package GeneticAlgo;

import java.util.ArrayList;

class Gene {
    private int depot;
    private int vehicleIndex;
    private int deployCost;
    private ArrayList<Integer> transportsIndicesList;
    private ArrayList<TransportNode> route;
    private int totalTime;
    private int totalMautKm;

    public Gene(int depot, int vehicleIndex, int deployCost) {
        this.depot = depot;
        this.vehicleIndex = vehicleIndex;
        this.deployCost = deployCost;
        this.transportsIndicesList = new ArrayList<>();
        this.route = new ArrayList<>();
        this.totalMautKm = -1;
        this.totalTime = -1;
    }

    public int getDepot() {
        return depot;
    }

    public int getVehicleIndex() {
        return vehicleIndex;
    }

    public ArrayList<Integer> getTransportsIndicesList() {
        return transportsIndicesList;
    }

    public void setTransportsIndicesList(ArrayList<Integer> transportsCodesList) {
        for(int i = 0 ; i < transportsCodesList.size() ; i++) {
            this.transportsIndicesList.add(transportsCodesList.get(i));
        }
    }

    public void addToTransportsIndicesList(int transportCode) {
        this.transportsIndicesList.add(transportCode);
    }

    public ArrayList<TransportNode> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<TransportNode> route) {
        this.route = route;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public int getDeployCost() {
        return deployCost;
    }

    public void setDepot(int depot) {
        this.depot = depot;
    }

    public void setVehicleIndex(int vehicleIndex) {
        this.vehicleIndex = vehicleIndex;
    }

    public void setDeployCost(int deployCost) {
        this.deployCost = deployCost;
    }

    public int getTotalMautKm() {
        return totalMautKm;
    }

    public void setTotalMautKm(int totalMautKm) {
        this.totalMautKm = totalMautKm;
    }
    public Gene clone() {
        Gene clone = new Gene(this.depot,this.vehicleIndex,this.getDeployCost());
        clone.setTransportsIndicesList((ArrayList<Integer>) this.getTransportsIndicesList().clone());
        clone.setRoute((ArrayList<TransportNode>) this.getRoute().clone());
        return clone;
    }
}
