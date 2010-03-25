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

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.objects.util.AggregationFunction;

/**
 * Evaluator for basic arithmetic functions.
 * Basic arithmetic operations (+, -, *, /) are supported
 * as well as numeric constants (treated as floats).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ThresholdFunctionSimpleEvaluator extends AggregationFunction {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Pattern used to retrieve tokens from the function string */
    private static final Pattern tokenizerPattern = Pattern.compile("\\s*(([\\w.,]+)|([-+*/]))\\s*");


    //****************** Attributes ******************//

    /** Parsed variable names that are used in evaluation */
    private final String[] variableNames;

    /** Parsed variable coefficients (weights) that are used in evaluation */
    private final float[] variableCoeffs;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ThresholdFunctionSimpleEvaluator.
     * The specified function is parsed and compiled. Basic arithmetic operations
     * (+, -, *, /) are supported as well as numeric constants (float).
     * Blank space is ignored and everything else is considered to be a variable.
     * @param function the function string
     * @throws IllegalArgumentException if the specified function cannot be parsed
     */
    public ThresholdFunctionSimpleEvaluator(String function) throws IllegalArgumentException {
        Map<String, Float> descriptorMap = parse(function);
        if (descriptorMap.isEmpty())
            throw new IllegalArgumentException("Specified function contains no variables");

        // Create internal arrays
        variableNames = new String[descriptorMap.size()];
        variableCoeffs = new float[descriptorMap.size()];
        
        // Fill internal arrays
        int i = 0;
        for (Map.Entry<String, Float> entry : descriptorMap.entrySet()) {
            variableNames[i] = entry.getKey();
            variableCoeffs[i] = entry.getValue();
            i++;
        }
    }


    //****************** Expression parsing ******************//

    /**
     * This is simple parser of arithmetic expressions with variables.
     * Parsing only recognizes + - * / operators (no brackets!) and constant numbers.
     * Whitespace is ignored.
     * Everything else is treated as variable name.
     *
     * @param function the function to be parsed
     * @return map where the keys are variable names found in the expression and the values are variable multiplication coefficients
     */
    private Map<String, Float> parse(String function) {
        Map<String, Float> rtv = new HashMap<String, Float>();

        // Parse string function (only +-*/ operations are supported)
        Matcher token = tokenizerPattern.matcher(function);

        // Internal variables for expression
        float currentCoeffValue = 1;
        String currentCoeffName = null;
        char lastOperator = 0;
        boolean isInverted = false;

        // Get next token
        while (token.find()) {
            String operand = token.group(2);
            if (operand != null) {
                // Token is operand
                try {
                    // If token is a number, adjust current coefficient
                    float value = Float.parseFloat(operand);
                    switch (lastOperator) {
                        case '/':
                            currentCoeffValue = currentCoeffValue/value;
                            break;
                        default:
                            currentCoeffValue *= value;
                    }
                } catch (NumberFormatException e) {
                    // The token is variable name
                    currentCoeffName = operand;
                    isInverted = lastOperator == '/';
                }
            } else {
                // Token is operator
                lastOperator = token.group(3).charAt(0);

                // Addition or subtraction delimits variables
                if (lastOperator == '+' || lastOperator == '-') {
                    if (currentCoeffName != null)
                        rtv.put(currentCoeffName, isInverted?1.0f/currentCoeffValue:currentCoeffValue);
                    // Reset coefficient parameters (value & name)
                    currentCoeffValue = (lastOperator == '-')?-1:1;
                    currentCoeffName = null;
                }
            }
        }
        if (currentCoeffName != null)
            rtv.put(currentCoeffName, isInverted?1.0f/currentCoeffValue:currentCoeffValue);

        return rtv;
    }


    //****************** Evaluating methods ******************//

    public float compute(float... distances) {
        if (distances.length < variableCoeffs.length)
            throw new IndexOutOfBoundsException("Distance must be provided for each parameter");
        
        float rtv = 0;
        for (int i = 0; i < variableCoeffs.length; i++)
            rtv += variableCoeffs[i] * distances[i];
        
        return rtv;
    }

    public String[] getParameterNames() {
        return variableNames;
    }


    //****************** String conversion ******************//

    /**
     * Returns a string representation of the encapsulated function.
     * @return a string representation of the encapsulated function
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("f = ");
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(5);
        for (int i = Math.min(variableCoeffs.length, variableNames.length) - 1; i >= 0; i--) {
            buf.append(nf.format(variableCoeffs[i])).append('*').append(variableNames[i]);
            if (i != 0)
                buf.append(" + ");
        }
        return buf.toString();
    }

}
