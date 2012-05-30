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

import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.CollectionProviders;
import messif.objects.util.DistanceRankedSortedCollection;
import messif.objects.util.RankedJoinObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.EndOperationListener;
import messif.operations.QueryOperation;
import messif.utility.ErrorCode;

/**
 * Similarity join query operation.
 * It retrieves all pairs of objects that are closer than the parameter <code>mu</code> to each other 
 * (according to object's distance function).
 * 
 * The top-k closest pairs can be retrieved by specifying the parameter <code>k</code>.
 * 
 * It also supports approximate parameters for early termination.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("similairity join query")
public class JoinQueryOperation extends QueryOperation<RankedJoinObject> {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Attributes ******************//

    /** Set holding the answer of this query */
    private DistanceRankedSortedCollection<RankedJoinObject> answer;

    /** Distance threshold */
    protected final float mu;

    /** Number of nearest pairs to retrieve */
    protected final int k;
    
    /** Flag whether symmetric pairs should be avoided in the answer */
    protected boolean skipSymmetricPairs;
    
    //****************** Constructors ******************//

    /**
     * Creates a new instance of JoinQueryOperation for a given distance threshold.
     * The pairs added to the answer are not checked for reoccurrence, so even pairs of swapped objects are added.
     * Objects in qualifying pairs added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param mu the distance threshold
     */
    @AbstractOperation.OperationConstructor({"Distance threshold"})
    public JoinQueryOperation(float mu) {
        this(mu, Integer.MAX_VALUE, false, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of JoinQueryOperation for a given distance threshold and the flag whether symmetric pairs can be added or not.
     * Objects in qualifying pairs added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param mu the distance threshold
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     * @param answerType the type of objects this operation stores in pairs in its answer
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Skip symmetric pairs", "Answer type"})
    public JoinQueryOperation(float mu, boolean skipSymmetricPairs, AnswerType answerType) {
        this(mu, Integer.MAX_VALUE, skipSymmetricPairs, answerType);
    }

    /**
     * Creates a new instance of JoinQueryOperation for a given distance threshold and maximal number of pairs to return.
     * Objects in qualifying pairs added to answer are updated to {@link AnswerType#NODATA_OBJECTS no-data objects}.
     * @param mu the distance threshold
     * @param k the number of nearest pairs to retrieve
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Number of nearest pairs", "Skip symmetric pairs"})
    public JoinQueryOperation(float mu, int k, boolean skipSymmetricPairs) {
        this(mu, k, skipSymmetricPairs, AnswerType.NODATA_OBJECTS);
    }

    /**
     * Creates a new instance of JoinQueryOperation for a given distance threshold and maximal number of objects to return.
     * @param mu the distance threshold
     * @param k the number of nearest pairs to retrieve
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     * @param answerType the type of objects this operation stores in pairs in its answer
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Number of nearest pairs", "Skip symmetric pairs", "Answer type"})
    public JoinQueryOperation(float mu, int k, boolean skipSymmetricPairs, AnswerType answerType) {
        super(answerType);
        if (k < Integer.MAX_VALUE)
            this.answer = new DistanceRankedSortedCollection<RankedJoinObject>(k, k);
        else
            this.answer = new DistanceRankedSortedCollection<RankedJoinObject>();
        this.mu = mu;
        this.k = k;
        this.skipSymmetricPairs = skipSymmetricPairs;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the threshold on distance between objects of a pair to qualify.
     * @return the distance threshold
     */
    public float getMu() {
        return mu;
    }

    /**
     * Returns the threshold on distance between objects of a pair to qualify.
     * @return the distance threshold
     */
    public final float getDistanceThreshold() {
        return getMu();
    }

    /**
     * Returns the maximum number of nearest pairs to retrieve.
     * @return the maximum number of nearest pairs to retrieve
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
            return mu;
        case 1:
            return k;
        case 2:
            return skipSymmetricPairs;
        default:
            throw new IndexOutOfBoundsException("JoinQueryOperation has only two arguments");
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

        // Make a local list of all passed objects
        AbstractObjectList<LocalAbstractObject> list = new AbstractObjectList<LocalAbstractObject>(objects);

        if (skipSymmetricPairs) {
            for (int i1 = 0; i1 < list.size(); i1++) {
                LocalAbstractObject o1 = list.get(i1);
                for (int i2 = i1+1; i2 < list.size(); i2++)
                    addToAnswer(o1, list.get(i2));
            }
        } else {
            for (int i1 = 0; i1 < list.size(); i1++) {
                LocalAbstractObject o1 = list.get(i1);
                for (int i2 = 0; i2 < i1; i2++)
                    addToAnswer(o1, list.get(i2));
                for (int i2 = i1+1; i2 < list.size(); i2++)
                    addToAnswer(o1, list.get(i2));
            }
        }

        return getAnswerCount() - beforeCount;
    }


    //****************** Cloning ******************//
    
    @Override
    @SuppressWarnings("unchecked")
    public JoinQueryOperation clone(boolean preserveAnswer) throws CloneNotSupportedException {
        JoinQueryOperation operation = (JoinQueryOperation)super.clone();

        // Create a new collection for the answer set (without data)
        if (!preserveAnswer)
            operation.answer = (DistanceRankedSortedCollection)operation.answer.clone(false);       // This is checked.

        return operation;
    }


    //****************** Overrides for answer set ******************//

    /**
     * Add a new pair of objects to the answer. The rank of the object is computed automatically
     * as a distance between the left object and the right object. The pair is added if and only if 
     * its distance is less than or equal to the current query threshold <code>mu</code>.
     * 
     * @param leftObject  left object of the pair
     * @param rightObject right object of the pair
     * @return the distance-ranked join object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedJoinObject addToAnswer(LocalAbstractObject leftObject, LocalAbstractObject rightObject) {
        return addToAnswer(leftObject, rightObject, mu);
    }

    /**
     * Add a new pair of objects to the answer. The rank of the pair is computed automatically
     * as a distance between the passed objects.
     * 
     * @param leftObject  left object of the pair
     * @param rightObject right object of the pair
     * @param distThreshold the threshold on distance;
     *      if the computed distance exceeds the threshold (sharply) or the join constraint (threshold),
     *      the pair is not added
     * @return the distance-ranked join object that was added to answer or <tt>null</tt> if the object was not added
     */
    public RankedJoinObject addToAnswer(LocalAbstractObject leftObject, LocalAbstractObject rightObject, float distThreshold) {
        if (leftObject == null || rightObject == null)
            return null;
        return addToAnswer(leftObject, rightObject, leftObject.getDistance(rightObject), distThreshold);
    }

    /**
     * Add a new pair of objects to the answer. The rank of the pair is computed automatically
     * as a distance between the passed objects.
     * 
     * @param leftObject  left object of the pair
     * @param rightObject right object of the pair
     * @param distance distance between the left and right objects (this distance is compared against the passed threshold as well as any internal join query threshold)
     * @param distThreshold the threshold on distance;
     *      if the computed distance exceeds the threshold (sharply) or the join constraint (threshold),
     *      the pair is not added
     * @return the distance-ranked join object that was added to answer or <tt>null</tt> if the object was not added
     */
    public synchronized RankedJoinObject addToAnswer(LocalAbstractObject leftObject, LocalAbstractObject rightObject, float distance, float distThreshold) {
        if (leftObject == null || rightObject == null)
            return null;
        
        if (distance > mu || distance > distThreshold)
            return null;
        
        RankedJoinObject rankedObject;
        try {
            // Create the ranked object encapsulation
            rankedObject = new RankedJoinObject(answerType.update(leftObject), answerType.update(rightObject), distance);
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }

        // Add the encapsulated object to the answer
        if (answer.add(rankedObject))
            return rankedObject;
        else
            return null;
    }

    @Override
    public Class<? extends RankedJoinObject> getAnswerClass() {
        return RankedJoinObject.class;
    }

    @Override
    public int getAnswerCount() {
        return answer.size();
    }

    @Override
    public Iterator<RankedJoinObject> getAnswer() {
        return answer.iterator();
    }

    @Override
    public Iterator<RankedJoinObject> getAnswer(int skip, int count) {
        return answer.iterator(skip, count);
    }

    @Override
    public Iterator<AbstractObject> getAnswerObjects() {
        throw new UnsupportedOperationException("Not supported for join queries.");
    }

    @Override
    public int getSubAnswerCount() {
        return CollectionProviders.getCollectionCount(answer);
    }

    @Override
    public Iterator<? extends RankedJoinObject> getSubAnswer(int index) throws IndexOutOfBoundsException {
        return CollectionProviders.getCollectionIterator(answer, index, getAnswerClass());
    }

    @Override
    public Iterator<? extends RankedJoinObject> getSubAnswer(Object key) {
        return CollectionProviders.getCollectionByKeyIterator(answer, key, getAnswerClass(), false);
    }

    /**
     * Returns an iterator over all objects in the answer that are ranked higher
     * than the <code>minDistance</code> but lower than the <code>maxDistance</code>.
     * @param minDistance the minimal distance of the answer objects to return
     * @param maxDistance the maximal distance of the answer objects to return
     * @return an iterator over the objects in the answer to this query
     */
    public Iterator<RankedJoinObject> getAnswerDistanceRestricted(float minDistance, float maxDistance) {
        return answer.iteratorDistanceRestricted(minDistance, maxDistance);
    }

    /**
     * Returns an iterator over all objects in the answer that are ranked lower
     * than the <code>maxDistance</code>.
     * @param maxDistance the maximal distance of the answer objects to return
     * @return an iterator over the objects in the answer to this query
     */
    public Iterator<RankedJoinObject> getAnswerDistanceRestricted(float maxDistance) {
        return answer.iteratorDistanceRestricted(maxDistance);
    }

    /**
     * Returns the current last ranked object in the answer.
     * @return the current last ranked object in the answer
     * @throws NoSuchElementException if the answer is empty
     */
    public RankedJoinObject getLastAnswer() throws NoSuchElementException {
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
        if (operation instanceof JoinQueryOperation)
            updateFrom((JoinQueryOperation)operation);
        else
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be updated from " + operation.getClass().getSimpleName());
        super.updateFrom(operation);
    }

    /**
     * Update the answer of this operation from a {@link JoinQueryOperation}.
     * @param operation the source operation from which to get the update
     */
    protected void updateFrom(JoinQueryOperation operation) {
        answer.addAll(operation.answer);
    }

    @Override
    public void endOperation(ErrorCode errValue) throws IllegalArgumentException {
        super.endOperation(errValue);
        if (answer instanceof EndOperationListener)
            ((EndOperationListener)answer).onEndOperation(this, errValue);
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
        for (RankedJoinObject obj : answer)
            obj.clearSurplusData();
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
        // The argument obj is always JoinQueryOperation or its descendant, because it has only abstract ancestors
        JoinQueryOperation castObj = (JoinQueryOperation)obj;

        return k == castObj.k && mu == castObj.mu;
    }

    /**
     * Returns a hash code value for the data of this operation.
     * @return a hash code value for the data of this operation
     */
    @Override
    public int dataHashCode() {
        return Float.floatToIntBits(mu) + ((k == Integer.MAX_VALUE) ? 0 : k*31);
    }
}
