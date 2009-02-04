package messif.objects.keys;

import java.io.Serializable;

/**
 *
 * @author xnovak8
 */
public class DoubleKeyOperator extends KeyOperator<DoubleKey> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    public int compare(DoubleKey o1, DoubleKey o2) {
        return o1.compareTo(o2);
    }

    @Override
    public DoubleKey getMaxKey() {
        return new DoubleKey(null, Double.MAX_VALUE);
    }

    @Override
    public DoubleKey getMinKey() {
        return new DoubleKey(null, 0d);
    }

    @Override
    public DoubleKey getNextKey(DoubleKey key) {
        return new DoubleKey(null, key.key + Double.MIN_VALUE);
    }

    @Override
    public DoubleKey getPreviousKey(DoubleKey key) {
        return new DoubleKey(null, key.key - Double.MIN_VALUE);
    }

    @Override
    public KeyInterval<DoubleKey> createInteral(DoubleKey from, DoubleKey to) {
        return new DoubleKeyInterval(from, to);
    }

    @Override
    public KeyInterval<DoubleKey> makeLeftOpen(KeyInterval<DoubleKey> interval) {
        return new DoubleKeyInterval(getNextKey(interval.getFrom()), interval.getTo());
    }

    /**
     * Given two keys, return their distance (difference)
     * @param first the first key
     * @param second the second
     * @return distance of the keys
     */
    @Override
    public DoubleKey getDifference(DoubleKey first, DoubleKey second) {
        return new DoubleKey(null, first.key - second.key);
    }

    @Override
    public DoubleKey getMiddleKey(DoubleKey first, DoubleKey second) {
        return new DoubleKey(null, (first.key + second.key) / 2);
    }
}
