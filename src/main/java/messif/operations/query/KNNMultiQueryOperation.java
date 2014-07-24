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

import messif.objects.DistanceFunctionMultiObject;
import messif.objects.DistanceFunctionMultiObjectAggregation;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingMultiQueryOperation;

/**
 * K-nearest neighbors query operation with multiple query objects.
 * Retrieves <code>k</code> objects that are nearest to the specified query objects
 * (according to the distance measure).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("KNN multi-object query")
public class KNNMultiQueryOperation extends RankingMultiQueryOperation {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Number of nearest objects to retrieve */
    private final int k;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of KNNMultiQueryOperation for given query objects and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * The distance function is {@link DistanceFunctionMultiObjectAggregation#SUM sum aggregation}.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects"})
    public KNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k) {
        this(queryObjects, k, DistanceFunctionMultiObjectAggregation.SUM);
    }

    /**
     * Creates a new instance of KNNMultiQueryOperation for given query objects and maximal number of objects to return.
     * Objects added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Distance aggregation function"})
    public KNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) {
        this(queryObjects, k, AnswerType.NODATA_OBJECTS, distanceFunction);
    }

    /**
     * Creates a new instance of KNNMultiQueryOperation for given query objects and maximal number of objects to return.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param answerType the type of objects this operation stores in its answer
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Answer type", "Distance aggregation function"})
    public KNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k, AnswerType answerType, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) {
        this(queryObjects, k, false, answerType, distanceFunction);
    }

    /**
     * Creates a new instance of KNNMultiQueryOperation for given query objects and maximal number of objects to return.
     * @param queryObjects the objects to which the nearest neighbors are searched
     * @param k the number of nearest neighbors to retrieve
     * @param storedIndividualDistances if <tt>true</tt>, all distances between the data object and all query objects are
     *          stored in {@link messif.objects.util.RankedAbstractMetaObject sub-distances}
     * @param answerType the type of objects this operation stores in its answer
     * @param distanceFunction the distance function for computing the distances between a data object and all query objects
     */
    @AbstractOperation.OperationConstructor({"Query objects", "Number of nearest objects", "Store individual distances", "Answer type", "Distance aggregation function"})
    public KNNMultiQueryOperation(LocalAbstractObject[] queryObjects, int k, boolean storedIndividualDistances, AnswerType answerType, DistanceFunctionMultiObject<? super LocalAbstractObject> distanceFunction) {
        super(queryObjects, distanceFunction, storedIndividualDistances, answerType, k);
        this.k = k;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the number of nearest objects to retrieve.
     * @return the number of nearest objects to retrieve
     */
    public int getK() {
        return k;
    }

    @Override
    public Object getArgument(int index) throws IndexOutOfBoundsException {
        switch (index) {
        case 0:
            return getQueryObjects();
        case 1:
            return k;
        case 2:
            return getDistanceFunction();
        default:
            throw new IndexOutOfBoundsException(getClass().getSimpleName() + " has only three arguments");
        }
    }

    @Override
    public int getArgumentCount() {
        return 3;
    }


    //****************** Implementation of query evaluation ******************//

    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        int beforeCount = getAnswerCount();

        // Iterate through all supplied objects
        while (objects.hasNext())
            addToAnswerInternal(objects.next(), null, getAnswerThreshold());

        return getAnswerCount() - beforeCount;
    }


    //****************** Equality driven by operation data ******************//

    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        // The argument obj is always kNNQueryOperation or its descendant, because it has only abstract ancestors
        KNNMultiQueryOperation castObj = (KNNMultiQueryOperation)obj;

        if (k != castObj.k)
            return false;

        return super.dataEquals(obj);
    }

    @Override
    public int dataHashCode() {
        return super.dataHashCode() + k;
    }

}
