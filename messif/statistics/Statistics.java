/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
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
 * Base class for all statistics.
 *
 * @param <TSelf> the type of this statistic
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Statistics<TSelf extends Statistics<TSelf>> implements Serializable {
    
    /** Class id for serialization */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//
    
    /** Name of this statistic */
    private final String name;
    /** Specifies statistic object, which is this statistic bound to */
    private transient TSelf boundTo = null;
    /**
     * Set of statistics that are bound to this statistic,
     * i.e. the statistics that receive notifications when the value
     * of this statistic is updated.
     * Only keys are relevant, values are not used.
     */
    private transient Map<TSelf, Object> boundStatistics = new WeakHashMap<TSelf, Object>();


    //****************** Constructors ******************//

    /**
     * Creates instance of Statistics with filled internal data
     * @param name the name of this stat
     */
    protected Statistics(String name) {
        // Set statistic name
        this.name = name;
    }

    /**
     * Creates a new instance of a {@link Statistics}.
     * This factory method is called either from global statistics registry or from the {@link OperationStatistics}.
     *
     * @param <T> the type of the statistic to create
     * @param statisticName the name of the statistic to create
     * @param statisticClass the type of the statistic to create
     * @return a new instance of a {@link Statistics}
     * @throws IllegalArgumentException if the statistics cannot be created
     */
    static <T extends Statistics<? extends T>> T createInstance(String statisticName, Class<? extends T> statisticClass) throws IllegalArgumentException {
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
            throw new IllegalArgumentException("Can't create a statistics object", e.getCause());
        }
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the registered name of this statistic.
     * @return the registered name of this statistic
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the statistic that this stat is bound to.
     * If this statistic is not bound, <tt>null</tt> is returned.
     * @return the statistic that this stat is bound to
     */
    protected TSelf getBoundTo() {
        return boundTo;
    }

    /**
     * Returns <tt>true</tt> if this statistic is bound to another one.
     * @return <tt>true</tt> if this statistic is bound to another one
     */
    protected boolean isBound() {
        return boundTo != null;
    }

    /**
     * Returns this statistics as the type provided typed argument.
     * This is a convenience method to avoid unchecked casts.
     * @return this statistics
     */
    protected abstract TSelf cast();


    //****************** Statistical data access methods ******************//

    /**
     * Updates the value of this statistic from the given {@code sourceStat}.
     * Specifically, this method merges the value of the {@code sourceStat}
     * with this statistic.
     *
     * <p>
     * The actual implementation depends on the type of the statistic.
     * </p>
     * 
     * @param sourceStat the statistic from which to update this stat
     */
    protected abstract void updateFrom(TSelf sourceStat);

    /**
     * Set the value of this statistic to the actual value of the given {@code sourceStat}.
     * @param sourceStat the statistic from which to set this stat
     */
    protected abstract void setFrom(TSelf sourceStat);
    
    /**
     * Reset the value of this statistic.
     */
    public abstract void reset();
    

    //****************** Statistic binding ******************//
    
    /**
     * Bind a statistic to this statistic.
     * That is, register the {@code stat} to receive notifications when
     * the value of this statistic is updated.
     * @param stat the statistic to register
     */
    protected void addBoundStat(TSelf stat) {
        boundStatistics.put(stat, null);
    }

    /**
     * Remove a statistic from list
     */
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
                            boundTo.removeBoundStat(cast());
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
                object.addBoundStat(cast()); // This cast IS checked, if the destination statistics object is correctly typed (which sould be)
                boundTo = object;
            }
        }
    }


    //****************** Thread operation locking ******************//

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


    //****************** Suspending Stastics Counting ******************//
    
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


    //****************** Serialization ******************//
    
    protected transient Statistics<TSelf> replaceWith = null;
            
    // Initialize boundStatistics attribute (must be empty after deserialization)
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boundStatistics = new WeakHashMap<TSelf,Object>();

        // If this stat was registered globally
        if (in.readBoolean()) {
            // Get global statistics with this name
            replaceWith = (Statistics<TSelf>)statistics.get(getName());
            
            // Register this statistic
            if (replaceWith == null) {
                statistics.add(this);
                replaceWith = this;
            }

            // Read boundTo statistic name
            String boundToName = (String)in.readObject();
            Class<TSelf> boundToClass = (Class)in.readObject();

            // Restore bindings
            if (boundToName != null && replaceWith.boundTo == null)
                replaceWith.bindTo(statistics.get(boundToName, boundToClass)); // This cast IS checked, because replaceWith.getClass() is the correct class...
        }
    }
    
    
    protected Object readResolve() throws ObjectStreamException {
        if (replaceWith != null)
            return replaceWith;
        else
            return this;
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

    
    //****************** Checkpoint facilities ***********//
    
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
    public static Iterator<Statistics<?>> getAllStatistics() {
        return getAllStatistics(null);
    }
   
    /** Access statistics whose names match the given regular expression */
    public static Iterator<Statistics<?>> getAllStatistics(String regex) {
        return statistics.iterator(regex);
    }

}
