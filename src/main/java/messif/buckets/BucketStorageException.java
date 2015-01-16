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
package messif.buckets;

import messif.utility.HttpErrorCodeProvider;

/**
 * The ancestor of all <code>Throwables</code> that indicate an illegal
 * condition occurred while operating with buckets.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class BucketStorageException extends Exception implements HttpErrorCodeProvider {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;    

    //****************** Attributes ******************//

    /** Bucket error code associated with this exception */
    private final BucketErrorCode errorCode;

    /** HTTP error code from {@link HttpErrorCodeProvider} */
    private int httpErrorCode = HttpErrorCodeProvider.ERROR_CODE_NOT_SET;

    //****************** Constructors ******************//

    /**
     * Constructs a new exception with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     * @param errorCode the bucket error code associated with this exception
     */
    public BucketStorageException(BucketErrorCode errorCode) {
	super();
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param errorCode the bucket error code associated with this exception
     * @param   message   the detail message. The detail message is saved for 
     *          later retrieval by the {@link #getMessage()} method.
     */
    public BucketStorageException(BucketErrorCode errorCode, String message) {
	super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param errorCode the bucket error code associated with this exception
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public BucketStorageException(BucketErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt>.
     *
     * @param errorCode the bucket error code associated with this exception
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public BucketStorageException(BucketErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the bucket error code associated with this exception.
     * @return the bucket error code associated with this exception
     */
    public BucketErrorCode getErrorCode() {
        return errorCode;
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

    /**
     * Sets a new HTTP code to be later returned by this provider.
     * @param httpErrorCode a new HTTP code to be later returned by this provider.
     */
    public void setHttpErrorCode(int httpErrorCode) {
        this.httpErrorCode = httpErrorCode;
    }
    
}
