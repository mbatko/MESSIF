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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.AbstractObjectList;


/**
 * NetworkBucketDispatcher message that can request several operations to be performed with 
 *  the specified bucket: <ul>
 *  <li>add given objects,</li> 
 *  <li>add ALL objects from a specified local bucket</li>
 *  <li>remove objects (by locator or equal),</li>
 *  <li>find and return stored objects (specified by locator, key or return ALL objects).</li>
 * </ul>
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BucketManipulationRequestMessage extends BucketRequestMessage<BucketManipulationReplyMessage> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Logger */
    private static final Logger log = Logger.getLogger("netnode.creator");

    //****************** Attributes ******************//

    private final LocalAbstractObject object;
    private final AbstractObjectList<LocalAbstractObject> objects;
    private final String objectLocator;
    private final AbstractObjectKey objectKey;
    private final int deleteObjects;
    private final boolean addResultToOperation;
    private final int sourceBucketID;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of object to a remote bucket
     */
    public BucketManipulationRequestMessage(LocalAbstractObject object, int remoteBucketID) {
        this(object, remoteBucketID, -1);
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition/removal of object to/from a remote bucket
     */
    public BucketManipulationRequestMessage(LocalAbstractObject object, int remoteBucketID, int deleteObjects) {
        super(remoteBucketID);
        this.object = object;
        this.objects = null;
        this.objectLocator = null;
        this.objectKey = null;
        this.deleteObjects = deleteObjects;
        this.addResultToOperation = false;
        this.sourceBucketID = BucketDispatcher.UNASSIGNED_BUCKET_ID;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of list of objects to a remote bucket
     */
    public BucketManipulationRequestMessage(Iterator<? extends LocalAbstractObject> objects, int remoteBucketID) {
        super(remoteBucketID);
        this.object = null;
        this.objects = new AbstractObjectList<LocalAbstractObject>();
        this.objectLocator = null;
        this.objectKey = null;
        this.deleteObjects = -1;
        this.addResultToOperation = false;
        this.sourceBucketID = BucketDispatcher.UNASSIGNED_BUCKET_ID;

        while (objects.hasNext())
            this.objects.add(objects.next());
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(String remoteObjectLocator, int remoteBucketID) {
        this(remoteObjectLocator, remoteBucketID, -1);
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(String remoteObjectLocator, int remoteBucketID, int deleteObjects) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.objectLocator = remoteObjectLocator;
        this.objectKey = null;
        this.deleteObjects = deleteObjects;
        this.addResultToOperation = false;
        this.sourceBucketID = BucketDispatcher.UNASSIGNED_BUCKET_ID;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(AbstractObjectKey remoteObjectKey, int remoteBucketID) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.objectLocator = null;
        this.objectKey = remoteObjectKey;
        this.deleteObjects = -1;
        this.addResultToOperation = false;
        this.sourceBucketID = BucketDispatcher.UNASSIGNED_BUCKET_ID;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of all objects from a remote bucket
     */
    public BucketManipulationRequestMessage(int remoteBucketID) {
        this(remoteBucketID, BucketDispatcher.UNASSIGNED_BUCKET_ID);
    }
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of ALL object from specified bucket.
     */
    public BucketManipulationRequestMessage(int remoteBucketID, int sourceBucketID) {
        super(remoteBucketID);
        this.object = null;
        this.objects = null;
        this.deleteObjects = -1;
        this.objectLocator = null;
        this.objectKey = null;
        this.addResultToOperation = false;
        this.sourceBucketID = sourceBucketID;
    }


    //****************** Perform operation ******************//

    @Override
    public BucketManipulationReplyMessage execute(BucketDispatcher bucketDispatcher) throws BucketStorageException {
        // Get bucket from bucket dispatcher
        LocalBucket bucket = bucketDispatcher.getBucket(bucketID);

        if (deleteObjects >= 0) {
            if (object != null) {
                log.log(Level.INFO, "Deleting from {0} object {1} from {2}", new Object[]{getSender(), object, bucket});
                bucket.deleteObject(object, deleteObjects);
                return new BucketManipulationReplyMessage(this, null, true);
            } else {
                log.log(Level.INFO, "Deleting from {0} object with locator {1} from {2}", new Object[]{getSender(), objectLocator, bucket});
                bucket.deleteObject(objectLocator, deleteObjects);
                return new BucketManipulationReplyMessage(this, null, true);
            }
        } else if (object != null) {
            log.log(Level.INFO, "Adding {0} from {1} into {2}", new Object[]{object, getSender(), bucket});
            bucket.addObject(object);
            return new BucketManipulationReplyMessage(this, bucket.isSoftCapacityExceeded() ? BucketErrorCode.SOFTCAPACITY_EXCEEDED : BucketErrorCode.OBJECT_INSERTED);
        } else if (objects != null) {
            log.log(Level.INFO, "Adding set of {0} objects from {1} into {2}", new Object[]{objects.size(), getSender(), bucket});
            bucket.addObjects(objects);
            return new BucketManipulationReplyMessage(this, BucketErrorCode.OBJECT_INSERTED);
        } else if (sourceBucketID != BucketDispatcher.UNASSIGNED_BUCKET_ID) {
            LocalBucket sourceBucket = bucketDispatcher.getBucket(sourceBucketID);
            log.log(Level.INFO, "Adding ALL {0} objects from bucket {1} into bucket {2}", new Object[]{sourceBucket.getObjectCount(), sourceBucketID, bucket});
            bucket.addObjects(sourceBucket.getAllObjects());
            return new BucketManipulationReplyMessage(this, BucketErrorCode.OBJECT_INSERTED);
        } else if (objectLocator != null) {
            log.log(Level.INFO, "Returning object with locator {0} from {1} to {2}", new Object[]{objectLocator, bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getObject(objectLocator));
        } else if (objectKey != null) {
            log.log(Level.INFO, "Returning object with key {0} from {1} to {2}", new Object[]{objectKey, bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getObject(objectKey));
        } else {
            log.log(Level.INFO, "Returning all objects from {0} to {1}", new Object[]{bucket, getSender()});
            return new BucketManipulationReplyMessage(this, bucket.getAllObjects());
        }
    }

    @Override
    public Class<BucketManipulationReplyMessage> replyMessageClass() {
        return BucketManipulationReplyMessage.class;
    }

}
