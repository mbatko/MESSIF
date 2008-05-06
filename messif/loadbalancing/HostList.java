/*
 * HostList.java
 *
 * Created on September 12, 2006, 16:49
 */

package messif.loadbalancing;

import java.net.UnknownHostException;
import messif.network.NetworkNode;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class encapsulates list of low-loaded nodes that can be exploited for
 * load balancing.
 *
 * @author xnovak8
 */
public class HostList implements Serializable {

    /** Class id for serialization */
    private static final long serialVersionUID = 3L;
    
    /** Sorted set of values. */
    private SortedSet<HostLoad> values;
    
    /** max number of hosts in the list */
    protected int maxSize = 10;
    /** set the maximal size of this list; remove the last objects if necessary
     * @throws IllegalArgumentException if the new size is a negative number */
    public void setMaxSize(int maxSize) {
        if (maxSize <= 0)
            throw new IllegalArgumentException("List size must be positive");
        
        this.maxSize = maxSize;
        consolidate();
    }
    
    /** Cut the size of the list in order not to acceed the max size
     * (with expetion of data = 0) */
    private void consolidate() {
        // the max size is ignored if the whole list is composed of LOAD_EMPTY hosts
        while ((values.size() > maxSize)) // && (values.last().storage != 0))
            values.remove(values.last());
    }
    
    /** A random number generator */
    private transient Random random = new Random();
    
    /** This host id */
    private final NetworkNode thisHost;

    /** If true, this list is sorted increasingly - it's a UnloadedList */
    private boolean increasing;
    
    /** Ordering of the list. If true, then this list is increasing (list of least loaded peers). */
    private transient Comparator<? super HostLoad> comparator;
    
    /**
     * Creates a new instance of HostList
     */
    public HostList(int maxSize, NetworkNode thisHost, boolean increasing) {//, MessageDispatcher messageDisp) {
        this.increasing = increasing;
        if (increasing)
            comparator = new IncreasingComparator();
        else comparator = new DecreasingComparator();
        
        values = Collections.synchronizedSortedSet(new TreeSet<HostLoad>(comparator));
        setMaxSize(maxSize);
        this.thisHost = thisHost;
    }
    
    /** Serialization method */
    private void writeObject(ObjectOutputStream out) throws IOException {
        synchronized (values) {
            out.defaultWriteObject();
        }
    }
    
    /** Deserialization method */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        random = new Random();
        comparator = values.comparator();
    }
    
    /** Return the size of the list */
    public int size() {
        return values.size();
    }
    
    
    /********************* Manipulation *************************/
    
    /** given a list, update this list from it - checking the timestamps, of course.
     *    Return the host list that should be replied to the sender -
     *         always return values for hosts that are passed to me, but I know them better (newer)
     *  NEW VERSION: always remember and store those values that
     *        - I have a newer information then somebody else
     *        - somebody else has a newer information than me
     */
    public void updateFrom(HostList newList) {
        synchronized (values) {
            // always return values for hosts that are passed to me, but I know them better (newer)
            //List<HostLoad> returnValues = new ArrayList<HostLoad>();
            // always store values that the sender knows better than me (and I stored them before)
            List<HostLoad> storeValues = new ArrayList<HostLoad>();
            for (HostLoad newVal : newList.values) {
                for (HostLoad myVal : values) {
                    if (newVal.equals(myVal)) {
                        if (myVal.timeStamp > newVal.timeStamp) {
                            storeValues.add(myVal);
                            //returnValues.add(myVal);
                        } else if (myVal.timeStamp < newVal.timeStamp)
                            storeValues.add(newVal);
                    }
                }
            }
            consolidate();
            // now normally add the values to my list
            for (HostLoad hl : newList.values)
                if (! hl.host.equals(thisHost))
                    add(hl);
            values.addAll(storeValues);
            
            /*HostList listToReply = new HostList(maxSize, thisHost, increasing);
            listToReply.values.addAll(values);
            listToReply.values.addAll(returnValues);
            return listToReply;*/
        }
    }
    
    /** adds given tripple host, load, timeStamp to this list
     *   - checks whether the host is already in the list
     *   - if so, update the "load" info if having newer info about a host in the list
     *   - add the host only if the capacity allows it to be added (or lower load then some host in the list)
     *
     * @return boolean whether the host was added (or has already been in the list)
     */
    public boolean add(HostLoad hl) {
        synchronized (values) {
            for (Iterator<HostLoad> iterator = values.iterator(); iterator.hasNext(); ) {
                HostLoad existing = iterator.next();
                if (hl.equals(existing)) {
                    if (hl.timeStamp <= existing.timeStamp)
                        return true;
                    // if we have newer information then replace the old value
                    iterator.remove();
                    break;
                    //values.add(hl);
                    //return true;
                }
            }
            // if the host has not been in the set, then add it if it should be added
            // do not add host to a full list if not "better" then the worst of them
            //if ((values.size() >= maxSize) && (! (hl.storage == 0)) && (hl.compareTo(values.last()) > 0))
            if ((values.size() >= maxSize) && (comparator.compare(hl, values.last()) > 0))
                return false;
            
            // add it and remove if overfull
            values.add(hl);
            // the max size is ignored if the whole list is composed of empty hosts
            if ((values.size() > maxSize)) // && (values.last().storage != 0))
                values.remove(values.last());
            return true;
        }
    }

    /** Add information about this host even if it shouldn't be added */
    public void addMyself(Host host) {
        HostLoad me = new HostLoad(host);
        synchronized (values) {
            values.remove(me);
            values.add(me);
        }
    }
    
    /** Returns the list of the current state of the peer list - excluding this peer */
    protected List<HostLoad> getCurrentPeers() {
        List<HostLoad> retVal = new ArrayList<HostLoad>(maxSize);
        synchronized (values) {
            for (HostLoad hostLoad : values) {
                if (! hostLoad.host.equals(thisHost))
                    retVal.add(hostLoad);
            }
        }
        return retVal;
    }
    
    /** Returns a random host from the list - different from this host */
    protected NetworkNode getRandomHost() {
        synchronized (values) {
            if (values.isEmpty())
                return null;
            if (values.size() == 1) {
                if (values.first().host.equals(thisHost))
                    return null;
                return values.first().host;
            }
            int position = random.nextInt(values.size()-1);
            Iterator<HostLoad> it = values.iterator();
            for (int i = 0 ; i < position ; i++)
                it.next();
            NetworkNode host = it.next().host;
            if (host.equals(thisHost))
                host = it.next().host;
            return host;
        }
    }
    
    /** Clear the list */
    public void clear() {
        values.clear();
    }
    
    /** to string */
    public String toString() {
        synchronized (values) {
            return values.toString();
        }
    }
    
    public static void main(String[] args) {
        NetworkNode thisNode;
        NetworkNode thisNode2;
        try {
            thisNode = new NetworkNode("manwe-c.ics.muni.cz", 20001);
            thisNode2 = new NetworkNode("manwe-c.ics.muni.cz", 20001);
            HostList unloadedPeers = new HostList(LBSettings.PEER_LIST_SIZE, thisNode, false);
            HostList unloadedPeers2 = new HostList(LBSettings.PEER_LIST_SIZE, thisNode, false);
            HostLoad thisLoad1 = new HostLoad(thisNode);
            thisLoad1.busyLoad = 0.0;
            thisLoad1.singleLoad = Double.valueOf(LBSettings.LOAD_DONT_KNOW);
            thisLoad1.storage = 13669.0;
            thisLoad1.timeStamp = System.currentTimeMillis();
            
            HostLoad thisLoad2 = new HostLoad(thisNode2);
            thisLoad2.busyLoad = Double.valueOf(LBSettings.LOAD_DONT_KNOW);
            thisLoad2.singleLoad = Double.valueOf(LBSettings.LOAD_DONT_KNOW);
            thisLoad2.storage = 11971.0;
            thisLoad2.timeStamp = System.currentTimeMillis()+10000;

//{manwe-c.ics.muni.cz:20001, 0, 2147483647, 13669.0 at Thu Apr 26 17:06:59 CEST 2007}, {manwe-c.ics.muni.cz:20001, 2147483647, 2147483647, 11971.0 at Thu Apr 26 17:07:20 CEST 2007}
            List<HostLoad> storeValues = new ArrayList<HostLoad>();
            storeValues.add(thisLoad1);
            storeValues.add(thisLoad2);
            
            unloadedPeers.values.add(thisLoad1);
            System.out.println(unloadedPeers);
            unloadedPeers.add(thisLoad2);
            System.out.println(unloadedPeers);
            unloadedPeers.values.remove(thisLoad1);
            unloadedPeers.values.add(thisLoad1);
            System.out.println(unloadedPeers);
            
            unloadedPeers.values.addAll(storeValues);
            System.out.println(unloadedPeers);
            System.out.println();
            
            unloadedPeers.add(thisLoad1);
            unloadedPeers.add(new HostLoad(new NetworkNode("zephyr", 3000)));

            unloadedPeers2.add(thisLoad2);
            unloadedPeers.updateFrom(unloadedPeers2);
            System.out.println(unloadedPeers);
            
            unloadedPeers2.updateFrom(unloadedPeers);
            System.out.println(unloadedPeers2);
            
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
    }
    
    protected static class IncreasingComparator implements Comparator<HostLoad>, Serializable {
        /** Class id for serialization */
        private static final long serialVersionUID = 1L;
        
        public int compare(HostList.HostLoad o1, HostList.HostLoad o2) {
            if (o1.host.equals(o2.host))
                return 0;
            //if ((o1.storage == 0) && (o2.storage != 0)) return -1;
            //if ((o2.storage == 0) && (o1.storage != 0)) return 1;
            
            return o1.compareToInternal(o2);
        }
    }

    protected static class DecreasingComparator implements Comparator<HostLoad>, Serializable {
        /** Class id for serialization */
        private static final long serialVersionUID = 1L;
        
        public int compare(HostList.HostLoad o1, HostList.HostLoad o2) {
            if (o1.host.equals(o2.host))
                return 0;
            if ((o1.busyLoad == LBSettings.LOAD_DONT_KNOW) && (o2.busyLoad != LBSettings.LOAD_DONT_KNOW)) return 1;
            if ((o2.busyLoad == LBSettings.LOAD_DONT_KNOW) && (o1.busyLoad != LBSettings.LOAD_DONT_KNOW)) return -1;
            
            return o2.compareToInternal(o1);
        }
    }
        
    /** internal class encapsulating a pair of Host + its load + time when the load was measured (local time of the host) */
    protected static class HostLoad implements Serializable {
        /** class id for serialization */
        private static final long serialVersionUID = 4L;
        
        /** Host address */
        public NetworkNode host;
        /** Host busy laod */
        public Double busyLoad;
        /** Host single laod */
        public Double singleLoad;
        /** Host storage size */
        public Double storage;
        /** Time stamp */
        public long timeStamp;
        
        /** Create a HostLoad instance given an NNID and setting all other values to 0 */
        public HostLoad(NetworkNode host) {
            this.host = host;
            this.busyLoad = 0d;
            this.singleLoad = 0d;
            this.storage = 0d;
            this.timeStamp = 0;
        }
        
        public HostLoad(Host host) {
            this.host = host.getThisNode();
            this.busyLoad = host.getBusyLoad();
            this.singleLoad = host.getSingleLoad();
            this.storage = host.getDataLoad();
            this.timeStamp = System.currentTimeMillis();
        }
        
        public HostLoad(HostLoad hl) {
            this.host = hl.host;
            this.singleLoad = hl.singleLoad;
            this.busyLoad = hl.busyLoad;
            this.storage = hl.storage;
            this.timeStamp = hl.timeStamp;
        }
        
        /** Internal compaterTo method. It's result is either simply returned or inverted depending on the order of the list */
        private int compareToInternal(HostLoad hl) {
            int c = busyLoad.compareTo(hl.busyLoad);
            if (c != 0) return c;
            
            c = singleLoad.compareTo(hl.singleLoad);
            if (c != 0) return c;
            
            c = storage.compareTo(hl.storage);
            if (c != 0) return c;
            
            c = ((Long) timeStamp).compareTo(hl.timeStamp);
            if (c != 0) return (-1) * c;
            
            // WARNING: here cannot be simple: return -1; since the function wouldn't be symetric!!!!!!!!!!!
            return host.toString().compareTo(hl.host.toString());
        }
        
        /** host-load equals iff the nosts (network nodes) equal */
        public boolean equals(Object o) {
            if (! (o instanceof HostLoad))
                return false;
            return host.equals(((HostLoad) o).host);
        }
        
        /** Override the hashCode to be consistent with equals */
        public int hashCode() {
            return host.hashCode();
        }
        
        public String toString() {
            return "{" + host + ", " + busyLoad.intValue() +", " + singleLoad.intValue() + ", " + storage + " at "+new Date(timeStamp)+"}";
        }
    }
    
    
}