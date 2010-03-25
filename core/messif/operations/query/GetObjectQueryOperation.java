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
package messif.operations.query;

import java.util.NoSuchElementException;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.SingletonQueryOperation;


/**
 * Operation for retriving an instance of object having the desired ID (passed in constructor).
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Get object query")
public class GetObjectQueryOperation extends SingletonQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Identifier for which to retrieve object */
    protected final UniqueID objectID;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of GetObjectQueryOperation for the specified object ID.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param objectID the object ID to search for
     */
    @AbstractOperation.OperationConstructor({"Object ID"})
    public GetObjectQueryOperation(UniqueID objectID) {
        super();
        this.objectID = objectID;
    }

    /**
     * Creates a new instance of GetObjectQueryOperation for the specified object ID.
     * @param objectID the object ID to search for
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Object ID", "Answer type"})
    public GetObjectQueryOperation(UniqueID objectID, AnswerType answerType) {
        super(answerType);
        this.objectID = objectID;
    }


    //****************** Parameter access methods ******************//

    /**
     * Returns the identifier for which to retrieve object.
     * @return the identifier for which to retrieve object
     */
    public UniqueID getObjectID() {
        return objectID;
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
            throw new IndexOutOfBoundsException("GetObjectQueryOperation has only one argument");
        return objectID;
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 1;
    }


    //****************** Default implementation of query evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        // Iterate through all supplied objects
        try {
            addToAnswer(objects.getObjectByID(objectID));
            return 1;
        } catch (NoSuchElementException e) {
            // If there was no object with the specified ID
            return 0;
        }
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
        // The argument obj is always GetObjectQueryOperation or its descendant, because it has only abstract ancestors
        return objectID.equals(((GetObjectQueryOperation)obj).objectID);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return objectID.hashCode();
    }

}
