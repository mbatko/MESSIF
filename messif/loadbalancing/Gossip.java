/*
 * Gossip.java
 *
 * Created on September 13, 2006, 15:26
 */

package messif.loadbalancing;

import java.io.Serializable;

/**
 * The information spread together with messages to share the info about average load of the nodes
 * using the gossip-based distributed algorithm that keeps the peers informed about the
 * average values of load of other nodes. See paper
 * inproceedings{kempe03gossip,
 *   author = {David Kempe and Alin Dobra and Johannes Gehrke},
 *   title = {Gossip-Based Computation of Aggregate Information},
 *   booktitle = {FOCS '03: Proceedings of the 44th Annual IEEE Symposium on Foundations of Computer Science},
 *   year = {2003},
 *   publisher = {IEEE Computer Society},
 *   address = {Washington, DC, USA}
 * }
 * 
 * It also works to exchange information about the least loaded nodes in the system
 *
 * @author xnovak8
 */
public class Gossip implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 2L;
        
    /** Creates a new instance of Gossip setting all fields */
    public Gossip(double processingCostsAvg, double waitingCostsAvg, double dataLoadAvg, double estimationWeight, HostList unloadedPeers, HostList loadedPeers) {
        this.procLoadAvg = processingCostsAvg;
        this.waitLoadAvg = waitingCostsAvg;
        this.dataLoadAvg = dataLoadAvg;
        this.estimationWeight = estimationWeight;
        this.unloadedPeers = unloadedPeers;
        this.loadedPeers = loadedPeers;
    }
    
    
    /**********************   Load information    ***************************/
    /** processing-costs estimation of the sender */ 
    protected double procLoadAvg = 0;
    
    /** waiting-costs estimation of the sender */ 
    protected double waitLoadAvg = 0;
    
    /** storage-costs estimation of the sender */ 
    protected double dataLoadAvg = 0;
    
    /** weight of the two estimations */
    protected double estimationWeight = 0;
    
    /** this method sets the average estimations of the current node */
    public void setEstimations(double procLoadAvg, double waitLoadAvg, double dataLoadAvg, double estimationWeight) {
        this.procLoadAvg = procLoadAvg;
        this.waitLoadAvg = waitLoadAvg;
        this.dataLoadAvg = dataLoadAvg;
        this.estimationWeight = estimationWeight;
    }
    
    /*********************** Unloaded hosts information ***********************/
    
    /** List of low-loaded hosts */
    protected HostList unloadedPeers;
    /** List of heavily loaded hosts */
    protected HostList loadedPeers;

    public HostList getLoadedPeers() {
        return loadedPeers;
    }

    public HostList getUnloadedPeers() {
        return unloadedPeers;
    }
    
    /** toString() method */
    public String toString() {
        return "single-load: "+procLoadAvg+", busy-load: "+waitLoadAvg+", data: "+dataLoadAvg+", weight: "+estimationWeight+"\n  unloaded peers: "+unloadedPeers+"\n  loaded peers: "+loadedPeers;
    }
}
