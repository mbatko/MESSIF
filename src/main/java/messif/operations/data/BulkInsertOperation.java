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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import messif.buckets.BucketErrorCode;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectList;
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
public class BulkInsertOperation extends DataManipulationOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Name of a parameter that is used to store list of objects stored by previous processing of this operation */
    public static final String INSERTED_OBJECTS_PARAM = "inserted_objects_param";
    
    //****************** Attributes ******************//

    /** List of objects to insert */
    private List<? extends LocalAbstractObject> insertedObjects;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BulkInsertOperation.
     * 
     * @param insertedObjects a list of objects to be inserted by this operation
     * @param permitEmpty flag whether the empty list of objects to insert is permitted
     *          (<tt>true</tt>) or a {@link NoSuchElementException} is thrown (<tt>false</tt>)
     * @throws NoSuchElementException if the inserted objects list is empty 
     */
    protected BulkInsertOperation(List<? extends LocalAbstractObject> insertedObjects, boolean permitEmpty) throws NoSuchElementException {
        this.insertedObjects = insertedObjects;
        if (!permitEmpty && this.insertedObjects.isEmpty())
            throw new NoSuchElementException();
    }

    /**
     * Creates a new instance of BulkInsertOperation.
     * Empty collection is <em>not</em> permitted.
     *
     * @param insertedObjects a list of objects to be inserted by this operation
     * @throws NoSuchElementException if the inserted objects list is empty 
     */
    public BulkInsertOperation(Collection<? extends LocalAbstractObject> insertedObjects) throws NoSuchElementException {
        this(new ArrayList<LocalAbstractObject>(insertedObjects), false);
    }

    /**
     * Creates a new instance of BulkInsertOperation from all objects provided by the iterator.
     * Empty collection is <em>not</em> permitted.
     * 
     * @param insertedObjects a list of objects to be inserted by this operation
     * @throws NoSuchElementException if the inserted objects list is empty 
     */
    @AbstractOperation.OperationConstructor({"Iterator of objects to insert"})
    public BulkInsertOperation(Iterator<? extends LocalAbstractObject> insertedObjects) throws NoSuchElementException {
        this(new AbstractObjectList<LocalAbstractObject>(insertedObjects), false);
    }

    /**
     * Creates a new instance of BulkInsertOperation.
     * Empty collection <em>is</em> permitted.
     *
     * @param objectsIterator an iterator from which to get the list of objects to be inserted
     * @param count the number of objects to read from the iterator
     * @throws NoSuchElementException if the inserted objects list is empty 
     */
    @AbstractOperation.OperationConstructor({"Iterator (e.g. object stream) to read the objects to insert from", "Number of objects to read"})
    public BulkInsertOperation(Iterator<? extends LocalAbstractObject> objectsIterator, int count) throws NoSuchElementException {
        this(objectsIterator, count, true);
    }

    /**
     * Creates a new instance of BulkInsertOperation.
     *
     * @param objectsIterator an iterator from which to get the list of objects to be inserted
     * @param count the number of objects to read from the iterator
     * @param permitEmpty flag whether the empty list of objects to insert is permitted
     *          (<tt>true</tt>) or a {@link NoSuchElementException} is thrown (<tt>false</tt>)
     * @throws NoSuchElementException if the inserted objects list is empty 
     */
    @AbstractOperation.OperationConstructor({"Iterator (e.g. object stream) to read the objects to insert from", "Number of objects to read", "Permit empty list"})
    public BulkInsertOperation(Iterator<? extends LocalAbstractObject> objectsIterator, int count, boolean permitEmpty) throws NoSuchElementException {
        this(new AbstractObjectList<LocalAbstractObject>(objectsIterator, count), permitEmpty);
    }


    //****************** Attribute access method ******************//

    /**
     * Returns the list of objects to insert.
     * @return the list of objects to insert
     */
    public List<? extends LocalAbstractObject> getInsertedObjects() {
        return Collections.unmodifiableList(insertedObjects);
    }

    /**
     * Return the number of objects to be inserted by this bulk insert operation.
     * @return the number of objects to be inserted by this bulk insert operation.
     */
    public int getNumberInsertedObjects() {
        return insertedObjects.size();
    }

    //****************** Overrides ******************//

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        if (index != 0)
            throw new IndexOutOfBoundsException("BulkInsertOperation has only one argument");
        return getInsertedObjects();
    }

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public boolean wasSuccessful() {
        return isErrorCode(BucketErrorCode.OBJECT_INSERTED, BucketErrorCode.SOFTCAPACITY_EXCEEDED);
    }

    @Override
    public void endOperation() {
        endOperation(BucketErrorCode.OBJECT_INSERTED);
    }

    @Override
    public String toString() {
        return new StringBuffer().append("BulkInsertOperation: object to be inserted: ").append(insertedObjects.size()).toString();
    }

    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        // TODO: solve this better
        insertedObjects.clear();
        //for (LocalAbstractObject object : insertedObjects)
        //    object.clearSurplusData();
    }

    //****************** Cloning ******************//

    @Override
    public BulkInsertOperation clone() throws CloneNotSupportedException {
        BulkInsertOperation operation = (BulkInsertOperation)super.clone();
        if (operation.insertedObjects != null) {
            AbstractObjectList<LocalAbstractObject> abstractObjectList = new AbstractObjectList<LocalAbstractObject>();
            for (LocalAbstractObject localAbstractObject : insertedObjects)
                abstractObjectList.add(localAbstractObject.clone());
            operation.insertedObjects = abstractObjectList;
        }
        return operation;
    }

}
