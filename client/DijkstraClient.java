package Client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.*;
import java.util.*;
import java.rmi.*;

import Shared.*;

public class DijkstraClient {
    public DijkstraClient(Map map, String host, String[] serverPorts) throws Exception {
        workerServersCount = serverPorts.length;
        workerServers = new ServerInterface[workerServersCount];
        workerNodesCount = new int[workerServersCount];
        workerFromNodes = new int[workerServersCount];
        nodesAlreadySeen = new HashSet<>();
        
        this.map = map;
        
        for(int i=0; i<workerServersCount; ++i) {
            Registry reg = LocateRegistry.getRegistry(host, Integer.parseInt(serverPorts[i]));
            workerServers[i] = (ServerInterface) reg.lookup("server");
        }
        executor = Executors.newFixedThreadPool(workerServersCount);
    }
    
    private Map map;
    private int workerServersCount;
    private ExecutorService executor;
    private ServerInterface[] workerServers;
    private int[] workerNodesCount;
    private int[] workerFromNodes;
    private HashSet<Integer> nodesAlreadySeen;
    
    public void run() throws InterruptedException, RemoteException {
        final int[][] weights = map.getWeights();
        //String[] nodesNames = map.getNodesNames();
        int nodesCount = map.getNodesCount();
        
        int[] distances = new int[nodesCount];
        int[] prevNodes = new int[nodesCount];
        
        for(int i=0; i<nodesCount; ++i)
            distances[i] = prevNodes[i] = Integer.MAX_VALUE;
        
        int initialNode = 0; // TODO
        PriorityQueue<Integer> nodesToVisitQ = new PriorityQueue<>();
        nodesToVisitQ.add(initialNode);
        
        System.out.println("Sending weights to workers...");
        List<Callable<Object>> calls = new ArrayList<>();
        for(int i=0; i<workerServersCount; ++i) {
            final int workerId = i;
            calls.add(Executors.callable(() -> {
                System.out.println("Sending weights to worker " + workerId);
                try {
                    int[] nodeRanges = calculateWorkerNodeRanges(workerId);
                    int fromNode = nodeRanges[0];
                    int toNode = nodeRanges[1];
                    workerNodesCount[workerId] = toNode - fromNode + 1;
                    workerFromNodes[workerId] = fromNode;
                    workerServers[workerId].setInitialData(workerId, nodesCount, nodeRanges, weights);
                }
                catch(RemoteException e) {
                    e.printStackTrace();
                }
            }));
        }
        executor.invokeAll(calls);

        distances[initialNode] = 0;
        nodesAlreadySeen.add(initialNode);
        
        while(nodesToVisitQ.size() != 0) {
            Integer currentNode = nodesToVisitQ.poll();
            System.out.println("Going through node = " + currentNode);
            
            calls = new ArrayList<>();
            for(int i=0; i<workerServersCount; ++i) {
                final int workerId = i;
                calls.add(Executors.callable(() -> {
                    System.out.println("Sending weights to worker " + workerId);
                    try {
                        int[] workerDistances = workerServers[workerId].computeDistances(currentNode, distances[currentNode]);
                        System.arraycopy(workerDistances, 0, distances, workerFromNodes[workerId], workerNodesCount[workerId]);
                    }
                    catch(RemoteException e) {
                        e.printStackTrace();
                    }
                }));
            }
            executor.invokeAll(calls);
            
            for(int node=0; node<nodesCount; ++node)
                if (nodesAlreadySeen.contains(node) == false && isConnected(currentNode, node)) {
                    nodesToVisitQ.add(node);
                    nodesAlreadySeen.add(node);
                }
        }
        
        calls = new ArrayList<>();
        for(int i=0; i<workerServersCount; ++i) {
            final int workerId = i;
            calls.add(Executors.callable(() -> {
                try {
                    int[] workerPrevNodes = workerServers[workerId].getWorkerPrevNodesPart();
                    System.out.println(workerId + ", fromNode=" + workerFromNodes[workerId] + ", count=" + workerNodesCount[workerId]);
                    System.arraycopy(workerPrevNodes, 0, prevNodes, workerFromNodes[workerId], workerNodesCount[workerId]);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }));
        }
        executor.invokeAll(calls);
        
        System.out.println("Dijkstra algorithm over");
        System.out.println("Started from node index = " + initialNode);
        System.out.print("Distances (X means no path) = [");
        for(int node=0; node<nodesCount; ++node) {
            if (distances[node] == Integer.MAX_VALUE)
                System.out.print("X, ");
            else
                System.out.print(distances[node] + ", ");
        }
        System.out.println("\b\b]");
        
        System.out.print("PrevNodes (X means initialNode) = [");
        for(int node=0; node<nodesCount; ++node) {
            if (node == initialNode)
                System.out.print("X, ");
            else
                System.out.print(prevNodes[node] + ", ");
        }
        System.out.println("\b\b]");
        
        executor.shutdown();
    }
    
    private boolean isConnected(int fromNode, int toNode) {
        return this.map.getWeights()[fromNode][toNode] != -1;
    }
    private int[] calculateWorkerNodeRanges(int workerServerId) {
        int nodesCount = map.getNodesCount();
        int[] results = new int[2];
        
        int fromNode = (nodesCount / workerServersCount) * workerServerId;
        int toNode = (nodesCount / workerServersCount) * (workerServerId + 1) - 1;
        
        int restNodes = nodesCount % workerServersCount;
        
        if (workerServerId < restNodes) {
            fromNode += workerServerId;
            toNode += workerServerId + 1;
        }
        else {
            fromNode += restNodes;
            toNode += restNodes;
        }
        
        results[0] = fromNode;
        results[1] = toNode;
        
        return results;
    }
}
