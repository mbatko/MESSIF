/*
 * Statistics.java
 *
 * Created on 25. duben 2004, 14:04
 */

package messif.statistics;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 *
 * @author  xbatko
 */
public abstract class Statistics<TSelf extends Statistics> implements Serializable {
    
    /** Class id for serialization */
    private static final long serialVersionUID = 3L;

    /****************** Name internal ******************/
    
    // Statistic name, used for registration and is mandatory
    protected final String name;
    
    /** Returns the registered name of the statistics */
    public String getName() {
        return name;
    }


    /****************** Constructors ******************/

    /** Creates instance of Statistics with filled internal data */
    protected Statistics(String name) {
        // Set statistic name
        this.name = name;
    }

    protected static <T extends Statistics> T createInstance(String statisticName, Class<T> statisticClass) throws IllegalArgumentException {
        try {
            // Create a new instance of the statistics
            return statisticClass.getDeclaredConstructor(String.class).newInstance(statisticName);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(statisticClass.getName() + " can't be used, because it has no valid constructor");
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Can't create a statistics object", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Can't create a statistics object", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Can't create a statistics object", e);
        }
    }


    /****************** Statistics data methods ******************/
    
    protected abstract void updateFrom(TSelf sourceStat);
    protected abstract void setFrom(TSelf sourceStat);
    
    /** Reset the current statistic (this one only).
     */
    public abstract void reset();
    

    /****************** Statistic objects bindings ******************/
    
    /** Specifies statistic object, which is this statistic bound to */
    protected transient TSelf boundTo = null;
    
    /** In this list there are stored all the statistics that has registered to get the same values as this stat */
    private transient Map<TSelf,Object> boundStatistics = new WeakHashMap<TSelf,Object>();
    
    /** Add a statistic into list */
    protected void addBoundStat(TSelf stat) {
        boundStatistics.put(stat, null);
    }

    /** Remove a statistic from list */
    protected void removeBoundStat(TSelf stat) {
        boundStatistics.remove(stat);
    }

    /** Enumeration of statistics bound to this (for subclasses only) */
    protected final Set<TSelf> getBoundStats() {
        return boundStatistics.keySet();
    }
   
    /** Deregister ourselves from list of "parent" object */
    public void unbind() {
        try {
            if (boundTo != null)
                synchronized (boundTo) {
                    synchronized (this) {
                        if (boundTo != null) {
                            boundTo.removeBoundStat(this);
                            boundTo = null;
                        }
                    }
                }
        } catch (NullPointerException e) {
            // This exception can be silently ignored, because boundTo was null...
        }
    }

    /** Bind current statistics object to receive notifications at the same time as the specified statistics receives some.
     *  @param object the parent statistics object
     */
    public void bindTo(TSelf object) throws IllegalArgumentException {
        // Ignore null argument
        if (object == null)
            return;
        
        // Must maintain lock order (as in the statistics internal method call)
        synchronized (object) {
            synchronized (this) {
                if (boundTo == object) return; // Object already bound to the same object, can be ignored silently
                if (boundTo != null) throw new IllegalArgumentException("Can't bind statistics twice");
            
                // Adding this statistics into the list of notifications in the target statistic
                object.addBoundStat(this); // This cast IS checked, if the destination statistics object is correctly typed (which sould be)
                boundTo = object;
            }
        }
    }


    /****************** Thread operation locking ******************/

    // The next serial number to be assigned
    private static AtomicInteger nextThreadNum = new AtomicInteger(1);
    private static ThreadLocal<Integer> threadNum = new InheritableThreadLocal<Integer>() {
     protected synchronized Integer initialValue() {
         return nextThreadNum.getAndIncrement();
     }
    };
    static Integer getCurrentThreadLock() { return threadNum.get(); }
    
    // Operations thread number lock
    private transient Integer lockedThreadNum = null;

    /** Lock the operations of this statistic to current thread and all its descendants */
    void lockToThread() {
        lockedThreadNum = getCurrentThreadLock();
    }
    
    /** Unlock the operations from thread */
    void unlockFromThread() {
        lockedThreadNum = null;
    }
    
    /** Check lock (only for subclasses) */
    protected final boolean canPerformOperation() {
        return statisticsEnabled && ((lockedThreadNum == null) || lockedThreadNum.equals(threadNum.get()));
    }


    /****************** Suspending Stastics Counting ******************/
    
    /** Flag for enabling/disabling statistics globally */
    private static boolean statisticsEnabled = true;

    /**
     * Returns <tt>true</tt> if statistics are globally enabled.
     * @return <tt>true</tt> if statistics are globally enabled
     */
    public static final boolean isEnabledGlobally() {
        return statisticsEnabled;
    }

    /** Disables all statistic counting globally. */
    public static void disableGlobally() {
        statisticsEnabled = false;
    }

    /** Enables all statistic counting globally. */
    public static void enableGlobally() {
        statisticsEnabled = true;
    }


    /****************** Serialization ******************/
    
    protected transient Statistics replaceWith = null;
            
    // Initialize boundStatistics attribute (must be empty after deserialization)
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boundStatistics = new WeakHashMap<TSelf,Object>();

        // If this stat was registered globally
        if (in.readBoolean()) {
            // Get global statistics with this name
            replaceWith = statistics.get(getName());
            
            // Register this statistic
            if (replaceWith == null) {
                statistics.add(this);
                replaceWith = this;
            }

            // Read boundTo statistic name
            String boundToName = (String)in.readObject();
            Class boundToClass = (Class)in.readObject();

            // Restore bindings
            if (boundToName != null && replaceWith.boundTo == null)
                replaceWith.bindTo(statistics.get(boundToName, boundToClass)); // This cast IS checked, because replaceWith.getClass() is the correct class...
        }
    }
    
    
    protected Object readResolve() throws ObjectStreamException {
        if (replaceWith != null)
            return replaceWith;
        else return this;
    }


    // Store flags for deserialization - global stat registration & bindings
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        // Check if this statistic is registered in global registry (thus not using equals!)
        if (isRegisteredGlobally()) {
            out.writeBoolean(true);
            if ((boundTo != null) && boundTo.isRegisteredGlobally()) {
                out.writeObject(boundTo.getName());
                out.writeObject(boundTo.getClass());
            } else {
                out.writeObject(null);
                out.writeObject(null);
            }
        } else out.writeBoolean(false);
    }

    
    /****************** Checkpoint facilities ***********/
    
    /** Sets checkpoint. The implementation should store the current state of statistics and
     * provide the information about changes in statistics through changedSinceCheckpoint().
     */
    public abstract void setCheckpoint();
    
    /** Reports if statistics have been changed since the last setCheckpoint() call.
     */
    public abstract boolean changedSinceCheckpoint();

    
    /****************** Global statistics registry ******************/
    
    protected static final StatisticsList statistics = new StatisticsList();

    /** Returns true if current (this) statistic is present in global statistics registry */
    protected boolean isRegisteredGlobally() {
        // This check if this particular object is the one returned by its name in global registry
        return this == statistics.get(getName());
    }

    /** 
     * Remove the statistic from global registry 
     * @param name name of the statistic
     * @return <code>true</code> if the statistic exist(ed)
     */
    public static boolean removeStatistic(String name) {
        return statistics.remove(name) != null;
    }
    
    /** Resets all statistics */
    public static void resetStatistics() {
        statistics.reset();
    }
    
    /** Resets statistics matching the regular expression */
    public static void resetStatistics(String regex) {
        statistics.reset(regex);
    }
    
    /** Returns String containing current states of registered statistics with names matching the provided regular expression
        and separated by specified separator
     */
    public static String printStatistics(String regex, String statSeparator) {
        return statistics.print(regex, statSeparator);
    }

    /** Returns String containing current states of registered statistics with names matching the provided regular expression */
    public static String printStatistics(String regex) {
        return statistics.print(regex);
    }

    /** Returns String containing current states of registered statistics */
    public static String printStatistics() {
        return statistics.print();
    }
    
    /** Access all statistics */
    public static Iterator<Statistics> getAllStatistics() {
        return getAllStatistics(null);
    }
   
    /** Access statistics whose names match the given regular expression */
    public static Iterator<Statistics> getAllStatistics(String regex) {
        return statistics.iterator(regex);
    }

}
