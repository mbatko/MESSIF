/*
 * StatisticObject.java
 *
 * Created on 25. duben 2004, 13:55
 */

package messif.statistics;

import java.util.*;

/**
 *
 * @author  xbatko
 */
public class StatisticObject extends Statistics<StatisticObject> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Object operation ******************/
    protected Object value = null;
    
    private Object valueCheckpoint = null;      /* backup value for checkpoints */
    
    
    public void set(Object object) {
        if (!canPerformOperation())
            return;

        synchronized (this) {
            this.value = object;
            for (StatisticObject stat : getBoundStats())
                stat.set(object);
        }
    }
    
    public Object get() { return value; }
    
    
    /****************** Statistics merging ******************/
    
    protected synchronized void updateFrom(StatisticObject sourceStat) {
        this.value = sourceStat.value;
    }
    
    protected synchronized void setFrom(StatisticObject sourceStat) {
        this.value = sourceStat.value;
    }
    
    /** Reset the current statistic (this one only).
     */
    public void reset() {
        this.value = null;
        setCheckpoint();
    }
    
    
    /****************** Constructors ******************/
        
    /** Creates a new instance of StatisticObject */
    protected StatisticObject(String name) {
        super(name);
    }
    

    /****************** Creator ******************/
    
    /** Create new statistic object with specified name or get the one already existing */
    public static StatisticObject getStatistics(String name) throws ClassCastException {
        return statistics.get(name, StatisticObject.class);
    }

    
    /****************** Text representation ******************/
    
    @Override
    public String toString() {
        return getName() + ": " + value;
    }


    /** Reports if value of statistic has been changed since the last setCheckpoint() call.
     */
    public boolean changedSinceCheckpoint() {
        return (value != valueCheckpoint);
    }
    
    /** Sets checkpoint. Stores the current state of value object.
     */
    public void setCheckpoint() {
        valueCheckpoint = value;
    }
    
}
