/*
 * OperationHandler
 *
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.algorithms.Algorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectList;
import messif.operations.AbstractOperation;
import messif.operations.RankingQueryOperation;

/**
 * Utility methods for this package.
 *
 * @author xbatko
 */
public abstract class HttpApplicationUtils {
    /** Parser regular expression for HTTP request parameters */
    private static final Pattern paramParser = Pattern.compile("([^=]+)=([^&]*)(?:&|$)");

    //****************** Handler factory method ******************//

    /**
     * Factory method for creating {@link HttpHandler handlers}.
     * The first argument is expected to be a class according to which this factory
     * picks the handler.
     *
     * <p>
     * The following classes are supported:
     * <ul>
     * <li>{@link AbstractOperation} - the {@link OperationHandler} is created</li>
     * <li>anything else - the {@link SimpleHandler} is created</li>
     * </ul>
     * </p>
     *
     * @param algorithm the algorithm on which the new handler operates
     * @param args arguments for the handler
     * @param offset the index into {@code args} where the first argument is
     * @param length the number of arguments to use
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @return a new handler instance
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
     * @throws IllegalArgumentException if there was a problem creating the handler
     */
    @SuppressWarnings("unchecked")
    public static HttpHandler createHandler(Algorithm algorithm, String args[], int offset, int length, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        try {
            Class<?> handlerClass = Class.forName(args[offset]);
            offset++;
            length--;
            if (AbstractOperation.class.isAssignableFrom(handlerClass))
                return new OperationHandler(algorithm, handlerClass, args, offset, length, namedInstances); // This IS checked on previous line
            else
                return new SimpleHandler<Object>(true, createProcessor(handlerClass, algorithm, args, offset, length, namedInstances));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class: " + e.getMessage());
        }
    }

    //****************** Processor factory method ******************//

    /**
     * Factory method for creating {@link HttpApplicationProcessor processors}.
     * The first argument is expected to be a class according to which this factory
     * picks the processor.
     *
     * <p>
     * The following classes are supported:
     * <ul>
     * <li>{@link RankingQueryOperation} - the {@link OperationProcessorResponse} is created</li>
     * <li>{@link AbstractOperation} - the {@link OperationHandler} is created</li>
     * </ul>
     * </p>
     *
     * @param <T> the class of the instances created by the processor
     * @param processorClass the class of the instances created by the processor
     * @param algorithm the algorithm on which the new processor operates
     * @param args arguments for the processor
     * @param offset the index into {@code args} where the first argument is
     * @param length the number of arguments to use
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @return a new processor instance
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
     * @throws IllegalArgumentException if there was a problem creating the processor
     */
    @SuppressWarnings("unchecked")
    public static <T> HttpApplicationProcessor<T> createProcessor(Class<? extends T> processorClass, Algorithm algorithm, String args[], int offset, int length, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        if (isQuoted(args[offset], '"'))
            return new ParameterProcessor<T>(args[offset].substring(1, args[offset].length() - 1), processorClass, namedInstances);
        else if (LocalAbstractObject.class.isAssignableFrom(processorClass))
            return new ExtractionProcessor(args[offset], processorClass, namedInstances);
        else if (AbstractObjectList.class.isAssignableFrom(processorClass))
            return new ExtractionListProcessor(args[offset], processorClass, namedInstances);
        else if (AbstractOperation.class.isAssignableFrom(processorClass))
            return new OperationProcessor(algorithm, processorClass, args, offset, length, namedInstances);
        else if (args[offset].indexOf('(') != -1)
            return new InstantiatorProcessor<T>(algorithm, args[offset], processorClass, namedInstances);
        else
            return new ValueProcessor<T>(args[offset], processorClass, namedInstances);
    }


    //****************** Request helper methods ******************//

    /**
     * Parse query string parameters.
     * The query string has the following format:
     * <code>name1=value1&amp;name2=value2&amp;...</code>
     * Note that the query string can be get by
     * {@link HttpExchange#getRequestURI()}{@link java.net.URI#getQuery() .getQuery()}.
     *
     * @param query the string with URI-like query parameters
     * @return a hash map with parsed query string parameters (key represents the
     *          parameter name and value is the parameter value)
     */
    public static Map<String, String> parseParameters(String query) {
        if (query == null || query.isEmpty())
            return Collections.emptyMap();
        Matcher matcher = paramParser.matcher(query);
        Map<String, String> parameters = new HashMap<String, String>();
        while (matcher.find())
            parameters.put(matcher.group(1), matcher.group(2));
        return parameters;
    }

    /**
     * Returns <tt>true</tt> if the given {@code string} is surrounded by
     * {@code quote}. That is, if the first and last character of the
     * {@code string} is {@code quote}.
     * @param string the string to check
     * @param quote the quote character
     * @return <tt>true</tt> if the given {@code string} is surrounded by {@code quote} and <tt>false</tt> otherwise
     */
    public static boolean isQuoted(String string, char quote) {
        return string != null && string.length() >= 2 && string.charAt(0) == quote && string.charAt(string.length() - 1) == quote;
    }

}
