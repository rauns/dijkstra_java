package Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.HashSet;
import Shared.*;

public class Server extends UnicastRemoteObject implements ServerInterface {
    public Server() throws RemoteException {
        super();
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 2) {
            System.out.println("Server usage: <host> <ports>...");
            return;
        }

        String host = args[0];

        for(int i=1; i<args.length; ++i) {
            try {
                String port = args[i];
                System.setProperty("java.rmi.server.hostname", host);
                Registry reg = LocateRegistry.createRegistry(Integer.parseInt(port));
                reg.rebind("server", new Server());
                System.out.println("Server started on " + host + ":" + port);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private int[][] weights;
    private int fromNode;
    private int toNode;
    private int workerId;
    private int nodesCount;
    private int[] distances;
    private int[] prevNodes;
    private HashSet<Integer> visitedNodes;
    
    public void setInitialData(int workerId, int nodesCount, int[] ranges, int[][] weights) throws RemoteException {
        this.weights = weights;
        this.workerId = workerId;
        this.fromNode = ranges[0];
        this.toNode = ranges[1];
        this.nodesCount = nodesCount;
        this.visitedNodes = new HashSet<>();
        this.distances = new int[nodesCount];
        this.prevNodes = new int[nodesCount];
        
        for(int i=0; i<nodesCount; ++i)
            this.distances[i] = this.prevNodes[i] = Integer.MAX_VALUE;
    }
    
    public int[] computeDistances(Integer currentNode, int distanceToCurrentNode) throws RemoteException {
        distances[currentNode] = distanceToCurrentNode;
        
        for(int node=this.fromNode; node<=this.toNode; ++node) {
            if (visitedNodes.contains(node)) {
                System.out.println("Worker " + this.workerId + ": node " + node + " already visited.");
                continue;
            }
            
            if (isConnected(currentNode, node)) {
                int nodeDistance = this.weights[currentNode][node];
                int totalCostToNode = distances[currentNode] + nodeDistance;
                
                System.out.println("Worker " + workerId + ": Node " + currentNode + " is connected to " + node + " (dist: " + nodeDistance + ", totalCostToNode: " + totalCostToNode + ")");
                if (totalCostToNode < distances[node]) {
                    distances[node] = totalCostToNode;
                    prevNodes[node] = currentNode;
                    System.out.println("New total cost is less then the old, replacing");
                }
            }
        }
        
        visitedNodes.add(currentNode);
        return this.getWorkerDistancesPart();
    }
    
    public int[] getWorkerPrevNodesPart() throws RemoteException {
        return this.getWorkerArrayPart(this.prevNodes);
    }
    
    private boolean isConnected(int fromNode, int toNode) {
        return this.weights[fromNode][toNode] != -1;
    }
    
    private int[] getWorkerDistancesPart() {
        return this.getWorkerArrayPart(this.distances);
    }
    
    private int[] getWorkerArrayPart(int[] array) {
        int[] result = new int[this.toNode - this.fromNode + 1];
        for(int i=this.fromNode; i<=this.toNode; ++i)
            result[i-this.fromNode] = array[i];
        return result;
    }
}
