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
package messif.netbucket;

import messif.buckets.BucketStorageException;

/**
 * Message for returning exception thrown while manipulating objects on a remote bucket.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketExceptionReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Bucket storage exception to pass back to the originator */
    private final BucketStorageException bucketException;
    /** Runtime exception to pass back to the originator */
    private final RuntimeException runtimeException;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketExceptionReplyMessage for a given bucket storage exception.
     * @param message the original message this message is response to
     * @param bucketException the exception to pass back to the originator
     * @throws NullPointerException if the <code>bucketException</code> is <tt>null</tt>
     */
    public BucketExceptionReplyMessage(BucketRequestMessage<?> message, BucketStorageException bucketException) throws NullPointerException {
        super(message);
        if (bucketException == null)
            throw new NullPointerException();
        this.bucketException = bucketException;
        this.runtimeException = null;
    }

    /**
     * Creates a new instance of BucketReplyMessage for a given runtime exception.
     * @param message the original message this message is response to
     * @param runtimeException the exception to pass back to the originator
     * @throws NullPointerException if the <code>runtimeException</code> is <tt>null</tt>
     */
    public BucketExceptionReplyMessage(BucketRequestMessage<?> message, RuntimeException runtimeException) throws NullPointerException {
        super(message);
        if (runtimeException == null)
            throw new NullPointerException();
        this.bucketException = null;
        this.runtimeException = runtimeException;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the bucket storage exception stored in this message.
     * If the message has a runtime exception instead, it is thrown instead of returning
     * the bucket storage exception.
     * @return the bucket storage exception
     * @throws RuntimeException if this message contains a {@link RuntimeException}
     */
    public BucketStorageException getException() throws RuntimeException {
        if (runtimeException != null)
            throw runtimeException;
        return bucketException;
    }

}
