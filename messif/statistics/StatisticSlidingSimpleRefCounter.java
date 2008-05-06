/*
 * StatisticSlidingSimpleRefCounter.java
 *
 * Created on 15. unor 2007, 12:59
 *
 */

package messif.statistics;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author xbatko
 */
public class StatisticSlidingSimpleRefCounter extends StatisticSimpleWeakrefCounter {
    /** Class version id for serialization */
    private static final long serialVersionUID = 2L;
    
    
    /** This simple class encapsulates the pair: key and integer value + automatically updated time when it was added/updated */
    protected static final class SlidingValue implements Serializable {
        /** Class version id for serialization */
        private static final long serialVersionUID = 1L;
        
        /** The hash key of this value */
        private final Object key;
        /** The numeric value */
        private long value;
        /** The time this value was last updated */
        private long time;
        
        /** Creates a new instance of SlidingValue */
        public SlidingValue(long value, Object key) {
            this.value = value;
            this.time = System.currentTimeMillis();
            this.key = key;
        }
        
        public final void setValue(long value) {
            this.value = value;
            time = System.currentTimeMillis();
        }
        
        public final void addValue(long value) {
            this.value += value;
            time = System.currentTimeMillis();
        }
        
        public final long getTime() {
            return time;
        }
        
        public final long getValue() {
            return value;
        }
        
        public final Object getKey() {
            return key;
        }
        
        /** Compare the items according to the key */
        public final boolean equals(Object obj) {
            if (!(obj instanceof SlidingValue))
                return false;
            if (key == null)
                return super.equals(obj);
            SlidingValue source = (SlidingValue)obj;
            if (source.key == null)
                return super.equals(obj);
            return key.equals(source.key);
        }
        
        /** HashCode is consistent with equals */
        public final int hashCode() {
            if (key == null)
                return super.hashCode();
            return key.hashCode();
        }
        
        /** To string method */
        public String toString() {
            return "<"+value+","+time+">";
        }
    }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of StatisticSlidingSimpleRefCounter */
    protected StatisticSlidingSimpleRefCounter(String name) {
        super(name);
        // set the resetTime to current time
        resetTime = System.currentTimeMillis();
    }
    
    /** Create new statistic counter with specified name or get the one already existing */
    public static StatisticSlidingSimpleRefCounter getStatistics(String name) throws ClassCastException {
        return statistics.get(name, StatisticSlidingSimpleRefCounter.class);
    }
    
    
    /** Deserialization of the object - crate the transient fields */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        window = new LinkedList<SlidingValue>();
        sum = 0;
        resetTime = System.currentTimeMillis();
        
        if (isRegisteredGlobally()) {
            Map<String, Class<? extends StatisticSimpleWeakrefCounter>> storedNames = (Map<String, Class<? extends StatisticSimpleWeakrefCounter>>)in.readObject(); // This cast IS checked, because of the serialization
            
            // Restore bindings
            for (Map.Entry<String, Class<? extends StatisticSimpleWeakrefCounter>> entry : storedNames.entrySet())
                multiBindTo(statistics.get(entry.getKey(), entry.getValue()));
        }
        if (replaceWith != null) {
            ((StatisticSlidingSimpleRefCounter) replaceWith).maxNumberOfValues = maxNumberOfValues;
            ((StatisticSlidingSimpleRefCounter) replaceWith).windowSizeMilis = windowSizeMilis;
        }
    }
    
    /** Serialization - including references to bound statistics */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        // Check if this statistic is registered in global registry (thus not using equals!)
        if (isRegisteredGlobally()) {
            Map<String, Class<? extends StatisticSimpleWeakrefCounter>> storedNames = new HashMap<String, Class<? extends StatisticSimpleWeakrefCounter>>();
            if (multiBoundTo != null) {
                for (StatisticSimpleWeakrefCounter stat : multiBoundTo)
                    if (stat.isRegisteredGlobally())
                        storedNames.put(stat.getName(), stat.getClass());
            }
            out.writeObject(storedNames);
        }
    }
    
    /****************** Sliding window settings ******************/
    
    /**
     * The maximal number of values stored in the sliding window.
     * If the parameter is zero all values will be used.
     * Default is 100.
     */
    private int maxNumberOfValues = 100;
    
    /**
     * Returns the maximal number of values stored in the sliding window.
     * @return the maximal number of values stored in the sliding window
     */
    public int getMaxNumberOfValues() {
        return maxNumberOfValues;
    }
    
    /**
     * Sets the maximal number of values stored in the sliding window. The
     * sliding window is NOT restricted by time.
     * @param maxNumberOfValues the maximal number of values stored in the sliding window.
     *        If zero the sliding window is not restricted by size.
     */
    public void setMaxNumberOfValues(int maxNumberOfValues) {
        this.maxNumberOfValues = maxNumberOfValues;
        this.windowSizeMilis = 0;
    }
    
    /**
     * Size of the time window in miliseconds
     * If this parameter is zero no time restrictions are placed.
     * Default is zero.
     */
    private long windowSizeMilis = 0;
    
    /**
     * Returns the size of the time window in miliseconds.
     * @return the size of the time window in miliseconds
     */
    public long getWindowSizeMilis() {
        return windowSizeMilis;
    }
    /**
     * Sets the size of the time window in miliseconds. The sliding window is
     * NOT restricted by number of values.
     * @param windowSizeMilis the size of the time window in miliseconds.
     *        If zero the sliding window is not restricted by time.
     */
    public void setWindowSizeMilis(long windowSizeMilis) {
        this.windowSizeMilis = windowSizeMilis;
        this.maxNumberOfValues = 0;
    }
    
    /**
     * Sets the maximal number of values stored in the sliding window and
     *  the size of the time window in miliseconds at the same time.
     * @param maxNumberOfValues the maximal number of values stored in the sliding window.
     *        If zero the sliding window is not restricted by size.
     * @param windowSizeMilis the size of the time window in miliseconds.
     *        If zero the sliding window is not restricted by time.
     */
    public void setNumberAndMilis(int maxNumberOfValues, long windowSizeMilis) {
        this.maxNumberOfValues = maxNumberOfValues;
        this.windowSizeMilis = windowSizeMilis;
    }
    
    /**
     * Store the time stamp of reseting (or creating) the statistics
     */
    private long resetTime;
    
    /**
     * Checks whether the sliding window has already been used for the full time period
     * @return true if the sliding window has already been used for the full time period
     */
    public boolean checkUsedTime() {
        if (windowSizeMilis <= 0)
            return true;
        return (System.currentTimeMillis() - resetTime > windowSizeMilis);
    }
    
    
    /****************** Sliding window values ******************/
    
    /** Queue of actual values with their time stamps - expect that this queue is sorted according to the times */
    private transient Queue<SlidingValue> window = new LinkedList<SlidingValue>();
    
    /** Sum of all stored values - to speed up computations and locking safety */
    protected transient int sum = 0;
    
    /** Method for consolidating the queue - removes values that are either too old or the queue is too large */
    private synchronized void consolidate() {
        //If the sliding window is restricted by time.
        if (windowSizeMilis > 0) {
            long windowBeginning = System.currentTimeMillis() - windowSizeMilis;
            while (!window.isEmpty() && window.peek().getTime() < windowBeginning)
                pollLastValue();
        }
        //If the sliding window is restricted by number of values
        if (maxNumberOfValues > 0) {
            while (window.size() > maxNumberOfValues)
                pollLastValue();
        }
    }
    
    /** Removes the last (oldest) value from the sliding window */
    private synchronized SlidingValue pollLastValue() throws NoSuchElementException {
        SlidingValue rtv = window.remove();
        sum -= rtv.getValue();
        if (rtv.getKey() != null)
            super.remove(rtv.getKey(), false);
        return rtv;
    }
    
    /** Removes the value with the specified key from the sliding window */
    private synchronized SlidingValue removeValue(Object key) {
        Iterator<SlidingValue> i = window.iterator();
        while (i.hasNext()) {
            SlidingValue value = i.next();
            if (key.equals(value.getKey())) {
                i.remove();
                return value;
            }
        }
        return null;
    }
    /*
    public synchronized void add(long value) {
        if (!canPerformOperation())
            return;
        
        window.add(new SlidingValue(value, null));
        sum += value;
    }*/
    
    public void add(Object key, long value) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            SlidingValue slidingValue;
            if (containsKey(key)) {
                slidingValue = removeValue(key);
                slidingValue.addValue(value);
            } else slidingValue = new SlidingValue(value, key);

            super.add(key, value);

            window.add(slidingValue);
            sum += value;
        }
    }
    
    public void set(Object key, long value) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            SlidingValue slidingValue;
            if (containsKey(key)) {
                slidingValue = removeValue(key);
                sum -= slidingValue.getValue();
                slidingValue.setValue(value);
            } else slidingValue = new SlidingValue(value, key);

            super.set(key, value);

            window.add(slidingValue);
            sum += value;
        }
    }
    
    public synchronized boolean remove(Object key, boolean propagateDelete) {
        if (!super.remove(key, propagateDelete))
            return false;
        
        SlidingValue slidingValue = removeValue(key);
        if (slidingValue != null)
            sum -= slidingValue.getValue();
        return (slidingValue != null);
    }
    
    public synchronized void reset() {
        super.reset();
        window.clear();
        sum = 0;
        resetTime = System.currentTimeMillis();
    }
    
    
    /***********************  Getters  *************************/
    
    /** return the sum of queue values */
    public int getSum() {
        if (windowSizeMilis > 0)
            consolidate();
        return sum;
    }
    /** return number of values in the queue */
    public int getCnt() {
        if (windowSizeMilis > 0)
            consolidate();
        return window.size();
    }
    /** return average of the values in queue */
    public double getAvg() {
        return getAvg(true);
    }
    /** return the average but do not consolidate the list of values if the parameter is false */
    public double getAvg(boolean consolidate) {
        if (windowSizeMilis > 0 && consolidate)
            consolidate();
        synchronized (this) {
            return window.isEmpty()?0:((double)sum)/window.size();
        }
    }
    /** return minimum value from the queue */
    public synchronized long getMin() {
        if (windowSizeMilis > 0)
            consolidate();
        long min = Integer.MAX_VALUE;
        for (SlidingValue val : window)
            if (min > val.getValue())
                min = val.getValue();
        return min;
    }
    /** return maximum value from the queue */
    public synchronized long getMax() {
        if (windowSizeMilis > 0)
            consolidate();
        long max = 0;
        for (SlidingValue val : window)
            if (max < val.getValue())
                max = val.getValue();
        return max;
    }
    
    
    /****************** Statistic objects bindings ******************/
    
    /** Specifies statistic object, which is this statistic bound to */
    protected transient Set<StatisticSimpleWeakrefCounter> multiBoundTo = null;
    
    /**
     * Bind current statistics object to receive notifications at the same time as the specified statistics receives some.
     * This method allows to bind to multiple StatisticSimpleWeakrefCounter statistics at a time.
     * @param stat the parent statistics object
     */
    public void multiBindTo(StatisticSimpleWeakrefCounter stat) {
        if (stat == null)
            return;
        
        synchronized (stat) {
            synchronized (this) {
                if (multiBoundTo == null)
                    multiBoundTo = new HashSet<StatisticSimpleWeakrefCounter>();
                if (multiBoundTo.contains(stat))
                    return;
                stat.addBoundStat(this);
                multiBoundTo.add(stat);
            }
        }
    }
    
    /** Deregister ourselves from specified object */
    public void multiUnbindFrom(StatisticSimpleWeakrefCounter stat) {
        if (multiBoundTo == null)
            return;
        
        synchronized (stat) {
            synchronized (this) {
                if (multiBoundTo.remove(stat))
                    stat.removeBoundStat(this);
            }
        }
    }
    
    /** Deregister ourselves from list of "parent" object */
    public void multiUnbind() {
        if (multiBoundTo == null)
            return;
        
        Iterator<StatisticSimpleWeakrefCounter> i = multiBoundTo.iterator();
        while (i.hasNext()) {
            StatisticSimpleWeakrefCounter stat = i.next();
            synchronized (stat) {
                synchronized (this) {
                    stat.removeBoundStat(this);
                    i.remove();
                }
            }
        }
    }
    
}
