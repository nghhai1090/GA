package GeneticAlgo;

class TransportNode {
    private int transportIndex;
    private boolean isPickUp;
    public TransportNode(int transportIndex, boolean isPickUp) {
        this.transportIndex = transportIndex;
        this.isPickUp = isPickUp;
    }

    public int getTransportIndex() {
        return transportIndex;
    }

    public boolean isPickUp() {
        return isPickUp;
    }

    @Override
    public String toString() {
        return "TransportNode{" +
                "transportIndex=" + transportIndex +
                ", isPickUp=" + isPickUp +
                '}';
    }
}
