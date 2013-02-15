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
package messif.operations;

import java.lang.reflect.InvocationTargetException;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.RankedAbstractMetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;


/**
 * The base class for query operations that return objects ranked by a distance
 * to a single object.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class RankingSingleQueryOperation extends RankingQueryOperation {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query object */
    private LocalAbstractObject queryObject;

    /** Flag whether to store sub-distances for metaobjects */
    private final boolean storeMetaDistances;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RankingSingleQueryOperation.
     * Objects added to answer are {@link AnswerType#NODATA_OBJECTS changed to no-data objects}.
     * Unlimited number of objects can be added to the answer.
     * @param queryObject the query object for this operation
     */
    protected RankingSingleQueryOperation(LocalAbstractObject queryObject) {
        super();
        this.queryObject = queryObject;
        this.storeMetaDistances = false;
    }

    /**
     * Creates a new instance of RankingSingleQueryOperation.
     * Objects added to answer are {@link AnswerType#NODATA_OBJECTS changed to no-data objects}.
     * @param queryObject the query object for this operation
     * @param maxAnswerSize sets the maximal answer size
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingSingleQueryOperation(LocalAbstractObject queryObject, int maxAnswerSize) throws IllegalArgumentException {
        super(maxAnswerSize);
        this.queryObject = queryObject;
        this.storeMetaDistances = false;
    }

    /**
     * Creates a new instance of RankingSingleQueryOperation.
     * @param queryObject the query object for this operation
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingSingleQueryOperation(LocalAbstractObject queryObject, AnswerType answerType, int maxAnswerSize) throws IllegalArgumentException {
        this(queryObject, answerType, maxAnswerSize, false);
    }

    /**
     * Creates a new instance of RankingSingleQueryOperation.
     * Unlimited number of objects can be added to the answer.
     * @param queryObject the query object for this operation
     * @param answerType the type of objects this operation stores in its answer
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link MetaObject meta objects} will
     *          store their {@link RankedAbstractMetaObject sub-distances} in the answer
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingSingleQueryOperation(LocalAbstractObject queryObject, AnswerType answerType, boolean storeMetaDistances) throws IllegalArgumentException {
        super(answerType);
        this.queryObject = queryObject;
        this.storeMetaDistances = storeMetaDistances;
    }

    /**
     * Creates a new instance of RankingSingleQueryOperation.
     * @param queryObject the query object for this operation
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link MetaObject meta objects} will
     *          store their {@link RankedAbstractMetaObject sub-distances} in the answer
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingSingleQueryOperation(LocalAbstractObject queryObject, AnswerType answerType, int maxAnswerSize, boolean storeMetaDistances) throws IllegalArgumentException {
        super(answerType, maxAnswerSize);
        this.queryObject = queryObject;
        this.storeMetaDistances = storeMetaDistances;
    }

    /**
     * Creates a new instance of RankingSingleQueryOperation.
     * @param queryObject the query object for this operation
     * @param answerType the type of objects this operation stores in its answer
     * @param answerCollection collection to be used as answer (it must be empty, otherwise it will be cleared)
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link MetaObject meta objects} will
     *          store their {@link RankedAbstractMetaObject sub-distances} in the answer
     * @throws NullPointerException if the passed collection is <code>null</code>
     */
    protected RankingSingleQueryOperation(LocalAbstractObject queryObject, AnswerType answerType, RankedSortedCollection answerCollection, boolean storeMetaDistances) {
        super(answerType, answerCollection);
        this.queryObject = queryObject;
        this.storeMetaDistances = storeMetaDistances;
    }


    //****************** Data access methods ******************//

    /**
     * Returns the single object that the answer is ranked to.
     * @return the query object of this operation
     */
    public LocalAbstractObject getQueryObject() {
        return queryObject;
    }

    /**
     * Returns <tt>true</tt> if sub-distances for metaobjects are stored in the answer.
     * @return <tt>true</tt> if sub-distances for metaobjects are stored in the answer
     */
    public boolean isStoringMetaDistances() {
        return storeMetaDistances;
    }


    //****************** Cloning ******************//

    @Override
    public RankingSingleQueryOperation clone(boolean preserveAnswer) throws CloneNotSupportedException {
        RankingSingleQueryOperation operation = (RankingSingleQueryOperation)super.clone(preserveAnswer);
        if (operation.queryObject != null)
            operation.queryObject = operation.queryObject.clone();
        return operation;
    }


    //****************** Overrides ******************//

    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        if (queryObject != null)
            queryObject.clearSurplusData();
    }

    /**
     * Adds an object to the answer. The rank of the object is computed automatically
     * as a distance between the {@link #getQueryObject() query object} and the specified object.
     * 
     * @param object the object to add
     * @param distThreshold the threshold on distance;
     *      if the computed distance exceeds the threshold (sharply),
     *      the object is not added to the answer
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedAbstractObject addToAnswer(LocalAbstractObject object, float distThreshold) {
        if (object == null)
            return null;
        if (queryObject == null)
            return super.addToAnswer(object, LocalAbstractObject.UNKNOWN_DISTANCE, null);
        float[] metaDistances = storeMetaDistances ? queryObject.createMetaDistancesHolder() : null;
        float distance = queryObject.getDistance(object, metaDistances, distThreshold);
        if (distance > distThreshold)
            return null;
        return addToAnswer(object, distance, metaDistances);
    }

    /**
     * Adds an object to the answer. The rank of the object is computed automatically
     * as a distance between the {@link #getQueryObject() query object} and the specified object.
     *
     * @param object the object to add
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedAbstractObject addToAnswer(LocalAbstractObject object) {
        return addToAnswer(object, LocalAbstractObject.MAX_DISTANCE);
    }

    /**
     * Creates a new ranking single-query operation of the specified class.
     * @param <E> the class of the operation that should be created
     * @param operationClass the class of the operation that should be created
     * @param queryObject the query object for the operation
     * @param arguments arguments supplied to the constructor; they should match the types of getConstructorArguments(operationClass)
     * @return a new instance of operation
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     * @throws IllegalArgumentException if the argument count or their types don't match the specified operation class constructor
     * @throws InvocationTargetException if there was an exception in the operation's constructor
     */
    public static <E extends RankingSingleQueryOperation> E createOperation(Class<E> operationClass, LocalAbstractObject queryObject, Object... arguments) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        // Add query object to the arguments
        Object[] args = new Object[arguments.length + 1];
        args[0] = queryObject;
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        return createOperation(operationClass, arguments);
    }
}
