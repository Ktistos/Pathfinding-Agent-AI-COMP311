import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


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
        int numOfRoads;
        Vertex source;
        Vertex destination;
        HashMap<String,Edge> roads = new HashMap<>();

        Experiment(BufferedReader in , Output out){
            this.in=in;
            this.out=out;
            numOfRoads=0;
            source=null;
            destination= null ;

        }
        
        public void experiment() throws IOException{
            constructGraph();
        
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
            for(String s : vertexMap.keySet()){
                out.print(s + " " );
                for(int i=0; i<vertexMap.get(s).edges.size();i++){
                    out.print(vertexMap.get(s).edges.get(i).name + " ");
                }
                out.println("");
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
                    endVertex=destination;
                else{
                    endVertex = new Vertex(endVertexName);
                    vertexMap.put(endVertexName, endVertex);
                }
                Edge road = new Edge(roadName,startingVertex,endVertex,normalWeight);
                
                roads.put(roadName,road);

                startingVertex.addEdge(road);
                endVertex.addEdge(road);
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

    }
    
    static class Edge{
        String name;
        float normalWeight;
        Vertex start,end;

        Edge(String name,Vertex start,Vertex end,float normalWeight){
            this.name= name;
            this.start=start;
            this.end=end;
            this.normalWeight= normalWeight;
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
