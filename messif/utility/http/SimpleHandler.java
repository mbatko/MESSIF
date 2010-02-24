/*
 * SimpleResponseProcessor
 *
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Encapsulates a given {@link HttpApplicationProcessor} and returns its
 * value as {@link SimpleTextResponse} or {@link SimpleXmlResponse}.
 * Note that the value returned by the processor is converted to text using
 * {@link Object#toString()}.
 * If the processor throws an exception, it is captured and returned as
 * text response too.
 *
 * @author xbatko
 */
public class SimpleHandler<T> implements HttpHandler {
    /** Type of simple response to return (text or XML) */
    private final boolean asXml;
    /** Processor that actually handles the request */
    private final HttpApplicationProcessor<? extends T> processor;

    /**
     * Creates a new instance of SimpleResponseProcessor with the given
     * processor to encapsulate.
     * @param asXml type of simple response to return - text (<tt>false</tt>) or XML (<tt>true</tt>)
     * @param processor the processor that will actually handle the requests
     */
    public SimpleHandler(boolean asXml, HttpApplicationProcessor<? extends T> processor) {
        this.processor = processor;
        this.asXml = asXml;
    }

    /**
     * Handles a HTTP request by executing the processor and returning
     * response.
     *
     * @param exchange the HTTP request/response exchange
     * @throws IOException if there was a problem reading the HTTP request or writing the HTTP response
     */
    public final void handle(HttpExchange exchange) throws IOException {
        // Process the exchange by processor
        HttpApplicationResponse response;
        try {
            response = toResponse(processor.processHttpExchange(exchange, HttpApplicationUtils.parseParameters(exchange.getRequestURI().getQuery())));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            response = toResponse(e);
        }

        // Return result
        exchange.getResponseHeaders().set("Content-type", response.getContentType());
        exchange.sendResponseHeaders(response.getErrorCode(), response.getLength());
        response.write(exchange.getResponseBody());
        exchange.close();
    }

    protected HttpApplicationResponse toResponse(T value) {
        return asXml ? new SimpleTextResponse(value) : new SimpleXmlResponse(value);
    }

    protected HttpApplicationResponse toResponse(Exception exception) {
        return asXml ? new SimpleTextResponse(exception) : new SimpleXmlResponse(exception);
    }

    public HttpApplicationProcessor<? extends T> getProcessor() {
        return processor;
    }

    public boolean isAsXml() {
        return asXml;
    }

}
