/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.keys;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Closed interval, comparable by the lower bound.
 * 
 * @param <T> specific type of the key
 * @author xnovak8
 */
public abstract class KeyInterval<T> implements Comparable<KeyInterval<T>> {

    /** Return the lower bound of the interval.
     * @return the lower bound of the interval
     */
    public abstract T getFrom();

    /** Return the upper bound of the interval.
     * @return the upper bound of the interval
     */
    public abstract T getTo();
    
    /** 
     * Return <b>true</b> if the interval covers given key. 
     * @param key the key to be tested
     * @param operator the operator fot this key type
     * @return <b>true</b> if the interval covers given key
     */
    public boolean isCovered(T key, KeyOperator<T> operator) {
        return (operator.compare(getFrom(), key) <= 0) && (operator.compare(getTo(), key) >= 0);
    }

    /** 
     * Return <b>true</b> if the interval intersects with this interval.
     * @param interval the interval to be tested
     * @param operator the operator fot this key type
     * @return <b>true</b> if the interval covers given key
     */
    public boolean intersect(KeyInterval<T> interval, KeyOperator<T> operator) {
        return (operator.compare(getTo(), interval.getFrom()) >= 0) && (operator.compare(getFrom(), interval.getTo()) <= 0);
    }
    
    /** 
     * Given a list of intervals, cut from them the parts that intersects with "this" interval.
     * The intervals can be MODULO the domain size
     * @param intervals list of intervals
     * @param operator the operator fot this key type
     * @return the list of intervals cut off
     */
    public List<KeyInterval<T>> cutIntersectingIntervals(List<KeyInterval<T>> intervals, KeyOperator<T> operator) {
        List<KeyInterval<T>> retVal = null;
        if (operator.compare(getFrom(), getTo()) >= 0) {
            retVal = operator.createInteral(getFrom(), operator.getMaxKey()).cutIntersectingIntervalsInner(intervals, operator);
            retVal.addAll(operator.createInteral(operator.getMinKey(), getTo()).cutIntersectingIntervalsInner(intervals, operator));
        } else {
            retVal = cutIntersectingIntervalsInner(intervals, operator);
        }
        return retVal;
    }
    
    /** 
     * Given a list of intervals, cut from them the parts that intersects with "this" interval.
     * @param intervals list of intervals
     * @param operator the operator fot this key type
     * @return the list of intervals cut off
     */
    protected List<KeyInterval<T>> cutIntersectingIntervalsInner(List<KeyInterval<T>> intervals, KeyOperator<T> operator) {
        List<KeyInterval<T>> retVal = new ArrayList<KeyInterval<T>>();
        for (ListIterator<KeyInterval<T>> it = intervals.listIterator(); it.hasNext(); ) {
            KeyInterval<T> interval = it.next();
            // if the current interval intersects "this"
            if (operator.intersect(this, interval)) {
                retVal.add(operator.createInteral(operator.max(this.getFrom(), interval.getFrom()), operator.min(this.getTo(), interval.getTo()))); 
                if (operator.compare(this.getFrom(), interval.getFrom()) <= 0) {
                    if (operator.compare(this.getTo(), interval.getTo()) >= 0) {
                        it.remove();
                    } else {
                        it.set(operator.createInteral(operator.getNextKey(this.getTo()), interval.getTo()));
                    }
                } else {
                    it.set(operator.createInteral(interval.getFrom(), operator.getPreviousKey(this.getFrom())));
                    if (!(operator.compare(getTo(), interval.getTo()) >= 0)) {
                        it.add(operator.createInteral(operator.getNextKey(this.getTo()), interval.getTo()));
                    }
                }
            }
        }
        return retVal;
    }       

    /**
     * Given a set of keys, this method cuts the given interval to dijcunct set of "right-open" intervals.
     */
    //public List<KeyInterval<T>> cutIntervalsByKeys()
    
    /** 
     * Return the string representation of this interval.
     */
    @Override
    public String toString() {
        return (new StringBuffer("[")).append(getFrom()).append(",").append(getTo()).append("]").toString();
    }
    
}
