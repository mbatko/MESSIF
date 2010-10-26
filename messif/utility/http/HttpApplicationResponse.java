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
import java.io.IOException;
import java.io.OutputStream;

/**
 * Application response that is returned back to client using {@link HttpExchange}.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface HttpApplicationResponse {
    /** Constant holding a plain text content type */
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    /** Constant holding a XML text content type */
    public static final String CONTENT_TYPE_XML = "text/xml";
    /** Constant holding a HTTP error code of successful operation */
    public static final int ERROR_CODE_SUCCESS = 200;
    /** Constant holding a HTTP error code of internal error */
    public static final int ERROR_CODE_INTERNAL_ERROR = 500;
    /** Constant holding a HTTP error code of invalid argument error */
    public static final int ERROR_CODE_INVALID_ARGUMENT = 400;

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
