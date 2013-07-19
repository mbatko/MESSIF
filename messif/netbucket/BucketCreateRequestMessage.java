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

import java.util.Map;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;

/**
 * Message for requesting creation of a remote bucket.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketCreateRequestMessage extends BucketRequestMessage<BucketCreateReplyMessage> {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    /** If true, the passed parameters are used instead of the default parameters of the bucket dispatcher */
    private final boolean useParameters;
    
    private final Class<? extends LocalBucket> storageClass;
    private final Map<String, Object> storageClassParams;
    private final long capacity;
    private final long softCapacity;
    
    //****************** Constructor ******************//

    /**
     * Creates a new instance of BucketCreateRequestMessage.
     */
    public BucketCreateRequestMessage() {
        this(false, null, null, 0L, 0L);
    }

    /**
     * If this constructor is used, a bucket with default class and params is created 
     *  but with specified capacity and soft capacity.
     * @param capacity capacity of the new bucket
     * @param softCapacity soft capacity of the new bucket
     */
    public BucketCreateRequestMessage(long capacity, long softCapacity) {
        this(true, null, null, capacity, softCapacity);
    }

    /**
     * If this constructor is used, a bucket given parameters is created.
     * @param storageClass storage class
     * @param storageClassParams parameters of the new storage
     * @param capacity capacity of the new bucket
     * @param softCapacity soft capacity of the new bucket
     */
    public BucketCreateRequestMessage(Class<? extends LocalBucket> storageClass, Map<String, Object> storageClassParams, long capacity, long softCapacity) {
        this(true, storageClass, storageClassParams, capacity, softCapacity);
    }
    
    /**
     * Internal constructor setting all the parameters.
     */
    protected BucketCreateRequestMessage(boolean useParameters, Class<? extends LocalBucket> storageClass, Map<String, Object> storageClassParams, long capacity, long softCapacity) {
        super(0);
        this.useParameters = useParameters;
        this.storageClass = storageClass;
        this.storageClassParams = storageClassParams;
        this.capacity = capacity;
        this.softCapacity = softCapacity;
    }        

    //****************** Executing the request ******************//

    @Override
    public BucketCreateReplyMessage execute(BucketDispatcher bucketDispatcher) throws BucketStorageException {
        try {
            LocalBucket bucket;
            if (!useParameters) {
                bucket = bucketDispatcher.createBucket();
            } else {
                if (storageClass == null) {
                    bucket = bucketDispatcher.createBucket(capacity, softCapacity, bucketDispatcher.getBucketLowOccupation());
                } else {
                    bucket = bucketDispatcher.createBucket(storageClass, storageClassParams, capacity, softCapacity, bucketDispatcher.getBucketLowOccupation());                    
                }
            }
            return new BucketCreateReplyMessage(this, bucket.getBucketID(), bucket.getCapacity());
        } catch (CapacityFullException ignore) {
            return new BucketCreateReplyMessage(this, 0, 0L);
        }
    }

    @Override
    public Class<BucketCreateReplyMessage> replyMessageClass() {
        return BucketCreateReplyMessage.class;
    }

}
