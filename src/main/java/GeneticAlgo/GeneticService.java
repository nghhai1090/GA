package GeneticAlgo;

import Model.*;
import Service.HelperService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class GeneticService {

    private int[][] distanceMatrix;
    private int[][] mautKmMatrix;
    private Vehicle[] vehiclesArray;
    private Transport[] transportsArray;
    private final HelperService helperService;


    public GeneticService() {
        this.helperService = new HelperService();
    }

    public void setParameters(int[][] distanceMatrix, int[][] mautKmMatrix, Vehicle[] vehiclesArray, Transport[] transportsArray) {
        this.distanceMatrix = distanceMatrix;
        this.mautKmMatrix = mautKmMatrix;
        this.vehiclesArray = vehiclesArray;
        this.transportsArray = transportsArray;
    }


    /**
     * This method generate an individual using strategy 1 or 2. Both will assign all transports to randoms vehicles with respect to their capacity.
     * Strategy 1 builds route continously by inserting each transports node to best suitable position in pre-route.
     * Strategy 2 builds route continously by greedy choosing best next suitable transport node.
     *
     * @param strategy choosen strategy.
     * @return an Individual.
     */
    public Chromosome createChromosomeBestRouteRandomDepot(int strategy) {
        // take all transports and vehicles
        ArrayList<Integer> vehiclesIndicesList = new ArrayList<>();
        for (int i = 0; i < vehiclesArray.length; i++) {
            vehiclesIndicesList.add(i);
        }
        ArrayList<Integer> transportsIndicesList = new ArrayList<>();
        for (int i = 0; i < transportsArray.length; i++) {
            transportsIndicesList.add(i);
        }
        // take some of vehicles
        Collections.shuffle(vehiclesIndicesList);
        int numVehiclesToTake = (int) (Math.random() * vehiclesIndicesList.size() + 1);
        ArrayList<Integer> takenVehiclesIndicesList = new ArrayList<>();
        for (int i = 0; i < numVehiclesToTake; i++) {
            takenVehiclesIndicesList.add(vehiclesIndicesList.remove(0));
        }

        ArrayList<Gene> genesList = new ArrayList<>();
        // for each taken vehicle, assign some transports and create gene if the number of assigned transports is > 0
        for (int v = 0; v < takenVehiclesIndicesList.size(); v++) {

            int takeVehicleCode = takenVehiclesIndicesList.get(v);
            Vehicle vehicle = vehiclesArray[takeVehicleCode];

            ArrayList<Integer> assignedTransportsIndices = new ArrayList<>();
            transportsIndicesList.forEach(index -> {
                if (transportsArray[index].getAmount() <= vehicle.getCap()) {
                    assignedTransportsIndices.add(index); // assign-able
                }
            });

            Collections.shuffle(assignedTransportsIndices);
            int numTransportsToAssign = (int) (Math.random() * assignedTransportsIndices.size() + 1);

            for (int i = 0; i < assignedTransportsIndices.size() - numTransportsToAssign; i++) {
                assignedTransportsIndices.remove(0);
            }

            assignedTransportsIndices.forEach(integer -> {
                transportsIndicesList.removeIf(integer1 -> integer1 == integer);
            });

            if (assignedTransportsIndices.size() != 0) {
                Gene gene = createGene(vehicle.getDepot(), takeVehicleCode, vehicle.getFixCost(), assignedTransportsIndices, strategy);
                genesList.add(gene);
            }
        }

        // assign rest transports to one of the rest vehicle , or to one of the choosen vehicles
        // at this point vehicleIndicesList and transportsCodeList have only unassigned indices of vehicles and transports

        //max load is the maximum load of the unassigned transports
        int maxTransportLoad = 0;
        for (int i = 0; i < transportsIndicesList.size(); i++) {
            maxTransportLoad = Math.max(maxTransportLoad, transportsArray[transportsIndicesList.get(i)].getAmount());
        }

        Collections.shuffle(vehiclesIndicesList);
        Vehicle bonusVehicle = null;
        int bonusVehicleIndex = -1;
        // find bonus vehicle with load >= max load
        for (int i = 0; i < vehiclesIndicesList.size(); i++) {
            if (vehiclesArray[vehiclesIndicesList.get(i)].getCap() >= maxTransportLoad) {
                bonusVehicleIndex = vehiclesIndicesList.get(i);
                bonusVehicle = vehiclesArray[bonusVehicleIndex];
                break;
            }
        }
        // bonus vehicle != null then there is a unused vehicle with load >= maxload -> assign rest transports to this bonus vehicle
        if (bonusVehicle != null) {
            Gene bonusGene = null;
            if (transportsIndicesList.size() != 0) {
                bonusGene = createGene(bonusVehicle.getDepot(), bonusVehicleIndex, bonusVehicle.getFixCost(), transportsIndicesList, strategy);
                genesList.add(bonusGene);
            }
        }
        //when bonus vehicle == null, at least one of the used vehicles muss have load >= max load of unassigned transport
        else {
            // for every unassigned transports, find used the assinable vehicles and assign to one random of them
            for (int i = 0; i < transportsIndicesList.size(); i++) {
                ArrayList<Integer> assignableIndicesInGenes = new ArrayList<>();

                // find all the vehicles on use that have load >= this unassigned transport
                for (int j = 0; j < genesList.size(); j++) {
                    if (vehiclesArray[genesList.get(j).getVehicleIndex()].getCap() >= transportsArray[transportsIndicesList.get(i)].getAmount()) {
                        assignableIndicesInGenes.add(j);
                    }
                }
                // get one random assignable vehicle
                Collections.shuffle(assignableIndicesInGenes);
                Gene choosenGeneToAssign = genesList.get(assignableIndicesInGenes.get(0));
                // assign to vehicle
                ArrayList<Integer> additionalIndex = new ArrayList<>();
                additionalIndex.add(transportsIndicesList.get(i));
                assignMoreTransportsToGene(choosenGeneToAssign, additionalIndex);
            }
        }
        return new Chromosome(genesList.toArray(Gene[]::new));
    }

    /**
     * This method generate an individual using strategy 3. Here, each transport will be assigned to nearest depot.
     * Strategy 3 build route randomly by shuffling the transport nodes.
     *
     * @return an Individual.
     */
    public Chromosome createChromosomeBestDepotsRandomRoute() {
        // map depots with transports and depots with vehicles
        // each transport should be assigned to nearest depot
        // on each depot, a number of vehicles would be choosen.
        HashMap<Integer, ArrayList<Integer>> mapDepotsAndVehicles = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> mapDepotsAndAssignedTransports = new HashMap<>();
        // generate keys (depots) of maps.
        for (int i = 0; i < vehiclesArray.length; i++) {
            mapDepotsAndVehicles.put(vehiclesArray[i].getDepot(), new ArrayList<>());
            mapDepotsAndAssignedTransports.put(vehiclesArray[i].getDepot(), new ArrayList<>());
        }
        // add all vehicles available as values to their corresponding depots
        for (int i = 0; i < vehiclesArray.length; i++) {
            mapDepotsAndVehicles.get(vehiclesArray[i].getDepot()).add(i);
        }
        // assigns transports to best depots
        for (int i = 0; i < transportsArray.length; i++) {
            // get all depots
            HashSet<Integer> depots = new HashSet<>();
            mapDepotsAndVehicles.keySet().forEach(key -> {
                depots.add(key);
            });
            // find best depots of this transport to assign
            ArrayList<Integer> choosenDepots = new ArrayList<>();
            final int[] minDistance = {Integer.MAX_VALUE};
            int finalI = i;
            // find min distance
            depots.forEach(depot -> {
                int totalDistance = distanceMatrix[depot][transportsArray[finalI].getFrom()] + distanceMatrix[transportsArray[finalI].getTo()][depot];
                if (totalDistance < minDistance[0] && transportsArray[finalI].getAmount() <= maxVehicleCap(mapDepotsAndVehicles.get(depot))) {
                    minDistance[0] = totalDistance;
                }
            });
            // find all depot with this min distance and have max vehicle cap bigger than max transport load, then save to choosenDepots List
            depots.forEach(depot -> {
                int totalDistance = distanceMatrix[depot][transportsArray[finalI].getFrom()] + distanceMatrix[transportsArray[finalI].getTo()][depot];
                if (totalDistance == minDistance[0] && transportsArray[finalI].getAmount() <= maxVehicleCap(mapDepotsAndVehicles.get(depot))) {
                    choosenDepots.add(depot);
                }
            });

            // choose one random of best depot and assign transport to it
            Collections.shuffle(choosenDepots);
            mapDepotsAndAssignedTransports.get(choosenDepots.get(0)).add(i);
        }
        ArrayList<Gene> genesList = new ArrayList<>();
        // at each depots, assign transports to available vehicles
        mapDepotsAndAssignedTransports.forEach((depot, assignedTransports) -> {
            // get vehicles at depot
            ArrayList<Integer> indexOfVehiclesAtDepot = mapDepotsAndVehicles.get(depot);
            Collections.shuffle(indexOfVehiclesAtDepot);
            Gene[] genes = new Gene[indexOfVehiclesAtDepot.size()];
            // assigns transports to vehicles
            for (int v = 0; v < indexOfVehiclesAtDepot.size(); v++) {
                int takeVehicleCode = indexOfVehiclesAtDepot.get(v);
                Vehicle vehicle = vehiclesArray[takeVehicleCode];
                ArrayList<Integer> assignedTransportsIndices = assignRandomTransportsToThisVehicle(assignedTransports, vehicle.getCap()); // transportsIndicesList changed
                if (assignedTransportsIndices.size() != 0) {
                    // firstly create gene with emty route, because there will be rest transports to re-assign
                    Gene gene = createGene(depot, takeVehicleCode, vehicle.getFixCost(), assignedTransportsIndices, 3);
                    genes[v] = gene;
                } else {
                    Gene gene = new Gene(vehicle.getDepot(), takeVehicleCode, vehicle.getFixCost());
                    genes[v] = gene;
                }

            }
            // assign rest transports
            while (assignedTransports.size() != 0) {
                for (int i = 0; i < genes.length; i++) {
                    if (vehiclesArray[genes[i].getVehicleIndex()].getCap() >= transportsArray[assignedTransports.get(0)].getAmount()) {
                        genes[i].addToTransportsIndicesList(assignedTransports.get(0));
                        assignedTransports.remove(0);
                        if (assignedTransports.size() == 0) {
                            break;
                        }
                    }
                }
            }
            // create route with strategy 3
            for (int i = 0; i < genes.length; i++) {
                ArrayList<Integer> assignedTransportsOfThisGene = genes[i].getTransportsIndicesList();
                if (assignedTransportsOfThisGene.size() != 0) {
                    ArrayList<TransportNode> route = createTransportsRouteOfThisGene3(genes[i].getVehicleIndex(), assignedTransportsOfThisGene);
                    if (transportsRouteIsInvalid(depot, genes[i].getVehicleIndex(), assignedTransportsOfThisGene, route)) {
                        throw new RuntimeException("ROUTE IS INVALID");
                    }
                    genes[i].setRoute(route);
                    genes[i].setTotalTime(caculateTotalTimeOfThisRoute(genes[i].getVehicleIndex(), route));
                    genes[i].setTotalMautKm(caculateTotalMautKmOfThisRoute(genes[i].getVehicleIndex(), route));
                    genesList.add(genes[i]);
                }
            }
        });
        return new Chromosome(genesList.toArray(new Gene[genesList.size()]));
    }


    public Chromosome recombinationFromGenesArray(Gene[] genesArray) {
        ArrayList<Integer> allTransportsIndices = new ArrayList<>();
        for (int i = 0; i < transportsArray.length; i++) {
            allTransportsIndices.add(transportsArray[i].getCode());
        }
        HashMap<Integer, HashSet<Integer>> vehicleTransportsMap = new HashMap<>();
        for (int i = 0; i < vehiclesArray.length; i++) {
            vehicleTransportsMap.put(vehiclesArray[i].getCode(), new HashSet<>());
        }
        for (int i = 0; i < genesArray.length; i++) {
            vehicleTransportsMap.get(genesArray[i].getVehicleIndex()).addAll(genesArray[i].getTransportsIndicesList());
            allTransportsIndices.removeAll(genesArray[i].getTransportsIndicesList());
        }
        while (allTransportsIndices.size() != 0) {
            int transportIndex = allTransportsIndices.remove(0);
            Set<Integer> vehicles = vehicleTransportsMap.keySet();
            int vehicleIndex = (int) vehicles.stream().filter((key) -> vehiclesArray[key].getCap() >= transportsArray[transportIndex].getAmount()).toArray()[0];
            vehicleTransportsMap.get(vehicleIndex).add(transportIndex);
        }

        HashSet<Integer> assignedSet = new HashSet<>();
        vehicleTransportsMap.forEach((key, value) -> {
            value.removeIf(v -> assignedSet.contains(v));
            assignedSet.addAll(value);
        });

        ArrayList<Gene> genesOfChild = new ArrayList<>();
        vehicleTransportsMap.forEach((key, value) -> {
            if (value.size() != 0) {
                Gene gene = new Gene(vehiclesArray[key].getDepot(), vehiclesArray[key].getCode(), vehiclesArray[key].getFixCost());
                ArrayList<Integer> transports = new ArrayList<>(value);
                gene.setTransportsIndicesList(transports);
                ArrayList<TransportNode> route = createTransportsRouteOfThisGene1(key, transports);
                gene.setRoute(route);
                gene.setTotalTime(caculateTotalTimeOfThisRoute(key, route));
                gene.setTotalMautKm(caculateTotalMautKmOfThisRoute(key, route));
                genesOfChild.add(gene);
            }
        });
        return new Chromosome(genesOfChild.toArray(Gene[]::new));
    }

    public Chromosome recombinationOf2Chromosomes(Chromosome parent1, Chromosome parent2) {
        ArrayList<Integer> allTransportsIndices = new ArrayList<>();
        for (int i = 0; i < transportsArray.length; i++) {
            allTransportsIndices.add(transportsArray[i].getCode());
        }
        int upperBound = Math.max(parent1.getGenesList().length, parent2.getGenesList().length);
        int lowerBound = Math.min(parent1.getGenesList().length, parent2.getGenesList().length);
        int numberOfGenesInChild = new Random().nextInt(upperBound - lowerBound + 1) + lowerBound;
        HashMap<Integer, HashSet<Integer>> vehicleTransportsMap = new HashMap<>();
        for (int i = 0; i < vehiclesArray.length; i++) {
            vehicleTransportsMap.put(vehiclesArray[i].getCode(), new HashSet<>());
        }
        int count1 = 0;
        int count2 = 1;
        while (numberOfGenesInChild != 0) {
            if (numberOfGenesInChild % 2 == 0) {
                int index = Math.min(count1, parent1.getGenesList().length - 1);
                vehicleTransportsMap.get(parent1.getGenesList()[index].getVehicleIndex()).addAll(parent1.getGenesList()[index].getTransportsIndicesList());
                allTransportsIndices.removeAll(parent1.getGenesList()[index].getTransportsIndicesList());
                count1 = count1 + 2;
            } else {
                int index = Math.min(count2, parent2.getGenesList().length - 1);
                vehicleTransportsMap.get(parent2.getGenesList()[index].getVehicleIndex()).addAll(parent2.getGenesList()[index].getTransportsIndicesList());
                allTransportsIndices.removeAll(parent2.getGenesList()[index].getTransportsIndicesList());
                count2 = count2 + 2;
            }
            numberOfGenesInChild--;
        }
        while (allTransportsIndices.size() != 0) {
            int transportIndex = allTransportsIndices.remove(0);
            Set<Integer> vehicles = vehicleTransportsMap.keySet();
            int vehicleIndex = (int) vehicles.stream().filter((key) -> vehiclesArray[key].getCap() >= transportsArray[transportIndex].getAmount()).toArray()[0];
            vehicleTransportsMap.get(vehicleIndex).add(transportIndex);
        }

        HashSet<Integer> assignedSet = new HashSet<>();
        vehicleTransportsMap.forEach((key, value) -> {
            value.removeIf(v -> assignedSet.contains(v));
            assignedSet.addAll(value);
        });

        ArrayList<Gene> genesOfChild = new ArrayList<>();
        vehicleTransportsMap.forEach((key, value) -> {
            if (value.size() != 0) {
                Gene gene = new Gene(vehiclesArray[key].getDepot(), vehiclesArray[key].getCode(), vehiclesArray[key].getFixCost());
                ArrayList<Integer> transports = new ArrayList<>(value);
                gene.setTransportsIndicesList(transports);
                ArrayList<TransportNode> route = createTransportsRouteOfThisGene1(key, transports);
                gene.setRoute(route);
                gene.setTotalTime(caculateTotalTimeOfThisRoute(key, route));
                gene.setTotalMautKm(caculateTotalMautKmOfThisRoute(key, route));
                genesOfChild.add(gene);
            }
        });
        return new Chromosome(genesOfChild.toArray(Gene[]::new));
    }


    /**
     * This Method create a gene corresponding to a strategy. The given strategy will determine how the route of this gene is generated.
     * Strategy 1 random depot, best node position.
     * Strategy 2 random depot, best next node.
     * Strategy 3 route is not yet assigned.
     *
     * @param depot                     the depot
     * @param vehicleIndex              the index of vehicle in vehicle array
     * @param deployCost                the deploy cost of vehicle
     * @param assignedTransportsIndices the transports indices to be assigned to this gene
     * @param strategy                  the strategy
     * @return Gene
     */
    private Gene createGene(int depot, int vehicleIndex, int deployCost, ArrayList<Integer> assignedTransportsIndices, int strategy) {
        Gene gene = new Gene(depot, vehicleIndex, deployCost);
        gene.setTransportsIndicesList(assignedTransportsIndices);
        ArrayList<TransportNode> route;
        if (strategy == 1 || strategy == 2) { // only strategy 1 and 2
            if (strategy == 1) {
                route = createTransportsRouteOfThisGene1(vehicleIndex, assignedTransportsIndices);
            } else {
                route = createTransportsRouteOfThisGene2(vehicleIndex, (ArrayList<Integer>) assignedTransportsIndices.clone());
            }
            if (transportsRouteIsInvalid(depot, vehicleIndex, assignedTransportsIndices, route)) {
                throw new RuntimeException("ROUTE IS INVALID");
            }
            gene.setRoute(route);
            gene.setTotalMautKm(caculateTotalMautKmOfThisRoute(vehicleIndex, route));
            gene.setTotalTime(caculateTotalTimeOfThisRoute(vehicleIndex, route));
        }
        return gene;
    }

    /**
     * this method creates a route where every node has best position
     *
     * @param vehicleIndex              index of vehicle in vehicle array, which is used by this route
     * @param assignedTransportsIndices assigned transports in this route
     * @return a route
     */
    public ArrayList<TransportNode> createTransportsRouteOfThisGene1(int vehicleIndex, ArrayList<Integer> assignedTransportsIndices) {
        ArrayList<TransportNode> route = new ArrayList<>();
        TransportNode start = new TransportNode(-1, false);
        TransportNode end = new TransportNode(-1, false);
        Vehicle vehicle = vehiclesArray[vehicleIndex];
        int vehicleCap = vehicle.getCap();
        int vehicleLoadFactor = vehicle.getLoadFactor();
        int vehicleSpeed = vehicle.getSpeed();
        int depot = vehicle.getDepot();
        route.add(start);
        route.add(end);
        for (int i = 0; i < assignedTransportsIndices.size(); i++) {
            assignThisTransportToRouteInBestPosition(assignedTransportsIndices.get(i), route, vehicleCap, vehicleLoadFactor, vehicleSpeed, depot);
        }
        return route;
    }

    /**
     * this method creates a route where every next node is the best suitable
     *
     * @param vehicleIndex              index of vehicle in vehicle array, which is used by this route
     * @param assignedTransportsIndices assigned transports in this route
     * @return a route
     */
    public ArrayList<TransportNode> createTransportsRouteOfThisGene2(int vehicleIndex, ArrayList<Integer> assignedTransportsIndices) {
        Vehicle vehicle = vehiclesArray[vehicleIndex];
        int actualNode = vehicle.getDepot();
        int speed = vehicle.getSpeed();
        int cap = vehicle.getCap();
        int loadFactor = vehicle.getLoadFactor();
        int load = 0;
        int actualTime = 0;
        TransportNode start = new TransportNode(-1, false);
        TransportNode end = new TransportNode(-1, false);
        ArrayList<TransportNode> route = new ArrayList<>();
        route.add(start);
        ArrayList<Integer> toDeliveryTransportsIndices = new ArrayList<>();
        ArrayList<Integer> pickupTransportsIndices = new ArrayList<>(assignedTransportsIndices);
        Collections.shuffle(pickupTransportsIndices);
        while (assignedTransportsIndices.size() != 0) {
            ObjectivesPoint optimizeValuePickUp = new ObjectivesPoint(Double.MAX_VALUE, Double.MAX_VALUE);
            ObjectivesPoint optimizeValueDelivery = new ObjectivesPoint(Double.MAX_VALUE, Double.MAX_VALUE);
            int choosenPickUpTransportIndex = -1;
            int choosenDeliveryTransportIndex = -1;
            int optTimePickUp = 0;
            int optTimeDelivery = 0;
            for (int i = 0; i < pickupTransportsIndices.size(); i++) {
                if (load + transportsArray[pickupTransportsIndices.get(i)].getAmount() <= cap) {
                    int mautKm = mautKmMatrix[actualNode][transportsArray[pickupTransportsIndices.get(i)].getFrom()];
                    int timePickUp = Math.max(actualTime + distanceMatrix[actualNode][transportsArray[pickupTransportsIndices.get(i)].getFrom()] / speed, transportsArray[pickupTransportsIndices.get(i)].getActiveTimeOfPickup())
                            + transportsArray[pickupTransportsIndices.get(i)].getAmount() / loadFactor;
                    ObjectivesPoint opt = new ObjectivesPoint(mautKm, timePickUp);
                    if (opt.isDominance(optimizeValuePickUp)) {
                        choosenPickUpTransportIndex = pickupTransportsIndices.get(i);
                        optimizeValuePickUp.setToThisPoint(opt);
                        optTimePickUp = timePickUp;
                    }
                }
            }
            for (int i = 0; i < toDeliveryTransportsIndices.size(); i++) {
                int mautKm = mautKmMatrix[actualNode][transportsArray[toDeliveryTransportsIndices.get(i)].getTo()];
                int timeDelivery = Math.max(actualTime + distanceMatrix[actualNode][transportsArray[toDeliveryTransportsIndices.get(i)].getTo()] / speed, transportsArray[toDeliveryTransportsIndices.get(i)].getActiveTimeOfDelivery())
                        + transportsArray[toDeliveryTransportsIndices.get(i)].getAmount() / loadFactor;
                ObjectivesPoint opt = new ObjectivesPoint(mautKm, timeDelivery);
                if (opt.isDominance(optimizeValueDelivery)) {
                    choosenDeliveryTransportIndex = toDeliveryTransportsIndices.get(i);
                    optimizeValueDelivery.setToThisPoint(opt);
                    optTimeDelivery = timeDelivery;
                }
            }
            if (optimizeValuePickUp.isDominance(optimizeValueDelivery)) {
                route.add(new TransportNode(choosenPickUpTransportIndex, true));
                pickupTransportsIndices.remove(Integer.valueOf(choosenPickUpTransportIndex));
                toDeliveryTransportsIndices.add(choosenPickUpTransportIndex);
                load = load + transportsArray[choosenPickUpTransportIndex].getAmount();
                actualTime = optTimePickUp;
            } else {
                route.add(new TransportNode(choosenDeliveryTransportIndex, false));
                toDeliveryTransportsIndices.remove(Integer.valueOf(choosenDeliveryTransportIndex));
                assignedTransportsIndices.remove(Integer.valueOf(choosenDeliveryTransportIndex));
                load = load - transportsArray[choosenDeliveryTransportIndex].getAmount();
                actualTime = optTimeDelivery;
            }
        }
        route.add(end);
        return route;
    }

    /**
     * this method creates a route by firstly assigns transports pickup and delivery points near each other, then shuffle the transports nodes for a given time
     * to optimize the route.
     *
     * @param vehicleIndex              index of vehicle in vehicle array, which is used by this route
     * @param assignedTransportsIndices assigned transports in this route
     * @return a route
     */
    public ArrayList<TransportNode> createTransportsRouteOfThisGene3(int vehicleIndex, ArrayList<Integer> assignedTransportsIndices) {
        ArrayList<TransportNode> route = new ArrayList<>();
        route.add(new TransportNode(-1, false));
        assignedTransportsIndices.forEach(index -> {
            TransportNode pickup = new TransportNode(index, true);
            route.add(pickup);
            TransportNode delivery = new TransportNode(index, false);
            route.add(delivery);
        });
        route.add(new TransportNode(-1, false));
        shuffleThisRoute(vehicleIndex, route, assignedTransportsIndices, true);
        return route;
    }

    /**
     * this method shuffles a route for a given time.
     *
     * @param vehicleIndex index of vehicle in vehicle array, which is used by this route
     * @param route        the route to shuffle
     * @param toOptimize   determine if this method is called to optimize the route
     * @return a new route
     */
    public void shuffleThisRoute(int vehicleIndex, ArrayList<TransportNode> route, ArrayList<Integer> transportsIndices, boolean toOptimize) {
        int vehicleDepot = vehiclesArray[vehicleIndex].getDepot();
        int vehicleSpeed = vehiclesArray[vehicleIndex].getSpeed();
        int vehicleLoadFactor = vehiclesArray[vehicleIndex].getLoadFactor();
        int vehicleCap = vehiclesArray[vehicleIndex].getCap();

        for (int i = 0; i < route.size(); i++) {
            PickUpDeliveryIndexValue posiblePosition = null;
            double time = caculateTotalTimeOfThisRoute(vehicleIndex, route);
            double mautKm = caculateTotalMautKmOfThisRoute(vehicleIndex, route);
            ObjectivesPoint routeOldValue = new ObjectivesPoint(mautKm, time);
            ArrayList<Integer> pickUpIndices = new ArrayList<>();
            for (int j = 1; j < route.size() - 1; j++) {
                if (route.get(j).isPickUp()) {
                    pickUpIndices.add(j);
                }
            }
            Collections.shuffle(pickUpIndices);
            ArrayList<TransportNode> examRoute = new ArrayList<>(route);
            int randomPickUpIndex = pickUpIndices.get(0);
            int correspondingDeliveryIndex = -1;
            for (int r = 0; r < route.size() - 1; r++) {
                if (route.get(r).getTransportIndex() == route.get(randomPickUpIndex).getTransportIndex() && !route.get(r).isPickUp()) {
                    correspondingDeliveryIndex = r;
                    break;
                }
            }
            int swapPositionPickUp = helperService.generateNRandomNumberBetween(examRoute.size() - 2, 1, 1).get(0);
            Collections.swap(examRoute, randomPickUpIndex, swapPositionPickUp);
            if (swapPositionPickUp != route.size() - 2) {
                int swapPositionDelivery = helperService.generateNRandomNumberBetween(examRoute.size() - 2, swapPositionPickUp + 1, 1).get(0);
                Collections.swap(examRoute, correspondingDeliveryIndex, swapPositionDelivery);
                if (!transportsRouteIsInvalid(vehicleDepot, vehicleIndex, transportsIndices, examRoute)) {
                    ObjectivesPoint valueAfterSwap = caculateValueOfTransportsRoute(vehicleCap, vehicleSpeed, vehicleLoadFactor, vehicleDepot, examRoute);
                    posiblePosition = new PickUpDeliveryIndexValue(randomPickUpIndex, swapPositionPickUp, correspondingDeliveryIndex, swapPositionDelivery, valueAfterSwap);
                }
            } else {
                TransportNode delivery = examRoute.remove(correspondingDeliveryIndex);
                examRoute.add(examRoute.size() - 1, delivery);
                if (!transportsRouteIsInvalid(vehicleDepot, vehicleIndex, transportsIndices, examRoute)) {
                    ObjectivesPoint valueAfterSwap = caculateValueOfTransportsRoute(vehicleCap, vehicleSpeed, vehicleLoadFactor, vehicleDepot, examRoute);
                    posiblePosition = new PickUpDeliveryIndexValue(randomPickUpIndex, swapPositionPickUp, correspondingDeliveryIndex, route.size(), valueAfterSwap);
                }
            }
            if (posiblePosition != null) {
                if (toOptimize) {
                    if (posiblePosition.getValue().isDominance(routeOldValue)) {
                        Collections.swap(route, posiblePosition.getPickUpIndex(), posiblePosition.getPickUpIndexSwap());
                        if (posiblePosition.getDeliveryIndexSwap() != route.size()) {
                            Collections.swap(route, posiblePosition.getDeliveryIndex(), posiblePosition.getDeliveryIndexSwap());
                        } else {
                            TransportNode delivery = route.remove(posiblePosition.getDeliveryIndex());
                            route.add(route.size() - 1, delivery);
                        }
                    }
                } else {
                    if (!routeOldValue.isDominance(posiblePosition.getValue())) {
                        Collections.swap(route, posiblePosition.getPickUpIndex(), posiblePosition.getPickUpIndexSwap());
                        if (posiblePosition.getDeliveryIndexSwap() != route.size()) {
                            Collections.swap(route, posiblePosition.getDeliveryIndex(), posiblePosition.getDeliveryIndexSwap());
                        } else {
                            TransportNode delivery = route.remove(posiblePosition.getDeliveryIndex());
                            route.add(route.size() - 1, delivery);
                        }
                    }
                }
            }
        }
    }

    /**
     * this method reassign a random sub-route to optimize the route.
     *
     * @param vehicleIndex      index of vehicle in vehicle array, which is used by this route
     * @param route             the route
     * @param transportsIndices the assigned Transports Indices
     * @param toOptimize        determine if this method is called to optimize the route
     * @return a new route
     */
    public void reassignSubRoute(int vehicleIndex, ArrayList<TransportNode> route, ArrayList<Integer> transportsIndices, boolean toOptimize) {
        ArrayList<TransportNode> subRoute = new ArrayList<>();
        ArrayList<Integer> pickupIndices = new ArrayList<>();
        Vehicle vehicle = vehiclesArray[vehicleIndex];
        ObjectivesPoint originalValue = caculateValueOfTransportsRoute(vehicle.getCap(), vehicle.getSpeed(), vehicle.getLoadFactor(), vehicle.getDepot(), route);


        for (int i = 1; i < route.size() - 1; i++) {
            if (route.get(i).isPickUp()) {
                pickupIndices.add(i);
            }
        }
        Collections.shuffle(pickupIndices);
        int transportsIndex = route.get(pickupIndices.get(0)).getTransportIndex();
        for (int i = pickupIndices.get(0); i < route.size() - 1; i++) {
            subRoute.add(route.get(i));
            if (route.get(i).getTransportIndex() == transportsIndex && !route.get(i).isPickUp()) {
                break;
            }
        }
        route.removeAll(subRoute);
        ArrayList<TransportNode> copyOfRoute = (ArrayList<TransportNode>) route.clone();
        ObjectivesPoint maxValue = new ObjectivesPoint(Double.MAX_VALUE, Double.MAX_VALUE);
        int index = 0;
        ArrayList<Integer> assignableIndices = new ArrayList<>();
        for (int i = 1; i < copyOfRoute.size(); i++) {
            copyOfRoute.addAll(i, subRoute);
            ObjectivesPoint value = caculateValueOfTransportsRoute(vehicle.getCap(), vehicle.getSpeed(), vehicle.getLoadFactor(), vehicle.getDepot(), copyOfRoute);
            if (!transportsRouteIsInvalid(vehicle.getDepot(), vehicleIndex, transportsIndices, copyOfRoute)) {
                if (toOptimize) {
                    if (value.isDominance(maxValue)) {
                        index = i;
                        maxValue.setToThisPoint(value);
                    }
                    else if(!maxValue.isDominance(value)) {
                        boolean take = new Random().nextBoolean();
                        if(take) {
                            index = i;
                            maxValue.setToThisPoint(value);
                        }
                    }
                } else {
                    assignableIndices.add(i);
                }
            }
            copyOfRoute.removeAll(subRoute);
        }
        if (toOptimize) {
            if (!originalValue.isDominance(maxValue)) {
                route.addAll(index, subRoute);
            }
        } else {
            Collections.shuffle(assignableIndices);
            route.addAll(assignableIndices.get(0), subRoute);
        }
    }

    public void reassignRoutesToBestDepots(Chromosome chromosome) {
        ArrayList<Vehicle> vehicleList = new ArrayList<>();
        Collections.addAll(vehicleList, vehiclesArray);
        Gene[] genesArray = chromosome.getGenesList();

        for (int i = 0; i < genesArray.length; i++) {
            Gene gene = genesArray[i];
            ObjectivesPoint bestValue = new ObjectivesPoint(Double.MAX_VALUE, Double.MAX_VALUE);
            int choosenVehicleIndex = -1;
            for (int j = 0; j < vehicleList.size(); j++) {
                Vehicle vehicle = vehicleList.get(j);
                ArrayList<TransportNode> route = gene.getRoute();
                ObjectivesPoint value = caculateValueOfTransportsRoute(vehicle.getCap(), vehicle.getSpeed(), vehicle.getLoadFactor(), vehicle.getDepot(), route);
                if (value.isValid()) {
                    if (value.isDominance(bestValue)) {
                        bestValue.setToThisPoint(value);
                        choosenVehicleIndex = j;
                    }
                    else if(!bestValue.isDominance(value)) {
                        boolean take = new Random().nextBoolean();
                        if(take) {
                            bestValue.setToThisPoint(value);
                            choosenVehicleIndex = j;
                        }
                    }
                }
            }
            if (choosenVehicleIndex != -1) {
                Vehicle choosenVehicle = vehicleList.get(choosenVehicleIndex);
                gene.setDepot(choosenVehicle.getDepot());
                gene.setVehicleIndex(choosenVehicle.getCode());

                gene.setDeployCost(choosenVehicle.getFixCost());
                gene.setTotalMautKm(caculateTotalMautKmOfThisRoute(choosenVehicle.getCode(), gene.getRoute()));
                gene.setTotalTime(caculateTotalTimeOfThisRoute(choosenVehicle.getCode(), gene.getRoute()));
                vehicleList.remove(choosenVehicle);
            }
        }
    }

    public void reassignTransportsBetweenRoutes(Chromosome chromosome) {
        Random rand = new Random();
        if (chromosome.getGenesList().length > 1) {
            int numberOfReassignments = rand.nextInt(chromosome.getGenesList().length - 1) + 1;
            while (numberOfReassignments != 0) {
                int choosenIndex = rand.nextInt(chromosome.getGenesList().length);
                Gene choosenGene = chromosome.getGenesList()[choosenIndex];
                if (choosenGene.getTransportsIndicesList().size() > 0) {
                    int choosenIndexOfTransportsList = rand.nextInt(choosenGene.getTransportsIndicesList().size());
                    int removedTransport = choosenGene.getTransportsIndicesList().get(choosenIndexOfTransportsList);
                    removeTransportFromGene(choosenGene, removedTransport);
                    ArrayList<Integer> assignableGenes = new ArrayList<>();
                    for (int i = 0; i < chromosome.getGenesList().length; i++) {
                        if (vehiclesArray[chromosome.getGenesList()[i].getVehicleIndex()].getCap() >= transportsArray[removedTransport].getAmount()) {
                            assignableGenes.add(i);
                        }
                    }
                    Collections.shuffle(assignableGenes);
                    Gene assignable = chromosome.getGenesList()[assignableGenes.get(0)];
                    assignable.addToTransportsIndicesList(removedTransport);
                    ArrayList<TransportNode> route = createTransportsRouteOfThisGene1(assignable.getVehicleIndex(), assignable.getTransportsIndicesList());
                    assignable.setRoute(route);
                    assignable.setTotalTime(caculateTotalTimeOfThisRoute(choosenGene.getVehicleIndex(), route));
                    assignable.setTotalMautKm(caculateTotalMautKmOfThisRoute(choosenGene.getVehicleIndex(), route));
                }
                numberOfReassignments--;
            }
            ArrayList<Gene> notEmptyGenes = new ArrayList<>();
            Gene[] genes = chromosome.getGenesList();
            for (int i = 0; i < genes.length; i++) {
                if (genes[i].getRoute().size() > 2) {
                    notEmptyGenes.add(genes[i]);
                }
            }
            chromosome.setGenesList(notEmptyGenes.toArray(Gene[]::new));
        }
    }

    private void removeTransportFromGene(Gene choosenGene, int removedTransportIndex) {
        Vehicle v = vehiclesArray[choosenGene.getVehicleIndex()];
        choosenGene.getTransportsIndicesList().remove(Integer.valueOf(removedTransportIndex));
        ArrayList<TransportNode> route = createTransportsRouteOfThisGene1(choosenGene.getVehicleIndex(), choosenGene.getTransportsIndicesList());
        choosenGene.setRoute(route);
        choosenGene.setTotalTime(caculateTotalTimeOfThisRoute(choosenGene.getVehicleIndex(), route));
        choosenGene.setTotalMautKm(caculateTotalMautKmOfThisRoute(choosenGene.getVehicleIndex(), route));
    }

    private void assignMoreTransportsToGene(Gene gene, ArrayList<Integer> moreTransportsAssignmentsIndices) {
        ArrayList<TransportNode> route = gene.getRoute();
        Vehicle vehicle = vehiclesArray[gene.getVehicleIndex()];
        int vehicleCap = vehicle.getCap();
        int vehicleLoadFactor = vehicle.getLoadFactor();
        int vehicleSpeed = vehicle.getSpeed();
        int depot = vehicle.getDepot();

        for (int i = 0; i < moreTransportsAssignmentsIndices.size(); i++) {
            int transportIndex = moreTransportsAssignmentsIndices.get(i);
            gene.addToTransportsIndicesList(transportIndex);
            assignThisTransportToRouteInBestPosition(transportIndex, route, vehicleCap, vehicleLoadFactor, vehicleSpeed, depot);
        }
        gene.setTotalTime(caculateTotalTimeOfThisRoute(gene.getVehicleIndex(), route));
        gene.setTotalMautKm(caculateTotalMautKmOfThisRoute(gene.getVehicleIndex(), route));
    }

    private void assignThisTransportToRouteInBestPosition(int assignedTransportsIndex, ArrayList<TransportNode> route, int vehicleCap, int vehicleLoadFactor, int vehicleSpeed, int depot) {
        TransportNode pickup = new TransportNode(assignedTransportsIndex, true);
        TransportNode delivery = new TransportNode(assignedTransportsIndex, false);
        ArrayList<TransportNode> examRoute = new ArrayList<>(route);
        ArrayList<PickUpDeliveryIndexValue> posiblePositions = new ArrayList<>();
        ArrayList<Integer> totalLoadTilThisNode = new ArrayList<>();
        totalLoadTilThisNode.add(0);
        int load = 0;
        for (int i = 1; i < route.size() - 1; i++) {
            TransportNode node = route.get(i);
            if (i != route.size() - 1) {
                load = getLoad(load, node);
                totalLoadTilThisNode.add(load);
            }
        }

        totalLoadTilThisNode.add(0);
        for (int p = 1; p < Math.max(2, examRoute.size() - 1); p++) {
            if (totalLoadTilThisNode.get(p - 1) + transportsArray[pickup.getTransportIndex()].getAmount() <= vehicleCap) {
                int assignableDeliveryIndexBoundBeforeVisitedPickUp = p;
                for (int i = p; i < totalLoadTilThisNode.size() - 1; i++) {
                    if (totalLoadTilThisNode.get(i) + transportsArray[pickup.getTransportIndex()].getAmount() <= vehicleCap) {
                        assignableDeliveryIndexBoundBeforeVisitedPickUp = i;
                    } else {
                        i = totalLoadTilThisNode.size();
                    }
                }
                examRoute.add(p, pickup);
                for (int d = p + 1; d <= Math.max(2, assignableDeliveryIndexBoundBeforeVisitedPickUp + 1); d++) {
                    examRoute.add(d, delivery);
                    ObjectivesPoint routeValue = caculateValueOfTransportsRoute(vehicleCap, vehicleSpeed, vehicleLoadFactor, depot, examRoute);
                    examRoute.remove(d);
                    if (routeValue.isValid()) {
                        posiblePositions.add(new PickUpDeliveryIndexValue(p, d, routeValue));
                    } else {
                        d = examRoute.size();
                    }

                }
                examRoute.remove(p);
            }
        }
        ArrayList<PickUpDeliveryIndexValue> bestPositions = new ArrayList<>();
        ArrayList<ChromosomeKeyObjectiveValue> toSort= new ArrayList<>();
        AtomicInteger index = new AtomicInteger();
        posiblePositions.forEach(a->{
            toSort.add(new ChromosomeKeyObjectiveValue(index.get(),a.getValue()));
            index.getAndIncrement();
        });
        ArrayList<ArrayList<ChromosomeKeyObjectiveValue>> fronts = helperService.nonDominanceSort(toSort);
        fronts.get(0).forEach(f -> {
            bestPositions.add(posiblePositions.get(f.getChromosomeKey()));
        });
        Collections.shuffle(bestPositions);
        int pickupIndex = bestPositions.get(0).getPickUpIndex();
        int deliveryIndex = bestPositions.get(0).getDeliveryIndex();
        route.add(pickupIndex, pickup);
        route.add(deliveryIndex, delivery);
    }

    private ArrayList<Integer> assignRandomTransportsToThisVehicle(ArrayList<Integer> transportsIndicesList, int vehicleCap) {
        ArrayList<Integer> assignedIndicesList = new ArrayList<>();
        transportsIndicesList.forEach(index -> {
            if (transportsArray[index].getAmount() <= vehicleCap) {
                assignedIndicesList.add(index); // assign-able
            }
        });
        Collections.shuffle(assignedIndicesList);
        int numTransportsToAssign = (int) (Math.random() * assignedIndicesList.size() + 1);
        for (int i = 0; i < assignedIndicesList.size() - numTransportsToAssign; i++) {
            assignedIndicesList.remove(0);
        }
        assignedIndicesList.forEach(integer -> {
            transportsIndicesList.removeIf(integer1 -> integer1 == integer);
        });
        return assignedIndicesList;
    }

    private boolean transportsRouteIsInvalid(int depot, int vehicleIndex, ArrayList<Integer> assignedTransportsIndices, ArrayList<TransportNode> route) {
        ObjectivesPoint obj = caculateValueOfTransportsRoute(vehiclesArray[vehicleIndex].getCap(), vehiclesArray[vehicleIndex].getSpeed(), vehiclesArray[vehicleIndex].getLoadFactor(), depot, route);
        return !obj.isValid()
                || !checkTransportRouteHasAllAssignments(route, assignedTransportsIndices);
    }

    private ObjectivesPoint caculateValueOfTransportsRoute(int vehicleCap, int vehicleSpeed, int vehicleLoadFactor, int depot, ArrayList<TransportNode> route) {
        int totalTime = 0;
        int totalMautKm = 0;
        int load = 0;
        int previous = -1;
        int actual = -1;
        int activeTime = 0;
        if (route.get(route.size() - 1).getTransportIndex() != -1 || route.get(0).getTransportIndex() != -1) {
            //System.out.println("return to depot violated");
            return new ObjectivesPoint(-1, -1);
        }


        for (int i = 1; i < route.size(); i++) {
            TransportNode node = route.get(i);
            if (i != route.size() - 1) {
                if (node.isPickUp()) {
                    boolean found = false;
                    for (int j = i + 1; j < route.size(); j++) {
                        if (route.get(j).getTransportIndex() == node.getTransportIndex() && !route.get(j).isPickUp()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return new ObjectivesPoint(-1, -1);
                    }
                } else {
                    boolean found = false;
                    for (int j = i - 1; j > 0; j--) {
                        if (route.get(j).getTransportIndex() == node.getTransportIndex() && route.get(j).isPickUp()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return new ObjectivesPoint(-1, -1);
                    }
                }
            }
            previous = getPrevious(route, depot, i);
            actual = getActual(route, depot, i, node);
            activeTime = getActiveTime(route, i, node);
            if (i != route.size() - 1) {
                load = getLoad(load, node);
                if (load > vehicleCap) {
                    //System.out.println(oldLoad);
                    //System.out.println(node);
                    //System.out.println(route.get(i+1));
                    //System.out.println("load violation"+load+" cap "+vehicleCap +"at"+i+" node"+node);
                    return new ObjectivesPoint(-1, -1);
                }
            }
            int rideTime = distanceMatrix[previous][actual] / vehicleSpeed;
            int time = Math.max(totalTime + rideTime, activeTime);
            int serviceTime = 0;
            if (i != route.size() - 1) {
                serviceTime = transportsArray[node.getTransportIndex()].getAmount() / vehicleLoadFactor;
            }
            totalTime = time + serviceTime;
            totalMautKm = totalMautKm + mautKmMatrix[previous][actual];
        }

        return new ObjectivesPoint(totalMautKm, totalTime);
    }

    private boolean checkTransportRouteHasAllAssignments(ArrayList<TransportNode> route, ArrayList<Integer> transportsIndices) {
        ArrayList<Integer> toTest = new ArrayList<>(transportsIndices);
        ArrayList<Integer> toTest2 = new ArrayList<>(transportsIndices);
        for (int i = 1; i < route.size() - 1; i++) {
            if (route.get(i).isPickUp()) {
                if (toTest.contains(route.get(i).getTransportIndex())) {
                    toTest.remove(Integer.valueOf(route.get(i).getTransportIndex()));
                } else {
                    System.out.println("wrong");
                    return false;
                }
            }
        }
        return toTest.size() == 0;
    }

    public void checkChromosomeIsValid(Chromosome chromosome) {
        ArrayList<Integer> transports = new ArrayList<>();
        HashSet<Integer> vehicles = new HashSet<>();
        Gene[] genes = chromosome.getGenesList();
        for (int i = 0; i < genes.length; i++) {
            ArrayList<Integer> tr = genes[i].getTransportsIndicesList();
            transports.addAll(tr);
            Vehicle vehicle = vehiclesArray[genes[i].getVehicleIndex()];

            if (transportsRouteIsInvalid(vehicle.getDepot(), vehicle.getCode(), genes[i].getTransportsIndicesList(), genes[i].getRoute())) {
                throw new RuntimeException("ROUTE INVALID");
            }

            if (vehicles.contains(genes[i].getVehicleIndex())) {
                throw new RuntimeException("there are vehicle which is used more than 1 time");// 1 vehicle 2 times
            }
            vehicles.add(genes[i].getVehicleIndex());
            for (int j = 0; j < tr.size(); j++) {
                if (transportsArray[tr.get(j)].getAmount() > vehiclesArray[genes[i].getVehicleIndex()].getCap()) {
                    throw new RuntimeException("there are transport assigned to wrong vehicle");// assigned transport bigger load than cap
                }
            }
        }
        for (int i = 0; i < transportsArray.length; i++) {
            int count = 0;
            for (int j = 0; j < transports.size(); j++) {
                if (transports.get(j) == i) {
                    count++;
                }
            }
            if (count < 1) {
                throw new RuntimeException("there are unassigned transports" + transports + " " + transports.size() + " " + i);
            }
            if (count > 1) {
                throw new RuntimeException("there are transport that assigned more than 1 time");
            }
        }
    }

    public int caculateTotalTimeOfThisRoute(int vehicleIndex, ArrayList<TransportNode> route) {
        int totalTime = 0;
        int previous = -1;
        int actual = -1;
        int activeTime = 0;
        Vehicle vehicle = vehiclesArray[vehicleIndex];
        int vehicleSpeed = vehicle.getSpeed();
        int vehicleLoadFactor = vehicle.getLoadFactor();
        int depot = vehicle.getDepot();
        for (int i = 1; i < route.size(); i++) {
            TransportNode node = route.get(i);
            previous = getPrevious(route, depot, i);
            actual = getActual(route, depot, i, node);
            activeTime = getActiveTime(route, i, node);
            int rideTime = distanceMatrix[previous][actual] / vehicleSpeed;
            int time = Math.max(totalTime + rideTime, activeTime);
            int serviceTime = 0;
            if (i != route.size() - 1) {
                serviceTime = transportsArray[node.getTransportIndex()].getAmount() / vehicleLoadFactor;
            }
            totalTime = time + serviceTime;
        }
        return totalTime;

    }

    public int caculateTotalMautKmOfThisRoute(int vehicleIndex, ArrayList<TransportNode> route) {
        int totalMautKm = 0;
        int previous = -1;
        int actual = -1;
        Vehicle vehicle = vehiclesArray[vehicleIndex];
        int depot = vehicle.getDepot();
        for (int i = 1; i < route.size(); i++) {
            TransportNode node = route.get(i);
            previous = getPrevious(route, depot, i);
            actual = getActual(route, depot, i, node);
            totalMautKm = totalMautKm + mautKmMatrix[previous][actual];
        }
        return totalMautKm;
    }

    private int getLoad(int load, TransportNode node) {
        if (node.isPickUp()) {
            load = load + transportsArray[node.getTransportIndex()].getAmount();
        } else {
            load = load - transportsArray[node.getTransportIndex()].getAmount();
        }
        return load;
    }

    private int getActiveTime(ArrayList<TransportNode> route, int i, TransportNode node) {
        int activeTime;
        if (i != route.size() - 1) {
            if (node.isPickUp()) {
                activeTime = transportsArray[node.getTransportIndex()].getActiveTimeOfPickup();
            } else {
                activeTime = transportsArray[node.getTransportIndex()].getActiveTimeOfDelivery();
            }
        } else {
            activeTime = 0;
        }
        return activeTime;
    }

    private int maxVehicleCap(ArrayList<Integer> vehiclesIndicesList) {
        AtomicInteger max = new AtomicInteger();
        vehiclesIndicesList.forEach(i -> {
            if (vehiclesArray[i].getCap() > max.get()) {
                max.set(vehiclesArray[i].getCap());
            }
        });
        return max.get();
    }

    private int getActual(ArrayList<TransportNode> route, int depot, int i, TransportNode node) {
        int actual;
        if (i != route.size() - 1) {
            if (node.isPickUp()) {
                actual = transportsArray[node.getTransportIndex()].getFrom();
            } else {
                actual = transportsArray[node.getTransportIndex()].getTo();
            }
        } else {
            actual = depot;
        }
        return actual;
    }

    private int getPrevious(ArrayList<TransportNode> route, int depot, int i) {
        int previous;
        if (i != 1) {
            if (route.get(i - 1).isPickUp()) {
                previous = transportsArray[route.get(i - 1).getTransportIndex()].getFrom();
            } else {
                previous = transportsArray[route.get(i - 1).getTransportIndex()].getTo();
            }
        } else {
            previous = depot;
        }
        return previous;
    }

}
