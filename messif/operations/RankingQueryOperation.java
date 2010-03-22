/*
 * RankingQueryOperation.java
 *
 * Created on 3. cervenec 2008, 13:16
 */

package messif.operations;

import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.DistanceRanked;
import messif.objects.util.RankedAbstractMetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;


/**
 * The base class for query operations that return {@link AbstractObject objects}
 * ranked by a distance. For example, all basic metric queries are ranking, since
 * range or k-nearest neighbor queries return objects ranked according to their
 * distance to the query object.
 * 
 * <p>
 * Each object in the answer must implement a {@link DistanceRanked} interface
 * that provides access to the distance of the particular answer.
 * Note that the distance only makes sense in the context of a query.
 * </p>
 * 
 * @author xbatko
 */
public abstract class RankingQueryOperation extends QueryOperation<RankedAbstractObject> {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Set holding the answer of this query */
    private RankedSortedCollection answer;

    /** Flag whether to store sub-distances for metaobjects */
    private final boolean storeMetaDistances;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of RankingQueryOperation.
     * Objects added to answer are {@link AnswerType#REMOTE_OBJECTS changed to remote objects}.
     * Unlimited number of objects can be added to the answer.
     */
    protected RankingQueryOperation() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * Objects added to answer are {@link AnswerType#REMOTE_OBJECTS changed to remote objects}.
     * @param maxAnswerSize sets the maximal answer size
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingQueryOperation(int maxAnswerSize) throws IllegalArgumentException {
        this(AnswerType.REMOTE_OBJECTS, maxAnswerSize);
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingQueryOperation(AnswerType answerType, int maxAnswerSize) throws IllegalArgumentException {
        this(answerType, maxAnswerSize, false);
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @param storeMetaDistances if <tt>true</tt>, all processed {@link MetaObject meta objects} will
     *          store their {@link RankedAbstractMetaObject sub-distances} in the answer
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingQueryOperation(AnswerType answerType, int maxAnswerSize, boolean storeMetaDistances) throws IllegalArgumentException {
        super(answerType);
        this.storeMetaDistances = storeMetaDistances;
        if (maxAnswerSize < Integer.MAX_VALUE)
            this.answer = new RankedSortedCollection(maxAnswerSize, maxAnswerSize);
        else
            this.answer = new RankedSortedCollection();
    }


    //****************** Clonning ******************//
    
    /**
     * Create a duplicate of this operation.
     * The answer of the query is not clonned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public RankingQueryOperation clone() throws CloneNotSupportedException {
        RankingQueryOperation operation = (RankingQueryOperation)super.clone();

        // Create a new collection for the answer set
        int maxAnswerSize = operation.answer.getMaximalCapacity();
        if (maxAnswerSize < Integer.MAX_VALUE)
            operation.answer = new RankedSortedCollection(maxAnswerSize, maxAnswerSize);
        else
            operation.answer = new RankedSortedCollection();

        return operation;
    }


    //****************** Overrides for answer set ******************//

    /**
     * Set a new collection that maintains the answer list of this ranking query.
     * Note that this method should be used only for changing the reranking/filtering
     * of the results.
     * @param collection a new instance of answer collection
     */
    public void setAnswerCollection(RankedSortedCollection collection) {
        if (!collection.isEmpty())
            collection.clear();
        collection.addAll(answer);
        this.answer = collection;
    }

    @Override
    public Class<? extends RankedAbstractObject> getAnswerClass() {
        return RankedAbstractObject.class;
    }

    @Override
    public int getAnswerCount() {
        return answer.size();
    }
    
    @Override
    public Iterator<RankedAbstractObject> getAnswer() {
        return answer.iterator();
    }

    public Iterator<RankedAbstractObject> getAnswer(int skip, int count) {
        return answer.iterator(skip, count);
    }

    @Override
    public Iterator<AbstractObject> getAnswerObjects() {
        final Iterator<RankedAbstractObject> iterator = getAnswer();
        return new Iterator<AbstractObject>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public AbstractObject next() {
                return iterator.next().getObject();
            }

            public void remove() {
                iterator.remove();
            }
        };
    }

    /**
     * Returns the current last ranked object in the answer.
     * @return the current last ranked object in the answer
     * @throws NoSuchElementException if the answer is empty
     */
    public RankedAbstractObject getLastAnswer() throws NoSuchElementException {
        return answer.last();
    }

    /**
     * Returns the distance of the last object in the answer.
     * @return the distance of the last object in the answer
     * @throws NoSuchElementException if the answer is empty
     */
    public float getAnswerDistance() throws NoSuchElementException {
        return answer.getLastDistance();
    }

    /**
     * Returns <tt>true</tt> if the current answer has reached 
     * the maximal number of objects, i.e., the <code>maxAnswerSize</code>
     * specified in constructor.
     * @return <tt>true</tt> if the current answer has reached the maximal size
     */
    public boolean isAnswerFull() {
        return answer.isFull();
    }

    /**
     * Returns the threshold distance for the current answer of this query.
     * If the answer has not reached the maximal size (specified in constructor) yet,
     * {@link LocalAbstractObject#MAX_DISTANCE} is returned.
     * Otherwise, the distance of the last answer's object is returned.
     * @return the distance to the last object in the answer list or
     *         {@link LocalAbstractObject#MAX_DISTANCE} if there are not enough objects.
     */
    public float getAnswerThreshold() {
        return answer.getThresholdDistance();
    }

    /**
     * Returns <tt>true</tt> if sub-distances for metaobjects are stored in the answer.
     * @return <tt>true</tt> if sub-distances for metaobjects are stored in the answer
     */
    public boolean isStoringMetaDistances() {
        return storeMetaDistances;
    }
    
    /**
     * Add an object to the answer. The rank of the object is computed automatically
     * as a distance between the query object and the specified object.
     * 
     * @param queryObject the query object against which to compute the distance (rank)
     * @param object the object to add
     * @param distThreshold the threshold on distance;
     *      if the computed distance exceeds the threshold (sharply),
     *      the object is not added to the answer
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedAbstractObject addToAnswer(LocalAbstractObject queryObject, LocalAbstractObject object, float distThreshold) {
        if (object == null)
            return null;
        float[] metaDistances = storeMetaDistances?queryObject.createMetaDistancesHolder():null;
        float distance = queryObject.getDistance(object, metaDistances, distThreshold);
        if (distance > distThreshold)
            return null;
        return addToAnswer(object, distance, metaDistances);
    }

     /**
     * Add a distance-ranked object to the answer.
     * Preserve the information about distances of the respective sub-objects.
     * @param object the object to add
     * @param distance the distance of object
     * @param objectDistances the array of distances to the respective sub-objects (can be <tt>null</tt>)
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     * @throws IllegalArgumentException if the answer type of this operation requires clonning but the passed object cannot be cloned
     */
    public final RankedAbstractObject addToAnswer(AbstractObject object, float distance, float[] objectDistances) throws IllegalArgumentException {
        if (object == null)
            return null;
        RankedAbstractObject rankedObject;
        try {
            // Create the ranked object encapsulation
            if (objectDistances == null)
                rankedObject = new RankedAbstractObject(answerType.update(object), distance);
            else
                rankedObject = new RankedAbstractMetaObject(answerType.update(object), distance, objectDistances);
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }

        // Add the encapsulated object to the answer
        if (answer.add(rankedObject))
            return rankedObject;
        else
            return null;
    }

    /**
     * Reset the current query answer.
     * All objects from the answer are deleted, {@link #getAnswerCount()} will return zero.
     */
    @Override
    public void resetAnswer() {
        answer.clear();
    }

    /**
     * Update the error code and answer of this operation from another operation.
     * @param operation the source operation from which to get the update
     * @throws IllegalArgumentException if the answer of the specified operation is incompatible with this one
     */
    @Override
    public final void updateFrom(AbstractOperation operation) throws IllegalArgumentException {
        super.updateFrom(operation);
        if (operation instanceof RankingQueryOperation)
            updateFrom((RankingQueryOperation)operation);
        else
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
    }

    /**
     * Update the answer of this operation from a {@link RankingQueryOperation}.
     * @param operation the source operation from which to get the update
     */
    protected void updateFrom(RankingQueryOperation operation) {
        answer.addAll(operation.answer);
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
        for (RankedAbstractObject obj : answer)
            obj.clearSurplusData();
    }

}
