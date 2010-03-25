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

import java.nio.charset.Charset;

/**
 * This class provides a simple implementation of {@link HttpApplicationResponse response}
 * that returns the data as "text/xml" content type. The output is encoded using
 * the given charset (which defaults to {@link Charset#defaultCharset()}).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class SimpleXmlResponse extends SimpleResponse {

    /** Opening tag for data XML */
    private static final String DATA_TAG_OPEN = "<response>";
    /** Closing tag for data XML */
    private static final String DATA_TAG_CLOSE = "</response>";
    /** Opening tag for errors */
    private static final String THROWABLE_TAG_OPEN = "<error>";
    /** Closing tag for errors */
    private static final String THROWABLE_TAG_CLOSE = "</error>";

    /**
     * Creates a new instance of SimpleXmlResponse.
     * Note that a valid XML document is expected in {@code xmlData}, but
     * the syntax is not checked.
     * @param errorCode an error code returned by the new response
     * @param xmlData XML data written by the new response (will be converted to
     *          binary data using the given {@code charset})
     * @param charset the charset used to convert the string data to binary data
     */
    public SimpleXmlResponse(int errorCode, String xmlData, Charset charset) {
        super(CONTENT_TYPE_XML + ";charset=" + charset.name(), errorCode, xmlData.getBytes(charset));
    }

    /**
     * Creates a new instance of SimpleXmlResponse.
     * Note that a valid XML document is expected in {@code xmlData}, but
     * the syntax is not checked.
     * @param errorCode an error code returned by the new response
     * @param xmlData XML data written by the new response (will be converted to
     *          binary data using {@link Charset#defaultCharset()})
     */
    public SimpleXmlResponse(int errorCode, String xmlData) {
        this(errorCode, xmlData, Charset.defaultCharset());
    }

    /**
     * Creates a new instance of SimpleXmlResponse.
     * <p>
     * If {@code data} is instance of {@link Throwable}, {@link #ERROR_CODE_INTERNAL_ERROR}
     * is set and the {@link Throwable} is converted to XML by taking
     * the {@link Throwable#toString() string representation} and wrapping it into
     * {@link #THROWABLE_TAG_OPEN} and {@link #THROWABLE_TAG_CLOSE}.
     * </p>
     * <p>
     * Otherwise, {@link #ERROR_CODE_SUCCESS} is set and the object is converted to XML by taking
     * the {@link Throwable#toString() string representation} and wrapping it into
     * {@link #DATA_TAG_OPEN} and {@link #DATA_TAG_CLOSE}.
     * </p>
     *
     * @param data the data written by the new response (will be converted to
     *          binary data using {@link Object#toString()} and the given {@code charset})
     * @param charset the charset used to convert the string data to binary data
     */
    public SimpleXmlResponse(Object data, Charset charset) {
        this(data instanceof Throwable ? ERROR_CODE_INTERNAL_ERROR : ERROR_CODE_SUCCESS,
                createXmlData(data, charset), charset);
    }

    /**
     * Creates a new instance of SimpleXmlResponse.
     * The {@link Charset#defaultCharset() default charset} is used and
     * the conversion to XML proceeds as in {@link #SimpleXmlResponse(java.lang.Object, java.nio.charset.Charset)}.
     * @param data the data written by the new response (will be converted to
     *          binary data using {@link Object#toString()} and the {@link Charset#defaultCharset() default charset})
     */
    public SimpleXmlResponse(Object data) {
        this(data, Charset.defaultCharset());
    }

    /**
     * Appends a XML header with version 1.0 and given encoding.
     * @param str the string to which to append a XML header
     * @param charset the charset to use
     * @return the string builder passed as {@code str} (useful for chaining)
     */
    public static StringBuilder appendXmlHeader(StringBuilder str, Charset charset) {
        return str.append("<?xml version=\"1.0\" encoding=\"").append(charset.name()).append("\"?>");
    }

    /**
     * Creates a XML string for given object.
     * <p>
     * If {@code data} is instance of {@link Throwable}, it is converted to XML by taking
     * the {@link Throwable#toString() string representation} and wrapping it into
     * {@link #THROWABLE_TAG_OPEN} and {@link #THROWABLE_TAG_CLOSE}.
     * </p>
     * <p>
     * Otherwise, the object is converted to XML by taking
     * the {@link Throwable#toString() string representation} and wrapping it into
     * {@link #DATA_TAG_OPEN} and {@link #DATA_TAG_CLOSE}.
     * </p>
     * @param data the object converted to XML string
     * @param charset charset used in XML header
     * @return XML string
     */
    private static String createXmlData(Object data, Charset charset) {
        StringBuilder str = new StringBuilder();
        appendXmlHeader(str, charset);
        if (data instanceof Throwable)
            str.append(THROWABLE_TAG_OPEN).append(data).append(THROWABLE_TAG_CLOSE);
        else
            str.append(DATA_TAG_OPEN).append(data).append(DATA_TAG_CLOSE);
        return str.toString();
    }
}
