
package messif.objects.util.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.objects.util.AggregationFunction;

/**
 * Evaluator for basic arithmetic operations + application of arithmethmetic functins on particular sub-distances.
 * Basic arithmetic operations (+, -, *, /) are supported as well as numeric constants (treated as floats).
 * Application of "log" function
 * @author xbatko
 */
public class AggregationFunctionEvaluator extends AggregationFunction {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Pattern used to retrieve tokens from the function string */
      private static final Pattern tokenizerPattern = Pattern.compile("\\s*(([\\w.,]+)|([-+*/])|(\\([^\\(\\)]*)|(\\)))\\s*");

    //****************** Attributes ******************//

    /** Parsed variable names that are used in evaluation */
    private final String[] variableNames;

    /** The top level token that encapsulates the whole aggregation function string. */
    private final PatternToken pattern;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of ThresholdFunctionSimpleEvaluator.
     * The specified function is parsed and compiled. Basic arithmetic operations
     * (+, -, *, /) are supported as well as numeric constants (float).
     * Blank space is ignored and everything else is considered to be a variable.
     * @param function the function string
     * @throws IllegalArgumentException if the specified function cannot be parsed
     */
    public AggregationFunctionEvaluator(String function) throws IllegalArgumentException {
        List<SubdistanceToken> subdistancesList = new ArrayList<SubdistanceToken>();
        
        this.pattern = parse(function, subdistancesList);
        variableNames = new String[subdistancesList.size()];
        for (int i = 0; i < variableNames.length; i++) {
            variableNames[i] = subdistancesList.get(i).getName();
        }
    }


    //****************** Expression parsing ******************//

    private PatternToken parse(String patternString, List<SubdistanceToken> currentSubdistanceList) throws IllegalArgumentException {

        // Parse pattern string
        Matcher token = tokenizerPattern.matcher(patternString);

        // the first operand of the currently constructed operation or function
        PatternToken operand1 = null;
        String operationString = null;
        String functionString = null;

        // this counter is set positive in case we are building the "smallest string with the right brackets"
        int bracketsLevel = 0;
        StringBuffer bracketsString = new StringBuffer();

        // Get next token
        while (token.find()) {

            // if it is expression starting by "left bracket"
            String tokenString = token.group(4);
            if (tokenString != null) {
                if ((operand1 != null) && (operationString == null)) {
                    throw new IllegalArgumentException("Parsing brackets " + tokenString + " while operator should be here: "+patternString);
                }
                bracketsLevel ++;
                bracketsString.append(tokenString);
                continue;
            }

            // if an operand is constructed in this iteration, it is stored here (constant, subdist, brackets=tree)
            PatternToken operand = null;
            
            // if we constructing a term with the right number of brackets ((3 - (2 * log)) + 1)
            // and we have reached an expression with "right bracket" ")"
            if (bracketsLevel > 0) {
                tokenString = token.group(5);
                if (tokenString != null) {
                    bracketsLevel --;
                    bracketsString.append(tokenString);

                    // if a good-bracket string was parsed
                    if (bracketsLevel != 0) {
                        continue;
                    }
                    String bracket = bracketsString.toString();
                    bracketsString = new StringBuffer();
                    bracket = bracket.substring(1, bracket.length() - 1);
                    operand = parse(bracket, currentSubdistanceList);
                } else {
                    tokenString = token.group();
                    bracketsString.append(tokenString);
                    continue;
                }
            }
            // if the bracket-expression was NOT ended now
            if (operand == null) {
                // if the token is an operator
                tokenString = token.group(3);
                if (tokenString != null) {
                    if (operand1 == null) {
                        throw new IllegalArgumentException("Arithmetic operator "+tokenString+" must be after a meaningful first operand: "+patternString);
                    }
                    operationString = tokenString;
                    continue;
                }

                // if the token is a constant or if the token is application of a function or a subdistance identifier
                tokenString = token.group(2);
                if (tokenString != null) {
                    // if it is function (log)
                    if (ArithmeticFunctionToken.isFunctionString(tokenString)) {
                        if ((operand1 != null) && (operationString == null)) {
                            throw new IllegalArgumentException("Arithm. function parsed but operation expected: "+patternString);
                        }
                        functionString = tokenString;
                        continue;
                    } else { // this is constant or sub-distance
                        // crate the operand
                        try {
                            operand = new ConstantToken(tokenString);
                        } catch (IllegalArgumentException e) {
                            operand = new SubdistanceToken(tokenString, currentSubdistanceList);
                        }
                    }
                } else {
    //                // if it is a "right bracket"
    //                tokenString = token.group(5);
    //                if (tokenString != null) {
    //                    if (bracketsLevel <= 0) {
    //                        throw new IllegalArgumentException("parsing ')' while no '(' matches: " + patternString);
    //                    }
    //                    bracketsLevel --;
    //                    bracketsString.append(tokenString);
    //
    //                    // if a good bracket string was parsed
    //                    if (bracketsLevel != 0) {
    //                        continue;
    //                    }
    //                    String bracket = bracketsString.toString();
    //                    bracketsString = new StringBuffer();
    //                    bracket = bracket.substring(1, bracket.length() - 1);
    //                    operand = parse(bracket, currentSubdistanceList);
    //                }
                }
            }
            
            // if an operand was created then construct function and/or operation
            if (operand != null) {
                // if this operand succeeds function symbol
                if (functionString != null) {
                    if ((operand1 != null) && (operationString == null)) {
                        throw new IllegalArgumentException("function cannot be applied where operation expected: " + patternString);
                    }
                    operand = new ArithmeticFunctionToken(functionString, operand);
                    functionString = null;
                }

                // if this is second operand for an operation
                if (operationString != null) {
                    if (operand1 == null) {
                        throw new IllegalArgumentException("second operand parsed while the first was not parsed before: "+ patternString);
                    }
                    operand1 = new ArithmeticOperatorToken(operand1, operationString, operand);
                    operationString = null;
                } else { // this was the first operand
                    if (operand1 != null) {
                        throw new IllegalArgumentException("fist operand expected but one already created: "+ operand1.toString() +", while parsing " + tokenString +" in: " + patternString);
                    }
                    operand1 = operand;
                }
                continue;
            }

            // error parsing string - none of the groups matched???
            throw new IllegalArgumentException("None of the groups matched? Error parsing: " + patternString);
        }

        if (operand1 == null) {
            throw new IllegalArgumentException("Error parsing: " + patternString);
        }
        return operand1;
    }
    
    //****************** Evaluating methods ******************//

    public float compute(float... distances) {
        return pattern.evaluate(distances);
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
        StringBuffer buf = new StringBuffer("f = ").append(pattern.toString());
        return buf.toString();
    }

}
