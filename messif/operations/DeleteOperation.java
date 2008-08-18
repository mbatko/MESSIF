/*
 * DeleteOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import messif.buckets.BucketErrorCode;
import messif.objects.LocalAbstractObject;

/**
 * Operation for deleting an object.
 * The operation keeps one {@link messif.objects.AbstractObject abstract object}.
 * All the objects that are {@link messif.objects.LocalAbstractObject#dataEquals data equal} to this specified
 * object are deleted from an index structure.
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Delete")
public class DeleteOperation extends AbstractOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Operation request attributes ******************/
    
    /** Inserted object (accessible directly) */
    public final LocalAbstractObject deletedObject;

    /** The maximal number of deleted objects, zero means unlimited (accessible directly) */
    public final int deleteLimit;

    /** The number of objects deleted by this operation */
    protected int objectsDeleted = 0;

    /** The total size of objects deleted by this operation */
    protected int totalSizeDeleted = 0;

    /**
     * Returns the number of objects deleted by this operation.
     * @return the number of objects deleted by this operation
     */
    public int getObjectsDeleted() {
        return objectsDeleted;
    }

    /**
     * Returns the total size of objects deleted by this operation.
     * @return the total size of objects deleted by this operation
     */
    public int getTotalSizeDeleted() {
        return totalSizeDeleted;
    }


    /****************** Constructors ******************/

    /**
     * Creates a new instance of DeleteOperation.
     * @param deletedObject the object to match the data against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     */
    @AbstractOperation.OperationConstructor({"Object to delete", "Limit for # of deletions"})
    public DeleteOperation(LocalAbstractObject deletedObject, int deleteLimit) {
        this.deletedObject = deletedObject;
        this.deleteLimit = deleteLimit;
    }

    /**
     * Creates a new instance of DeleteOperation.
     * @param deletedObject the object to match the data against
     */
    @AbstractOperation.OperationConstructor({"Object to delete"})
    public DeleteOperation(LocalAbstractObject deletedObject) {
        this(deletedObject, 0);
    }

    /**
     * Returns argument that was passed while constructing instance.
     * If the argument is not stored within operation, <tt>null</tt> is returned.
     * @param index index of an argument passed to constructor
     * @return argument that was passed while constructing instance
     * @throws IndexOutOfBoundsException if index parameter is out of range
     */
    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        if (index != 0)
            throw new IndexOutOfBoundsException("Delete operation has only one argument");
        return deletedObject;
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 1;
    }

    /**
     * Returns <tt>true</tt> if this operation was successfuly completed.
     * @return <tt>true</tt> if this operation was successfuly completed
     */
    public boolean wasSuccessful() {
        return errValue.equals(BucketErrorCode.OBJECT_DELETED) || errValue.equals(BucketErrorCode.LOWOCCUPATION_EXCEEDED);
    }

    /**
     * End operation successfully.
     */
    public void endOperation() {
        endOperation(1, deletedObject.getSize());
    }

    /**
     * End operation successfully.
     * @param deletedObjects the number of objects deleted by this operation
     * @param totalSizeDeleted total size of objects deleted by this operation
     */
    public void endOperation(int deletedObjects, int totalSizeDeleted) {
        this.errValue = BucketErrorCode.OBJECT_DELETED;
        this.objectsDeleted = deletedObjects;
        this.totalSizeDeleted = totalSizeDeleted;
    }

    /**
     * Mark the specified object as deleted by this operation.
     * @param deletedObject the object that was deleted
     */
    public void addDeletedObject(LocalAbstractObject deletedObject) {
        this.errValue = BucketErrorCode.OBJECT_DELETED;
        this.objectsDeleted++;
        this.totalSizeDeleted += deletedObject.getSize();
    }

    /**
     * Update the operation result.
     * @param operation foreign operation from which to update this one
     */
    @Override
    public void updateFrom(AbstractOperation operation) {
        DeleteOperation castOperation = (DeleteOperation)operation;
        this.objectsDeleted += castOperation.objectsDeleted;
        this.totalSizeDeleted += castOperation.totalSizeDeleted;

        if (errValue.equals(BucketErrorCode.OBJECT_NOT_FOUND) && operation.errValue.isSet())
            errValue = operation.errValue;
        else super.updateFrom(operation);
    }    

    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        deletedObject.clearSurplusData();
    }


    /****************** Equality driven by operation data ******************/

    /** 
     * Indicates whether some other operation has the same data as this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always DeleteOperation or its descendant, because it has only abstract ancestors
        return deletedObject.dataEquals(((DeleteOperation)obj).deletedObject);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return deletedObject.dataHashCode();
    }

}
