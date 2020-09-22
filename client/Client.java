package Client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.*;
import java.util.concurrent.*;

import Shared.*;

public class Client
{
    public static void main( String args[] ) throws Exception
    {
        System.out.println("Client started, args count = " + args.length);

        if (args.length < 3)
        {
            System.out.println("Usage: <testcaseNumber> <host> <serverPorts>...");
            return;
        }

        String testcaseNumber = args[0];
        String host = args[1];
        String[] serversPorts = new String[args.length - 2];

        for(int i=2; i<args.length; ++i)
            serversPorts[i-2] = args[i];

        try
        {
            System.out.println("Getting map");
            Map m = Map.fromFile("testcases/testcase" + testcaseNumber);
            m.printWeights();

            System.out.println("Launching Dijkstra");
            new DijkstraClient(m, host, serversPorts).run();
        }
        catch(Exception e)
        {
            System.out.println("Sth crashed");
            e.printStackTrace();
        }
    }
}
