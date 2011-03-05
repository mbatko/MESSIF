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
package messif.operations.data;

import java.util.Collection;
import java.util.Iterator;
import messif.buckets.BucketErrorCode;
import messif.objects.AbstractObject;
import messif.objects.util.AbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;

/**
 * Operation for inserting several objects at once.
 * The operation keeps a list of {@link messif.objects.AbstractObject abstract objects}
 * that are going to be inserted into an index structure.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Bulk insert")
public class BulkInsertOperation extends AbstractOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** List of objects to insert */
    private final AbstractObjectList<? extends LocalAbstractObject> insertedObjects;


    //****************** Constructors ******************//

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
     * @param insertedObjects a list of objects to be inserted by this operation
     */
    public BulkInsertOperation(Collection<? extends LocalAbstractObject> insertedObjects) {
        this.insertedObjects = new AbstractObjectList<LocalAbstractObject>(insertedObjects);
    }

    /**
     * Creates a new instance of BulkInsertOperation.
     * 
     * @param insertedObjects a list of objects to be inserted by this operation
     */
    public BulkInsertOperation(Iterator<? extends LocalAbstractObject> insertedObjects) {
        this.insertedObjects = new AbstractObjectList<LocalAbstractObject>(insertedObjects);
    }

    /**
     * Creates a new instance of BulkInsertOperation.
     *
     * @param objectsIterator an iterator from which to get the list of objects to be inserted
     * @param count the number of objects to read from the iterator
     */
    @AbstractOperation.OperationConstructor({"Iterator (e.g. object stream) to read the objects to insert from", "Number of objects to read"})
    public BulkInsertOperation(Iterator<? extends LocalAbstractObject> objectsIterator, int count) {
        this.insertedObjects = new AbstractObjectList<LocalAbstractObject>(objectsIterator, count);
    }


    //****************** Attribute access method ******************//

    /**
     * Returns the list of objects to insert.
     * @return the list of objects to insert
     */
    public AbstractObjectList<? extends LocalAbstractObject> getInsertedObjects() {
        return insertedObjects;
    }


    //****************** Overrides ******************//

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        if (index != 0)
            throw new IndexOutOfBoundsException("BulkInsertOperation has only one argument");
        return insertedObjects;
    }

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public boolean wasSuccessful() {
        return errValue.equals(BucketErrorCode.OBJECT_INSERTED) || errValue.equals(BucketErrorCode.SOFTCAPACITY_EXCEEDED);
    }

    @Override
    public void endOperation() {
        errValue = BucketErrorCode.OBJECT_INSERTED;
    }

    /**
     * Update the error code of this operation from another operation.
     * All codes other than OBJECT_INSERTED have priority and should propagete up.
     * @param operation the source operation from which to get the update
     */
    @Override
    public void updateFrom(AbstractOperation operation) {
        if (!errValue.isSet() || errValue.equals(BucketErrorCode.OBJECT_INSERTED))
            errValue = operation.getErrorCode();
    }

    @Override
    public String toString() {
        return new StringBuffer().append("BulkInsertOperation: object to be inserted: ").append(insertedObjects.size()).toString();
    }

    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        for (LocalAbstractObject object : insertedObjects)
            object.clearSurplusData();
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always BulkInsertOperation or its descendant, because it has only abstract ancestors
        return insertedObjects.dataEquals(((BulkInsertOperation)obj).insertedObjects);
    }

    @Override
    public int dataHashCode() {
        return insertedObjects.dataHashCode();
    }

}
