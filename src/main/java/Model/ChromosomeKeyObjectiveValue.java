package Model;

public class ChromosomeKeyObjectiveValue {
    private int chromosomeKey;
    private ObjectivesPoint objectivesPoint;

    public ChromosomeKeyObjectiveValue(int chromosomeKey, ObjectivesPoint objectivesPoint) {
        this.chromosomeKey = chromosomeKey;
        this.objectivesPoint = objectivesPoint;
    }

    public int getChromosomeKey() {
        return chromosomeKey;
    }

    public void setChromosomeKey(int chromosomeKey) {
        this.chromosomeKey = chromosomeKey;
    }

    public ObjectivesPoint getObjectivesPoint() {
        return objectivesPoint;
    }

    public void setObjectivesPoint(ObjectivesPoint objectivesPoint) {
        this.objectivesPoint = objectivesPoint;
    }

    @Override
    public String toString() {
        return "key:"+chromosomeKey+" obj:"+objectivesPoint.toString();

    }
}
