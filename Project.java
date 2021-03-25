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
        /*Used to read input from the file.*/
        BufferedReader in;

        Output out;
        /*Initial vertex of the searching path.*/
        Vertex source;
        /*Goal vertex of the path.*/
        Vertex destination;
        /*
         *Hashmap which is contains the edges of the graph and allows easy access during the initializations for the
         *predictions and the actual traffic outcomes of the edges.
         */
        static HashMap<String, Edge> roads = new HashMap<>();
        /*the current day of the experiment.It is initialized at -1 because it is used in such way
         * during the initialization of the vertex heuristics
         */
        static int day = -1;
        /*HashMap which contains all the vertices of the graph.*/
        HashMap<String, Vertex> vertexMap ;


        Experiment(BufferedReader in, Output out) {
            this.in = in;
            this.out = out;
            source = null;
            destination = null;
            vertexMap = new HashMap<>();
        }

        /*This is the main method of the program.It is responsible to
         * output the results , the initiation of the searching algorithms and
         * generally the orchestration of all the measurements*/
        public void experiment() throws IOException {
            /*Reading from the file and initializing the graph.*/
            constructGraph();

            /*initialization of predictions and actual outcomes of road traffic status.*/
            initializeTraffic(false);
            initializeTraffic(true);

            initializeVertexHeuristics();
            day++;
            /*Starting from day 1(0)*/
            /*variable to compute the mean real cost for the chosen path for 80 days.*/
            float sumOfAllRealCosts=0;
            /*array to store the real cost accumulated for every 20 days for the experiment.It is then used to
             *calculate the mean per 20 days.*/
            float[] monthlySumOfRealCosts=new float[4];
            int count=0;
            out.println("=================================================");
            for (int i = 0; i < 80; i++) {
                out.println("Day " + (i + 1));
                out.print("<Uninformed Search Algorithm>:");
                /*starting to measure the duration of the UCS algorithm*/
                Instant start = Instant.now();
                SearchNode goal = UCS(source, destination.name);
                Instant end = Instant.now();
                /*ending of measurement*/
                Duration duration= Duration.between(start,end);

                out.println("\t" +"Excecution time(μs): "+(double)(duration.toNanos()/1000));
                out.println("\t" +"Visited Nodes number: " + goal.numOfExpandedNodes);
                out.println("\t" + getPathInfo(goal));
                out.println("\t" +"Predicted Cost: " + goal.predictedCost);
                out.println("\t" +"Real Cost:" + goal.realCost);


                out.println("IDA*:");
                /*starting to measure the duration of the IDA* algorithm*/
                start=Instant.now();
                goal = IDA(source, destination.name);
                end = Instant.now();
                duration= Duration.between(start,end);
                /*ending of measurement*/
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

                /*condition to change the index of monthlySumOfRealCosts to compute the mean
                 *for the next 20 days of the experiment*/
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

        /*This method runs UCS for every vertex of the graph to compute its heuristic.
         * The heuristic of its vertex is its distance to the goal if all traffic on all roads was low.*/
        void initializeVertexHeuristics() {
            for (Vertex start : vertexMap.values())
                start.heuristic = UCS(start, destination.name).predictedCost;
        }

        /*
        This method is used to get information about the path
        each has chosen and turn it to a string in order to output it.
         */
        String getPathInfo(SearchNode goal) {
            LinkedList<SearchNode> nodeList = new LinkedList<>();

            /*Since the searching algorithms return the final node it needed to traverse the path until the
             * source node is found in order to print information about it.
             * Every node in the path is stored in the nodeList during this procedure.*/
            SearchNode node = goal;
            while (node.parentNode != null) {
                nodeList.addFirst(node);
                node = node.parentNode;
            }
            nodeList.addFirst(node);

            StringBuilder pathInfo= new StringBuilder();

            /*We later append information about every node of the path to a string builder in order
            to then return it to a string form.*/

            for (int i = 0; i < nodeList.size() - 1; i++) {
                pathInfo.append(nodeList.get(i).getName());
                pathInfo.append("(");
                pathInfo.append(nodeList.get(i+1).roadCostToHere);
                pathInfo.append( " ) -> ");
            }
            pathInfo.append(nodeList.get(nodeList.size() - 1).getName()  );

            return pathInfo.toString();
        }

        /*This method is used to initialize the information about all the predictions and actual outcomes of traffic status for every day of the experiment,
         * for every road of the experiment.*/
        void initializeTraffic(boolean actualTraffic) throws IOException {

            /*choosing whether to initialize predictions or actualTrafficPerDay*/
            String fileBound;
            if (actualTraffic)
                fileBound = "</ActualTrafficPerDay>";
            else
                fileBound = "</Predictions>";

            /*skip lines <Predictions> and <Day> of the file*/
            in.readLine();
            in.readLine();


            String roadStatus = in.readLine();
            while (!roadStatus.equals(fileBound)) {
                while (!roadStatus.equals("</Day>")) {
                    /*splitting string based on ';' character*/
                    String[] lineTokens = roadStatus.replaceAll(" ", "").split(";");
                    if (lineTokens.length > 1) {
                        String roadName = lineTokens[0];
                        String trafficState = lineTokens[1];
                        /*adding the traffic status to the history (of predictions/actual outcomes) of the road*/
                        roads.get(roadName).addToTrafficHistory(trafficState, actualTraffic);
                    }
                    /*read next line*/
                    roadStatus = in.readLine();
                }
                /*skip <Day>*/
                roadStatus = in.readLine();
            }
        }

        /*
        This method is used to construct the graph of the experiment.
        It uses the addToGraph method to add to the graph info from the file lines.
         */
        void constructGraph() throws IOException {

            /*aquiring and creating source vertex from file*/
            String sourceName = in.readLine();
            sourceName = sourceName.substring(8, sourceName.length() - 9);
            source = new Vertex(sourceName);

            /*aquiring and creating destination vertex from file*/
            String destinationName = in.readLine();
            destinationName = destinationName.substring(13, destinationName.length() - 14);
            destination = new Vertex(destinationName);


            vertexMap.put(sourceName, source);
            vertexMap.put(destinationName, destination);

            /* bypassing the <Roads> string in the file*/
            in.readLine();
            /*reading all lines until line == </Roads> to get the graph info*/
            String fileLine = in.readLine().replaceAll(" ", "");
            while (!fileLine.equals("</Roads>")) {
                String[] lineTokens = fileLine.split(";");

                addToGraph(lineTokens, vertexMap);

                fileLine = in.readLine().replaceAll(" ", "");
            }

        }

        /*This method adds to the graph the vertices of the start and the and of the road.
         * It associates those vertices with the road that connects them and initializes
         * some information about the graph roads.*/
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

        /*Uninformed Search Algorithm
         * Uniform cost search
         * Vertex source: the starting vertex
         * String destination : the name of the destination vertex
         *
         */
        SearchNode UCS(Vertex source, String destination) {
            /*Structure to store and sort the nodes to visit next, based on their predicted cost*/
            PriorityQueue<SearchNode> fringe = new PriorityQueue<>();

            /*Adding the source node in the fringe*/
            fringe.add(source.createSearchNode(day));

            /*HashMap to store visited nodes in order not to visit them again*/
            HashMap<String, SearchNode> visitedNodes = new HashMap<>();

            /*Loops until a path is found
             * Problem statement guaranties that a path exists
             * if fringe is empty, it means we have nowhere else to go, and a path has not been found
             * */
            while (!fringe.isEmpty()) {
                /*Get node with the lowest predicted cost*/
                SearchNode node = fringe.poll();

                /*if the node's name is the name of the destination vertex, we have found a path
                 * Update the numOfExpandedNodes variable in the destination node
                 * return the destination node
                 * */
                if (node.getName().equals(destination)) {
                    node.numOfExpandedNodes = visitedNodes.size();
                    return node;
                }

                /*If the node doesn't exist in visited nodes, add it
                 * then add neighbours of this node in the fringe using SearchNode.expand()*/
                if (!visitedNodes.containsKey(node.getName())) {
                    /*add node to visited nodes*/
                    visitedNodes.put(node.getName(), node);

                    /*add neighbours to fringe
                     * Expand method is called with includeHeuristic = false because UCS does not use a heuristic*/
                    node.expand(fringe, false);
                }

                /*if the node had been visited before, loop starts all over again, with next best node from the fringe*/

            }

            /*return null if a path was not found*/
            return null;
        }

        /*Informed search Algorithm
         * Iterative deepening A*
         * Implemented with multiple UCS Algorithms
         * Every time starting from the source node
         * This algorithm does not have a visited node list
         * */
        SearchNode IDA(Vertex source, String destination) {

            /*Cost limit is the max cost of the nodes the algorithm will explore
            * Only algorithms with cost<=costLimit will be explored*/
            float costLimit = 0;

            /*Number of expanded nodes, only used in destination vertex*/
            int numOfExpandedNodes;

            /*Infinite loop to search for a path
            * A path is guaranteed to exist*/
            while (true) {

                /*Structure to store and sort the nodes to visit next, based on their predicted cost
                * At every iteration of IDA* a new queue is made*/
                PriorityQueue<SearchNode> fringe = new PriorityQueue<>();

                /*Adding the source node in the fringe*/
                fringe.add(source.createSearchNode(day));

                /*Number of expanded nodes is set to 0, we have not made an expand yet*/
                numOfExpandedNodes = 0;

                /*basic while loop of a UCS algorithm */
                while (!fringe.isEmpty()) {
                    /*Get node with the lowest predicted cost*/
                    SearchNode node = fringe.poll();

                    /*if the node's name is the name of the destination vertex, we have found a path
                     * Update the numOfExpandedNodes variable in the destination node
                     * return the destination node
                     * */
                    if (node.getName().equals(destination)) {
                        node.numOfExpandedNodes = numOfExpandedNodes;
                        return node;
                    }


                    /*if predictedCost of this node to the destination vertex is <= costLimit
                    * we expand the current node*/
                    if (node.predictedCost <= costLimit) {
                        /*add neighbours to fringe
                         * Expand method is called with includeHeuristic = true because UCS uses a heuristic*/
                        node.expand(fringe, true);
                        numOfExpandedNodes++;
                    } else {
                        /*if the node with the lowest predictedCost can't be expanded (fringe.poll())
                        * update the costLimit as the predictedCost of the current node
                        */
                        costLimit = node.predictedCost;

                        /*Break the loop and start all over again, with new costLimit*/
                        break;
                    }
                }
            }
        }
    }

    /*Vertex class, implements a vertex in the graph*/
    static class Vertex{
        /*Name of the node*/
        String name;

        /*A list of the roads that are connected to a vertex*/
        List<Edge> edges;

        /*Heuristic of the vertex
         * Cost of the best path to destination, given that all traffic is low*/
        float heuristic;

        /*Class constructor*/
        Vertex(String name){
            this.name=name;
            edges= new LinkedList<>();
            heuristic=0;
        }

        /*Method to add a road in the list of roads of a vertex*/
        void addEdge(Edge edge){
            edges.add(edge);
        }

        /*Method to make a SearchNode node from a vertex
         *Actual search is performed on search nodes */
        SearchNode createSearchNode(int day){
            return new SearchNode(name,this,day);
        }

    }

    /*Class to implement a SearchNode
     * SearchNodes are used in the actual searches
     * SearchNodes are used to represent different states of a vertex
     * Implements Comparable interface in order to be sorted in a Priority Queue */
    static class SearchNode implements Comparable<SearchNode>{

        /*Parent of the SearchNode*/
        SearchNode parentNode;

        /*Cost to get to a node + heuristic according to the decision of what is the
         * real traffic given the prediction */
        float predictedCost;

        /*Real cost to get to a node  */
        float realCost;

        /*The vertex that this SearchNode was made from*/
        Vertex originVertex;

        /*Variable used to store the number of expanded nodes
         * Only used in the destination SearchNode */
        int numOfExpandedNodes;

        /*Predicted cost of path to a certain node*/
        float costToGetHere;

        /*Predicted cost of the road from a node to a neighbouring node*/
        float roadCostToHere;

        /*Day of the experiment*/
        int day;


        /*Class constructor, initializes member variables */
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
        /*Overridden method of comparable interface
         * Needed in order to sort SearchNodes,based on their predicted cost, using a PriorityQueue */
        public int compareTo(Project.SearchNode arg0) {
            if(this.predictedCost < arg0.predictedCost)
                return -1;
            else if(this.predictedCost >arg0.predictedCost)
                return 1;
            return 0;
        }


        /*Method to return the name of the node*/
        String getName(){
            return originVertex.name;
        }

        /*Method to add neighbouring nodes in the fringe and update the costs
         *
         * PriorityQueue<SearchNode> fringe: structure with all possible nodes to visit next,
         * the one with the lowest predicted cost will be chosen.
         *
         * boolean includeHeuristic : boolean value in order to include the heuristic value or not
         *
         * UCS algorithm : includeHeuristic = false
         * IDA* algorithm : includeHeuristic = true
         * */
        void expand(PriorityQueue<SearchNode> fringe,boolean includeHeuristic){

            /*looping through all roads that are connected with the vertex*/
            for(Edge edge : originVertex.edges){

                /*For every road, find neighbour of current vertex*/
                SearchNode node = edge.getNeighbourVertex(this.originVertex).createSearchNode(day);

                /*If statement to prevent adding the node we came from, in the fringe
                 * We are not allowed to do a step back
                 * */
                if(!(this.parentNode!=null && this.parentNode.getName().equals(node.getName()))){

                    /*temp variable to store the heuristic if needed*/
                    float heuristic=0;

                    /*If statement in order to check whether or not the heuristic should be included*/
                    if(includeHeuristic)
                        heuristic=node.originVertex.heuristic;

                    /*Get predicted cost of the road to get to the neighbour node*/
                    node.roadCostToHere=edge.getPredictedCost(day);

                    /*Update the total path cost from start to the next node we may visit*/
                    node.costToGetHere= this.costToGetHere+node.roadCostToHere;

                    /*Update the total predicted cost from start to the next node we may visit*/
                    node.predictedCost =  node.costToGetHere+heuristic;

                    /*Update the real cost from start to the next node we may visit*/
                    node.realCost= this.realCost +  edge.getRealCost(day);

                    /*Set parent of the node we may visit as the node we currently are in*/
                    node.parentNode=this;

                    /*At last, add neighbour (next node we may visit) in the fringe*/
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
