/*
 * DeleteOperation.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private static final long serialVersionUID = 2L;
    
    //****************** Operation request attributes ******************//
    
    /** Object to match the data against */
    protected final LocalAbstractObject deletedObject;

    /** Maximal number of deleted objects, zero means unlimited */
    protected final int deleteLimit;


    //****************** Operation response attributes ******************//

    /** List of all actually deleted objects */
    protected final List<LocalAbstractObject> objects;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of DeleteOperation.
     * @param deletedObject the object to match the data against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     */
    @AbstractOperation.OperationConstructor({"Object to delete", "Limit for # of deletions"})
    public DeleteOperation(LocalAbstractObject deletedObject, int deleteLimit) {
        this.deletedObject = deletedObject;
        this.deleteLimit = deleteLimit;
        this.objects = new ArrayList<LocalAbstractObject>();
    }

    /**
     * Creates a new instance of DeleteOperation.
     * @param deletedObject the object to match the data against
     */
    @AbstractOperation.OperationConstructor({"Object to delete"})
    public DeleteOperation(LocalAbstractObject deletedObject) {
        this(deletedObject, 0);
    }

    //****************** Attribute access ******************//

    /**
     * Returns the object against which to match the deleted objects.
     * @return the object against which to match the deleted objects
     */
    public LocalAbstractObject getDeletedObject() {
        return deletedObject;
    }

    /**
     * Returns the maximal number of deleted objects.
     * Zero means unlimited.
     * @return the maximal number of deleted objects.
     */
    public int getDeleteLimit() {
        return deleteLimit;
    }

    /**
     * Returns the list of all actually deleted objects.
     * @return the list of all actually deleted objects
     */
    public List<LocalAbstractObject> getObjects() {
        return Collections.unmodifiableList(objects);
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


    //****************** Implementation of abstract methods ******************//

    /**
     * Returns <tt>true</tt> if this operation was successfuly completed.
     * @return <tt>true</tt> if this operation was successfuly completed
     */
    public boolean wasSuccessful() {
        return errValue.equals(BucketErrorCode.OBJECT_DELETED);
    }

    /**
     * End operation successfully.
     */
    public void endOperation() {
        this.errValue = BucketErrorCode.OBJECT_DELETED;
    }

    /**
     * Mark the specified object as deleted by this operation.
     * @param deletedObject the object that was deleted
     */
    public void addDeletedObject(LocalAbstractObject deletedObject) {
        objects.add(deletedObject);
    }

    /**
     * Update the operation result.
     * @param operation foreign operation from which to update this one
     */
    @Override
    public void updateFrom(AbstractOperation operation) {
        DeleteOperation castOperation = (DeleteOperation)operation;
        for (LocalAbstractObject object : castOperation.objects)
            this.objects.add(object);

        super.updateFrom(operation);
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
        for (LocalAbstractObject object : objects)
            object.clearSurplusData();
    }


    //****************** Equality driven by operation data ******************//

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
