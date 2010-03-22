package messif.operations.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import messif.buckets.BucketErrorCode;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;

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

    //****************** Attributes ******************//

    /** Object to match the data against */
    private final LocalAbstractObject deletedObject;

    /** Maximal number of deleted objects, zero means unlimited */
    private final int deleteLimit;

    /** Flag whether to check that the deleted object's locator is equal to {@link #deletedObject}'s locator */
    private final boolean checkLocator;

    /** List of all actually deleted objects */
    private final List<LocalAbstractObject> objects;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of DeleteOperation.
     * @param deletedObject the object to match the data against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     * @param checkLocator flag whether to check that the deleted object's locator is equal to {@link #deletedObject}'s locator
     */
    @AbstractOperation.OperationConstructor({"Object to delete", "Limit for # of deletions", "Check locator when deleting"})
    public DeleteOperation(LocalAbstractObject deletedObject, int deleteLimit, boolean checkLocator) {
        this.deletedObject = deletedObject;
        this.deleteLimit = deleteLimit;
        this.checkLocator = checkLocator;
        this.objects = new ArrayList<LocalAbstractObject>();
    }

    /**
     * Creates a new instance of DeleteOperation.
     * @param deletedObject the object to match the data against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     */
    @AbstractOperation.OperationConstructor({"Object to delete", "Limit for # of deletions"})
    public DeleteOperation(LocalAbstractObject deletedObject, int deleteLimit) {
        this(deletedObject, deleteLimit, false);
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
     * Returns the flag whether to check that the deleted object's locator
     * is equal to {@link #deletedObject}'s locator.
     * @return the flag whether to check the locator when deleting
     */
    public boolean isCheckingLocator() {
        return checkLocator;
    }

    /**
     * Returns the list of all actually deleted objects.
     * @return the list of all actually deleted objects
     */
    public List<LocalAbstractObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    /**
     * Mark the specified object as deleted by this operation.
     * @param deletedObject the object that was deleted
     */
    public void addDeletedObject(LocalAbstractObject deletedObject) {
        objects.add(deletedObject);
    }

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        switch (index) {
            case 0:
                return getDeletedObject();
            case 1:
                return getDeleteLimit();
            case 2:
                return isCheckingLocator();
            default:
                throw new IndexOutOfBoundsException("Delete operation has only two arguments");
        }
    }

    @Override
    public int getArgumentCount() {
        return 3;
    }


    //****************** Implementation of abstract methods ******************//

    public boolean wasSuccessful() {
        return errValue.equals(BucketErrorCode.OBJECT_DELETED);
    }

    public void endOperation() {
        this.errValue = BucketErrorCode.OBJECT_DELETED;
    }

    @Override
    public void updateFrom(AbstractOperation operation) {
        DeleteOperation castOperation = (DeleteOperation)operation;
        for (LocalAbstractObject object : castOperation.objects)
            this.objects.add(object);

        super.updateFrom(operation);
    }    

    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        deletedObject.clearSurplusData();
        for (LocalAbstractObject object : objects)
            object.clearSurplusData();
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always DeleteOperation or its descendant, because it has only abstract ancestors
        return deletedObject.dataEquals(((DeleteOperation)obj).deletedObject);
    }

    @Override
    public int dataHashCode() {
        return deletedObject.dataHashCode();
    }

}
