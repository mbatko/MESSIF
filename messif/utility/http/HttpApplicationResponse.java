/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Application response that is returned back to client using {@link HttpExchange}.
 *
 * @author xbatko
 */
public interface HttpApplicationResponse {
    /** Constant holding a plain text content type */
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    /** Constant holding a XML text content type */
    public static final String CONTENT_TYPE_XML = "text/xml";
    /** Constant holding a HTTP error code of successful operation */
    public static final int ERROR_CODE_SUCCESS = 200;
    /** Constant holding a HTTP error code of internal error */
    public static final int ERROR_CODE_INTERNAL_ERROR = 400;

    /**
     * Returns the content type of this response.
     * @return the content type of this response
     */
    public String getContentType();

    /**
     * Returns the HTTP error code of this response.
     * @return the HTTP error code of this response
     */
    public int getErrorCode();

    /**
     * Returns the length of data stored in this response.
     * If the length is greater than zero, it specifies an exact number of bytes
     * that will be written by {@link #write(java.io.OutputStream) write} method.
     * If the length is zero, then arbitrary amount of data may be written.
     * If the length has the value -1 then the {@link #write(java.io.OutputStream) write}
     * method does not write any data.
     *
     * @return the length of data stored in this response
     */
    public long getLength();

    /**
     * Writes the data stored in this response to the given output.
     * @param os the output stream to which the data are written
     * @throws IOException if there was a problem writing to the stream
     */
    public void write(OutputStream os) throws IOException;
}
