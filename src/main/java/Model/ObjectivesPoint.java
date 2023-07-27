package Model;

public class ObjectivesPoint {
    private double x; // km
    private double y; // time
    public ObjectivesPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public boolean isDominance(ObjectivesPoint p) {
        return  ( getX() <= p.getX() && getY() <= p.getY() ) && ( (getY() < p.getY()) || (getX() < p.getX()) );
    }

    public void setToThisPoint(ObjectivesPoint a) {
        setX(a.getX());
        setY(a.getY());
    }

    public boolean isValid() {
        return this.x != -1 && this.y != -1;
    }

    @Override
    public String toString() {
        return "x: "+this.getX()+" y: "+this.getY();
    }
}
