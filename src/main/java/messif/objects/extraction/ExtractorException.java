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
package messif.objects.extraction;

import messif.utility.HttpErrorCodeProvider;

/**
 * Throwable that indicates an error during extraction.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractorException extends Exception implements HttpErrorCodeProvider {
    /** Serial version for {@link java.io.Serializable} */
    private static final long serialVersionUID = 2L;

    /** HTTP error code from {@link HttpErrorCodeProvider} */
    private int httpErrorCode = HttpErrorCodeProvider.ERROR_CODE_NOT_SET;
    
    /**
     * Creates a new instance of <code>ExtractorException</code> without detail message.
     */
    public ExtractorException() {
    }

    /**
     * Constructs an instance of <code>ExtractorException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ExtractorException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>ExtractorException</code> with the
     * specified detail message and cause.
     * @param msg the detail message.
     * @param cause the cause which is saved for later retrieval by the
     *         {@link #getCause()} method); a <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown
     */
    public ExtractorException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs an instance of <code>ExtractorException</code> with the specified detail message 
     *  and HTTP error code to be returned.
     * @param msg the detail message.
     * @param httpErrorCode HTTP error code to be returned, if this exception is raised
     */
    public ExtractorException(String msg, int httpErrorCode) {
        super(msg);
        this.httpErrorCode = httpErrorCode;
    }
    
    
    // ************************       Interface HttpErrorCodeProvider      ************************** //
    
    @Override
    public boolean isHttpErrorCodeSet() {
        return httpErrorCode != HttpErrorCodeProvider.ERROR_CODE_NOT_SET;
    }    
    
    @Override
    public int getHttpErrorCode() {
        return httpErrorCode;
    }

    @Override
    public void setHttpErrorCode(int httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
    }

}
