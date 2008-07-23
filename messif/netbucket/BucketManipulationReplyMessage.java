/*
 * BucketManipulationReplyMessage.java
 *
 * Created on 3. kveten 2003, 17:07
 */

package messif.netbucket;

import messif.buckets.BucketErrorCode;
import messif.network.ReplyMessage;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;
import messif.operations.QueryOperation;

/**
 *
 * @author xbatko
 */
public class BucketManipulationReplyMessage extends ReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    /****************** Attributes ******************/
    
    protected final BucketErrorCode errorCode;
    protected final LocalAbstractObject object;
    protected final AbstractObjectList<LocalAbstractObject> objects;
    protected final QueryOperation query;
    protected final int changesCount;
    
    /****************** Attribute access methods ******************/

    public LocalAbstractObject getObject() {
        return object;
    }
    
    public AbstractObjectList<LocalAbstractObject> getObjects() {
        return objects;
    }

    public int getChangesCount() {
        return changesCount;
    }

    public BucketErrorCode getErrorCode() {
        return errorCode;
    }

    public QueryOperation getQuery() {
        return query;
    }


    /****************** Constructors ******************/

    /**
     * Creates a new instance of BucketManipulationReplyMessage for adding object
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, BucketErrorCode errorCode, int changesCount) {
        super(message);
        this.errorCode = errorCode;
        this.object = null;
        this.objects = null;
        this.changesCount = changesCount;
        this.query = null;
    }
    
    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting object
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, LocalAbstractObject object) {
        this(message, object, false);
    }
     
    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting object
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, LocalAbstractObject object, boolean deleteObject) {
        super(message);
        this.errorCode = deleteObject?BucketErrorCode.OBJECT_DELETED:null;
        this.object = object;
        this.objects = null;
        this.changesCount = 0;
        this.query = null;
    }
     
    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting all objects
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        super(message);
        errorCode = null;
        object = null;
        this.objects = new AbstractObjectList<LocalAbstractObject>(objects);
        this.changesCount = 0;
        this.query = null;
    }

    /**
     * Creates a new instance of BucketManipulationReplyMessage for getting query results
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, int changesCount, QueryOperation query) {
        super(message);
        errorCode = null;
        object = null;
        this.objects = null;
        this.changesCount = changesCount;
        this.query = query;
    }

    /**
     * Creates a new instance of BucketManipulationReplyMessage for deleting object by data
     */
    public BucketManipulationReplyMessage(BucketManipulationRequestMessage message, int changesCount) {
        this(message, changesCount, null);
    }

}
