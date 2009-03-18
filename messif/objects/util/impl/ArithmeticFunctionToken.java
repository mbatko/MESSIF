
package messif.objects.util.impl;

/**
 * This is class for arithmethic functions, like "log", for aggregation function evaluator.
 * 
 * @author xnovak8
 */
public class ArithmeticFunctionToken implements PatternToken {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    /** Function type */
    protected static enum FunctionType {
        LOG,
        LOG10
    }

    /** This static method should be in compliance with types recognized by consructor. */
    public static boolean isFunctionString(String functionString) {
        return ("log".equalsIgnoreCase(functionString) || "log10".equalsIgnoreCase(functionString));
    }

    /** Function type */
    protected final FunctionType function;

    /** Operand */
    protected final PatternToken operand;

    /**
     * Currently, this class recognizes only the "log" function, which is "ln" in fact.
     * @param functionString string to be parsed
     * @param operand already created token
     * @throws java.lang.IllegalArgumentException
     */
    public ArithmeticFunctionToken(String functionString, PatternToken operand) throws IllegalArgumentException {
        if ("log".equalsIgnoreCase(functionString)) {
            this.function = FunctionType.LOG;
        } else if ("log10".equalsIgnoreCase(functionString)) {
            this.function = FunctionType.LOG10;
        } else {
            throw new IllegalArgumentException("Unknown function identifier: "+functionString);
        }
        this.operand = operand;
    }

    /**
     * Apply the arithmetic function on the argument evaluated on the passed subdistances.
     * @param subdistances specific subdistances for the two meta objects compared
     * @return result of the arithmetic function application to the argument evaluation
     */
    public final float evaluate(float[] subdistances) {
        switch (function) {
            case LOG:
                return (float) Math.log(operand.evaluate(subdistances));
            case LOG10:
                return (float) Math.log10(operand.evaluate(subdistances));
            default:
                return 0f;

        }
    }

    @Override
    public String toString() {
        return new StringBuffer().append(function.toString()).append("( ").append(operand.toString()).append(" )").toString();
    }

}
