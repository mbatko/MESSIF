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

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;

/**
 * This is a statistic that counts aggregated functions (especially avg) from
 * 1) last n values - if n+1 value is added, the first is removed and not considered any more   or
 * 2) values stored in the time-sliding window
 * 3) combination of 1) and 2)
 *
 * Moreover, there is a possibility of linking each value (and time) with a "key" and then all
 *  values added to this counter are sumed up according to the key
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class StatisticSlidingAvgCounter extends Statistics<StatisticSlidingAvgCounter> implements Serializable {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Remember the time stamp of reseting (or creating) the statistics */
    protected long resetTime;
    /** Checks whether the sliding window was used for full time period */
    public boolean checkUsedTime() {
        if (windowSizeMilis <= 0)
            return true;
        return (System.currentTimeMillis() - resetTime > windowSizeMilis);
    }
    
    /** Queue of actual values with their time stamps - expect that this queue is sorted according to the times */
    LinkedList<ValueTime> values = new LinkedList<ValueTime>();
    
    /** This simple class encapsulates the pair: double value + time when it was added */
    protected static class ValueTime implements Serializable {
        /** Class version id for serialization */
        private static final long serialVersionUID = 2L;
        
        protected double value;
        protected long time;
        protected final Object key;
        
        /** Default constructor sets the key to null */
        protected ValueTime(double value, long time) {
            this(value, time, null);
        }
        /** Full constructor */
        protected ValueTime(double value, long time, Object key) {
            this.value = value;
            this.time = time;
            this.key = key;
        }
        /** Compare the items according to the key */
        public boolean equals(Object obj) {
            if (! (obj instanceof ValueTime))
                return false;
            if ((this.key == null) || (((ValueTime) obj).key == null))
                return false;
            return this.key.equals(((ValueTime)obj).key);
        }
        /** HashCode is consistent with equals */
        public int hashCode() {
            if (key == null)
                return super.hashCode();
            return key.hashCode();
        }
        /** To string method */
        public String toString() {
            return "<"+value+","+time+">";
        }
    }
    
    /** aggregated values */
    protected double sum = 0;
    protected int count = 0;
    
    /*******************   Set the behaviour of this counter ******************/
    
    /** number of values taken into account if <= 0 then use all values - default is 100 */
    private int maxNumberOfValues = 100;
    public int getMaxNumberOfValues() { return maxNumberOfValues; }
    /** sets the size of window to consider
     * @param maxNumberOfValues if < 0 then - use all values
     */
    public void setMaxNumberOfValues(int maxNumberOfValues) {
        this.maxNumberOfValues = maxNumberOfValues;
    }
    
    /** size of time window in milis - default is -1 (do not use time) */
    private long windowSizeMilis = -1;
    public long getWindowSizeMilis() { return windowSizeMilis; }
    /** and sets the size of the time-sliding window in milis
     * @param windowSizeMilis if <= 0 - do not use the time window
     */
    public void setWindowSizeMilis(long windowSizeMilis) {
        this.windowSizeMilis = windowSizeMilis;
    }
    
    /** this private method consolidates the queue - removes values that are either too old or the queue is too large */
    private void consolidate(long currentTime) {
        if (windowSizeMilis > 0) {
            long windowBeginning = currentTime - windowSizeMilis;
            while ((! values.isEmpty()) && values.peek().time < windowBeginning)
                pollLastValue();
        }
        while (maxNumberOfValues > 0 && count > maxNumberOfValues)
            pollLastValue();
    }
    
    /** add a given time difference to ALL values in the list */
    public void shiftValuesByTime(long time) {
        for (ValueTime value : values) {
            value.time += time;
        }
    }
    
    /****************** Data manipulation methods *************************/
    
    /** add given pair "value-time" to the queue and consolidate the queue */
    private synchronized void addValue(ValueTime valueTime) {
        // if the list already contains value for this object (key), then sum
        // both values (and put the value to the end of the list)
        if ((valueTime.key != null) && (! values.isEmpty())) {
            for (ListIterator<ValueTime> it = values.listIterator(); it.hasNext(); ) {
                ValueTime val = it.next();
                if (val.equals(valueTime)) {
                    valueTime.value += val.value;
                    it.remove();
                    count--;
                    sum -= val.value;
                    break;
                }
            }
        }
        // now add the value to the end of the list
        values.offer(valueTime);
        sum += valueTime.value;
        count++;
        consolidate(valueTime.time);
        
        for (StatisticSlidingAvgCounter stat : getBoundStats())
            stat.addValue(valueTime);
    }
    
    /** add value and expect that it has been measured now - get current time */
    public void addValue(double value) {
        if (!canPerformOperation())
            return;
        addValue(new ValueTime(value, System.currentTimeMillis()));
    }
    
    /** add value and expect that it has been measured now - get current time.
     *   Before adding the value to the list, find out whether the "obj" is not stored in the list and if so, then
     *   increase the existing value by the given value
     */
    public void addValue(double value, Object obj) {
        if (!canPerformOperation())
            return;
        addValue(new ValueTime(value, System.currentTimeMillis(), obj));
    }
    
    /** this method polls the last value from the equeue and does not check the non-emptiness of the queue */
    private void pollLastValue() {
        sum -= values.poll().value;
        count--;
    }
    
    /***********************  Getters  *************************/
    
    /** return the sum of queue values */
    public double getSum() {
        if (windowSizeMilis > 0)
            consolidate(System.currentTimeMillis());
        return sum;
    }
    /** return number of values in the queue */
    public int getCnt() {
        if (windowSizeMilis > 0)
            consolidate(System.currentTimeMillis());
        return count;
    }
    /** return average of the values in queue */
    public double getAvg() {
        return getAvg(true);
    }
    /** return the average but do not consolidate the list of values if the parameter is false */
    public double getAvg(boolean consolidate) {
        if (windowSizeMilis > 0 && consolidate)
            consolidate(System.currentTimeMillis());
        return (count == 0)?0:sum/count;
    }
    /** return minimum value from the queue */
    public double getMin() {
        if (windowSizeMilis > 0)
            consolidate(System.currentTimeMillis());
        double min = Double.MAX_VALUE;
        for (ValueTime d : values)
            if (min > d.value) min = d.value;
        return min;
    }
    /** return maximum value from the queue */
    public double getMax() {
        if (windowSizeMilis > 0)
            consolidate(System.currentTimeMillis());
        double max = Double.MIN_VALUE;
        for (ValueTime d : values)
            if (max < d.value) max = d.value;
        return max;
    }
    
    
    /****************** Creator ******************/
    
    /** Creates a new instance of StatisticSlidingAvgCounter */
    protected StatisticSlidingAvgCounter(String name) {
        super(name);
        resetTime = System.currentTimeMillis();
    }
    
    /** Create new statistic counter with specified name or get the one already existing */
    public static StatisticSlidingAvgCounter getStatistics(String name) throws ClassCastException {
        return getStatistics(name, StatisticSlidingAvgCounter.class);
    }
    
    
    /****************** Statistics merging ******************/
    
    /** the final queue should be sorted according to time as required */
    protected synchronized void updateFrom(StatisticSlidingAvgCounter sourceStat) {
        Queue<ValueTime> oldQueue = values;
        values = new LinkedList<ValueTime>();
        sum = 0;
        count = 0;
        
        Iterator<ValueTime> oldIt = oldQueue.iterator();
        Iterator<ValueTime> updateIt = sourceStat.values.iterator();
        
        ValueTime oldValue = oldIt.hasNext()?oldIt.next():null;
        ValueTime updateValue = updateIt.hasNext()?updateIt.next():null;
        // iterate over both queues and always store the "older" record first
        while (oldValue != null || updateValue != null) {
            if (((oldValue!=null)?oldValue.time:Long.MAX_VALUE) < ((updateValue!=null)?updateValue.time:Long.MAX_VALUE)) {
                addValue(oldValue);
                oldValue = oldIt.hasNext()?oldIt.next():null;
            } else {
                addValue(updateValue);
                updateValue = updateIt.hasNext()?updateIt.next():null;
            }
        }
        consolidate(System.currentTimeMillis());
    }
    
    /** Set this statistic to values from given statistic */
    protected synchronized void setFrom(StatisticSlidingAvgCounter sourceStat) {
        this.values = new LinkedList<ValueTime>(sourceStat.values);
        this.sum = sourceStat.sum;
        this.count = sourceStat.count;
        consolidate(System.currentTimeMillis());
    }
    
    /** Reset the current statistic */
    public synchronized void reset() {
        values.clear();
        sum = 0;
        count = 0;
        setCheckpoint();
        resetTime = System.currentTimeMillis();
    }
    
    /** backed-up state for checkpointing feature */
    private long countCheckpoint = 0;
    
    /** Reports if value of min/max has been changed since the last setCheckpoint() call. */
    public boolean changedSinceCheckpoint() {
        return (count != countCheckpoint);
    }
    
    /** Sets checkpoint. Stores the current state of min/max. */
    public void setCheckpoint() {
        countCheckpoint = count;
    }
    
    /** Text representation of the SlidingWindow */
    @Override
    public String toString() {
        return new StringBuffer().
                //append(name).append(".min: ").append(getMin()).append(", ").
                //append(name).append(".max: ").append(getMax()).append(", ").
                append(getName()).append(".avg: ").append(getAvg()).append(", ").
                append(getName()).append(".sum: ").append(getSum()).append(", ").
                append(getName()).append(".cnt: ").append(getCnt()).toString();
    }

    @Override
    protected StatisticSlidingAvgCounter cast() {
        return this;
    }

}
