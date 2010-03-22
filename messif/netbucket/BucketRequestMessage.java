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

import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.network.Message;


/**
 * Generic message for requesting an object manipulation on a remote bucket.
 *
 * @param <T> the type of reply that is expected as a result for this request
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class BucketRequestMessage<T extends BucketReplyMessage> extends Message {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//

    /** ID of a remote bucket on which to process the request */
    protected final int bucketID;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketRequestMessage.
     * @param bucketID the ID of a remote bucket on which to process the request
     */
    protected BucketRequestMessage(int bucketID) {
        this.bucketID = bucketID;
    }


    //****************** Executing the request ******************//

    /**
     * Executes this request on the specified bucket dispatcher.
     * This method is intended to be used on the destination peer where the bucket is kept.
     * @param bucketDispatcher the dispatcher that can provide the bucket of for the request
     * @return the reply message with the result of the processing
     * @throws RuntimeException if there was an error processing this request
     * @throws BucketStorageException if there was an error processing this request
     */
    public abstract T execute(BucketDispatcher bucketDispatcher) throws RuntimeException, BucketStorageException;

    /**
     * Returns the class of the reply message that is received as a response to this request message.
     * @return the class of the reply message that is received as a response to this request message
     */
    public abstract Class<T> replyMessageClass();
}
