package messif.objects.keys;

import java.io.Serializable;

/**
 *
 * @author xnovak8
 */
public class DoubleKeyInterval extends KeyInterval<DoubleKey> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;    
    
    /**
     * Lower bound (inclusive).
     */
    protected final DoubleKey from;
    
    /**
     * Upeer bound (inclusive).
     */
    protected final DoubleKey to;
    
    /**
     * Returns the lower bound.
     * @return the lower bound.
     */
    @Override
    public DoubleKey getFrom() {
        return from;
    }

    /**
     * Returns the upper bound.
     * @return the upper bound.
     */
    @Override
    public DoubleKey getTo() {
        return to;
    }

    /**
     * Constructor for this interval.
     * @param from lower bound (inclusive)
     * @param to upper bound (inclusive)
     */
    public DoubleKeyInterval(DoubleKey from, DoubleKey to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public int compareTo(KeyInterval<DoubleKey> o) {
        return from.compareTo(o.getFrom());
    }
}
