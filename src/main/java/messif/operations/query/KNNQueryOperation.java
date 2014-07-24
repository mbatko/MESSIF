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

import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.RankedSortedCollection;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingSingleQueryOperation;

/**
 * K-nearest neighbors query operation.
 * Retrieves <code>k</code> objects that are nearest to the specified query object
 * (according to the distance measure).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("k-nearest neighbors query")
public class KNNQueryOperation extends RankingSingleQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Number of nearest objects to retrieve */
    protected final int k;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of kNNQueryOperation for a given query object and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects"})
    public KNNQueryOperation(LocalAbstractObject queryObject, int k) {
        this(queryObject, k, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of kNNQueryOperation for a given query object and maximal number of objects to return.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Answer type"})
    public KNNQueryOperation(LocalAbstractObject queryObject, int k, AnswerType answerType) {
        super(queryObject, answerType, k);
        this.k = k;
    }

    /**
     * Creates a new instance of kNNQueryOperation for a given query object and maximal number of objects to return.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Store the meta-object subdistances?", "Answer type"})
    public KNNQueryOperation(LocalAbstractObject queryObject, int k, boolean storeMetaDistances, AnswerType answerType) {
        super(queryObject, answerType, k, storeMetaDistances);
        this.k = k;
    }

    /**
     * Creates a new instance of kNNQueryOperation for a given query object and maximal number of objects to return.
     * @param queryObject the object to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link messif.objects.MetaObject meta objects} will
     *          store their {@link messif.objects.util.RankedAbstractMetaObject sub-distances} in the answer
     * @param answerType the type of objects this operation stores in its answer
     * @param answerCollection collection to be used as answer (it must be empty, otherwise it will be cleared)
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Store the meta-object subdistances?", "Answer type", "Answer collection"})
    public KNNQueryOperation(LocalAbstractObject queryObject, int k, boolean storeMetaDistances, AnswerType answerType, RankedSortedCollection answerCollection) {
        super(queryObject, answerType, answerCollection, storeMetaDistances);
        this.k = k;
        answerCollection.setMaximalCapacity(k);
    }


    //****************** Attribute access ******************//

    /**
     * Returns the number of nearest objects to retrieve.
     * @return the number of nearest objects to retrieve
     */
    public int getK() {
        return k;
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
            return k;
        default:
            throw new IndexOutOfBoundsException("kNNQueryOperation has only two arguments");
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


    //****************** Implementation of query evaluation ******************//

    /**
     * Evaluate this query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     *
     * @param objects the collection of objects on which to evaluate this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int beforeCount = getAnswerCount();
        
        // Iterate through all supplied objects
        while (objects.hasNext()) {
            // Get current object
            LocalAbstractObject object = objects.next();

            if (getQueryObject().excludeUsingPrecompDist(object, getAnswerThreshold()))
                continue;

            addToAnswer(object, getAnswerThreshold());
        }

        return getAnswerCount() - beforeCount;
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
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        KNNQueryOperation castObj = (KNNQueryOperation)obj;

        if (!getQueryObject().dataEquals(castObj.getQueryObject()))
            return false;

        return k == castObj.k;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return (getQueryObject().dataHashCode() << 8) + k;
    }

}
