/*
 * GossipModule.java
 *
 * Created on October 4, 2006, 11:01
 */

package messif.loadbalancing;

import messif.loadbalancing.HostList.HostLoad;
import messif.network.NetworkNode;
import messif.utility.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import messif.algorithms.DistAlgRequestMessage;

/**
 * The load-balancing module taking care of the gossip exchanging and estimating of the global average values
 *
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class GossipModule implements Serializable {
    
    /** Class id for serialization */
    private static final long serialVersionUID = 4L;
    
    /** Log */
    private static Logger log = Logger.getLoggerEx("Host");
    
    /** The corresponding host */
    private final Host host;
    
    /** This node id */
    private transient NetworkNode thisNode;
    
    /** Creates a new instance of GossipModule */
    public GossipModule(Host host) {
        this.host = host;
        this.thisNode = host.getThisNode();
        unloadedPeers = new HostList(LBSettings.PEER_LIST_SIZE, thisNode, true);
        loadedPeers = new HostList(LBSettings.PEER_LIST_SIZE, thisNode, false);
    }
    
    /** Start sending the messages, if there is no traffic */
    protected void startGossiping() {
        timerFlushGossips = new Timer();
        timerFlushGossips.schedule(new TimerTaskFlushGossips(), LBSettings.GOSSIP_T, LBSettings.GOSSIP_T);
    }
    /** Stops sending the messages */
    protected void stopGossiping() {
        timerFlushGossips.cancel();
        timerFlushGossips.purge();
        flushGossips();
    }
    
    /************************* (De)serialization ***************************/
    /** store the statistics in a correct way */
    private void writeObject(ObjectOutputStream out) throws IOException {
        flushGossips();
        
        out.defaultWriteObject();
    }
    
    /** Deserialization method - create the message dispatcher from the restored values "port", "broadcastport"
     * (for the top-most node). Create the activation receiver to wait for "start" message.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        procLoadCounter = (waitLoadCounter = (dataLoadCounter = (weightCounter = 0)));
        this.thisNode = host.getThisNode();
    }
    
    /***********************************   Management of average estimations using gossip  *****************/
    
    /** my waiting costs - the last value covered by the average estimation */
    private double lastWaitLoad = 0;
    /** my processing costs - the last value covered by the average estimation */
    private double lastProcLoad = 0;
    /** my storage load - the last value coverd by the gossip module */
    private double lastDataLoad = 0;
    
    /** my current processing-costs estimation */
    private double procLoadAvg = 0;
    /** my current waiting-costs estimation */
    private double waitLoadAvg = 0;
    /** my current storage load estimation */
    private double dataLoadAvg = 0;
    /** current weight of the two estimations */
    private double estimationWeight = 1;
    /** Getter */
    public double getEstimationWeight() {
        return estimationWeight;
    }
    
    /** counters according to the gossip algorithm */
    private transient double procLoadCounter = 0;
    private transient double waitLoadCounter = 0;
    private transient double dataLoadCounter = 0;
    private transient double weightCounter = 0;
    
    /** this method returns the actual estimation of the isProcessing costs average in the system */
    public double avgProcLoadEst() {
        return procLoadAvg / estimationWeight;
    }
    
    /** this method returns the actual estimation of the waiting costs average in the system */
    public double avgWaitLoadEst() {
        return waitLoadAvg / estimationWeight;
    }
    
    /** this method returns the actual estimation of the storage load average in the system */
    public double avgDataLoadEst() {
        return dataLoadAvg / estimationWeight;
    }
    
    /** gets the current values of my load and updates the system-average estimations using these values */
    private void updateMyLoads() {
        double newProcLoad = host.getSingleLoad();
        if (newProcLoad != LBSettings.LOAD_DONT_KNOW) {
            procLoadAvg += (newProcLoad - lastProcLoad);
            lastProcLoad = newProcLoad;
        }
        double newWaitLoad = host.getBusyLoad();
        if (newWaitLoad != LBSettings.LOAD_DONT_KNOW) {
            //log.info("SETTING WAIT LOAD in gossip module to "+newWaitLoad+". DONT_KNOW: "+LBSettings.LOAD_DONT_KNOW);
            waitLoadAvg += (newWaitLoad - lastWaitLoad);
            lastWaitLoad = newWaitLoad;
        }
        double newDataLoad = host.getDataLoad();
        if (newDataLoad != LBSettings.LOAD_DONT_KNOW) {
            dataLoadAvg += (newDataLoad - lastDataLoad);
            lastDataLoad = newDataLoad;
        }
    }
    
    /** update my average estimations using information from another host */
    protected synchronized void processGossip(Gossip gossip) {
        if (log.isLoggable(Level.FINE)) log.fine("Receiving message: "+this);
        //log.info("processing gossip: "+gossip);
        receiveGossipValues(gossip.procLoadAvg, gossip.waitLoadAvg, gossip.dataLoadAvg, gossip.estimationWeight);
        unloadedPeers.updateFrom(gossip.getUnloadedPeers());
        loadedPeers.updateFrom(gossip.getLoadedPeers());
    }
    
    /** sets my actual system-load estimation to a given message before it is send */
    public synchronized Gossip getGossip() {
        //    return getGossip(null);
        //}
        /* sets my actual system-load estimation to a given message before it is send */
        //public synchronized Gossip getGossip(HostList list) {
        if (host.loadBalancing) {
            if (log.isLoggable(Level.FINE)) log.fine("Sending message: "+this);
            updateMyLoads();
            
            //HostList listToSend = (list != null)?list:hostList;
            //listToSend.addMyself(host);
            unloadedPeers.addMyself(host);
            loadedPeers.addMyself(host);
            
            Gossip retVal = new Gossip(procLoadAvg/2, waitLoadAvg/2, dataLoadAvg/2, estimationWeight/2, unloadedPeers, loadedPeers);
            
            receiveGossipValues(procLoadAvg/2, waitLoadAvg/2, dataLoadAvg/2, estimationWeight/2);
            endOfGossipRound();
            return retVal;
        }
        return null;
    }
    
    /** process the received gossip values */
    private void receiveGossipValues(double procLoad, double waitLoad, double storageLoad, double estimationWeight) {
        if (log.isLoggable(Level.FINE)) log.fine("Receiving gossip values: "+procLoad+", "+waitLoad+", "+storageLoad+", "+estimationWeight);
        this.procLoadCounter += procLoad;
        this.waitLoadCounter += waitLoad;
        this.dataLoadCounter += storageLoad;
        this.weightCounter += estimationWeight;
    }
    
    /** remember the last time a round was ended */
    private long lastRoundEnd = 0;
    /** end the round according to the gossip algorithm */
    private void endOfGossipRound() {
        if (log.isLoggable(Level.FINE)) log.fine("End of gossip round; weight: "+weightCounter);
        procLoadAvg = procLoadCounter;
        waitLoadAvg = waitLoadCounter;
        dataLoadAvg = dataLoadCounter;
        estimationWeight = weightCounter;
        
        procLoadCounter = (waitLoadCounter = (dataLoadCounter = (weightCounter = 0)));
        lastRoundEnd = System.currentTimeMillis();
    }
    
    /** End the gossip round if it wasn't ended for quite a time */
    protected synchronized void flushGossips() {
        if (log.isLoggable(Level.FINE)) log.fine("Flushing gossip: "+this);
        updateMyLoads();
        if (weightCounter != 0) {
            receiveGossipValues(procLoadAvg, waitLoadAvg, dataLoadAvg, estimationWeight);
            endOfGossipRound();
        }
    }
    
    /** reset all values obtained from gossiping */
    protected synchronized void clearGossiping() {
        if (log.isLoggable(Level.FINE)) log.fine("Clearing gossip");
        lastWaitLoad = (lastProcLoad = (lastDataLoad = 0));
        
        procLoadAvg = (waitLoadAvg = (dataLoadAvg = 0));
        estimationWeight = 1;
        procLoadCounter = (waitLoadCounter = (dataLoadCounter = (weightCounter = 0)));
        unloadedPeers.clear();
        loadedPeers.clear();
    }
    
    /** periodically flush the gossip if no messages sent */
    protected transient Timer timerFlushGossips = null;
    /** TimerTask class to periodically flush (apply) the gossip info */
    protected class TimerTaskFlushGossips extends TimerTask {
        protected TimerTaskFlushGossips() { }
        
        public void run() {
            try {
                NetworkNode randomHost = null;
                if (System.currentTimeMillis() - lastRoundEnd >= LBSettings.GOSSIP_T) {
                    if (((randomHost = host.getRandomNode()) != null) && (! randomHost.equalsIgnoreNodeID(thisNode))) {
                        randomHost = new NetworkNode(randomHost, false);
                        host.getMessageDisp().sendMessageWaitReply(new DistAlgRequestMessage(null), randomHost);
                        if (log.isLoggable(Level.FINE)) log.fine("Sending gossip message to peer: "+randomHost);
                    } else flushGossips();
                } 
            } catch (IOException ex) {
                log.severe(ex);
            }
        }
    }
    
    /********************* Management of the low-loaded host available ************************/
    
    /** My list of low-loaded hosts */
    protected HostList unloadedPeers;
    
    /** My actual list of heavily loaded hosts */
    protected HostList loadedPeers;
    
    
    /** add given host+load to my list of low-loaded hosts */
    protected void addHostToLists(HostLoad hostLoad) {
        unloadedPeers.add(hostLoad);
        loadedPeers.add(hostLoad);
    }
    
    /** return the string representation of the load values */
    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append("avg_busy-load: "+(int) avgWaitLoadEst()+", avg_single-load: "+(int) avgProcLoadEst()+", avg_data-load: "+(int) avgDataLoadEst()+
                "; (weight: "+estimationWeight+"; wCounter: "+weightCounter+")\n");
        strBuf.append("Unloaded Peers: "+unloadedPeers+"\n");
        strBuf.append("Loaded Peers: "+loadedPeers);
        return strBuf.toString();
    }
}