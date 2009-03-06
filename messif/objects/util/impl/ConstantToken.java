
package messif.objects.util.impl;

/**
 * Simple float constant token for the aggragation function evaluator.
 * 
 * @author xnovak8
 */
public class ConstantToken implements PatternToken {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    /** The float constant. */
    private final float constant;

    /**
     * Constructs this object given a string which must be interpreted as a flost number.
     * @param constantString the float string
     * @throws java.lang.IllegalArgumentException
     */
    public ConstantToken(String constantString) throws IllegalArgumentException {
        try {
            this.constant = Float.parseFloat(constantString);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(numberFormatException);
        }
    }

    /**
     * Return the constant
     * @param subdistances distnaces that are ignored by the constant.
     * @return the constant value
     */
    public final float evaluate(float[] subdistances) {
        return constant;
    }

    @Override
    public String toString() {
        return String.valueOf(constant);
    }
}
