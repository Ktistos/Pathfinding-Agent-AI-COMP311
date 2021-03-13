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
            Experiment exp = new Experiment();
            exp.experiment(in, out);
            fileOut.close();
        }
    
    
    static class Experiment{
        public void experiment(InputReader in , Output out){
            
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
        PrintWriter consoleOut;
        PrintWriter fileOut;
        
        Output(PrintWriter  fileArg){
            fileOut= new PrintWriter(fileArg);
            consoleOut = fileArg;
        }


        void println(Object argument){
            fileOut.println(argument);
            consoleOut.println(argument);
        }

        void print(Object argument){
            fileOut.print(argument);
            consoleOut.print(argument);
        }

    }
}
