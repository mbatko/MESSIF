/*
 * BucketManipulationRequestMessage.java
 *
 * Created on 3. kveten 2003, 17:07
 */


package messif.netbucket;

import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.buckets.OccupationLowException;
import messif.objects.AbstractObject;
import messif.objects.GenericAbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.operations.QueryOperation;
import messif.utility.Logger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.network.Message;
import messif.objects.UniqueID;

/**
 *
 * @author  xbatko
 */
public class BucketManipulationRequestMessage extends Message {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    /** Logger */
    protected static Logger log = Logger.getLoggerEx("netnode.creator");
    
    /****************** Attributes ******************/

    protected final LocalAbstractObject object;
    protected final GenericAbstractObjectList<LocalAbstractObject> objects;
    protected final QueryOperation query;
    protected final int bucketID;
    protected final UniqueID objectID;
    protected final int deleteObjects;


    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of object to a remote bucket
     */
    public BucketManipulationRequestMessage(LocalAbstractObject object, int remoteBucketID) {
        this.object = object;
        this.objects = null;
        this.query = null;
        this.bucketID = remoteBucketID;
        this.objectID = null;
        this.deleteObjects = -1;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition of list of objects to a remote bucket
     */
    public BucketManipulationRequestMessage(Iterator<? extends AbstractObject> objects, int remoteBucketID) {
        this.object = null;
        this.objects = new GenericAbstractObjectList<LocalAbstractObject>();
        this.query = null;
        this.bucketID = remoteBucketID;
        this.objectID = null;
        this.deleteObjects = -1;

        while (objects.hasNext())
            this.objects.add(objects.next().getLocalAbstractObject());
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of object from a remote bucket
     */
    public BucketManipulationRequestMessage(UniqueID remoteObjectID, int remoteBucketID) {
        this(remoteObjectID, remoteBucketID, false);
    }
    
    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval/deletion of object from a remote bucket
     */
    public BucketManipulationRequestMessage(UniqueID remoteObjectID, int remoteBucketID, boolean deleteObject) {
        this.object = null;
        this.objects = null;
        this.query = null;
        this.bucketID = remoteBucketID;
        this.objectID = remoteObjectID;
        this.deleteObjects = deleteObject?0:-1;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests addition/deletion of object from a remote bucket
     */
    public BucketManipulationRequestMessage(LocalAbstractObject object, int remoteBucketID, int deleteObjects) {
        this.object = object;
        this.objects = null;
        this.query = null;
        this.bucketID = remoteBucketID;
        this.objectID = null;
        this.deleteObjects = deleteObjects;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that requests retrieval of all objects from a remote bucket
     */
    public BucketManipulationRequestMessage(int remoteBucketID) {
        this.object = null;
        this.objects = null;
        this.query = null;
        this.bucketID = remoteBucketID;
        this.objectID = null;
        this.deleteObjects = -1;
    }

    /**
     * Creates a new instance of BucketManipulationRequestMessage that executes a query operation on a remote bucket
     */
    public BucketManipulationRequestMessage(QueryOperation query, int remoteBucketID) {
        this.object = null;
        this.objects = null;
        this.query = query;
        this.bucketID = remoteBucketID;
        this.objectID = null;
        this.deleteObjects = -1;
    }


    /****************** Perform operation ******************/
    
    public BucketManipulationReplyMessage execute(BucketDispatcher bucketDispatcher) {
        try {
            // Get bucket from bucket dispatcher
            LocalBucket bucket = bucketDispatcher.getBucket(bucketID);
        
            if (query != null) {
                log.info("Executing query " + query + " from " + getSender() + " in " + bucket);
                return new BucketManipulationReplyMessage(this, bucket.processQuery(query), query);
            } else if (object != null) {
                if (deleteObjects >= 0) {
                    log.info("Deleting from " + getSender() + " object " + object + " from " + bucket);
                    return new BucketManipulationReplyMessage(this, bucket.deleteObject(object, deleteObjects));
                } else {
                    log.info("Adding " + object + " from " + getSender() + " into " + bucket);
                    return new BucketManipulationReplyMessage(this, bucket.addObject(object));
                }
            } else if (objects != null) {
                log.info("Adding set of " + objects.size() + " objects from " + getSender() + " into " + bucket);
                bucket.addObjects(objects);
                return new BucketManipulationReplyMessage(this, BucketErrorCode.OBJECT_INSERTED);
            } else if (deleteObjects >= 0) {
                log.info("Deleting from " + getSender() + " object " + objectID + " from " + bucket);
                return new BucketManipulationReplyMessage(this, bucket.deleteObject(objectID), true);
            } else if (objectID != null) {
                log.info("Returning object " + objectID + " from " + bucket + " to " + getSender());
                return new BucketManipulationReplyMessage(this, bucket.getObject(objectID));
            } else {
                log.info("Returning all objects from " + bucket + " to " + getSender());
                return new BucketManipulationReplyMessage(this, bucket.getAllObjects());
            }
        } catch (NoSuchElementException e) {
            return new BucketManipulationReplyMessage(this, BucketErrorCode.OBJECT_NOT_FOUND);
        } catch (CapacityFullException e) {
            return new BucketManipulationReplyMessage(this, BucketErrorCode.HARDCAPACITY_EXCEEDED);            
        } catch (OccupationLowException e) {
            return new BucketManipulationReplyMessage(this, BucketErrorCode.LOWOCCUPATION_EXCEEDED);
        }
    }
                
}
