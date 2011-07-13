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
import messif.objects.MetaObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AggregationFunction;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingSingleQueryOperation;


/**
 * Aggregation function kNN query operation.
 * Allows to retrieve the best-matching <code>k</code> objects (metaobjects) from any
 * storage using the {@link messif.objects.util.AggregationFunction} function to evalute the 
 * distance between the query object and the objects stored.
 * 
 * @see messif.objects.MetaObject
 * @see AggregationFunction
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Aggregation-function query")
public class AggregationFunctionQueryOperation extends RankingSingleQueryOperation {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 29601L;

    //****************** Attributes ******************//

    /** Number of nearest (top) objects to retrieve */
    protected final int k;

    /** Threshold function for measuring the overall similarity */
    protected final AggregationFunction aggregationFunction;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of AggregationFunctionQueryOperation.
     * The query object should be {@link messif.objects.MetaObject} to make the agg. function meaningful
     * The parameter names for the aggregation should match the names in the {@link messif.objects.MetaObject}.
     * 
     * @param queryObject the query object
     * @param k the number of results to retrieve
     * @param aggregationFunction the aggregation function for combining the distances from sorted lists
     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects",  "Aggregation function"})
    public AggregationFunctionQueryOperation(LocalAbstractObject queryObject, int k, AggregationFunction aggregationFunction) {
        this(queryObject, k, aggregationFunction, AnswerType.NODATA_OBJECTS, true);
    }

    /**
     * Creates a new instance of AggregationFunctionQueryOperation.
     * The query object should be {@link messif.objects.MetaObject} to make the agg. function meaningful
     * The parameter names for the aggregation should match the names in the {@link messif.objects.MetaObject}.
     *
     * @param queryObject the query object
     * @param k the number of results to retrieve
     * @param aggregationFunction the aggregation function for combining the distances from sorted lists
     * @param answerType the type of objects this operation stores in its answer
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link MetaObject meta objects} will

     */
    @AbstractOperation.OperationConstructor({"Query object", "Number of nearest objects", "Aggregation function", "Answer type", "store also the sub-distances?"})
    public AggregationFunctionQueryOperation(LocalAbstractObject queryObject, int k, AggregationFunction aggregationFunction, AnswerType answerType, boolean storeMetaDistances) {
        super((MetaObject)queryObject, answerType, k, storeMetaDistances);
        this.k = k;
        this.aggregationFunction = aggregationFunction;
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
        case 2:
            return aggregationFunction;
        default:
            throw new IndexOutOfBoundsException("TopCombinedQueryOperation has only four arguments");
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
     * Returns the query (meta) object of this query operation.
     * @return the query (meta) object of this query operation
     */
    @Override
    public MetaObject getQueryObject() {
        return (MetaObject)super.getQueryObject();
    }

    /**
     * Returns the number of nearest (top) objects to retrieve.
     * @return the number of nearest (top) objects to retrieve
     */
    public int getK() {
        return k;
    }

    /**
     * Returns the threshold function for measuring the overall similarity.
     * @return the threshold function for measuring the overall similarity
     */
    public AggregationFunction getThresholdFunction() {
        return aggregationFunction;
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

        // Prepare array for subdistances
        float[] descriptorDistances = isStoringMetaDistances() ? new float[aggregationFunction.getParameterNames().length] : null;

        while (objects.hasNext()) {
            // Get current object
            MetaObject object = (MetaObject)objects.next();

            // Compute overall distance (the object must be MetaObject otherwise ClassCastException is thrown)
            float distance = aggregationFunction.getDistance(getQueryObject(), object, descriptorDistances);

            // Object satisfies the query (i.e. distance is smaller than radius)
            addToAnswer(object, distance, descriptorDistances);
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
        // The argument obj is always TopCombinedQueryOperation or its descendant, because it has only abstract ancestors
        AggregationFunctionQueryOperation castObj = (AggregationFunctionQueryOperation)obj;

        if (!getQueryObject().dataEquals(castObj.getQueryObject()))
            return false;
        if (k != castObj.k)
            return false;
        return aggregationFunction.equals(castObj.aggregationFunction);
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
