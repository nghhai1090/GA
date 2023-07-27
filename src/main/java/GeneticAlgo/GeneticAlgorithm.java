package GeneticAlgo;

import Model.ChromosomeKeyObjectiveValue;
import Model.ObjectivesPoint;
import Model.Transport;
import Model.Vehicle;
import Service.HelperService;
import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

import java.io.IOException;
import java.util.*;

import static java.lang.Double.NaN;


public class GeneticAlgorithm {

    private final HelperService helperService = new HelperService();
    private GeneticService geneticService = new GeneticService();

    public GeneticAlgorithm() {
    }

    public void solve(int[][] distanceMatrix, int[][] mautKmMatrix, int[][] vehicleMatrix, int[][] pickupDeliveriesMatrix) throws PythonExecutionException, IOException {
        Vehicle[] vehiclesArray = helperService.createVehiclesArray(vehicleMatrix);
        Transport[] transportsArray= helperService.createTransportsArray(pickupDeliveriesMatrix);
        geneticService.setParameters(distanceMatrix,mautKmMatrix,vehiclesArray,transportsArray);
        geneticAlgorithm(30,10,0.7,0.3);
    }

    public void geneticAlgorithm (int initPopSize, int generation, double initialCrossoverRate, double initialMutationRate) throws PythonExecutionException, IOException {
        ArrayList<Chromosome> population = createInitPopulation(initPopSize);

        int generationCount = 0;
        ArrayList<Chromosome> bestIndividuals = new ArrayList<>();
        ArrayList<Double> xm = new ArrayList<>();
        ArrayList<Double> ym = new ArrayList<>();
        while (generationCount <= generation) {
            double crossoverRate = initialCrossoverRate;
            double mutationRate = initialMutationRate;
            ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> fronts = doSurvivalSelectionNSGAII(population,initPopSize, generationCount,xm,ym);
            //System.out.println("pop after select"+population.size());
            if(generationCount==generation) {
                caculateMedianAndPlotSolutionsFromFronts(fronts,"final non dominated solutions",false,xm,ym,true);
                ArrayList<ChromosomeKeyObjectiveValue> firstFront = fronts.get(0);
                System.out.println(population.size());
                System.out.println("Number Of final solutions: "+firstFront.size());
                firstFront.forEach(e-> {
                    bestIndividuals.add(population.get(e.getChromosomeKey()));
                });
            }
            else{
                System.out.println("GENERATION NUMBER "+generationCount+" GENERATED !");
                System.out.println("* mean of current population : "+ "("+xm.get(xm.size()-1)+";"+ym.get(ym.size()-1)+")");
                System.out.println("GENERATING CHILDS !");
                ArrayList<Chromosome> selectedParents = doCrossOverSelectionNSGAII(population,fronts,initPopSize/2);
                ArrayList<Chromosome> poolList = new ArrayList<>();

                doCrossOver(poolList, selectedParents, initPopSize, crossoverRate);

                doMutation(poolList, mutationRate);
                System.out.println("CHILDS GENERATED !");
              //  System.out.println("pool"+poolList.size());
                population.addAll(poolList);
              //  System.out.println("pop+pol"+population.size());
                System.out.println();
                System.out.println();
            }
            generationCount ++;
        }
        System.out.println("BEST INDIVIDUALS :");
        System.out.println(bestIndividuals.toString());
    }

    private void doCrossOver( ArrayList<Chromosome> poolList, ArrayList<Chromosome> selectedParents, int initPopSize, double crossoverRate) {
        int numOfChildsFromCrossOver = (int) (initPopSize * crossoverRate);
        ArrayList<Chromosome> childsList1 = crossOverStrategy1(selectedParents, numOfChildsFromCrossOver);
        int numOfChildsCopyFromParents = initPopSize - numOfChildsFromCrossOver;
        ArrayList<Integer> randomIndices = helperService.generateNRandomNumberBetween(selectedParents.size()-1,0,numOfChildsCopyFromParents);
       for(int i = 0 ; i < randomIndices.size() ; i++) {
           poolList.add(selectedParents.get(randomIndices.get(i)));
       }
        poolList.addAll(childsList1);
    }

    public ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> doSurvivalSelectionNSGAII(ArrayList<Chromosome> population, int popSize, int generation, ArrayList<Double> xm, ArrayList<Double> ym) throws PythonExecutionException, IOException {
        ArrayList<ChromosomeKeyObjectiveValue> chromosomeKeyObjectiveValues = new ArrayList<>();
        int selectNumber = popSize;
        ArrayList<Integer> deletedIndices = new ArrayList<>();
        ArrayList<Chromosome> remove = new ArrayList<>();
        for(int i = 0 ; i < population.size() ; i++) {
            chromosomeKeyObjectiveValues.add(new ChromosomeKeyObjectiveValue(i,new ObjectivesPoint(population.get(i).getTotalMautKM(),population.get(i).getMaxTime())));
        }
        ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> fronts = helperService.nonDominanceSort(chromosomeKeyObjectiveValues);
        //plotFronts(fronts,"before survival selection");
        for(int i = 0 ; i < fronts.size() ; i++) {
            ArrayList<ChromosomeKeyObjectiveValue> front = fronts.get(i);
            if(selectNumber>0) {
                if(selectNumber<front.size()) {
                    HashMap<Integer,Double> crowdingDistancesMap = helperService.assignCrowdingDistance(front);
                    front.sort(Comparator.comparingDouble(a->crowdingDistancesMap.get(a.getChromosomeKey())));
                    Collections.reverse(front);
                    List<ChromosomeKeyObjectiveValue> toRemove = front.subList(selectNumber,front.size());
                    for(int j = 0 ; j < toRemove.size() ; j++) {
                        remove.add(population.get(toRemove.get(j).getChromosomeKey()));
                        deletedIndices.add(toRemove.get(j).getChromosomeKey());
                    }
                    toRemove.clear();
                    selectNumber = 0;
                }
                else {
                    selectNumber = selectNumber - front.size();
                }
            }
            else {
                for(int j = 0 ; j < front.size() ; j++) {
                    remove.add(population.get(front.get(j).getChromosomeKey()));
                    deletedIndices.add(front.get(j).getChromosomeKey());
                }
                fronts.remove(i);
                i--;
            }
        }
        //System.out.println("ds"+deletedIndices.size());
        //System.out.println("rs"+remove.size());
        // reseting keys
        resetingKeysInFronts(population, deletedIndices, fronts);

        caculateMedianAndPlotSolutionsFromFronts(fronts,"solutions of generation number "+generation, false, xm,ym, false);

        return fronts;
    }

    private static void resetingKeysInFronts(ArrayList<Chromosome> population, ArrayList<Integer> deletedIndices, ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> fronts) {
        Collections.sort(deletedIndices);

        for(int i = 0; i < fronts.size() ; i++) {
            for(int j = 0; j < fronts.get(i).size() ; j++) {
                int key = fronts.get(i).get(j).getChromosomeKey();
                if(!deletedIndices.contains(key)) {
                    boolean reset = false;
                    for(int c = 0; c < deletedIndices.size() ; c++) {
                        if(key < deletedIndices.get(c)) {
                            fronts.get(i).get(j).setChromosomeKey(key - c );
                            reset = true;
                            break;
                        }
                    }
                    if(!reset) {
                        fronts.get(i).get(j).setChromosomeKey(key - deletedIndices.size() );
                    }
                }
            }
        }
        //System.out.println("pop"+population.size());
        //System.out.println("r"+remove.size());
        for(int i = 0 ; i < deletedIndices.size() ; i++) {
            population.remove(deletedIndices.get(i)-i);
        }
        //System.out.println("popr"+population.size());
    }

    private void caculateMedianAndPlotSolutionsFromFronts(ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> fronts, String title, boolean onlyFirstFront,  ArrayList<Double> xm, ArrayList<Double> ym, boolean graphic) throws PythonExecutionException, IOException {

        ArrayList<Double> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();
        ArrayList<Double> x1 = new ArrayList<>();
        ArrayList<Double> y1 = new ArrayList<>();
        ArrayList<Double> xmg = new ArrayList<>();
        ArrayList<Double> ymg = new ArrayList<>();
        ArrayList<ObjectivesPoint> allPoints = new ArrayList<>();
        for(int i = 1 ; i < fronts.size() ; i++) {
            fronts.get(i).forEach(e->
                    {y.add(e.getObjectivesPoint().getX()); x.add(e.getObjectivesPoint().getY()); allPoints.add(e.getObjectivesPoint());}
            );
        }
        fronts.get(0).forEach(e->
                {y1.add(e.getObjectivesPoint().getX()); x1.add(e.getObjectivesPoint().getY()); allPoints.add(e.getObjectivesPoint());}
        );
        ObjectivesPoint median = helperService.geometricMedian(allPoints);
        if(!(Double.isNaN(median.getX())) && !(Double.isNaN(median.getY()))) {
            xmg.add(median.getX());
            ymg.add(median.getY());
        }
        xm.addAll(xmg);
        ym.addAll(ymg);
        if(graphic) {
            Plot plot = Plot.create();
            plot.xlabel("max time");
            plot.ylabel("total toll kilometers");
            if(!onlyFirstFront) {plot.plot().add(x, y, "o").label("dominated solutions").color("blue");}
            plot.plot().add(x1,y1,"o").label("dominating solutions").color("red");
            //plot.plot().add(ym,xm).linestyle("-").label("medians line"); // plot meadian as y then x
            //plot.plot().add(ym,xm,"x").label("medians of previous generations");
            plot.plot().add(ymg,xmg,"X").label("median of this generation").color("black");
            plot.legend().loc("best");
            plot.title(title);
            plot.show();
        }
    }

    public ArrayList<Chromosome> doCrossOverSelectionNSGAII(ArrayList<Chromosome> population, ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> fronts, int crossOverPoolSize) {

        ArrayList<Chromosome> parentsPool = new ArrayList<>();
        for(int c = 0 ; c < crossOverPoolSize ; c++) {
            ArrayList<Integer> parentIndices = helperService.generateNRandomNumberBetween(population.size()-1,0,2);
            int firstFront = 0;
            int secondFront = 0;
            for(int i = 0 ; i < fronts.size() ; i++) {
                for(int j = 0 ; j < fronts.get(i).size() ; j++) {
                    if(fronts.get(i).get(j).getChromosomeKey() == parentIndices.get(0)) {
                        firstFront = i;
                    }
                    if(fronts.get(i).get(j).getChromosomeKey() == parentIndices.get(1)) {
                        secondFront = i;
                    }
                }
            }

            if(firstFront<secondFront) {parentsPool.add(population.get(parentIndices.get(0)));}
            else  if(firstFront>secondFront) {parentsPool.add(population.get(parentIndices.get(1)));}
            else {
                HashMap<Integer,Double>crowdingDistance = helperService.assignCrowdingDistance(fronts.get(firstFront));
                double firstValue = crowdingDistance.get(parentIndices.get(0));
                double secondValue = crowdingDistance.get(parentIndices.get(1));
                if(firstValue>=secondValue) {
                    parentsPool.add(population.get(parentIndices.get(0)));
                }
                else{
                    parentsPool.add(population.get(parentIndices.get(1)));
                }
            }
        }
        return parentsPool;
    }

    public void doMutation(ArrayList<Chromosome> poolList, double mutationRate) {
        int countSuccedd = 0;
        Random rand = new Random();
        Collections.shuffle(poolList);
        int numOfChildsMutated = (int) (poolList.size() * mutationRate);
        for(int i = 0 ; i < numOfChildsMutated ; i++) {
            Chromosome chromosome = poolList.get(i);
            Chromosome copy = chromosome.clone();
            boolean success = false;
            boolean totalSuccess = false;
            int count = 0;
            int randomNumber = rand.nextInt(3) + 1;
            if(randomNumber == 1) {
                while(!success && count < 50) {
                    try {
                        count ++;
                        boolean opt = rand.nextBoolean();
                        mutationOfRoute(chromosome,opt);
                        success = true;
                    }
                    catch (Exception e) {
                        chromosome = copy;
                    }
                }
                totalSuccess = totalSuccess || success;
            }
            if(randomNumber == 2) {
                success = false;
                count = 0;
                while(!success && count < 50) {
                    try {
                        count++;
                        mutationOfDepots(chromosome);
                        success = true;
                    }
                    catch (Exception e) {
                        chromosome = copy;
                    }
                }
                totalSuccess = totalSuccess || success;
            }
            if(randomNumber == 3) {
                success = false;
                count = 0;
                while(!success && count < 50) {
                    try {
                        count++;
                        mutationOfTransports(chromosome);
                        success = true;
                    }
                    catch (Exception e) {
                        chromosome = copy;
                    }
                }
                totalSuccess = totalSuccess || success;
            }
            if(totalSuccess) {countSuccedd++;}
        }
        System.out.println("do mutation succeed on "+countSuccedd+" chromosomes.");
    }
    private void mutationOfRoute(Chromosome chromosome, boolean toOptimize) {
        Gene[] genes = chromosome.getGenesList();
        for(int i = 0 ; i < genes.length ; i++) {
            ArrayList<TransportNode> route = genes[i].getRoute();
            int vehicleIndex = genes[i].getVehicleIndex();
            int option = new Random().nextInt(2);
            if(option<1) {
                geneticService.shuffleThisRoute(vehicleIndex,route,genes[i].getTransportsIndicesList(),toOptimize);
                geneticService.reassignSubRoute(vehicleIndex,route,genes[i].getTransportsIndicesList(),toOptimize);
                genes[i].setTotalTime(geneticService.caculateTotalTimeOfThisRoute(vehicleIndex,route));
                genes[i].setTotalMautKm(geneticService.caculateTotalMautKmOfThisRoute(vehicleIndex,route));
            }
            else {
                ArrayList<TransportNode> newRoute = geneticService.createTransportsRouteOfThisGene1(vehicleIndex,genes[i].getTransportsIndicesList());
                genes[i].setRoute(newRoute);
                genes[i].setTotalTime(geneticService.caculateTotalTimeOfThisRoute(vehicleIndex,newRoute));
                genes[i].setTotalMautKm(geneticService.caculateTotalMautKmOfThisRoute(vehicleIndex,newRoute));
            }
        }
        geneticService.checkChromosomeIsValid(chromosome);
     }
    private void mutationOfTransports(Chromosome chromosome) {
        geneticService.reassignTransportsBetweenRoutes(chromosome);
        geneticService.checkChromosomeIsValid(chromosome);
    }
    private  void mutationOfDepots(Chromosome chromosome) {
        geneticService.reassignRoutesToBestDepots(chromosome);
        geneticService.checkChromosomeIsValid(chromosome);
     }

    public ArrayList<Chromosome> crossOverStrategy1(ArrayList<Chromosome> parents, int size) {
        ArrayList<Chromosome> child = new ArrayList<>();
        for(int i = 0 ; i < size ; i= i+2) {
            Collections.shuffle(parents);
            Chromosome child1 = geneticService.recombinationOf2Chromosomes(parents.get(0),parents.get(1));
            Chromosome child2 = geneticService.recombinationOf2Chromosomes(parents.get(1),parents.get(0));
            geneticService.checkChromosomeIsValid(child1);
            child.add(child1);
            geneticService.checkChromosomeIsValid(child2);
            child.add(child2);
        }
        System.out.println("DONE CROSSOVER !");
        return child;
    }

    private ArrayList<Chromosome> createInitPopulation(int size) {

        long start = System.currentTimeMillis();
        Chromosome[] population = new Chromosome[size];
        int generatedInvidualCount = 0; // count generated individual
        int firstStrategy = size*20/100;
        int secondStrategy = size*20/100 + size*30/100;
        while (generatedInvidualCount != size) {
            Chromosome individual;
            if(generatedInvidualCount<= firstStrategy) {
                individual = geneticService.createChromosomeBestRouteRandomDepot(1);
            }
            else if(generatedInvidualCount > firstStrategy && generatedInvidualCount <= secondStrategy) {
                individual = geneticService.createChromosomeBestRouteRandomDepot(2);
            }
            else{
                individual = geneticService.createChromosomeBestDepotsRandomRoute();
            }
            geneticService.checkChromosomeIsValid(individual);
            population[generatedInvidualCount] = individual;
            System.out.println("Invidual number "+(generatedInvidualCount+1)+" generated!");
            generatedInvidualCount++;
        }
        long end = System.currentTimeMillis();
        double sum = 0;
        double max = Double.MAX_VALUE;
        double min = 0;
        int indexMax = 0;
        int indexMin = 0;
        for(int i = 0 ; i < population.length ; i++) {
            double optFac = helperService.caculateDistanceToCenter(population[i].getMaxTime(),population[i].getTotalMautKM());
            sum = sum + optFac;
            if(optFac <= max) {
                indexMax = i;
                max = optFac;
            }
            if(optFac > min) {
                indexMin = i;
                min = optFac;
            }
        }
        ArrayList<Chromosome> popList = new ArrayList<>(List.of(population));
        popList.sort((pop1,pop2) ->  { return (int) (helperService.caculateDistanceToCenter(pop1.getMaxTime(),pop1.getTotalMautKM()) - helperService.caculateDistanceToCenter(pop2.getMaxTime(),pop2.getTotalMautKM()));
        });
        System.out.println();
        System.out.println("****** Init Population takes " + (end - start) + " ms ******");
        System.out.println("****** Average fitness :"+ (sum/size) +" *********************************************");
        System.out.println("");
        System.out.println("           BEST : "+max);
        System.out.println("           BEST MAUT KM TOTAL :"+population[indexMax].getTotalMautKM());
        System.out.println("           BEST TIME TOTAL :"+population[indexMax].getMaxTime());
        System.out.println("");
        System.out.println("           MEDIAN :"+ helperService.caculateDistanceToCenter(popList.get(popList.size()/2).getMaxTime(),popList.get(popList.size()/2).getTotalMautKM()));
        System.out.println("");
        System.out.println("           WORST : "+min);
        System.out.println("           WORST MAUT KM TOTAL :"+population[indexMin].getTotalMautKM());
        System.out.println("           WORST TIME TOTAL :"+population[indexMin].getMaxTime());
        System.out.println("");
        System.out.println("****************************************************************************************");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        return new ArrayList<>(Arrays.asList(population));
    }
}

