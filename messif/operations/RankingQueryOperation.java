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

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.CollectionProviders;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.utility.ErrorCode;


/**
 * The base class for query operations that return {@link AbstractObject objects}
 * ranked by a distance. For example, all basic metric queries are ranking, since
 * range or k-nearest neighbor queries return objects ranked according to their
 * distance to the query object.
 * 
 * <p>
 * Each object in the answer is a {@link RankedAbstractObject}
 * that provides access to the distance and the object of the particular answer.
 * Note that the distance only makes sense in the context of a query.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class RankingQueryOperation extends QueryOperation<RankedAbstractObject> {
    /** class id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** Set holding the answer of this query */
    private RankedSortedCollection answer;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of RankingQueryOperation.
     * Objects added to answer are {@link AnswerType#NODATA_OBJECTS changed to no-data objects}.
     * Unlimited number of objects can be added to the answer.
     */
    protected RankingQueryOperation() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * Objects added to answer are {@link AnswerType#NODATA_OBJECTS changed to no-data objects}.
     * @param maxAnswerSize sets the maximal answer size
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingQueryOperation(int maxAnswerSize) throws IllegalArgumentException {
        this(AnswerType.NODATA_OBJECTS, maxAnswerSize);
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * Unlimited number of objects can be added to the answer.
     * @param answerType the type of objects this operation stores in its answer
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingQueryOperation(AnswerType answerType) throws IllegalArgumentException {
        this(answerType, Integer.MAX_VALUE);
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     * @param maxAnswerSize sets the maximal answer size
     * @throws IllegalArgumentException if the maximal answer size is negative
     */
    protected RankingQueryOperation(AnswerType answerType, int maxAnswerSize) throws IllegalArgumentException {
        this(answerType, ((maxAnswerSize < Integer.MAX_VALUE) ? new RankedSortedCollection(maxAnswerSize, maxAnswerSize) : new RankedSortedCollection()));
    }

    /**
     * Creates a new instance of RankingQueryOperation.
     * @param answerType the type of objects this operation stores in its answer
     * @param answerCollection collection to be used as answer (it must be empty, otherwise it will be cleared)
     * @throws NullPointerException if the passed collection is <code>null</code>
     */
    protected RankingQueryOperation(AnswerType answerType, RankedSortedCollection answerCollection) {
        super(answerType);
        if (!answerCollection.isEmpty())
            answerCollection.clear();
        this.answer = answerCollection;
    }


    //****************** Cloning ******************//
    
    /**
     * Create a duplicate of this operation.
     * The answer of the query is not cloned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public RankingQueryOperation clone() throws CloneNotSupportedException {
        RankingQueryOperation operation = (RankingQueryOperation)super.clone();

        // Create a new collection for the answer set (without data)
        operation.answer = (RankedSortedCollection)operation.answer.clone(false);

        return operation;
    }


    //****************** Overrides for answer set ******************//

    /**
     * Set a new collection that maintains the answer list of this ranking query.
     * Note that this method should be used only for changing the re-ranking/filtering
     * of the results.
     * @param collection a new instance of answer collection
     */
    public void setAnswerCollection(RankedSortedCollection collection) {
        if (!collection.isEmpty())
            collection.clear();
        collection.addAll(answer);
        this.answer = collection; // This assignment IS intended
    }

    @Override
    public Class<? extends RankedAbstractObject> getAnswerClass() {
        return RankedAbstractObject.class;
    }

    /**
     * Class of the current answer assigned in the operation.
     * @return class of answer collection.
     */
    public Class<? extends RankedSortedCollection> getAnswerCollectionClass() {
        if (answer == null)
            return RankedSortedCollection.class;
        else
            return answer.getClass();
    }

    @Override
    public int getAnswerCount() {
        return answer.size();
    }

    /**
     * Returns the maximal capacity of the answer collection.
     * @return the maximal capacity of the answer collection
     */
    public int getAnswerMaximalCapacity() {
        return answer.getMaximalCapacity();
    }
    
    /**
     * Returns the internal comparator of the answer collection (often null).
     * @return the internal comparator of the answer collection
     */
    public Comparator<? super RankedAbstractObject> getAnswerComparator() {
        return answer.getComparator();
    }
    
    @Override
    public Iterator<RankedAbstractObject> getAnswer() {
        return answer.iterator();
    }

    @Override
    public Iterator<RankedAbstractObject> getAnswer(int skip, int count) {
        return answer.iterator(skip, count);
    }

    /**
     * Returns an iterator over all objects in the answer that are ranked higher
     * than the <code>minDistance</code> but lower than the <code>maxDistance</code>.
     * @param minDistance the minimal distance of the answer objects to return
     * @param maxDistance the maximal distance of the answer objects to return
     * @return an iterator over the objects in the answer to this query
     */
    public Iterator<RankedAbstractObject> getAnswerDistanceRestricted(float minDistance, float maxDistance) {
        return answer.iteratorDistanceRestricted(minDistance, maxDistance);
    }

    /**
     * Returns an iterator over all objects in the answer that are ranked lower
     * than the <code>maxDistance</code>.
     * @param maxDistance the maximal distance of the answer objects to return
     * @return an iterator over the objects in the answer to this query
     */
    public Iterator<RankedAbstractObject> getAnswerDistanceRestricted(float maxDistance) {
        return answer.iteratorDistanceRestricted(maxDistance);
    }

    @Override
    public Iterator<AbstractObject> getAnswerObjects() {
        return RankedAbstractObject.getObjectsIterator(getAnswer());
    }

    @Override
    public int getSubAnswerCount() {
        return CollectionProviders.getCollectionCount(answer);
    }

    @Override
    public Iterator<? extends RankedAbstractObject> getSubAnswer(int index) throws IndexOutOfBoundsException {
        return CollectionProviders.getCollectionIterator(answer, index, getAnswerClass());
    }

    @Override
    public Iterator<? extends RankedAbstractObject> getSubAnswer(Object key) {
        return CollectionProviders.getCollectionByKeyIterator(answer, key, getAnswerClass(), false);
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
     * Add a distance-ranked object to the answer.
     * Preserve the information about distances of the respective sub-objects.
     * @param object the object to add
     * @param distance the distance of object
     * @param objectDistances the array of distances to the respective sub-objects (can be <tt>null</tt>)
     * @return the distance-ranked object object that was added to answer or <tt>null</tt> if the object was not added
     * @throws IllegalArgumentException if the answer type of this operation requires cloning but the passed object cannot be cloned
     */
    public RankedAbstractObject addToAnswer(AbstractObject object, float distance, float[] objectDistances) throws IllegalArgumentException {
        return answer.add(answerType, object, distance, objectDistances);
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
        if (operation instanceof RankingQueryOperation)
            updateFrom((RankingQueryOperation)operation);
        else
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
        super.updateFrom(operation);
    }

    /**
     * Update the answer of this operation from a {@link RankingQueryOperation}.
     * @param operation the source operation from which to get the update
     */
    protected void updateFrom(RankingQueryOperation operation) {
        answer.addAll(operation.answer);
    }

    @Override
    public void endOperation(ErrorCode errValue) throws IllegalArgumentException {
        super.endOperation(errValue);
        if (answer instanceof EndOperationListener)
            ((EndOperationListener)answer).onEndOperation(this, errValue);
    }
}
