/*
 * RangeQuery.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.MeasuredAbstractObjectList;

/**
 * This class represents a range query that distinguish the partition
 * from which a matching object comes from. It is fully controlled by
 * the algorithm that implements this operation - before it starts to
 * search a partition, it should set the partition differentiation object
 * using the {@link #setCurrentPartition setCurrentPartition} method.
 * 
 * For example:
 * <pre>
 *    PartitionedRangeQueryOperation<Integer> query;
 *    for (LocalBucket bucket : (LocalBucket[])null) {
 *        query.setCurrentObject(bucket.getBucketID());
 *        bucket.processQuery(query);
 *    }
 * </pre>
 * 
 * @author David Novak, david.novak@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Partitioned range query")
public class PartitionedRangeQueryOperation extends RangeQueryOperation {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;


    /****************** Query answer attributes ******************/

    /** The answer holder */
    protected final Map<Object, MeasuredAbstractObjectList<AbstractObject>> partitionedAnswer = 
            new HashMap<Object, MeasuredAbstractObjectList<AbstractObject>>();

    /** The locking flag for {@link #setCurrentPartition setCurrentPartition} */
    protected boolean isPartitionLocked = false;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of RangeQueryOperation given the query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public PartitionedRangeQueryOperation(LocalAbstractObject queryObject, float radius) {
        super(queryObject, radius);
        partitionedAnswer.put(null, answer);
    }
    

    /****************** Attribute access methods ******************/

    /**
     * Sets the current partition differentiation object.
     * @param currentPartition the object to differentiate the answer partition
     */
    public void setCurrentPartition(Object currentPartition) {
        if (isPartitionLocked)
            return;
        answer = partitionedAnswer.get(currentPartition);
        if (answer == null) {
            answer = new MeasuredAbstractObjectList<AbstractObject>();
            partitionedAnswer.put(currentPartition, answer);
        }
    }

    /**
     * Set lock for {@link #setCurrentPartition setCurrentPartition}.
     * If set to <tt>true</tt>, all calls to {@link #setCurrentPartition setCurrentPartition}
     * will be ignored until it is unlocked again.
     * 
     * @param isPartitionLocked pass <tt>true</tt> for enabling the lock or <tt>false</tt> to disable it
     */
    public void setCurrentPartitionLock(boolean isPartitionLocked) {
        this.isPartitionLocked = isPartitionLocked;
    }


    /****************** Answer methods ******************/
    

    /**
     * Returns the number of answered objects.
     * @return the number of answered objects
     */
    @Override
    public int getAnswerCount() { 
        int retVal = 0;
        for (MeasuredAbstractObjectList<AbstractObject> part : partitionedAnswer.values())
            retVal += part.size();
        return retVal;
    }
    
    /**
     * Returns an iterator over all objects in the answer to this query.
     * @return an iterator over all objects in the answer to this query
     */
    @Override
    public Iterator<AbstractObject> getAnswer() { 
        MeasuredAbstractObjectList<AbstractObject> totalAnswer = new MeasuredAbstractObjectList<AbstractObject>();
        for (MeasuredAbstractObjectList<AbstractObject> part : partitionedAnswer.values())
            totalAnswer.add(part);
        return totalAnswer.objects();
    }
    
    /**
     * Returns an iterator over pairs of objects and their distances from the query object of this query. 
     * The object of a pair is accessible through getObject().
     * The associated distance of a pair is accessible through getDistance().
     * @return an iterator over pairs of objects and their distances from the query object of this query
     */
    @Override
    public Iterator<MeasuredAbstractObjectList.Pair<AbstractObject>> getAnswerDistances() {
        MeasuredAbstractObjectList<AbstractObject> totalAnswer = new MeasuredAbstractObjectList<AbstractObject>();
        for (MeasuredAbstractObjectList<AbstractObject> part : partitionedAnswer.values())
            totalAnswer.add(part);
        return totalAnswer.iterator();
    }

    /**
     * Returns the partial answer for the specified partition.
     * @param partitionIdentifier the idetifier to select a particular partition
     * @return the partial answer for the specified partition
     */
    public MeasuredAbstractObjectList<AbstractObject> getPartitionAnswer(Object partitionIdentifier) {
        return partitionedAnswer.get(partitionIdentifier);
    }

    /**
     * Returns the whole answer divided by partitions.
     * @return the whole answer divided by partitions
     */
    public Map<Object, MeasuredAbstractObjectList<AbstractObject>> getAllPartitionsAnswer() {
        return Collections.unmodifiableMap(partitionedAnswer);
    }

    /**
     * Update all answer data of this operation from the another operation.
     *
     * @param operation the operation to update answer from
     */
    @Override
    protected void updateAnswer(QueryOperation operation) {
        if (operation instanceof PartitionedRangeQueryOperation) {
            Map<Object, MeasuredAbstractObjectList<AbstractObject>> sourceAnswer = ((PartitionedRangeQueryOperation)operation).partitionedAnswer;
            for (Map.Entry<Object, MeasuredAbstractObjectList<AbstractObject>> entry : sourceAnswer.entrySet()) {
                MeasuredAbstractObjectList<AbstractObject> actualList = partitionedAnswer.get(entry.getKey());
                if (actualList == null) {
                    actualList = new MeasuredAbstractObjectList<AbstractObject>();
                    partitionedAnswer.put(entry.getKey(), actualList);
                }
                actualList.add(entry.getValue());
            }
        } else super.updateAnswer(operation);
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("Partitioned range query <").append(queryObject).append(',').append(radius).append("> returned ").append(getAnswerCount()).append(" objects:");
        for (Map.Entry<Object, MeasuredAbstractObjectList<AbstractObject>> entry : partitionedAnswer.entrySet()) {
            buffer.append("\n").append(entry.getKey()).append(": ").append(entry.getValue().size());
        }
        return buffer.toString();
    }

}
