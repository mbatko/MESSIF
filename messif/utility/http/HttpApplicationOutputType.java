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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingQueryOperation;

/**
 * Type of output to use for the data.
 * According to the given type of output the data passed by the {@link HttpApplicationProcessor}
 * are written to the {@link HttpExchange} response.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public enum HttpApplicationOutputType {
    /** Respond with {@link Object#toString() toString()} text of the data */
    TEXT,
    /** Respond with simple XML that has a result tag with a body consisting of the {@link Object#toString() toString()} text of the data */
    XML,
    /** Respond with binary data provided by an input stream */
    INPUT_STREAM,
    /** Respond with binary data provided by a file */
    FILE,
    /** Respond with the status of the operation as plain text */
    OPERATION_STATUS_TEXT,
    /** Respond with the status of the operation as XML text */
    OPERATION_STATUS_XML,
    /** Respond with a JSON array of operation answer object locators */
    OPERATION_ANSWER_TEXT,
    /** Respond with a XML list of operation answer object locators */
    OPERATION_ANSWER_XML,
    /** Respond with a JSON array of operation answer distance-locator pairs */
    OPERATION_ANSWER_RANKED_TEXT,
    /** Respond with a XML list of operation answer distance-locator pairs */
    OPERATION_ANSWER_RANKED_XML;


    //****************** Internal constants ******************//

    /** Constant holding a plain text content type */
    private static final String CONTENT_TYPE_TEXT = "text/plain";
    /** Constant holding a XML text content type */
    private static final String CONTENT_TYPE_XML = "text/xml";
    /** Constant holding a binary content type */
    private static final String CONTENT_TYPE_BINARY = "application/bin";
    /** Constant holding a HTTP error code of successful operation */
    private static final int ERROR_CODE_SUCCESS = 200;
    /** Constant holding a HTTP error code of internal error */
    private static final int ERROR_CODE_INTERNAL_ERROR = 500;
    /** Constant holding a HTTP error code of invalid argument error */
    private static final int ERROR_CODE_INVALID_ARGUMENT = 400;
    /** Opening tag for XML data */
    private static final String XML_DATA_BEGIN = "<response>";
    /** Format of an object locator from the operation answer using XML */
    private static final String XML_DATA_OBJECT = "<result locator=\"%s\"/>";
    /** Format of a ranked object from the operation answer using XML */
    private static final String XML_DATA_RANKED_OBJECT = "<result distance=\"%f\" locator=\"%s\"/>";
    /** Closing tag for XML data */
    private static final String XML_DATA_END = "</response>";
    /** Opening tag for XML errors */
    private static final String XML_ERROR_BEGIN = "<error>";
    /** Closing tag for XML errors */
    private static final String XML_ERROR_END = "</error>";
    /** Opening tag for JSON data */
    private static final String JSON_DATA_BEGIN = "[";
    /** Format of an object locator from the operation answer using JSON array */
    private static final String JSON_DATA_OBJECT = "\"%s\"";
    /** Format of a ranked object from the operation answer using JSON */
    private static final String JSON_DATA_RANKED_OBJECT = "[%f,\"%s\"]";
    /** Closing tag for JSON data */
    private static final String JSON_DATA_END = "]";
    /** Buffer size for copying input stream to output stream */
    private static final int COPY_BUFFER_SIZE = 4096;


    //****************** External methods for processing the output ******************//

    /**
     * Returns whether this output type is compatible with the given data class.
     * @param dataClass the data class to check
     * @return <tt>true</tt> if this type of output supports the given data class as input
     */
    public boolean isCompatible(Class<?> dataClass) {
        switch (this) {
            case TEXT:
            case XML:
                return true;
            case INPUT_STREAM:
                return InputStream.class.isAssignableFrom(dataClass);
            case FILE:
                return File.class.isAssignableFrom(dataClass);
            case OPERATION_STATUS_TEXT:
            case OPERATION_STATUS_XML:
                return AbstractOperation.class.isAssignableFrom(dataClass);
            case OPERATION_ANSWER_TEXT:
            case OPERATION_ANSWER_XML:
                return QueryOperation.class.isAssignableFrom(dataClass);
            case OPERATION_ANSWER_RANKED_TEXT:
            case OPERATION_ANSWER_RANKED_XML:
                return RankingQueryOperation.class.isAssignableFrom(dataClass);
            default:
                throw new InternalError("No compatibility was defined for " + this);
        }
    }

    /**
     * Fills the response of the given {@code httpExchange} with the exception.
     * This means that an either {@link #ERROR_CODE_INVALID_ARGUMENT} or {@link #ERROR_CODE_INTERNAL_ERROR}
     * will be sent back with a body containing the explanation.
     * 
     * @param httpExchange the exchange object the response of which should be filled
     * @param exception the exception that occurred while processing
     * @throws IOException if there was a problem writing to the exchange result
     */
    public void respondHttpExchangeException(HttpExchange httpExchange, Exception exception) throws IOException {
        int errorCode = exception instanceof IllegalArgumentException ? ERROR_CODE_INVALID_ARGUMENT : ERROR_CODE_INTERNAL_ERROR;
        Appendable output;
        switch (this) {
            case XML:
            case OPERATION_STATUS_XML:
            case OPERATION_ANSWER_XML:
            case OPERATION_ANSWER_RANKED_XML:
                output = prepareXmlResponse(httpExchange, errorCode, Charset.defaultCharset())
                        .append(XML_ERROR_BEGIN).append(exception.toString()).append(XML_ERROR_END);
                break;
            default:
                output = prepareTextResponse(httpExchange, errorCode, CONTENT_TYPE_TEXT, Charset.defaultCharset())
                        .append(exception.toString());
                break;
        }

        if (output instanceof Closeable)
            ((Closeable)output).close();
    }

    /**
     * Fills the response of the given {@code httpExchange} with the given data.
     * This means that a {@link #ERROR_CODE_SUCCESS} will be sent back with a body filled with data.
     * 
     * @param httpExchange the exchange object the response of which should be filled
     * @param data the data to send
     * @throws IOException if there was a problem writing to the exchange result
     * @throws ClassCastException if the given data are not compatible with this output type
     */
    public void respondHttpExchangeData(HttpExchange httpExchange, Object data) throws IOException, ClassCastException {
        Appendable output;
        switch (this) {
            case TEXT:
                output = prepareTextResponse(httpExchange, ERROR_CODE_SUCCESS, CONTENT_TYPE_TEXT, Charset.defaultCharset())
                        .append(data.toString());
                break;
            case XML:
                output = prepareXmlResponse(httpExchange, ERROR_CODE_SUCCESS, Charset.defaultCharset())
                        .append(XML_DATA_BEGIN).append(data.toString()).append(XML_DATA_END);
                break;
            case INPUT_STREAM:
                appendInputStreamBinaryData(
                    prepareBinaryResponse(httpExchange, null, 0),
                    (InputStream)data
                ).close();
                return;
            case FILE:
                try {
                    File file = (File)data;
                    InputStream stream = new FileInputStream(file);
                    appendInputStreamBinaryData(
                        prepareBinaryResponse(httpExchange, file.getName(), file.length()),
                        stream
                    ).close();
                } catch (FileNotFoundException e) {
                    respondHttpExchangeException(httpExchange, e);
                }
                return;
            case OPERATION_STATUS_TEXT:
                output = appendOperationStatus(
                    prepareTextResponse(httpExchange, ERROR_CODE_SUCCESS, CONTENT_TYPE_TEXT, Charset.defaultCharset()),
                    (AbstractOperation)data
                );
                break;
            case OPERATION_STATUS_XML:
                output = appendOperationStatus(
                    prepareXmlResponse(httpExchange, ERROR_CODE_SUCCESS, Charset.defaultCharset()).append(XML_DATA_BEGIN),
                    (AbstractOperation)data
                ).append(XML_DATA_END);
                break;                
            case OPERATION_ANSWER_TEXT:
                output = appendAnswerObjects(
                    prepareTextResponse(httpExchange, ERROR_CODE_SUCCESS, CONTENT_TYPE_TEXT, Charset.defaultCharset()).append(JSON_DATA_BEGIN),
                    (QueryOperation<?>)data,
                    JSON_DATA_OBJECT,
                    ","
                ).append(JSON_DATA_END);
                break;
            case OPERATION_ANSWER_XML:
                output = appendAnswerObjects(
                    prepareXmlResponse(httpExchange, ERROR_CODE_SUCCESS, Charset.defaultCharset()).append(XML_DATA_BEGIN),
                    (QueryOperation<?>)data,
                    XML_DATA_OBJECT,
                    null
                ).append(XML_DATA_END);
                break;                
            case OPERATION_ANSWER_RANKED_TEXT:
                output = appendRankedAnswerObjects(
                    prepareTextResponse(httpExchange, ERROR_CODE_SUCCESS, CONTENT_TYPE_TEXT, Charset.defaultCharset()).append(JSON_DATA_BEGIN),
                    (RankingQueryOperation)data,
                    JSON_DATA_RANKED_OBJECT,
                    ","
                ).append(JSON_DATA_END);
                break;
            case OPERATION_ANSWER_RANKED_XML:
                output = appendRankedAnswerObjects(
                    prepareXmlResponse(httpExchange, ERROR_CODE_SUCCESS, Charset.defaultCharset()).append(XML_DATA_BEGIN),
                    (RankingQueryOperation)data,
                    XML_DATA_RANKED_OBJECT,
                    null
                ).append(XML_DATA_END);
                break;                
            default:
                throw new InternalError("No compatibility was defined for " + this);
        }

        if (output instanceof Closeable)
            ((Closeable)output).close();
    }


    //****************** Output preparation methods ******************//

    /**
     * Prepares the given {@code exchange} for sending text.
     * @param exchange the exchange object the response of which should be filled
     * @param errorCode the HTTP error code to return
     * @param contentType the string for the content-type header
     * @param charset the charset to used for conversion of the text data
     * @return the {@link Writer} prepared for sending the text data (note that it must be closed when finished)
     * @throws IOException if there was a problem preparing the exchange result
     */
    private Appendable prepareTextResponse(HttpExchange exchange, int errorCode, String contentType, Charset charset) throws IOException {
        exchange.getResponseHeaders().set("Content-type", contentType + ";charset=" + charset.name());
        exchange.sendResponseHeaders(errorCode, 0);
        return new OutputStreamWriter(exchange.getResponseBody(), charset);
    }

    /**
     * Prepares the given {@code exchange} for sending XML text.
     * A {@link #CONTENT_TYPE_XML} is used and the XML header is sent.
     * @param exchange the exchange object the response of which should be filled
     * @param errorCode the HTTP error code to return
     * @param charset the charset to used for conversion of the text data
     * @return the {@link Writer} prepared for sending the text data (note that it must be closed when finished)
     * @throws IOException if there was a problem preparing the exchange result
     */
    private Appendable prepareXmlResponse(HttpExchange exchange, int errorCode, Charset charset) throws IOException {
        Appendable writer = prepareTextResponse(exchange, errorCode, CONTENT_TYPE_XML, charset);
        writer.append("<?xml version=\"1.0\" encoding=\"");
        writer.append(charset.name());
        writer.append("\"?>");
        return writer;
    }

    /**
     * Prepares the given {@code exchange} for sending binary data.
     * A {@link #CONTENT_TYPE_BINARY} is used.
     * @param exchange the exchange object the response of which should be filled
     * @param fileName the name of the file to send via headers (can be <tt>null</tt>)
     * @param dataLength the length of the data that will be sent (use zero if unknown)
     * @return the {@link OutputStream} prepared for sending the binary data (note that it must be closed when finished)
     * @throws IOException if there was a problem preparing the exchange result
     */
    private OutputStream prepareBinaryResponse(HttpExchange exchange, String fileName, long dataLength) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-type", CONTENT_TYPE_BINARY);
        if (fileName != null)
            headers.set("Content-Disposition", "attachment; filename=" + fileName);
        exchange.sendResponseHeaders(ERROR_CODE_SUCCESS, dataLength);
        return exchange.getResponseBody();
    }


    //****************** Output writing methods ******************//

    /**
     * Write response text for a collection of {@link RankedAbstractObject}s from the {@link RankingQueryOperation} answer.
     * All ranked objects distance and locatorURI formated as specified by {@code itemFormat}
     * are appended to the given {@code output}.
     * @param output the output where the response text is appended
     * @param operation the operation the answer of which to append
     * @param itemFormat the format for exposing the distance and the locator of a single object
     * @param itemSeparator the separator added in between the exported objects
     * @return the passed {@code output} to allow chaining
     * @throws IOException if there was a problem appending to the output
     */
    private Appendable appendRankedAnswerObjects(Appendable output, RankingQueryOperation operation, String itemFormat, String itemSeparator) throws IOException {
        Iterator<RankedAbstractObject> iterator = operation.getAnswer();
        while (iterator.hasNext()) {
            RankedAbstractObject object = iterator.next();
            output.append(String.format(itemFormat, object.getDistance(), object.getObject().getLocatorURI()));
            if (itemSeparator != null && iterator.hasNext())
                output.append(itemSeparator);
        }
        return output;
    }

    /**
     * Write response text for a collection of {@link AbstractObject}s from the {@link QueryOperation} answer.
     * All objects locatorURIs formated as specified by {@code itemFormat} are appended to the given {@code output}.
     * @param output the output where the response text is appended
     * @param operation the operation the answer of which to append
     * @param itemFormat the format for exposing the distance and the locator of a single object
     * @param itemSeparator the separator added in between the exported objects
     * @return the passed {@code output} to allow chaining
     * @throws IOException if there was a problem appending to the output
     */
    private Appendable appendAnswerObjects(Appendable output, QueryOperation<?> operation, String itemFormat, String itemSeparator) throws IOException {
        Iterator<AbstractObject> iterator = operation.getAnswerObjects();
        while (iterator.hasNext()) {
            output.append(String.format(itemFormat, iterator.next().getLocatorURI()));
            if (itemSeparator != null && iterator.hasNext())
                output.append(itemSeparator);
        }
        return output;
    }

    /**
     * Write the status text for the given {@link AbstractOperation}.
     * If the operation has not finished successfully, its error code string is written.
     * @param output the output where the status text is appended
     * @param operation the operation the status of which to append
     * @return the passed {@code output} to allow chaining
     * @throws IOException if there was a problem appending to the output
     */
    private Appendable appendOperationStatus(Appendable output, AbstractOperation operation) throws IOException {
        if (!operation.wasSuccessful() && operation.isFinished()) {
            output.append("Operation failed: ");
            output.append(operation.getErrorCode().toString());
        } else {
            output.append("Operation finished successfully");
        }
        return output;
    }

    /**
     * Write the binary data from the given {@link InputStream} to the given {@code output}.
     * @param output the output where the binary data are written
     * @param data the stream providing the binary data to write
     * @return the passed {@code output} to allow chaining
     * @throws IOException if there was a problem appending to the output
     */
    private OutputStream appendInputStreamBinaryData(OutputStream output, InputStream data) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int bytesRead = data.read(buffer);
        while (bytesRead > 0) {
            output.write(buffer, 0, bytesRead);
            bytesRead = data.read(buffer);
        }
        return output;
    }

}
