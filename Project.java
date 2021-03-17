import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;



public class Project {


        public static void main(String[] args) {
    
            
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
            Output out =  new Output(fileOut);
            Experiment exp = new Experiment(inputStream, out);
            try {
                exp.experiment();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            fileOut.close();
        }
    
    
    static class Experiment{
        BufferedReader in ;
        Output out;
        Vertex source;
        Vertex destination;
        static HashMap<String,Edge> roads = new HashMap<>();
        static Probabilities probs=new Probabilities();
        static int day=0;

        Experiment(BufferedReader in , Output out){
            this.in=in;
            this.out=out;
            source=null;
            destination= null ;
        }
        
        public void experiment() throws IOException{

            constructGraph();
            //initialization of predictions and actual outcomes of road traffic status
            initializeTraffic(false);
            initializeTraffic(true);

            int numOfCorrectPredictions=0;
            for (int i = 0; i < 80; i++) {
                out.println("Day "+(i+1));
                printPathInfo(UCS(source,destination.name));
                int pred= findNumOfCorrectPredictionsPerDay();
                numOfCorrectPredictions+=pred;
                out.println(pred);
                if(i<79){
                    day++;
                    probs.computeDailyProbabilities();
                }

                out.println("");
            }
            out.println(numOfCorrectPredictions);
            out.println(((float)numOfCorrectPredictions/(float) 80*roads.size()) + " /" + 80*roads.size() );





        }


        int findNumOfCorrectPredictionsPerDay(){
            int numOfCorrectPredictions=0;
            for(Edge edge : roads.values()){
                if(edge.decision==edge.historyOfActualOutcomes.get(day))
                    numOfCorrectPredictions++;
            }
            return numOfCorrectPredictions;
        }


        void printPathInfo(SearchNode goal){
            LinkedList<SearchNode> nodeList = new LinkedList<>();
            SearchNode node =  goal;
            while(node.parentNode!=null){
                nodeList.addFirst(node);
                node=node.parentNode;
            }



            out.print("Predicted Cost:" + nodeList.getLast().predictedCost+" ");
            out.println("Actual Cost:"+ nodeList.getLast().realCost);



        }


        void initializeTraffic(boolean actualTraffic) throws IOException {

            //choosing whether to initialize predictions or actualTrafficPerDay
            String fileBound;
            if(actualTraffic)
                fileBound="</ActualTrafficPerDay>";
            else
                fileBound= "</Predictions>";

            //skip lines <Predictions> and <Day>
            in.readLine();
            in.readLine();

            String prediction = in.readLine();
            while(!prediction.equals(fileBound)){
                while(!prediction.equals("</Day>")){
                    //splitting string based on ';' character
                    String[] lineTokens =  prediction.replaceAll(" ", "").split(";");
                    if(lineTokens.length>1) {
                        String roadName = lineTokens[0];
                        String trafficState = lineTokens[1];
                        //adding predictions to the history of each road
                        roads.get(roadName).addToTrafficHistory(trafficState, actualTraffic);
                    }
                    //read next line
                    prediction=in.readLine();
                }
                //skip <Day>
                prediction=in.readLine();
            }
        }



        void constructGraph() throws IOException{
            
            //aquiring and creating source vertex from file
            String sourceName = in.readLine();
            sourceName =  sourceName.substring(8,sourceName.length()-9);
            source= new Vertex(sourceName);
            
            //aquiring and creating destination vertex from file
            String destinationName = in.readLine();
            destinationName= destinationName.substring(13,destinationName.length()-14);
            destination = new Vertex(destinationName);

            HashMap<String,Vertex> vertexMap = new HashMap<>();
            vertexMap.put(sourceName, source);
            vertexMap.put(destinationName, destination);

            //bypassing the <Roads> string in the file
            in.readLine();
            //reading all lines until line == </Roads> to get the graph info
            String fileLine=in.readLine().replaceAll(" ", "");
            while(!fileLine.equals("</Roads>")){
                String[] lineTokens =  fileLine.split(";");
                
                addToGraph(lineTokens, vertexMap);

                fileLine=in.readLine().replaceAll(" ", "");
            }

        }

        void addToGraph(String[] graphInfo,HashMap<String,Vertex> vertexMap){

                String roadName = graphInfo[0];
                String startVertexName  = graphInfo[1];
                String endVertexName= graphInfo[2];
                float normalWeight = Float.parseFloat(graphInfo[3]);

                Vertex startingVertex;
                if(vertexMap.containsKey(startVertexName))    
                    startingVertex=vertexMap.get(startVertexName);
                else{
                    startingVertex = new Vertex(startVertexName);
                    vertexMap.put(startVertexName, startingVertex);
                }
                Vertex endVertex;
                if(vertexMap.containsKey(endVertexName))
                    endVertex=vertexMap.get(endVertexName);
                else{
                    endVertex = new Vertex(endVertexName);
                    vertexMap.put(endVertexName, endVertex);
                }
                Edge road = new Edge(roadName,startingVertex,endVertex,normalWeight);
                
                roads.put(roadName,road);

                startingVertex.addEdge(road);
                endVertex.addEdge(road);
        }

        SearchNode UCS(Vertex source , String destination){
            PriorityQueue<SearchNode> fringe =  new PriorityQueue<>();
            fringe.add(source.createSearchNode());
            HashMap<String,SearchNode> visitedNodes = new HashMap<>();
            
            while(!fringe.isEmpty()){
                SearchNode node = fringe.poll();

                if(node.getName().equals(destination))
                    return node;

                if(!visitedNodes.containsKey(node.getName())){

                    visitedNodes.put(node.getName(), node);  
                    node.expand(fringe);
                }

            }
            return null;
        }


        SearchNode IDA(Vertex source , String destination){
            
            float costLimit=0;

            while(true){

                PriorityQueue<SearchNode> fringe =  new PriorityQueue<>();
                fringe.add(source.createSearchNode());
                HashMap<String,SearchNode> visitedNodes = new HashMap<>();

                while(!fringe.isEmpty()){
                    SearchNode node = fringe.poll();
    
                    if(node.getName().equals(destination))
                        return node;
    
                    if(!visitedNodes.containsKey(node.getName())){
    
                        visitedNodes.put(node.getName(), node);  
                        if(node.predictedCost <=costLimit)
                            node.expand(fringe);
                        else{ 
                            costLimit=node.predictedCost;
                            break;
                        }
    
                    }
    
                }
            }
            
            
            //return null;
        }
    }
    

    static class Vertex{
        String name;
        List<Edge> edges;
        
        Vertex(String name){
            this.name=name;
            edges= new LinkedList<>();
        }

        void addEdge(Edge edge){
            edges.add(edge);
        }

         SearchNode createSearchNode(){
            return new SearchNode(name,this);
        }

    }


    static class SearchNode implements Comparable<SearchNode>{

        SearchNode parentNode;
        float predictedCost;
        float realCost;
        Vertex originVertex;


        SearchNode(String name,Vertex originVertex) {
            predictedCost =0;
            realCost=0;
            parentNode=null;
            this.originVertex=originVertex;
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

        void expand(PriorityQueue<SearchNode> fringe){
            for(Edge edge : originVertex.edges){

                SearchNode node = edge.getNeighbourVertex(this.originVertex).createSearchNode();
                if(!(this.parentNode!=null && this.parentNode.getName().equals(node.getName()))){
                    node.predictedCost = this.predictedCost + edge.getPredictedCost();
                    node.realCost= this.realCost +  edge.getRealCost();
                    node.parentNode=this;
                    fringe.add(node);
                }
            
            }
        }

    }

    static class Probabilities{

            //a-priori statistics
            int numOfActualHeavy;
            int numOfActualNormal;
            int numOfActualLow;

            //a-posteriori statistics
            //Given Low
            int lowGivenLow;
            int normalGivenLow;
            int heavyGivenLow;
            //Given Normal
            int lowGivenNormal;
            int normalGivenNormal;
            int heavyGivenNormal;
            //Given Heavy
            int lowGivenHeavy;
            int normalGivenHeavy;
            int heavyGivenHeavy;

            int numOfActualStatus;


        public Probabilities() {

            numOfActualHeavy=0;
            numOfActualNormal=0;
            numOfActualLow=0;

            lowGivenLow=0;
            normalGivenLow=0;
            heavyGivenLow=0;

            lowGivenNormal=0;
            normalGivenNormal=0;
            heavyGivenNormal=0;

            lowGivenHeavy=0;
            normalGivenHeavy=0;
            heavyGivenHeavy=0;

            numOfActualStatus=0;
        }

        int MAP(int prediction){

            Map<Float,Integer> probabilityHashmap = new HashMap<>();
            float prod0=0;
            float prod1=0;
            float prod2=0;
            float maxProd;


            float p0 = ((float) numOfActualLow)/((float)numOfActualStatus);
            float p1 = ((float) numOfActualNormal)/((float)numOfActualStatus);
            float p2 = ((float) numOfActualHeavy)/((float)numOfActualStatus);



            float enumerator0 = 0;
            float enumerator1 = 0;
            float enumerator2 = 0;

            switch (prediction){
                case 0:
                    enumerator0 = lowGivenLow;
                    enumerator1 = lowGivenNormal;
                    enumerator2 = lowGivenHeavy;
                    break;
                case 1:
                    enumerator0 = normalGivenLow;
                    enumerator1 = normalGivenNormal;
                    enumerator2 = normalGivenHeavy;
                    break;
                case 2:
                    enumerator0 = heavyGivenLow;
                    enumerator1 = heavyGivenNormal;
                    enumerator2 = heavyGivenHeavy;
                    break;
            }


            prod0 = p0 *(((float)enumerator0)/((float) numOfActualLow));
            prod1 = p1 *(((float)enumerator1)/((float) numOfActualNormal));
            prod2 = p2 *(((float)enumerator2)/((float) numOfActualHeavy));


            probabilityHashmap.put(prod0,0);
            probabilityHashmap.put(prod1,1);
            probabilityHashmap.put(prod2,2);

            maxProd = Math.max(Math.max(prod0,prod1),prod2);
            return probabilityHashmap.get(maxProd);
        }

        void computeDailyProbabilities(){
            numOfActualStatus+=Experiment.roads.size();
            for(Edge edge : Experiment.roads.values()){
                switch (edge.historyOfActualOutcomes.get(Experiment.day)){
                    case 0:
                        switch (edge.historyOfPredictions.get(Experiment.day)){
                            case 0:
                                lowGivenLow++;
                                break;
                            case 1:
                                normalGivenLow++;
                                break;
                            case 2:
                                heavyGivenLow++;
                                break;
                        }
                        numOfActualLow++;
                        break;
                    case 1:
                        switch (edge.historyOfPredictions.get(Experiment.day)){
                            case 0:
                                lowGivenNormal++;
                                break;
                            case 1:
                                normalGivenNormal++;
                                break;
                            case 2:
                                heavyGivenNormal++;
                                break;
                        }
                        numOfActualNormal++;
                        break;
                    case 2:
                        switch (edge.historyOfPredictions.get(Experiment.day)){
                            case 0:
                                lowGivenHeavy++;
                                break;
                            case 1:
                                normalGivenHeavy++;
                                break;
                            case 2:
                                heavyGivenHeavy++;
                                break;
                        }
                        numOfActualHeavy++;
                        break;
                }
            }
        }
    }







    static class Edge{
        String name;
        float normalWeight;
        Vertex start,end;
        ArrayList<Integer> historyOfPredictions;
        ArrayList<Integer> historyOfActualOutcomes ;
        HashMap<Integer,Float> weightMap ;
        int decision;

        Edge(String name,Vertex start,Vertex end,float normalWeight){
            this.name= name;
            this.start=start;
            this.end=end;
            this.normalWeight= normalWeight;
            this.historyOfPredictions = new ArrayList<>(80);
            this.historyOfActualOutcomes = new ArrayList<>(80);
            weightMap = new HashMap<>();
            weightMap.put(0,(float)0.9);
            weightMap.put(1,(float)1.0);
            weightMap.put(2,(float)1.25);
            decision=0;
        }

        Vertex getNeighbourVertex(Vertex vertex){
            if(start.name.equals(vertex.name)) return end;
            else return start;
        }

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

        float getPredictedCost(){

            if(Experiment.day>0)
                decision=Experiment.probs.MAP(historyOfPredictions.get(Experiment.day));
            else {
                Random rand = new Random();
                decision= rand.nextInt(3);
            }
            return normalWeight*weightMap.get(decision);
        }



        float getRealCost(){
            return normalWeight*weightMap.get(historyOfActualOutcomes.get(Experiment.day));
        }
    }

   

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
