import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class Project {


        public static void main(String[] args) {
    
            
            InputStream inputStream=null;
            try {
                inputStream = new BufferedInputStream(new FileInputStream(args[0]));
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


            InputReader in = new InputReader(inputStream);
            PrintWriter fileOut = new PrintWriter(outputStream);
            Output out =  new Output(fileOut);
            Experiment exp = new Experiment(in, out);
            exp.experiment();
            fileOut.close();
        }
    
    
    static class Experiment{
        InputReader in ;
        Output out;
        int numOfRoads;
        Vertex source;
        Vertex destination;
        HashMap<String,Edge> roads = new HashMap<>();

        Experiment(InputReader in , Output out){
            this.in=in;
            this.out=out;
            numOfRoads=0;
            source=null;
            destination= null ;

        }
        
        public void experiment(){
            constructGraph();
            out.println(roads.toString());
        }




        void constructGraph(){
            
            //aquiring and creating source vertex from file
            String sourceName = in.next();
            sourceName =  sourceName.substring(8,sourceName.length()-9);
            source= new Vertex(sourceName);
            
            //aquiring and creating destination vertex from file
            String destinationName = in.next();
            destinationName= destinationName.substring(13,destinationName.length()-14);
            destination = new Vertex(destinationName);

            //bypassing the <Roads> string in the file
            in.next();
            //reading all lines until line == </Roads> to get the graph info
            String fileLine=in.next().replaceAll(" ", "");
            while(!fileLine.equals("</Roads>")){
                String[] lineTokens =  fileLine.split(";");
                out.println(lineTokens.toString());
                String roadName = lineTokens[0];
                Vertex startingVertex;
                if(lineTokens[1].equals(sourceName))    
                    startingVertex=source;
                else
                    startingVertex = new Vertex(lineTokens[1]);
                
                Vertex endVertex;
                if(lineTokens[2].equals(destinationName))
                    endVertex=destination;
                else
                    endVertex = new Vertex(lineTokens[2]);
                
                float normalWeight = Float.parseFloat(lineTokens[3]);
                
                Edge road = new Edge(roadName,startingVertex,endVertex,normalWeight);
                
                roads.put(roadName,road);

                startingVertex.addEdge(road);
                endVertex.addEdge(road);

                fileLine=in.next().replaceAll(" ", "");
            }

            
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

    static class InputReader {
        public BufferedReader reader;
        public StringTokenizer tokenizer;
    
        public InputReader(InputStream stream) {
            reader = new BufferedReader(new InputStreamReader(stream), 32768);
            tokenizer = null;
        }
        
        public String next() {
            while (tokenizer == null || !tokenizer.hasMoreTokens()) {
                try {
                    tokenizer = new StringTokenizer(reader.readLine());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return tokenizer.nextToken();
        }
    
        public int nextInt() {
            return Integer.parseInt(next());
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
