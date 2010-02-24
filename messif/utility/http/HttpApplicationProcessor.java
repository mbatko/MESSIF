package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;

/**
 * Processes a HTTP exchange and provides a value for the processors down in the chain.
 *
 * @param <T> the type of object that this processor returns
 *
 * @author xbatko
 */
public interface HttpApplicationProcessor<T> {
    /**
     * Processes a HTTP exchange and provides a value.
     *
     * @param httpExchange the HTTP exchange (request and response) to use
     * @param httpParams parsed parameters from the HTTP request
     * @return a processed value
     * @throws Exception if there was a problem processing the exchange
     */
    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws Exception;

    /**
     * Returns the number of arguments that this processor requires.
     * Always returns a value greater than 0.
     * @return the number of arguments that this processor requires
     */
    public int getProcessorArgumentCount();

    /**
     * Returns the class the instances of which are produced by this processor.
     * @return the class the returned instances
     */
    public Class<? extends T> getProcessorReturnType();
}
