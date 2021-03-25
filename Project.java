import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.*;



public class Project {


    public static void main(String[] args) {
    
            /*
            initializing the readers and writers of the files
             */
        BufferedReader inputStream=null;
        try {
            inputStream = new BufferedReader(new FileReader(args[0]));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        OutputStream outputStream=null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(args[1]));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        PrintWriter fileOut = new PrintWriter(outputStream);

        /*Using the class output to print simultaneously to the the chosen output file and to the console*/
        Output out =  new Output(fileOut);

        Experiment exp = new Experiment(inputStream, out);
        try {
            /* calling the experiment method to initiate the experiment */
            exp.experiment();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        fileOut.close();
    }


    /**
     * This class is used to conduct the measurements for the searching algorithms.
     * It hosts the searching algorithms and the methods for the whole experiment.
     */
    static class Experiment {
        BufferedReader in;
        Output out;
        Vertex source;
        Vertex destination;
        static HashMap<String, Edge> roads = new HashMap<>();
        /*the current day of the experiment.It is initialized at -1 because it is used in such way
        * during the initialization of the vertex heuristics
        */
        static int day = -1;
        HashMap<String, Vertex> vertexMap ;
        int numOfCorrectPredictions;

        Experiment(BufferedReader in, Output out) {
            this.in = in;
            this.out = out;
            source = null;
            destination = null;
            vertexMap = new HashMap<>();
            numOfCorrectPredictions=0;
        }

        public void experiment() throws IOException {

            constructGraph();

            //initialization of predictions and actual outcomes of road traffic status
            initializeTraffic(false);
            initializeTraffic(true);
            initializeVertexHeuristics();
            day++;

            float sumOfAllRealCosts=0;
            float[] monthlySumOfRealCosts=new float[4];
            int count=0;
            out.println("=================================================");
            for (int i = 0; i < 80; i++) {
                out.println("Day " + (i + 1));
                out.print("<Uninformed Search Algorithm>:");
                Instant start = Instant.now();
                SearchNode goal = UCS(source, destination.name);
                Instant end = Instant.now();
                Duration duration= Duration.between(start,end);
                out.println("\t" +"Excecution time(μs): "+(double)(duration.toNanos()/1000));
                out.println("\t" +"Visited Nodes number: " + goal.numOfExpandedNodes);
                out.println("\t" + getPathInfo(goal));
                out.println("\t" +"Predicted Cost: " + goal.predictedCost);
                out.println("\t" +"Real Cost:" + goal.realCost);


                out.println("IDA*:");
                start=Instant.now();
                goal = IDA(source, destination.name);
                end = Instant.now();
                duration= Duration.between(start,end);
                out.println("\t" + "Excecution time(μs): "+(double)(duration.toNanos()/1000));
                out.println("\t" +"Visited Nodes number: " + goal.numOfExpandedNodes);
                out.println("\t" + getPathInfo(goal));
                out.println("\t" +"Predicted Cost: " + goal.predictedCost);
                out.println("\t" +"Real Cost:" + goal.realCost);

                sumOfAllRealCosts+=goal.realCost;
                monthlySumOfRealCosts[count]+= goal.realCost;

                if (i < 79) {
                    day++;
                    /*updating the statistics for the day that had just passed*/
                    Probabilities.probs.computeDailyProbabilities(roads,day);

                }

                out.println("");

                if(i==19 || i==39 || i==59 || i==79 )
                    count++;

            }
            out.println("=================================================");

            out.println("");
            out.println("Frequency of correct predictions: " + (float)numOfCorrectPredictions/((float) 80* roads.size()));
            out.println("**************************************************");
            out.println("Average real path cost for days 1-20 :" + (monthlySumOfRealCosts[0]/20));
            out.println("**************************************************");
            out.println("Average real path cost for days 21-40 :" + (monthlySumOfRealCosts[1]/20));
            out.println("**************************************************");
            out.println("Average real path cost for days 41-60 :" + (monthlySumOfRealCosts[2]/20));
            out.println("**************************************************");
            out.println("Average real path cost for days 61-80 :" + (monthlySumOfRealCosts[3]/20));
            out.println("**************************************************");
            out.println("Average real path cost for the 3 months:" + (sumOfAllRealCosts/80));
            out.println("**************************************************");

        }


        void initializeVertexHeuristics() {
            for (Vertex start : vertexMap.values())
                start.heuristic = UCS(start, destination.name).predictedCost;
        }


        String getPathInfo(SearchNode goal) {
            LinkedList<SearchNode> nodeList = new LinkedList<>();
            SearchNode node = goal;
            while (node.parentNode != null) {
                nodeList.addFirst(node);
                node = node.parentNode;
            }
            nodeList.addFirst(node);

            StringBuilder pathInfo= new StringBuilder();


            for (int i = 0; i < nodeList.size() - 1; i++) {
                pathInfo.append(nodeList.get(i).getName());
                pathInfo.append("(");
                pathInfo.append(nodeList.get(i+1).roadCostToHere);
                pathInfo.append( " ) -> ");
            }
            pathInfo.append(nodeList.get(nodeList.size() - 1).getName()  );

            return pathInfo.toString();
        }


        void initializeTraffic(boolean actualTraffic) throws IOException {

            //choosing whether to initialize predictions or actualTrafficPerDay
            String fileBound;
            if (actualTraffic)
                fileBound = "</ActualTrafficPerDay>";
            else
                fileBound = "</Predictions>";

            //skip lines <Predictions> and <Day>
            in.readLine();
            in.readLine();

            String prediction = in.readLine();
            while (!prediction.equals(fileBound)) {
                while (!prediction.equals("</Day>")) {
                    //splitting string based on ';' character
                    String[] lineTokens = prediction.replaceAll(" ", "").split(";");
                    if (lineTokens.length > 1) {
                        String roadName = lineTokens[0];
                        String trafficState = lineTokens[1];
                        //adding predictions to the history of each road
                        roads.get(roadName).addToTrafficHistory(trafficState, actualTraffic);
                    }
                    //read next line
                    prediction = in.readLine();
                }
                //skip <Day>
                prediction = in.readLine();
            }
        }


        void constructGraph() throws IOException {

            //aquiring and creating source vertex from file
            String sourceName = in.readLine();
            sourceName = sourceName.substring(8, sourceName.length() - 9);
            source = new Vertex(sourceName);

            //aquiring and creating destination vertex from file
            String destinationName = in.readLine();
            destinationName = destinationName.substring(13, destinationName.length() - 14);
            destination = new Vertex(destinationName);


            vertexMap.put(sourceName, source);
            vertexMap.put(destinationName, destination);

            //bypassing the <Roads> string in the file
            in.readLine();
            //reading all lines until line == </Roads> to get the graph info
            String fileLine = in.readLine().replaceAll(" ", "");
            while (!fileLine.equals("</Roads>")) {
                String[] lineTokens = fileLine.split(";");

                addToGraph(lineTokens, vertexMap);

                fileLine = in.readLine().replaceAll(" ", "");
            }

        }

        void addToGraph(String[] graphInfo, HashMap<String, Vertex> vertexMap) {

            String roadName = graphInfo[0];
            String startVertexName = graphInfo[1];
            String endVertexName = graphInfo[2];
            float normalWeight = Float.parseFloat(graphInfo[3]);

            Vertex startingVertex;
            if (vertexMap.containsKey(startVertexName))
                startingVertex = vertexMap.get(startVertexName);
            else {
                startingVertex = new Vertex(startVertexName);
                vertexMap.put(startVertexName, startingVertex);
            }
            Vertex endVertex;
            if (vertexMap.containsKey(endVertexName))
                endVertex = vertexMap.get(endVertexName);
            else {
                endVertex = new Vertex(endVertexName);
                vertexMap.put(endVertexName, endVertex);
            }
            Edge road = new Edge(roadName, startingVertex, endVertex, normalWeight);

            roads.put(roadName, road);

            startingVertex.addEdge(road);
            endVertex.addEdge(road);
        }

        SearchNode UCS(Vertex source, String destination) {
            PriorityQueue<SearchNode> fringe = new PriorityQueue<>();
            fringe.add(source.createSearchNode(day));
            HashMap<String, SearchNode> visitedNodes = new HashMap<>();

            while (!fringe.isEmpty()) {
                SearchNode node = fringe.poll();

                if (node.getName().equals(destination)) {
                    node.numOfExpandedNodes = visitedNodes.size();
                    return node;
                }

                if (!visitedNodes.containsKey(node.getName())) {

                    visitedNodes.put(node.getName(), node);
                    node.expand(fringe, false);
                }

            }
            return null;
        }


        SearchNode IDA(Vertex source, String destination) {

            float costLimit = 0;
            int numOfExpandedNodes;
            while (true) {

                PriorityQueue<SearchNode> fringe = new PriorityQueue<>();
                fringe.add(source.createSearchNode(day));

                numOfExpandedNodes = 0;
                while (!fringe.isEmpty()) {
                    SearchNode node = fringe.poll();

                    if (node.getName().equals(destination)) {
                        node.numOfExpandedNodes = numOfExpandedNodes;
                        return node;
                    }


                    if (node.predictedCost <= costLimit) {
                        node.expand(fringe, true);
                        numOfExpandedNodes++;
                    } else {
                        costLimit = node.predictedCost;
                        break;
                    }

                }

            }

        }
    }

    static class Vertex{
        String name;
        List<Edge> edges;
        float heuristic;

        Vertex(String name){
            this.name=name;
            edges= new LinkedList<>();
            heuristic=0;
        }

        void addEdge(Edge edge){
            edges.add(edge);
        }

        SearchNode createSearchNode(int day){
            return new SearchNode(name,this,day);
        }

    }


    static class SearchNode implements Comparable<SearchNode>{

        SearchNode parentNode;
        float predictedCost;
        float realCost;
        Vertex originVertex;
        int numOfExpandedNodes;
        float costToGetHere;
        float roadCostToHere;
        int day;


        SearchNode(String name,Vertex originVertex,int day) {
            predictedCost =0;
            realCost=0;
            parentNode=null;
            this.originVertex=originVertex;
            numOfExpandedNodes=0;
            costToGetHere=0;
            roadCostToHere=0;
            this.day=day;
        }

        @Override
        public int compareTo(Project.SearchNode arg0) {
            if(this.predictedCost < arg0.predictedCost)
                return -1;
            else if(this.predictedCost >arg0.predictedCost)
                return 1;
            return 0;
        }



        String getName(){
            return originVertex.name;
        }

        void expand(PriorityQueue<SearchNode> fringe,boolean includeHeuristic){
            for(Edge edge : originVertex.edges){

                SearchNode node = edge.getNeighbourVertex(this.originVertex).createSearchNode(day);
                if(!(this.parentNode!=null && this.parentNode.getName().equals(node.getName()))){
                    float heuristic=0;
                    if(includeHeuristic)
                        heuristic=node.originVertex.heuristic;

                    node.roadCostToHere=edge.getPredictedCost(day);
                    node.costToGetHere= this.costToGetHere+node.roadCostToHere;
                    node.predictedCost =  node.costToGetHere+heuristic;

                    node.realCost= this.realCost +  edge.getRealCost(day);
                    node.parentNode=this;
                    fringe.add(node);
                }

            }
        }

    }

    /*
     * This class uses the daily data acquired to compute daily statistics.
     * These statistics are later used to compute a-posteriori probabilities which are used
     * to calculate the cost of each road.
     * The a-posteriori probabilities are based on the random variables X={0,1,2} and Y={0,1,2}
     * where X is the actual outcome on a given day for a given road and Y is the prediction.
     * (0=low traffic , 1= normal traffic , 2=heavy traffic)

     */
    static class Probabilities{

        /*
        The matrix which holds the desired statistics.
        Each value of the matrix is the number of occurrences of an event (X,Y),
        statistics[X][Y] is the number of occurrences for the event (X,Y).
        For example statistics[0][1] counts the number of times that a prediction for a road was normal
        but the actual traffic outcome for that road was low.
         */
        int [][] statistics;

        /*The class is implemented as singleton so this is its instance.*/
        static Probabilities probs = new Probabilities();

        public Probabilities() {

            statistics= new int[3][3];
        }

        /*For a given prediction(Y=prediction) this method computes E[g(X)|Y=prediction]
         * where g(X) is the weight to multiply to the normalCost of the road
         * since g(X)={0.9 for X=0 ,1 for X=1,1.25 for X=2}.
         * */
        double getAverageWeight(int prediction){

            double p0=0;
            double p1=0;
            double p2=0;

            /*computing the number of a road was predicted Y=prediction*/
            int numOfPredicted=statistics[2][prediction]+statistics[1][prediction]+statistics[0][prediction];

            /*to find the probability i.e P(X=1|Y=prediction) we divide the number of
             *occurrences of the event(1,prediction)/ number of the occurrences of the particular prediction
             */
            p0 =((double)statistics[0][prediction]/(double) numOfPredicted);
            p1 = ((double) statistics[1][prediction]/(double) numOfPredicted);
            p2 = ((double) statistics[2][prediction]/(double) numOfPredicted);

            return (p0*(0.9) + p1 + p2*(1.25));
        }

        /*This method is used to calculate the needed statistics for all the roads on a particular day.*/
        void computeDailyProbabilities(HashMap<String,Edge> roads , int day){

            for(Edge edge : roads.values()){
                int prediction = edge.historyOfPredictions.get(day);
                int actualOutcome =  edge.historyOfActualOutcomes.get(day);
                statistics[actualOutcome][prediction] ++;
            }
        }

    }

    /*
     This class is used to represent the roads.It holds information about the name , the weight of the road,
     which vertices are connected to the road, the prediction and actual outcomes for the traffic of the road
     for every day of the experiment.
     */
    static class Edge{
        String name;
        float normalWeight;
        Vertex start,end;
        ArrayList<Integer> historyOfPredictions;
        ArrayList<Integer> historyOfActualOutcomes ;
        float[] weightMap ;

        Edge(String name,Vertex start,Vertex end,float normalWeight){
            this.name= name;
            this.start=start;
            this.end=end;
            this.normalWeight= normalWeight;
            this.historyOfPredictions = new ArrayList<>(80);
            this.historyOfActualOutcomes = new ArrayList<>(80);
            weightMap = new float[3];
            weightMap[0]=(float)0.9;
            weightMap[1]=(float)1.0;
            weightMap[2]=(float)1.25;
        }
        /*This method gets a vertex as an argument to return the opposing vertex of the road (its neighbor)*/
        Vertex getNeighbourVertex(Vertex vertex){
            if(start.name.equals(vertex.name)) return end;
            else return start;
        }
        /*This method is used to initialize outcomes and predictions for the traffic of the road*/
        void addToTrafficHistory(String trafficState,boolean actualTraffic){
            List<Integer> history;
            if(actualTraffic)
                history= this.historyOfActualOutcomes;
            else
                history=this.historyOfPredictions;

            switch (trafficState){
                case "low":
                    history.add(0);
                    break;
                case "normal":
                    history.add(1);
                    break;
                case "heavy":
                    history.add(2);
                default:

            }
        }

        /*This method is used to get the predicted cost for a particular day.
         * It is used in the SearchNode expand method to compute the G(n) of a node for that given day.
         */
        float getPredictedCost(int day){
            /*Day -1 is used to initialize the heuristic of a vertex because the h(n) of a vertex
             *is the cost to the goal if every road had low traffic
             */
            if(day==-1)
                return normalWeight*((float)0.9);
                /*
                 *for the first day the probabilities have not been yet computed so the cost for each road
                 *will be its normal cost times the arithmetic mean weight(1.05)
                 */
            else if(day==0)
                return (float) 1.05 *normalWeight;

            /*for all the other days the normalWeight is multiplied by the average weight for a given prediction*/

            return  normalWeight*(float)Probabilities.probs.getAverageWeight(historyOfPredictions.get(day));
        }


        /*This method is used in expand to get the real cost of a road on a given day*/
        float getRealCost(int day){
            if(day>=0)
                return normalWeight*weightMap[historyOfActualOutcomes.get(day)];
            return 0 ;
        }
    }


    /*
     This class is used for convenience to output both to the specified file and the console
     */
    static class Output{
        PrintWriter fileOut;

        Output(PrintWriter  fileArg){
            fileOut= fileArg;

        }

        void println(Object argument){
            fileOut.println(argument);
            System.out.println(argument);
        }

        void print(Object argument){
            fileOut.print(argument);
            System.out.print(argument);
        }

    }

}
