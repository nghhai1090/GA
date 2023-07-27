package GeneticAlgo;

class Chromosome {
    private Gene[] genesList;

    public Chromosome(Gene[] genesList) {
        this.genesList = genesList;
    }

    public int getMaxTime() {
        int max = 0;
        for (int i = 0; i < genesList.length; i++) {
            max = Math.max(max, genesList[i].getTotalTime());
        }
        return max;
    }

    public int getTotalDeployCost() {
        int sum = 0;
        for (int i = 0; i < genesList.length; i++) {
            sum = sum + genesList[i].getDeployCost();
        }
        return sum;
    }

    public int getTotalMautKM() {
        int sum = 0;
        for (int i = 0; i < genesList.length; i++) {
            sum = sum + genesList[i].getTotalMautKm();
        }
        return sum;
    }

    public Gene[] getGenesList() {
        return genesList;
    }

    public void setGenesList(Gene[] genesList) {
        this.genesList = genesList;
    }

    public Chromosome clone() {
        Gene[] geneClone = new Gene[genesList.length];
        for(int i = 0 ; i < genesList.length ; i++) {
            Gene gene = new Gene(genesList[i].getDepot(),genesList[i].getVehicleIndex(),genesList[i].getDeployCost());
            gene.setTotalMautKm(genesList[i].getTotalMautKm());
            gene.setTotalTime(genesList[i].getTotalTime());
            gene.setRoute(genesList[i].getRoute());
            gene.setTransportsIndicesList(genesList[i].getTransportsIndicesList());
            geneClone[i] = gene;
        }
        return new Chromosome(geneClone);
    }

    public String toString() {
        String newLine = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder()
                .append("* CHROMOSOME INFO: ")
                .append(newLine)
                .append("NUM OF GENES : " + genesList.length);

        for (int i = 0; i < genesList.length; i++) {
            builder.append("   GENE NUM " + (i + 1) + ":");
            builder.append(newLine);
            builder.append("   VEHICLE " + genesList[i].getVehicleIndex());
            builder.append(newLine);
            builder.append("   DEPOT " + genesList[i].getDepot());
            builder.append(newLine);
            builder.append("   TRANSPORTS " + genesList[i].getTransportsIndicesList().toString());
            builder.append(newLine);
            builder.append("   ROUTE " + genesList[i].getRoute().toString());
            builder.append(newLine);
            builder.append(" total time " + genesList[i].getTotalTime());
            builder.append(newLine);
            builder.append(" total maut km " + genesList[i].getTotalMautKm());
            builder.append(newLine);
        }
        builder.append(newLine);
        builder.append("*");
        return builder.toString();
    }
}
