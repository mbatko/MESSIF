/*
 * ThresholdFunctionSimpleEvaluator.java
 *
 */

package messif.objects.impl;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.objects.ThresholdFunction;

/**
 *
 * @author xbatko
 */
public class ThresholdFunctionSimpleEvaluator extends ThresholdFunction {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;


    protected final String[] descriptorNames;
    protected final float[] descriptorCoeffs;

    /**
     * Creates a new instance of ThresholdFunctionSimpleEvaluator
     */
    public ThresholdFunctionSimpleEvaluator(String function) {
        Map<String, Float> descriptorMap = parse(function);

        // Create internal arrays
        descriptorNames = new String[descriptorMap.size()];
        descriptorCoeffs = new float[descriptorMap.size()];
        
        // Fill internal arrays
        int i = 0;
        for (Map.Entry<String, Float> entry : descriptorMap.entrySet()) {
            descriptorNames[i] = entry.getKey();
            descriptorCoeffs[i] = entry.getValue();
            i++;
        }
    }


    /****************** Expression parsing ******************/

    private static final Pattern tokenizerPattern = Pattern.compile("\\s*(([\\w.,]+)|([-+*/]))\\s*");

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


    /****************** Evaluating methods ******************/

    public float compute(float... distances) {
        if (distances.length < descriptorCoeffs.length)
            throw new IndexOutOfBoundsException("Distance must be provided for each parameter");
        
        float rtv = 0;
        for (int i = 0; i < descriptorCoeffs.length; i++)
            rtv += descriptorCoeffs[i] * distances[i];
        
        return rtv;
    }

    public String[] getParameterNames() {
        return descriptorNames;
    }


    /****************** Evaluating methods ******************/

    /**
     * Returns a string representation of the encapsulated function.
     * @return a string representation of the encapsulated function
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("f = ");
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(5);
        for (int i = Math.min(descriptorCoeffs.length, descriptorNames.length) - 1; i >= 0; i--) {
            buf.append(nf.format(descriptorCoeffs[i])).append('*').append(descriptorNames[i]);
            if (i != 0)
                buf.append(" + ");
        }
        return buf.toString();
    }

}
