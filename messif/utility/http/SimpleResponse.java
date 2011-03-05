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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class provides a simple implementation of {@link HttpApplicationResponse response}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public long getLength() {
        return data.length;
    }

    @Override
    public void write(OutputStream os) throws IOException {
        os.write(data);
    }

}
