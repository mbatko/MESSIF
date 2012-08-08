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

import messif.buckets.BucketErrorCode;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;

/**
 * Operation for inserting an object.
 * The operation keeps one {@link messif.objects.AbstractObject abstract object}
 * that is going to be inserted into an index structure.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Insert")
public class InsertOperation extends AbstractOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Operation request attributes ******************//
    
    /** Inserted object */
    protected LocalAbstractObject insertedObject;
         
    
    //****************** Constructors ******************//

    /**
     * Creates a new instance of InsertOperation.
     * @param insertedObject the object to insert by this operation
     */
    @AbstractOperation.OperationConstructor({"Object to insert"})
    public InsertOperation(LocalAbstractObject insertedObject) {
        this.insertedObject = insertedObject;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the object being inserted.
     * @return the object being inserted
     */
    public LocalAbstractObject getInsertedObject() {
        return insertedObject;
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
            throw new IndexOutOfBoundsException("InsertOperation has only one argument");
        return insertedObject;
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
     * Returns <tt>true</tt> if this operation has finished successfuly.
     * Otherwise, <tt>false</tt> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <tt>true</tt> if this operation has finished successfuly
     */
    @Override
    public boolean wasSuccessful() {
        return isErrorCode(BucketErrorCode.OBJECT_INSERTED, BucketErrorCode.SOFTCAPACITY_EXCEEDED);
    }

    /** End operation successfully */
    @Override
    public void endOperation() {
        endOperation(BucketErrorCode.OBJECT_INSERTED);
    }

    /**
     * Return a string representation of this operation.
     * @return a string representation of this operation
     */
    @Override
    public String toString() {
        return new StringBuffer().append("inserted object: ").append(insertedObject).toString();
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
        // TODO: solve this better
        //insertedObject.clearSurplusData();
    }

    
    //****************** Cloning ******************//

    @Override
    public InsertOperation clone() throws CloneNotSupportedException {
        InsertOperation operation = (InsertOperation)super.clone();
        if (operation.insertedObject != null)
            operation.insertedObject = operation.insertedObject.clone();
        return operation;
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
        // The argument obj is always InsertOperation or its descendant, because it has only abstract ancestors
        return insertedObject.dataEquals(((InsertOperation)obj).insertedObject);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return insertedObject.dataHashCode();
    }

}
