/*
 * StatisticMinMaxCounter.java
 *
 * Created on 25. duben 2004, 13:55
 */

package messif.statistics;

import java.util.*;


/**
 *
 * @author  xbatko
 */
public final class StatisticMinMaxCounter extends Statistics<StatisticMinMaxCounter> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Counter operation ******************/
    protected double min = Double.NaN;
    protected double max = Double.NaN;
    protected double sum = 0;
    protected long count = 0;
    
    private long countCheckpoint = 0;    /* backed-up state for checkpointing feature */
    
    public void addValue(double value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            sum += value;
            if (count == 0) {
                min = value;
                max = value;
            } else {
                if (value < min)
                    min = value;
                if (value > max)
                    max = value;
            }
            count++;
            for (StatisticMinMaxCounter stat : getBoundStats())
                stat.addValue(value);
        }
    }
    public void removeValue(double value) {
        if (!canPerformOperation()) return;
        synchronized (this) {
            sum -= value;
            count--;
            for (StatisticMinMaxCounter stat : getBoundStats())
                stat.removeValue(value);
        }
    }
    public synchronized void clear() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        sum = 0;
        count = 0;
    }
    
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getSum() { return sum; }
    public double getCnt() { return count; }
    public double getAvg() { return (count == 0)?0:sum/count; }

    
    /****************** Statistics merging ******************/
    
    protected synchronized void updateFrom(StatisticMinMaxCounter sourceStat) {
        if (this.min > sourceStat.min) this.min = sourceStat.min;
        if (this.max < sourceStat.max) this.max = sourceStat.max;
        this.sum += sourceStat.sum;
        this.count += sourceStat.count;
    }
    
    protected synchronized void setFrom(StatisticMinMaxCounter sourceStat) {
        this.min = sourceStat.min;
        this.max = sourceStat.max;
        this.sum = sourceStat.sum;
        this.count = sourceStat.count;
    }
    
    /** Reset the current statistic (this one only).
     */
    public void reset() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        sum = 0;
        count = 0;
        setCheckpoint();
    }
    

    /****************** Constructors ******************/

    /** Creates a new instance of StatisticCounter */
    protected StatisticMinMaxCounter(String name) {
        super(name);
    }

    /****************** Creator ******************/
    
    /** Create new statistic counter with specified name or get the one already existing */
    public static StatisticMinMaxCounter getStatistics(String name) throws ClassCastException {
        return statistics.get(name, StatisticMinMaxCounter.class);
    }

    
    /****************** Text representation ******************/
    
    public String toString() {
        return new StringBuffer().
                append(name).append(".min: ").append(min).append(", ").
                append(name).append(".max: ").append(max).append(", ").
                append(name).append(".avg: ").append(getAvg()).append(", ").
                append(name).append(".sum: ").append(sum).append(", ").
                append(name).append(".cnt: ").append(count).toString();
     }


    /** Reports if value of min/max has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        return (count != countCheckpoint);
    }
    
    /** Sets checkpoint. Stores the current state of min/max.
     */
    public void setCheckpoint() {
        countCheckpoint = count;
    }
    
}
