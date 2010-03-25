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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import messif.buckets.BucketErrorCode;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;

/**
 * Operation for deleting an object.
 * The operation contains a set of {@link messif.objects.AbstractObject#getLocatorURI() locator URIs}.
 * All the objects that have any of these locators set are deleted from an index structure.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Delete by locator")
public class DeleteByLocatorOperation extends AbstractOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** The locators of the objects to delete */
    private final Set<String> locators;

    /** Maximal number of deleted objects, zero means unlimited */
    private final int deleteLimit;

    /** List of all actually deleted objects */
    private final List<LocalAbstractObject> objects;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of DeleteByLocatorOperation.
     * The given set is directly used.
     * @param locators collection of locators of objects to delete
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     */
    protected DeleteByLocatorOperation(Set<String> locators, int deleteLimit) {
        this.locators = locators;
        this.deleteLimit = deleteLimit;
        this.objects = new ArrayList<LocalAbstractObject>();
    }

    /**
     * Creates a new instance of DeleteByLocatorOperation.
     * @param locators collection of locators of objects to delete
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     */
    public DeleteByLocatorOperation(Collection<String> locators, int deleteLimit) {
        this(new HashSet<String>(locators), deleteLimit);
    }

    /**
     * Creates a new instance of DeleteByLocatorOperation.
     * Number of objects to delete is unlimited
     * @param locators collection of locators of objects to delete
     */
    public DeleteByLocatorOperation(Collection<String> locators) {
        this(locators, 0);
    }

    /**
     * Creates a new instance of DeleteByLocatorOperation.
     * @param locators collection of locators of objects to delete
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     */
    @AbstractOperation.OperationConstructor({"Locators to delete", "Limit for # of deletions"})
    public DeleteByLocatorOperation(String[] locators, int deleteLimit) {
        this(Arrays.asList(locators), deleteLimit);
    }

    /**
     * Creates a new instance of DeleteByLocatorOperation.
     * Number of objects to delete is unlimited
     * @param locators collection of locators of objects to delete
     */
    @AbstractOperation.OperationConstructor({"Locators to delete"})
    public DeleteByLocatorOperation(String[] locators) {
        this(locators, 0);
    }


    //****************** Attribute access ******************//

    /**
     * Returns the locators of objects to delete.
     * @return the locators of objects to delete
     */
    public Set<String> getLocators() {
        return Collections.unmodifiableSet(locators);
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
     * Returns whether the number of deleted objects has reached the {@link #deleteLimit}.
     * @return <tt>true</tt> if the limit on the number of deleted objects was reached
     */
    public boolean isLimitReached() {
        if (deleteLimit <= 0)
            return false;
        return objects.size() >= deleteLimit;
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
        switch (index) {
            case 0:
                return getLocators();
            case 1:
                return getDeleteLimit();
            default:
                throw new IndexOutOfBoundsException("DeleteByLocator operation has only two arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 2;
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
        DeleteByLocatorOperation castOperation = (DeleteByLocatorOperation)operation;
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
        // The argument obj is always DeleteByLocatorOperation or its descendant, because it has only abstract ancestors
        return locators.equals(((DeleteByLocatorOperation)obj).locators);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return locators.hashCode();
    }

}
