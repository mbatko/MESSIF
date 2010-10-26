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
 * that returns the data as "text/plain" content type. The output is encoded using
 * the given charset (which defaults to {@link Charset#defaultCharset()}).
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SimpleTextResponse extends SimpleResponse {
    /**
     * Creates a new instance of SimpleResponse.
     * @param errorCode an error code returned by the new response
     * @param data text data written by the new response (will be converted to
     *          binary data using the given {@code charset})
     * @param charset the charset used to convert the string data to binary data
     */
    public SimpleTextResponse(int errorCode, String data, Charset charset) {
        super(CONTENT_TYPE_TEXT + ";charset=" + charset.name(), errorCode, data.getBytes(charset));
    }

    /**
     * Creates a new instance of SimpleResponse.
     * @param errorCode an error code returned by the new response
     * @param data text data written by the new response (will be converted to
     *          binary data using {@link Charset#defaultCharset()})
     */
    public SimpleTextResponse(int errorCode, String data) {
        this(errorCode, data, Charset.defaultCharset());
    }

    /**
     * Creates a new instance of SimpleResponse.
     * If {@code data} is instance of {@link IllegalArgumentException}, the
     * {@link #ERROR_CODE_INVALID_ARGUMENT invalid argument} error code is set.
     * If {@code data} is instance of {@link Throwable}, the
     * {@link #ERROR_CODE_INTERNAL_ERROR internal error} code is set.
     * Otherwise, {@link #ERROR_CODE_SUCCESS} is set.
     * In all the cases the {@code data} are converted to binary data using
     * {@link Object#toString()} and the given {@code charset}.
     *
     * @param data the data written by the new response (will be converted to
     *          binary data using {@link Object#toString()} and the given {@code charset})
     * @param charset the charset used to convert the string data to binary data
     */
    public SimpleTextResponse(Object data, Charset charset) {
        this(
                data instanceof IllegalArgumentException ? ERROR_CODE_INVALID_ARGUMENT :
                    (data instanceof Throwable ? ERROR_CODE_INTERNAL_ERROR : ERROR_CODE_SUCCESS),
                data.toString(),
                charset
        );
    }

    /**
     * Creates a new instance of SimpleResponse.
     * If {@code data} is instance of {@link Throwable}, {@link #ERROR_CODE_INTERNAL_ERROR}
     * will be set. Otherwise, {@link #ERROR_CODE_SUCCESS} is set.
     * In both cases the {@code data} are converted to binary data using
     * {@link Object#toString()} and the {@link Charset#defaultCharset() default charset}.
     *
     * @param data the data written by the new response (will be converted to
     *          binary data using {@link Object#toString()} and the {@link Charset#defaultCharset() default charset})
     */
    public SimpleTextResponse(Object data) {
        this(data, Charset.defaultCharset());
    }

}
