package messif.objects.keys;

import java.util.Comparator;

/**
 * This interface is to be implemented by 
 * 
 * @param <T> specific type of the key
 * @author xnovak8
 */
public abstract class KeyOperator<T> implements Comparator<T> {
    
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
