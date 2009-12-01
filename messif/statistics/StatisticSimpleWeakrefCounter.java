/*
 * StatisticSimpleWeakrefCounter.java
 *
 * Created on 15. unor 2007, 12:00
 *
 */

package messif.statistics;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 * @author xbatko
 */
public class StatisticSimpleWeakrefCounter extends Statistics<StatisticSimpleWeakrefCounter> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Reference counter data ******************/
    protected transient Map<Object, Long> values = new WeakHashMap<Object, Long>();
    
    /** Checkpoint value for recognizing change of state */
    private int checkpointValue = 0;

    
    /****************** Reference counter read operations ******************/

    public long get(Object key) {
        Long rtv = values.get(key);
        return (rtv == null)?0:rtv.longValue();
    }
    
    public Set<Object> getKeys() {
        return Collections.unmodifiableSet(values.keySet()); 
    }
    
    public int getKeyCount() {
        return values.size(); 
    }

    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }


    /****************** Reference counter modification operations ******************/
    
    public void set(Object key, long value) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            values.put(key, value);

            // Update all bound statistics
            for (StatisticSimpleWeakrefCounter stat : getBoundStats())
                stat.set(key, value);
        }
    }
    
    /** Return either the StatisticCounter for given key and remove it from the mapping or return null, if the key is not in the map */
    public boolean remove(Object key, boolean propagateDelete) {
        if (!canPerformOperation())
            return false;

        synchronized (this) {
            boolean rtv = values.remove(key) != null;

            // Update all bound statistics
            if (propagateDelete)
                for (StatisticSimpleWeakrefCounter stat : getBoundStats())
                    stat.remove(key, propagateDelete);

            return rtv;
        }
    }
    
    /** Adds the passed value to the current value associated with the passed key.
     */
    public void add(Object key, long value) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            values.put(key, get(key) + value);

            // Create this key on all bound statistics
            for (StatisticSimpleWeakrefCounter stat : getBoundStats())
                stat.add(key, value);
        }
    }

    public void add(Object key) {
        add(key, 1);
    }

    public void sub(Object key, long value) {
        add(key, -value);
    }

    public void sub(Object key) {
        add(key, -1);
    }


    /****************** Statistics merging ******************/
    
    protected synchronized void updateFrom(StatisticSimpleWeakrefCounter sourceStat) {
        synchronized (sourceStat) {
            // Iterate through all the entries in the source statistics
            for (Map.Entry<Object, Long> entry : sourceStat.values.entrySet()) {
                // Skip null values (can happen, because this is weak list
                if (entry.getKey() == null) continue;
                values.put(entry.getKey(), get(entry.getKey()) + entry.getValue());
            }
        }
    }
    
    protected synchronized void setFrom(StatisticSimpleWeakrefCounter sourceStat) {
        synchronized (sourceStat) {
            // Iterate through all the entries in the source statistics
            for (Map.Entry<Object, Long> entry : sourceStat.values.entrySet()) {
                // Skip null key values (can happen, because this is a weak list)
                if (entry.getKey() == null) continue;
                values.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /** Reset the current statistic (this one only).
     */
    public synchronized void reset() {
        values.clear();
        setCheckpoint();
    }


    /****************** Constructors ******************/

    /** Creates a new instance of StatisticRefCounter */
    protected StatisticSimpleWeakrefCounter(String name) {
        super(name);
    }
    
    
    /****************** Creator ******************/
    
    /** Create new statistic object with specified name or get the one already existing */
    public static StatisticSimpleWeakrefCounter getStatistics(String name) throws ClassCastException {
        return statistics.get(name, StatisticSimpleWeakrefCounter.class);
    }


    /****************** Constructors ******************/

    // Initialize boundStatistics attribute (must be empty after deserialization)
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        values = new WeakHashMap<Object, Long>();
    }


    /****************** Text representation ******************/
    
    public String toString() { 
	return new StringBuffer(getName()).append(": ").append(values.toString()).toString();
    }

    protected synchronized int deepHashCode() {
        int hashCode = 0;
        for (Object value : values.values())
            hashCode ^= value.hashCode();
        return hashCode;
    }

    /** Reports if value of refCounter has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        return checkpointValue != deepHashCode();
    }
    
    /** Sets checkpoint. Stores the current state of refCounter.
     */
    public void setCheckpoint() {
        checkpointValue = deepHashCode();
    }

}