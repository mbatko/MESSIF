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
package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates a given {@link HttpApplicationProcessor} and returns its
 * value as {@link SimpleTextResponse} or {@link SimpleXmlResponse}.
 * Note that the value returned by the processor is converted to text using
 * {@link Object#toString()}.
 * If the processor throws an exception, it is captured and returned as
 * text response too.
 *
 * @param <T> the class of the value returned by the encapsulated processor
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SimpleHandler<T> implements HttpHandler {
    /** Type of simple response to return (text or XML) */
    private final boolean asXml;
    /** Processor that actually handles the request */
    private final HttpApplicationProcessor<? extends T> processor;
    /** Logger */
    private final Logger log;

    /**
     * Creates a new instance of SimpleResponseProcessor with the given
     * processor to encapsulate.
     * @param log the logger to use for logging errors (if <tt>null</tt> is passed, no logging is done)
     * @param asXml type of simple response to return - text (<tt>false</tt>) or XML (<tt>true</tt>)
     * @param processor the processor that will actually handle the requests
     */
    public SimpleHandler(Logger log, boolean asXml, HttpApplicationProcessor<? extends T> processor) {
        this.log = log;
        this.asXml = asXml;
        this.processor = processor;
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
            if (log != null)
                log.log(Level.WARNING, "Error processing " + exchange.getRequestURI() + ": " + e);
            response = toResponse(e);
        }

        // Return result
        exchange.getResponseHeaders().set("Content-type", response.getContentType());
        exchange.sendResponseHeaders(response.getErrorCode(), response.getLength());
        response.write(exchange.getResponseBody());
        exchange.close();
    }

    /**
     * Converts a value returned by the {@link #processor} to {@link HttpApplicationResponse}.
     * @param value the value to convert
     * @return the converted response
     */
    protected HttpApplicationResponse toResponse(T value) {
        return asXml ? new SimpleXmlResponse(value) : new SimpleTextResponse(value);
    }

    /**
     * Converts an exception thrown by the {@link #processor} to {@link HttpApplicationResponse}.
     * @param exception the exception to convert
     * @return the converted response
     */
    protected HttpApplicationResponse toResponse(Exception exception) {
        return asXml ? new SimpleXmlResponse(exception) : new SimpleTextResponse(exception);
    }

    /**
     * Returns the processor that actually handles the request.
     * @return the encapsulated processor
     */
    public HttpApplicationProcessor<? extends T> getProcessor() {
        return processor;
    }

    /**
     * Returns whether the response is XML or plain text.
     * @return <tt>true</tt> if this handler returns XML response, otherwise
     *          this handler's response is a plain text
     */
    public boolean isAsXml() {
        return asXml;
    }

}
