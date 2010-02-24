/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.utility.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class provides a simple implementation of {@link HttpApplicationResponse response}.
 * 
 * @author xbatko
 */
public class SimpleResponse implements HttpApplicationResponse {
    /** Content type returned by this response */
    private final String contentType;
    /** Error code returnd by this response */
    private final int errorCode;
    /** Data written by this response */
    private final byte[] data;

    /**
     * Creates a new instance of SimpleResponse.
     * @param contentType a content type returned by the new response
     * @param errorCode an error code returned by the new response
     * @param data binary data written by the new response
     */
    public SimpleResponse(String contentType, int errorCode, byte[] data) {
        this.contentType = contentType;
        this.errorCode = errorCode;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public long getLength() {
        return data.length;
    }

    public void write(OutputStream os) throws IOException {
        os.write(data);
    }

}
