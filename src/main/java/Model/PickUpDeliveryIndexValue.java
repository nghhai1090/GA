package Model;

public class PickUpDeliveryIndexValue {
    int pickUpIndex;
    int deliveryIndex;

    int pickUpIndexSwap;
    int deliveryIndexSwap;
    ObjectivesPoint value;

    public PickUpDeliveryIndexValue(int pickUpIndex, int deliveryIndex, ObjectivesPoint value) {
        this.pickUpIndex = pickUpIndex;
        this.deliveryIndex = deliveryIndex;
        this.value = value;
    }

    public PickUpDeliveryIndexValue(int pickUpIndex, int pickUpIndexSwap, int deliveryIndex, int deliveryIndexSwap, ObjectivesPoint value) {
        this.pickUpIndex = pickUpIndex;
        this.pickUpIndexSwap = pickUpIndexSwap;
        this.deliveryIndexSwap = deliveryIndexSwap;
        this.deliveryIndex = deliveryIndex;
        this.value = value;
    }

    public int getPickUpIndex() {
        return pickUpIndex;
    }

    public void setPickUpIndex(int pickUpIndex) {
        this.pickUpIndex = pickUpIndex;
    }

    public int getDeliveryIndex() {
        return deliveryIndex;
    }

    public void setDeliveryIndex(int deliveryIndex) {
        this.deliveryIndex = deliveryIndex;
    }

    public int getPickUpIndexSwap() {
        return pickUpIndexSwap;
    }

    public void setPickUpIndexSwap(int pickUpIndexSwap) {
        this.pickUpIndexSwap = pickUpIndexSwap;
    }

    public int getDeliveryIndexSwap() {
        return deliveryIndexSwap;
    }

    public void setDeliveryIndexSwap(int deliveryIndexSwap) {
        this.deliveryIndexSwap = deliveryIndexSwap;
    }

    public ObjectivesPoint getValue() {
        return value;
    }

    public void setValue(ObjectivesPoint value) {
        this.value.setX(value.getX());
        this.value.setY(value.getY());
    }

    @Override
    public String toString() {
        return "PickUpDeliveryIndexValue{" +
                "pickUpIndex=" + pickUpIndex +
                ", deliveryIndex=" + deliveryIndex +
                ", value=" + value +
                '}';
    }
}
