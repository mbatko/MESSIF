
package messif.operations;

import messif.buckets.BucketErrorCode;
import messif.objects.util.AbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;

/**
 * Operation for inserting several objects at once.
 * The operation keeps a list of {@link messif.objects.AbstractObject abstract objects}
 * that are going to be inserted into an index structure.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
@AbstractOperation.OperationName("BulkInsert")
public class BulkInsertOperation extends AbstractOperation {
    
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Operation request attributes ******************/
    
    /** Inserted object (accessible directly) */
    public final AbstractObjectList<? extends LocalAbstractObject> insertedObjects;
         
    
    /****************** Constructors ******************/

    /**
     * Creates a new instance of BulkInsertOperation.
     * 
     * @param insertedObjects a list of objects to be inserted by this operation
     */
    @AbstractOperation.OperationConstructor({"List of objects to insert"})
    public BulkInsertOperation(AbstractObjectList<? extends LocalAbstractObject> insertedObjects) {
        this.insertedObjects = insertedObjects;
    }
        
    /**
     * Creates a new instance of BulkInsertOperation.
     * 
     * @param stream a stream from which to read the list of objects to be inserted
     * @param count the number of objects to read from the stream
     */
    @AbstractOperation.OperationConstructor({"Stream to read objects to insert from, Number of objects to read"})
    public BulkInsertOperation(StreamGenericAbstractObjectIterator<LocalAbstractObject> stream, int count) {
        this.insertedObjects = new AbstractObjectList<LocalAbstractObject>(stream, count);
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
            throw new IndexOutOfBoundsException("BulkInsertOperation has only one argument");
        return insertedObjects;
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 1;
    }

    /** Get status of the operation */
    public boolean wasSuccessful() {
        return errValue.equals(BucketErrorCode.OBJECT_INSERTED) || errValue.equals(BucketErrorCode.SOFTCAPACITY_EXCEEDED);
    }

    /** End operation successfully */
    public void endOperation() {
        errValue = BucketErrorCode.OBJECT_INSERTED;
    }

    /**
     * Update only the error codes - all codes other than OBJECT_INSERTED have priority and should propagete up.
     * @param operation the source operation from which to get the update
     */
    @Override
    public void updateAnswer(AbstractOperation operation) {
        if (!errValue.isSet() || errValue.equals(BucketErrorCode.OBJECT_INSERTED))
            errValue = operation.errValue;
    }
    
    /**
     * Return a string representation of this operation.
     * @return a string representation of this operation
     */
    @Override
    public String toString() {
        return new StringBuffer().append("BulkInsertOperation: object to be inserted: ").append(insertedObjects.size()).toString();
    }

    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSuplusData() {
        super.clearSuplusData();
        for (LocalAbstractObject object : insertedObjects)
            object.clearSurplusData();
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
        // The argument obj is always BulkInsertOperation or its descendant, because it has only abstract ancestors
        return insertedObjects.dataEquals(((BulkInsertOperation)obj).insertedObjects);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return insertedObjects.dataHashCode();
    }

}
