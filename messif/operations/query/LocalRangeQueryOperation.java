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

import messif.network.NetworkNode;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;

/**
 * Operation for retrieving objects locally stored at a given peer by a range operation.
 * All objects on the peer that have their distances to the specified query object
 * less than or equal to the specified radius are returned.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Local range query")
public class LocalRangeQueryOperation extends RangeQueryOperation {
    
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /****************** Query request attributes ******************/
    
    /** Identification of the peer which data will be returned. Accessed directly. */
    public final NetworkNode peer;

        
    /****************** Constructors ******************/

    /**
     * Creates a new instance of LocalRangeQueryOperation for a given query object, radius and peer.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject the query object
     * @param radius the query radius
     * @param peer the peer's network identification
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Network node"})
    public LocalRangeQueryOperation(LocalAbstractObject queryObject, float radius, NetworkNode peer) {
        super(queryObject, radius);
        this.peer = peer;
    }

    /**
     * Creates a new instance of LocalRangeQueryOperation for a given query object, radius and peer.
     * @param queryObject the query object
     * @param radius the query radius
     * @param peer the peer's network identification
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Network node", "Answer type"})
    public LocalRangeQueryOperation(LocalAbstractObject queryObject, float radius, NetworkNode peer, AnswerType answerType) {
        super(queryObject, radius, answerType);
        this.peer = peer;
    }

    /**
     * Creates a new instance of LocalRangeQueryOperation for a given query object, radius, maximal number of objects to return and peer.
     * {@link AnswerType#REMOTE_OBJECTS} will be returned in the result.
     * @param queryObject the query object
     * @param radius the query radius
     * @param peer the peer's network identification
     * @param maxAnswerSize sets the maximal answer size
     */
    // This cannot have annotation, since it has also four parameters
    public LocalRangeQueryOperation(LocalAbstractObject queryObject, float radius, NetworkNode peer, int maxAnswerSize) {
        super(queryObject, radius, maxAnswerSize);
        this.peer = peer;
    }

    /**
     * Creates a new instance of LocalRangeQueryOperation for a given query object, radius, maximal number of objects to return and peer.
     * @param queryObject the query object
     * @param radius the query radius
     * @param peer the peer's network identification
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius", "Network node", "Answer type", "Maximal answer size"})
    public LocalRangeQueryOperation(LocalAbstractObject queryObject, float radius, NetworkNode peer, AnswerType answerType, int maxAnswerSize) {
        super(queryObject, radius, answerType, maxAnswerSize);
        this.peer = peer;
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
            return getQueryObject();
        case 1:
            return radius;
        case 2:
            return peer;
        default:
            throw new IndexOutOfBoundsException("RangeQueryOperation has only two arguments");
        }
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * @return number of arguments that were passed while constructing this instance
     */
    @Override
    public int getArgumentCount() {
        return 3;
    }

    
    /**
     * Prints out all the content of answer in a fancy way.
     * @return textual representation of this query
     */
    @Override
    public String toString() {
        return new StringBuffer("Local range query <").append(getQueryObject()).append(',').append(radius).append("> returned ").append(getAnswerCount()).append(" objects").toString();
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
        if (!(obj instanceof LocalGetAllObjectsQueryOperation) || !super.dataEquals(obj))
            return false;
        return peer.equals(((LocalGetAllObjectsQueryOperation)obj).peer);
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return super.dataHashCode() << 8 + peer.hashCode();
    }

}
