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

import java.util.Comparator;

/**
 * This class provides an abstract operator for object keys that have a defined order.
 * The operator must know the maximal and minimal keys and must be able to provide
 * a successor and predecesor keys. Finally, it should be able to create
 * {@link KeyInterval key intervals} for the pair of keys.
 * 
 * @param <T> specific type of the key
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class KeyOperator<T extends Comparable<? super T>> implements Comparator<T> {
    
    /**
     * Returns the maximal value of given key type.
     * @return maximal value of given key type
     */
    public abstract T getMaxKey();

    /**
     * Returns the minimal value of given key type.
     * @return minimal value of given key type
     */
    public abstract T getMinKey();

    /**
     * Given a key, return the smallest key which is greater than the key specified.
     * @param key 
     * @return return the smallest key which is greater than the <code>key</code>
     */
    public abstract T getNextKey(T key);

    /**
     * Given a key, return the greatest key which is smaller than the key specified.
     * @param key 
     * @return return the greatest key which is smaller than the <code>key</code>
     */
    public abstract T getPreviousKey(T key);
    
    /**
     * Given two keys, return their distance (difference)
     * @param first the first key
     * @param second the second
     * @return distance of the keys
     */
    public abstract T getDifference(T first, T second);
    
    /**
     * Given two keys, return the key in the middle.
     * @param first the first key
     * @param second the second
     * @return key in the middle
     */
    public abstract T getMiddleKey(T first, T second);
    
    /**
     * This method creates an interval given two keys.
     * @param from the lower bound
     * @param to the upper bound
     * @return the created interval
     */
    public abstract KeyInterval<T> createInteral(T from, T to);

    /**
     * This method returns a new instance of KeyInterval which makes this interval opened from the left.
     * @param interval the closed interval 
     * @return left-opened interval
     */
    public abstract KeyInterval<T> makeLeftOpen(KeyInterval<T> interval);

    
    /****************************        Implemented methods       *************************************/
    
    /**
     * Return the maximum of the two values passed or the first one if they are equal.
     * @param first the first key
     * @param second the second key
     * @return the maximum of the two values passed or the first one if they are equal
     */
    public final T max(T first, T second) {
        return (compare(first, second) >= 0) ? first : second;
    }
    
    /**
     * Return the minimum of the two values passed or the first one if they are equal.
     * @param first the first key
     * @param second the second key
     * @return the minimum of the two values passed or the first one if they are equal
     */
    public final T min(T first, T second) {
        return (compare(first, second) <= 0) ? first : second;
    }
    
    /**
     * Finds out, whether this key is inner the interval specified by the passed
     * bounds - counting on the ring circle (module SIZE). key \in (low, up).
     * @param key the key to be tested
     * @param low the lower bound
     * @param up the upper bound
     * @return <b>true</b> if the low and the up keys equals; <b>false</b> if the key equals either the low or up keys
     */
    public final boolean isInBetween(T key, T low, T up) {

        if (low.equals(up)) {
            return true;
        }
        if (compare(low, up) < 0) { // if the low and up keys are "normal"
            return ((compare(low, key) < 0) && (compare(key, up) < 0));
        }
        // if the interval goes over the "0" point of the circle
        return ((compare(low, key) < 0) || (compare(key, up) < 0));
    }

    /**
     * finds out, whether this key is inner the interval specified by the passed
     * bounds - counting on the ring circle (module SIZE). key \in [low, up]
     * @param key the key to be tested
     * @param low the lower bound
     * @param up the upper bound
     * @return <b>true</b> if the low and the up keys equals; <b>true</b> if the key equals either the low or up keys
     */
    public final boolean isInBetweenEQ(T key, T low, T up) {
        return (isInBetween(key, low, up) || (key.equals(low)) || (key.equals(up)));
    }

    /**
     * Finds out, whether this key is inner the interval specified by the passed
     * bounds - counting on the ring circle (module SIZE). key \in [low, up)
     * @param key the key to be tested
     * @param low the lower bound
     * @param up the upper bound
     * @return <b>true</b> if the low and the up keys equals; 
     *   <b>true</b> if the key equals the low bound but <b>flase</b> for the up keys equality
     */
    public final boolean isInBetweenEQL(T key, T low, T up) {
        return (isInBetween(key, low, up) || (key.equals(low)));
    }

    /**
     * Finds out, whether this key is inner the interval specified by the passed
     * bounds - counting on the ring circle (module SIZE). key \in (low, up]
     * @param key the key to be tested
     * @param low the lower bound
     * @param up the upper bound
     * @return <b>true</b> if the low and the up keys equals; 
     *   <b>false</b> if the key equals the low bound but; <b>true</b> for the up keys equality
     */
    public final boolean isInBetweenEQH(T key, T low, T up) {
        return (isInBetween(key, low, up) || (key.equals(up)));
    }
    
    /** 
     * Return <b>true</b> if the two intervals intersect.
     * @param first first interval
     * @param second second interval
     * @return <b>true</b> if the two intervals intersect
     */
    public final boolean intersect(KeyInterval<T> first, KeyInterval<T> second) { 
        return (compare(first.getFrom(), second.getTo()) <= 0) && (compare(second.getFrom(), first.getTo()) <= 0);
    }
    
}
