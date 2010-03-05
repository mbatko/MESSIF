/*
 * ParameterProcessor
 *
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import messif.utility.Convert;

/**
 * Processor that returns a value from the HTTP parameter.
 *
 * @param <T> the type of the parameter value that this processor returns
 * @author xbatko
 */
public class ParameterProcessor<T> implements HttpApplicationProcessor<T> {
    /** Type to convert the http request parameter to */
    private final Class<? extends T> parameterClass;
    /** Collection of named instances that are used when converting string parameters */
    private final Map<String, Object> namedInstances;
    /** Name of the HTTP parameter to process */
    private final String parameterName;

    /**
     * Creates a new instance of parameter value processor.
     * @param parameterName the name of the HTTP parameter to process
     * @param parameterClass the class of parameter value
     * @param namedInstances collection of named instances that are used when converting string parameters
     */
    public ParameterProcessor(String parameterName, Class<? extends T> parameterClass, Map<String, Object> namedInstances) {
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.namedInstances = namedInstances;
    }

    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        String parameter = httpParams.get(parameterName);
        if (parameter == null)
            throw new IllegalArgumentException("Parameter '" + parameterName + "' was not specified");
        try {
            return Convert.stringToType(parameter, parameterClass, namedInstances);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage(), e.getCause());
        }
    }

    public int getProcessorArgumentCount() {
        return 1;
    }

    public Class<? extends T> getProcessorReturnType() {
        return parameterClass;
    }

}
