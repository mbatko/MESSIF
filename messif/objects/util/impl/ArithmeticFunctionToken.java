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
package messif.objects.util.impl;

/**
 * This is class for arithmethic functions, like "log", for aggregation function evaluator.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ArithmeticFunctionToken implements PatternToken {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    /** Function type */
    protected static enum FunctionType {
        LOG,
        LOG10
    }

    /**
     * This static method should be in compliance with types recognized by consructor - LOG (for ln), LOG10 (for log_10).
     * @param functionString string to check
     * @return true, if this string is recognized as some arithmetic function
     */
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
