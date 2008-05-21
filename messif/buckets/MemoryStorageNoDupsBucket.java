/*
 * MemoryStorageNoDupsBucket.java
 *
 * Created on 24. duben 2004, 12:17
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.objects.UniqueID;



/**
 * Extension of {@link MemoryStorageBucket} that doesn't allow to store duplicate objects.
 * If an object that is {@link messif.objects.LocalAbstractObject#dataEquals dataEquals} to
 * any object actually stored in the bucket, the <tt>addObject</tt> method will
 * return {@link BucketErrorCode#OBJECT_DUPLICATE OBJECT_DUPLICATE} instead of storing the object.
 *
 * @author  xbatko
 * @see MemoryStorageBucket
 */
public class MemoryStorageNoDupsBucket extends MemoryStorageBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;
    
    /** Bucket object holder which restricts duplicates in the bucket */
    protected transient Set<LocalAbstractObject.DataEqualObject> uniqueObjects = new HashSet<LocalAbstractObject.DataEqualObject>();

    /****************** Constructors ******************/

    /**
     * Constructs a new MemoryStorageNoDupsBucket instance
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    protected MemoryStorageNoDupsBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }

    /******************     Serialization     **********************/
    
    /** Deserialization -- restore the set using the objects already read by the ancestor */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        uniqueObjects = new HashSet<LocalAbstractObject.DataEqualObject>(objects.size());
        for (LocalAbstractObject object : objects)
            uniqueObjects.add(new LocalAbstractObject.DataEqualObject(object));
    }
    
    /****************** Storing duplicates override ******************/

    /**
     * Stores an object in a physical storage.
     * It should return OBJECT_INSERTED value if the object was successfuly inserted.
     * It returns OBJECT_DUPLICATE if the object (with equal data) already exists in the bucket
     * and the object is not stored.
     *
     * @param object The new object to be inserted
     * @return error code - for details, see documentation of {@link BucketErrorCode}
     */
    @Override
    protected BucketErrorCode storeObject(LocalAbstractObject object) {
        LocalAbstractObject.DataEqualObject wrappedObj = new LocalAbstractObject.DataEqualObject(object);

        if (uniqueObjects.contains(wrappedObj))
            return BucketErrorCode.OBJECT_DUPLICATE;
        
        uniqueObjects.add(wrappedObj);

        return super.storeObject(object);
    }

    @Override
    public LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, OccupationLowException {
        LocalAbstractObject rtv = super.deleteObject(objectID);
        uniqueObjects.remove(new LocalAbstractObject.DataEqualObject(rtv));
        return rtv;
    }


}
