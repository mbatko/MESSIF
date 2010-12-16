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

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class StatisticRefCounter extends Statistics<StatisticRefCounter> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Reference counter data ******************/
    protected Map<Object, StatisticCounter> values = new HashMap<Object, StatisticCounter>();
    
    private long valueSumCheckpoint = 0;    /* backed-up state of value for checkpointing */
    
    /****************** Reference counter read operations ******************/

    public long get(Object key) {
        StatisticCounter rtv = values.get(key);
        return (rtv == null)?0:rtv.get();
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
    
    @Override
    public Map<Object, StatisticCounter> getValue() {
        return Collections.unmodifiableMap(values);
    }

    /** Return the statistic counter associated with the provided key.
     *  @param key the reference key of the retrieved counter
     *  @return either the StatisticsCounter or null if there is no associated counter yet
     */
    protected StatisticCounter getStatisticCounter(Object key, boolean createIfNotExist, long initialValue) {
        StatisticCounter counter = values.get(key);
        
        // Create new counter, if there is not one associated with the key
        if (counter == null && createIfNotExist) {
            counter = new StatisticCounter(new StringBuffer(getName()).append(".").append(key.toString()).toString(), initialValue);
            values.put(key, counter);
        }
        
        return counter;
    }

    /** Return the statistic counter associated with the provided key.
     *  @param key the reference key of the retrieved counter
     *  @return either the StatisticsCounter or null if there is no associated counter yet
     */
    protected StatisticCounter getStatisticCounter(Object key, boolean createIfNotExist) {
        return getStatisticCounter(key, createIfNotExist, 0);
    }

    /****************** Reference counter modification operations ******************/
    
    public void set(Object key, long value) {
        if (!canPerformOperation())
            return;
        
        synchronized (this) {
            StatisticCounter counter = getStatisticCounter(key, true);

            // Create this key on all bound statistics
            for (StatisticRefCounter stat : getBoundStats())
                if (!stat.values.containsKey(key))
                    stat.getStatisticCounter(key, true).bindTo(counter);

            counter.set(value);
        }
    }
    
    /** Return either the StatisticCounter for given key and remove it from the mapping or return null, if the key is not in the map */
    public StatisticCounter remove(Object key) {
        if (!canPerformOperation())
            return null;

        synchronized (this) {
            StatisticCounter counter = values.remove(key);

            if (counter != null) {
                counter.unbind();
                for (StatisticCounter boundCounter : counter.getBoundStats())
                    boundCounter.unbind();
            }

            return counter;
        }
    }
    
    /** Adds the passed value to the current value associated with the passed key.
     */
    public void add(Object key, long value) {
        if (!canPerformOperation())
            return;
        
        synchronized (this) {
            StatisticCounter counter = getStatisticCounter(key, true);

            // Create this key on all bound statistics
            for (StatisticRefCounter stat : getBoundStats())
                if (!stat.values.containsKey(key))
                    stat.getStatisticCounter(key, true).bindTo(counter);

            counter.add(value);
        }
    }
    public void add(Object key) { add(key, 1); }
    public void sub(Object key, long value) { add(key, -value); }
    public void sub(Object key) { add(key, -1); }
    /** Set the passed value to the current value associated with the passed key if it is greater than current value.
     */
    public void max(Object key, long value) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            StatisticCounter counter = getStatisticCounter(key, true);

            // Create this key on all bound statistics
            for (StatisticRefCounter stat : getBoundStats())
                if (!stat.values.containsKey(key))
                    stat.getStatisticCounter(key, true).bindTo(counter);

            counter.max(value);
        }
    }
    
    /** Set the passed value to the current value associated with the passed key if it is smaller than current value.
     */
    public void min(Object key, long value) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            StatisticCounter counter = getStatisticCounter(key, true, value);

            // Create this key on all bound statistics
            for (StatisticRefCounter stat : getBoundStats())
                if (!stat.values.containsKey(key))
                    stat.getStatisticCounter(key, true, value).bindTo(counter);

            counter.min(value);
        }
    }

    public synchronized void clear() { values.clear(); }

    
    /****************** Statistics merging ******************/
    
    protected synchronized void updateFrom(StatisticRefCounter sourceStat) {
        // Iterate through all the entries in the source statistics
        for (Map.Entry<Object, StatisticCounter> entry : sourceStat.values.entrySet())
            // Update our counter from the source one (creating those that do not exist)
            getStatisticCounter(entry.getKey(), true).updateFrom(entry.getValue());
    }
    
    protected synchronized void setFrom(StatisticRefCounter sourceStat) {
        values.clear();
        values.putAll(sourceStat.values);
    }
    
    /** Reset the current statistic (this one only).
     */
    public void reset() {
        // Unbind all counters of the RefCounter bound to this
        if (isBound())
            for (StatisticCounter stat : getBoundTo().values.values())
                stat.unbind();
        
        // Unbind all counters of this RefCounter bound to anything else
        for (StatisticCounter stat : values.values())
            stat.unbind();
        
        values = new HashMap<Object, StatisticCounter>();
        setCheckpoint();
    }
        
    
    /****************** Counter binding ******************/

    /** Bind current statistics object to receive notifications at the same time as the specified statistics receives some.
     *  @param object the parent statistics counter object
     *  @param key the reference key of our counter to bind it to
     */
    public void bindTo(StatisticCounter object, Object key) throws IllegalArgumentException {
        StatisticCounter counter = getStatisticCounter(key, true);
        
        counter.bindTo(object);

        // If another refcounter has been bounded to this refcounter, bind also that statistics
        for (StatisticRefCounter stat : getBoundStats())
            stat.getStatisticCounter(key, true).bindTo(counter);
    }
    
    /** Deregister statistic counter for key from the binding list */
    public void unbind(Object key) {
        // If another refcounter has been bounded to this refcounter, unbind also that statistics
        for (StatisticRefCounter stat : getBoundStats())
            stat.getStatisticCounter(key, false).unbind();

        getStatisticCounter(key, false).unbind();
    }

    /****************** RefCounter binding overrides ******************/

    @Override
    public void unbind() {
        if (isBound())
            synchronized (getBoundTo()) {
                synchronized (this) {
                    /** Deregister ourselves from list of "parent" object */
                    for (StatisticCounter stat : values.values())
                        stat.unbind();

                    super.unbind();
                }
            }
    }

    /**
     * Bind current statistics object to receive notifications at the same time as the specified statistics receives some.
     * @param object the parent statistics object
     */
    @Override
    public void bindTo(StatisticRefCounter object) throws IllegalArgumentException {
        // Ignore null argument
        if (object == null)
            return;
        
        // Must maintain lock order (as in the statistics internal method call)
        synchronized (object) {
            synchronized (this) {
                super.bindTo(object);
                
                for (Map.Entry<Object, StatisticCounter> item : object.values.entrySet())
                    getStatisticCounter(item.getKey(), true).bindTo(item.getValue());
            }
        }
    }

    
    /****************** Constructors ******************/
        
    /** Creates a new instance of StatisticRefCounter */
    protected StatisticRefCounter(String name) {
        super(name);
    }
    
    
    /****************** Creator ******************/
    
    /** Create new statistic object with specified name or get the one already existing */
    public static StatisticRefCounter getStatistics(String name) throws ClassCastException {
        return getStatistics(name, StatisticRefCounter.class);
    }

    
    /****************** Text representation ******************/
    
    public String toString() { 
	StringBuffer buf = new StringBuffer();
        
        buf.append(getName());
	buf.append(": {");

	Iterator<Entry<Object, StatisticCounter>> it = values.entrySet().iterator();
        boolean hasNext = it.hasNext();
        while (hasNext) {
	    Entry<Object, StatisticCounter> e = it.next();
	    Object key = e.getKey();
            StatisticCounter counter = e.getValue();
            
            buf.append(key);
	    buf.append("=");
            buf.append(counter.get());
            
            hasNext = it.hasNext();
            if (hasNext)
                buf.append(", ");
        }

	buf.append("}");
	return buf.toString();
    }

    /** Reports if value of refCounter has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        long tstValue = 0;

        for (StatisticCounter counter : values.values()) 
            tstValue += counter.get();

        return (tstValue != valueSumCheckpoint);
}
    
    /** Sets checkpoint. Stores the current state of refCounter.
     */
    public void setCheckpoint() {
        valueSumCheckpoint = 0;
        
        for (StatisticCounter counter : values.values()) 
            valueSumCheckpoint += counter.get();
    }

    @Override
    protected StatisticRefCounter cast() {
        return this;
    }
    
}
