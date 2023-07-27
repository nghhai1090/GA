package Service;

import Model.ChromosomeKeyObjectiveValue;
import Model.ObjectivesPoint;
import Model.Transport;
import Model.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class HelperService {

    /**
     * Method to create a array of instances Vehicle class according to input vehicle matrix
     *
     * @param vehiclesMatrix Matrix which stores informations of vehicles
     *                       .Info : (code,depot,capacity,deploy cost,speed,load factor)
     * @return array of instances Vehicle class according to input vehicle matrix
     */
    public Vehicle[] createVehiclesArray(int[][] vehiclesMatrix) {
        Vehicle[] vehiclesArray= new Vehicle[vehiclesMatrix.length];

        for (int i = 0; i < vehiclesMatrix.length; i++) {
            Vehicle vehicle = new Vehicle(vehiclesMatrix[i][0], vehiclesMatrix[i][1], vehiclesMatrix[i][2], vehiclesMatrix[i][3], vehiclesMatrix[i][4], vehiclesMatrix[i][5]);
            vehiclesArray[i] = vehicle;
        }

        return vehiclesArray;
    }

    /**
     * Method to create an array of instances Transport class according to the input transports matrix.
     *
     * @param transportsMatrix Matrix which stores informations of transports
     *                         .Info : (pickup, delivery, active time of pickup, active time of delivery, transports amount)
     * @return array of instances Transport class according to the input transports matrix
     */

    public Transport[] createTransportsArray(int[][] transportsMatrix) {
        Transport[] transportsArray = new Transport[transportsMatrix.length];

        for (int i = 0; i < transportsMatrix.length; i++) {
            Transport transport = new Transport(transportsMatrix[i][0], transportsMatrix[i][1], transportsMatrix[i][2], transportsMatrix[i][3], transportsMatrix[i][4]);
            transportsArray[i] = transport;
            transportsArray[i].setCode(i);
        }

        return transportsArray;
    }


    /**
     * Method to create a time matrix, which stores ride time between 2 points in distance matrix of vehicles
     *
     * @param distanceMatrix distance matrix
     * @param vehiclesList   vehicles array
     * @return time matrix, which stores ride time between 2 points in distance matrix of vehicles
     */
    public int[][][] caculateTransportTimeMatrix(int[][] distanceMatrix, Vehicle[] vehiclesList) {
        int[][][] transportTime = new int[vehiclesList.length][distanceMatrix.length][distanceMatrix.length];

        for (int k = 0; k < vehiclesList.length; k++) {
            int speedOfK = vehiclesList[k].getSpeed();
            for (int i = 0; i < distanceMatrix.length; i++) {
                for (int j = 0; j < distanceMatrix[i].length; j++) {
                    int transportTimeIToJ = distanceMatrix[i][j] / speedOfK;
                    transportTime[k][i][j] = transportTimeIToJ;
                }
            }
        }

        return transportTime;
    }

    public ArrayList<Integer> generateNRandomNumberBetween(int up, int low, int num) {
        ArrayList<Integer> numList = new ArrayList<>();
        if(low > up) {
            throw new RuntimeException("low"+low+"up"+up);
        }
        while(low<=up) {
            numList.add(low);
            low++;
        }
        Collections.shuffle(numList);
        return new ArrayList<Integer>( numList.subList(0,Math.min(num+1,numList.size())));
    }

    public ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> nonDominanceSort(ArrayList<ChromosomeKeyObjectiveValue> population) {
        ArrayList<Integer>[] submissiveSetsArray = new ArrayList[population.size()];
        int[] individualDominationCountArray = new int[population.size()];
        ArrayList<Integer>[] frontsArray = new ArrayList[population.size()+1];
        for(int i = 0 ; i < population.size()+1 ; i++) {
            frontsArray[i] = new ArrayList<>();
        }
        for(int i = 0 ; i < population.size() ; i++) {
            submissiveSetsArray[i] = new ArrayList<>();
            individualDominationCountArray[i] = 0;
            for(int j = 0 ; j < population.size() ; j++) {
                if(i!=j) {
                    if(population.get(i).getObjectivesPoint().isDominance(population.get(j).getObjectivesPoint())) {
                        submissiveSetsArray[i].add(j);
                    }
                    else if (population.get(j).getObjectivesPoint().isDominance(population.get(i).getObjectivesPoint())) {
                        individualDominationCountArray[i] = individualDominationCountArray[i] + 1;
                    }
                }
            }
            if(individualDominationCountArray[i]== 0) {
                frontsArray[0].add(i);
            }
        }
        int i = 0;
        ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> result = new ArrayList<>();
        while(frontsArray[i].size()!=0) {
            ArrayList<Integer> individualsNextFrontList = new ArrayList<>();
            ArrayList<ChromosomeKeyObjectiveValue> front = new ArrayList<>();
            for(int j = 0 ; j < frontsArray[i].size() ;j++) {
                int index = frontsArray[i].get(j);
                for(int h = 0 ; h < submissiveSetsArray[index].size() ; h++ ) {
                    int subIndex = submissiveSetsArray[index].get(h);
                    individualDominationCountArray[subIndex] = individualDominationCountArray[subIndex] -1 ;
                    if(individualDominationCountArray[subIndex] == 0) {
                        individualsNextFrontList.add(subIndex);
                    }
                }
                front.add(population.get(frontsArray[i].get(j)));
            }
            result.add(front);
            i = i +1;
            frontsArray[i].addAll(individualsNextFrontList);
        }
        return result;
    }

    public HashMap<Integer,Double> assignCrowdingDistance(ArrayList<ChromosomeKeyObjectiveValue> front) {
        HashMap<Integer, Double> crowdingDistancesMap = new HashMap<>();
        for (int i = 0; i < front.size(); i++) {
            crowdingDistancesMap.put(front.get(i).getChromosomeKey(), 0.0);
        }

        front.sort(Comparator.comparingDouble(a -> a.getObjectivesPoint().getX()));
        crowdingDistancesMap.put(front.get(0).getChromosomeKey(),Double.MAX_VALUE);
        crowdingDistancesMap.put(front.get(front.size()-1).getChromosomeKey(),Double.MAX_VALUE);
        for(int i = 1; i < front.size()-1; i++) {
            double newDistance = Double.MAX_VALUE;
            if(crowdingDistancesMap.get(front.get(i).getChromosomeKey())!=Double.MAX_VALUE) {
                newDistance = crowdingDistancesMap.get(front.get(i).getChromosomeKey()) +
                        (front.get(i+1).getObjectivesPoint().getX() - front.get(i-1).getObjectivesPoint().getX() )
                                / (front.get(front.size()-1).getObjectivesPoint().getX() - front.get(0).getObjectivesPoint().getX());
            }
            crowdingDistancesMap.put(front.get(i).getChromosomeKey(),newDistance);
        }

        front.sort(Comparator.comparingDouble(a -> a.getObjectivesPoint().getY()));
        crowdingDistancesMap.put(front.get(0).getChromosomeKey(),Double.MAX_VALUE);
        crowdingDistancesMap.put(front.get(front.size()-1).getChromosomeKey(),Double.MAX_VALUE);
        for(int i = 1; i < front.size()-1; i++) {
            double newDistance = Double.MAX_VALUE;
            if(crowdingDistancesMap.get(front.get(i).getChromosomeKey())!=Double.MAX_VALUE) {
                newDistance = crowdingDistancesMap.get(front.get(i).getChromosomeKey()) +
                        (front.get(i+1).getObjectivesPoint().getY() - front.get(i-1).getObjectivesPoint().getY() )
                                / (front.get(front.size()-1).getObjectivesPoint().getY() - front.get(0).getObjectivesPoint().getY());
            }
            crowdingDistancesMap.put(front.get(i).getChromosomeKey(),newDistance);
        }

        return crowdingDistancesMap;
    }

    private double distSum(ObjectivesPoint p,ArrayList<ObjectivesPoint> objectivesPointArrayList)
    {
        double sum = 0;
        for (int i = 0; i < objectivesPointArrayList.size(); i++) {
            double distx = Math.abs(objectivesPointArrayList.get(i).getX()- p.getX());
            double disty = Math.abs(objectivesPointArrayList.get(i).getY() - p.getY());
            sum += Math.sqrt((distx * distx) + (disty * disty));
        }

        // Return the sum of Euclidean Distances
        return sum;
    }

    public ObjectivesPoint geometricMedian(ArrayList<ObjectivesPoint> objectivesPointArrayList)
    {

        // Current x coordinate and y coordinate
        ObjectivesPoint current_point = new ObjectivesPoint(0, 0);

        for (int i = 0; i < objectivesPointArrayList.size(); i++) {
            current_point.setX(current_point.getX()+objectivesPointArrayList.get(i).getX());
            current_point.setY(current_point.getY()+objectivesPointArrayList.get(i).getY());
        }

        // Here current_point becomes the
        // Geographic MidPoint
        // Or Center of Gravity of equal
        // discrete mass distributions
        current_point.setX(current_point.getX()/objectivesPointArrayList.size());
        current_point.setY(current_point.getY()/objectivesPointArrayList.size());

        // minimum_distance becomes sum of
        // all distances from MidPoint to
        // all given points
        double minimum_distance = distSum(current_point,objectivesPointArrayList);

        int k = 0;
        while (k < objectivesPointArrayList.size()) {
            for (int i = 0; i < objectivesPointArrayList.size() && i != k; i++) {
                ObjectivesPoint newpoint = new ObjectivesPoint(0, 0);
                newpoint.setX(objectivesPointArrayList.get(i).getX());
                newpoint.setY(objectivesPointArrayList.get(i).getY());
                double newd = distSum(newpoint, objectivesPointArrayList);
                if (newd < minimum_distance) {
                    minimum_distance = newd;
                    current_point.setX(newpoint.getX());
                    current_point.setY(newpoint.getY());
                }
            }
            k++;
        }

        // Assume test_distance to be 1000
        double test_distance = 1000;
        int flag = 0;
        ArrayList<ObjectivesPoint> testPoints = new ArrayList<>();
        testPoints.add(new ObjectivesPoint(-1,0));
        testPoints.add(new ObjectivesPoint(0,1));
        testPoints.add(new ObjectivesPoint(1,0));
        testPoints.add(new ObjectivesPoint(0,-1));
        // Test loop for approximation starts here
        while (test_distance > 0.01) {

            flag = 0;

            // Loop for iterating over all 4 neighbours
            for (int i = 0; i < 4; i++) {

                // Finding Neighbours done
                ObjectivesPoint newpoint = new ObjectivesPoint(0, 0);
                newpoint.setX(current_point.getX()+ test_distance*testPoints.get(i).getX() );
                newpoint.setY(current_point.getY()+ test_distance*testPoints.get(i).getY() );

                // New sum of Euclidean distances
                // from the neighbor to the given
                // data points
                double newd = distSum(newpoint, objectivesPointArrayList);

                if (newd < minimum_distance) {

                    // Approximating and changing
                    // current_point
                    minimum_distance = newd;
                    current_point.setX(newpoint.getX());
                    current_point.setY(newpoint.getY());
                    flag = 1;
                    break;
                }
            }

            // This means none of the 4 neighbours
            // has the new minimum distance, hence
            // we divide by 2 and reiterate while
            // loop for better approximation
            if (flag == 0)
                test_distance /= 2;
        }
        if(Double.isNaN(current_point.getY()) || Double.isNaN(current_point.getX())) {
            //System.out.println("                     NAN                        ");
            for(int i = 0 ; i < objectivesPointArrayList.size() ; i++) {
                if(Double.isNaN(objectivesPointArrayList.get(i).getY()) || Double.isNaN(objectivesPointArrayList.get(i).getX())) {
                    System.out.println(objectivesPointArrayList.get(i));
                }
            }
        }
        return current_point;
    }



    public double caculateDistanceToCenter(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

}
