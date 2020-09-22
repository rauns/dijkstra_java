package Client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;


public class Map
{
    static final int NO_EDGE = -1;

    private String[] nodesNames;
    private int[][] weights;
    private int nodesCount;
    
    final static String delimiter = ",";

    private Map(int verticesCount)
    {
        nodesNames = new String[verticesCount];
        weights = new int[verticesCount][verticesCount];
        this.nodesCount = verticesCount;
    }

    public int getNodesCount() { return nodesCount; }
    int[][] getWeights() { return weights; }
    String[] getNodesNames() { return nodesNames; }

    public static Map fromFile(String filename) throws Exception
    {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String header = br.readLine();
            if (!header.startsWith("vertices"))
                throw new RuntimeException("Invalid testcase format.");

            int verticesCount = Integer.parseInt(header.replaceAll("[^0-9]", ""));
            Map m = new Map(verticesCount);

            for (int i = 0; i < verticesCount; ++i) {
                String line = br.readLine();
                String[] cases = line.split(delimiter);

                for (int j = 0; j < verticesCount; ++j) {
                    String numstr = cases[j].trim();
                    if (numstr.contains("-"))
                        m.weights[i][j] = NO_EDGE;
                    else
                        m.weights[i][j] = Integer.parseInt(numstr);
                }
            }

            for (int i = 0; i < verticesCount; ++i) {
                String nodeName = String.valueOf((char)('A' + i));
                System.out.println("Adding node: " + nodeName);
                m.nodesNames[i] = nodeName;
            }

            return m;
        }
        catch (Exception e) {
            System.out.println("Couldn't handle file properly: " + e.getMessage());
            throw e;
        }
    }

    void printWeights()
    {
        for (int i = 0; i < nodesCount; ++i) {
            for (int j = 0; j < nodesCount; ++j) {
                if (weights[i][j] != NO_EDGE)
                    System.out.print(weights[i][j] + " ");
                else
                    System.out.print("-" + " ");
            }
            System.out.println();
        }
    }
}
