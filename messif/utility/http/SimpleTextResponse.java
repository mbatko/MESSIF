package messif.utility.http;

import java.nio.charset.Charset;

/**
 * This class provides a simple implementation of {@link HttpApplicationResponse response}
 * that returns the data as "text/plain" content type. The output is encoded using
 * the given charset (which defaults to {@link Charset#defaultCharset()}).
 *
 * @author xbatko
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
     * If {@code data} is instance of {@link Throwable}, {@link #ERROR_CODE_INTERNAL_ERROR}
     * will be set. Otherwise, {@link #ERROR_CODE_SUCCESS} is set.
     * In both cases the {@code data} are converted to binary data using
     * {@link Object#toString()} and the given {@code charset}.
     *
     * @param data the data written by the new response (will be converted to
     *          binary data using {@link Object#toString()} and the given {@code charset})
     * @param charset the charset used to convert the string data to binary data
     */
    public SimpleTextResponse(Object data, Charset charset) {
        this(data instanceof Throwable ? ERROR_CODE_INTERNAL_ERROR : ERROR_CODE_SUCCESS, data.toString(), charset);
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
