
package messif.objects.util.impl;

/**
 * Arithmetic operators like "+, -, *, /" for aggregation function evaluator.
 * @author xnovak8
 */
public class ArithmeticOperatorToken implements PatternToken {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    /** Operator type */
    protected static enum OperatorType {
        PLUS,
        MINUS,
        TIMES,
        SLASH
    }

    /** First operand */
    protected final PatternToken operand1;

    /** Operator type */
    protected final OperatorType operator;

    /** Second operand */
    protected final PatternToken operand2;

    /**
     * Constructs the object given two already created operands and operator string: "+", "-", "*", "/".
     * @param operand1 first operand
     * @param operator operator function "+", "-", "*", "/".
     * @param operand2 second operand
     * @throws java.lang.IllegalArgumentException
     */
    public ArithmeticOperatorToken(PatternToken operand1, String operator, PatternToken operand2) throws IllegalArgumentException {
        this.operand1 = operand1;
        if ("+".equals(operator)) {
            this.operator = OperatorType.PLUS;
        } else if ("-".equals(operator)) {
            this.operator = OperatorType.MINUS;
        } else if ("*".equals(operator)) {
            this.operator = OperatorType.TIMES;
        } else if ("/".equals(operator)) {
            this.operator = OperatorType.SLASH;
        } else {
            throw new IllegalArgumentException("none of known operators +,-,*,/: "+operator);
        }
        this.operand2 = operand2;
    }


    /**
     * Evalutes the arithmetic operator on its operands given a specific subdistances for the two meta objects compared
     * @param subdistances specific subdistances for the two meta objects compared
     * @return result of the arithmetic operation
     */
    public final float evaluate(float[] subdistances) {
        switch (this.operator) {
            case PLUS:
                return operand1.evaluate(subdistances) + operand2.evaluate(subdistances);
            case MINUS:
                return operand1.evaluate(subdistances) - operand2.evaluate(subdistances);
            case TIMES:
                return operand1.evaluate(subdistances) * operand2.evaluate(subdistances);
            case SLASH:
                return operand1.evaluate(subdistances) / operand2.evaluate(subdistances);
            default:
                return 0f;
        }
    }

    @Override
    public String toString() {
        return new StringBuffer("( ").append(operand1.toString()).append(" ").append(operator.toString()).append(" ").append(operand2.toString()).append(" )").toString();
    }

}
