package messif.objects.keys;

import java.io.Serializable;

/**
 *
 * @author xnovak8
 */
public class DoubleKeyInterval extends KeyInterval<DoubleKey> implements Serializable {
    
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the upper bound.
     * @return the upper bound.
     */
    @Override
    public DoubleKey getTo() {
        throw new UnsupportedOperationException("Not supported yet.");
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
}
