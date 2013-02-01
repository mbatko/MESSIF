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
package messif.objects.keys;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Closed interval, comparable by the lower bound.
 * 
 * @param <T> specific type of the key
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class KeyInterval<T extends Comparable<? super T>> implements Comparable<KeyInterval<T>> {

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
     * @return <b>true</b> if the interval covers given key
     */
    public final boolean isCovered(T key) {
        return (getFrom().compareTo(key) <= 0) && (getTo().compareTo(key) >= 0);
    }

    /** 
     * Return <b>true</b> if the interval intersects with this interval.
     * @param interval the interval to be tested
     * @return <b>true</b> if the interval covers given key
     */
    public final boolean intersect(KeyInterval<T> interval) {
        return (getTo().compareTo(interval.getFrom()) >= 0) && (getFrom().compareTo(interval.getTo()) <= 0);
    }
    
    //   TODO: remove in MESSIF 2.0
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
    //   TODO: remove in MESSIF 2.0
    
    
    
    
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof KeyInterval)) {
            return false;
        }
        return getFrom().equals(((KeyInterval) obj).getFrom()) && getTo().equals(((KeyInterval) obj).getTo());
    }

    @Override
    public int hashCode() {
        return (getFrom().hashCode() << 8) ^ getTo().hashCode();
    }
        
    /** 
     * Return the string representation of this interval.
     */
    @Override
    public String toString() {
        return (new StringBuffer("[")).append(getFrom()).append(",").append(getTo()).append("]").toString();
    }
}
