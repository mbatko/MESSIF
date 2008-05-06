/*
 * StatisticCounter.java
 *
 * Created on 25. duben 2004, 13:55
 */

package messif.statistics;

import java.util.*;


/**
 *
 * @author  xbatko
 */
public final class StatisticCounter extends Statistics<StatisticCounter> {    
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Counter operation ******************/
    protected long value;
    private long valueCheckpoint = 0;         /* backup value for checkpoint feature */
    
    public void set(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            this.value = value;
            for (StatisticCounter stat : getBoundStats())
                stat.set(value);
        }
    }    
    public void add(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            this.value += value;
            for (StatisticCounter stat : getBoundStats())
                stat.add(value);
        }
    }
    public void add() { add(1); }
    public void sub(long value) { add(-value); }
    public void sub() { add(-1); }
    
    public long get() { return value; }

    public void max(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            if (this.value < value)
                this.value = value;
            for (StatisticCounter stat : getBoundStats())
                stat.max(value);
        }
    }
    
    public void min(long value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            if (this.value > value)
                this.value = value;
            for (StatisticCounter stat : getBoundStats())
                stat.min(value);
        }
    }

    /****************** Statistics merging ******************/
    
    protected synchronized void updateFrom(StatisticCounter sourceStat) {
        this.value += sourceStat.value;
    }
    
    protected synchronized void setFrom(StatisticCounter sourceStat) {
        this.value = sourceStat.value;
    }
    
    /** Reset the current statistic (this one only).
     */
    public void reset() {
        this.value = 0;
        setCheckpoint();
    }
    
    /****************** Constructors ******************/

    /** Creates a new instance of StatisticCounter */
    protected StatisticCounter(String name) {
        super(name);
        this.value = 0;
    }

    /** Creates a new instance of StatisticCounter */
    protected StatisticCounter(String name, long value) {
        super(name);
        this.value = value;
    }

    /****************** Creator ******************/
    
    /** Create new statistic counter with specified name or get the one already existing */
    public static StatisticCounter getStatistics(String name) {
        return statistics.get(name, StatisticCounter.class);
    }

    /****************** Text representation ******************/
    
    public String toString() { return name + ": " + value; }

    /** Reports if value of counter has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        return (value != valueCheckpoint);
    }
    
    /** Sets checkpoint. Stores the current state of counter.
     */
    public void setCheckpoint() {
        valueCheckpoint = value;
    }
    
}
