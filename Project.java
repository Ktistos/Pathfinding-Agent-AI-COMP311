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

        Experiment(BufferedReader in , Output out){
            this.in=in;
            this.out=out;
            source=null;
            destination= null ;


        }
        
        public void experiment() throws IOException{

            constructGraph();
            //int
            initializeTraffic(false);
            initializeTraffic(true);



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
                        if(node.cost<=costLimit)
                            node.expand(fringe);
                        else{ 
                            costLimit=node.cost;
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
        float cost ;
        Vertex originVertex;


        SearchNode(String name,Vertex originVertex) {
            cost=0;
            parentNode=null;
            this.originVertex=originVertex;
        }

        @Override
        public int compareTo(Project.SearchNode arg0) {
            if(this.cost< arg0.cost)
                return -1;
            else if(this.cost>arg0.cost)
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
                    node.cost = this.cost + edge.normalWeight;
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

        void computeDailyProbabilities(int day){
            numOfActualStatus+=Experiment.roads.size();
            for(Edge edge : Experiment.roads.values()){
                switch (edge.historyOfActualOutcomes.get(day)){
                    case 0:
                        switch (edge.historyOfpredictions.get(day)){
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
                        switch (edge.historyOfpredictions.get(day)){
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
                        switch (edge.historyOfpredictions.get(day)){
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
        ArrayList<Integer> historyOfpredictions ;
        ArrayList<Integer> historyOfActualOutcomes ;

        Edge(String name,Vertex start,Vertex end,float normalWeight){
            this.name= name;
            this.start=start;
            this.end=end;
            this.normalWeight= normalWeight;
            this.historyOfpredictions = new ArrayList<>(80);
            this.historyOfActualOutcomes = new ArrayList<>(80);
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
                history=this.historyOfpredictions;

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
