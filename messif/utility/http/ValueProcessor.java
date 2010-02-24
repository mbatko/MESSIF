/*
 * ValueProcessor
 *
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import messif.utility.Convert;

/**
 * Processor that returns a fixed value.
 *
 * @param <T> the type of the fixed value that this processor returns
 * @author xbatko
 */
public class ValueProcessor<T> implements HttpApplicationProcessor<T> {
    /** Fixed value returned by this processor */
    private final T value;

    /**
     * Creates a new instance of fixed value processor.
     * @param value the fixed value returned by this processor
     */
    public ValueProcessor(T value) {
        this.value = value;
    }

    /**
     * Creates a new instance of fixed value processor.
     * The value is converted from the given string value to the class using
     * {@link Convert#stringToType(java.lang.String, java.lang.Class) stringToType} method.
     *
     * @param value the string of the fixed value
     * @param valueClass the class of the fixed value
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IllegalArgumentException if the specified {@code value} cannot be converted to {@code valueClass}
     */
    public ValueProcessor(String value, Class<? extends T> valueClass, Map<String, Object> namedInstances) throws IllegalArgumentException {
        try {
            this.value = Convert.stringToType(value, valueClass, namedInstances);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        return value;
    }

    public int getProcessorArgumentCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends T> getProcessorReturnType() {
        return (Class<T>)value.getClass();
    }

}
