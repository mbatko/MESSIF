/*
 * RangeQuery.java
 *
 * Created on 6. kveten 2004, 17:31
 */

package messif.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.utility.SortedCollection;

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

    //****************** Attributes ******************//

    /** The answer holder */
    protected final Map<Object, SortedCollection<RankedAbstractObject>> partitionedAnswer; 

    /** Current partition list */
    protected SortedCollection<RankedAbstractObject> currentPartition;

    /** The locking flag for {@link #setCurrentPartition setCurrentPartition} */
    protected boolean isPartitionLocked = false;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of RangeQueryOperation given the query object and radius.
     * @param queryObject the query object
     * @param radius the query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public PartitionedRangeQueryOperation(LocalAbstractObject queryObject, float radius) {
        super(queryObject, radius);
        partitionedAnswer = new HashMap<Object, SortedCollection<RankedAbstractObject>>();
    }


    //****************** Attribute access methods ******************//

    /**
     * Sets the current partition differentiation object.
     * @param partitionObject the object to differentiate the answer partition
     */
    public void setCurrentPartition(Object partitionObject) {
        if (isPartitionLocked)
            return;
        if (partitionObject == null) {
            currentPartition = null;
        } else {
            currentPartition = partitionedAnswer.get(partitionObject);
            if (currentPartition == null) {
                currentPartition = new SortedCollection<RankedAbstractObject>();
                partitionedAnswer.put(partitionObject, currentPartition);
            }
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


    //****************** Answer methods ******************//

    @Override
    public boolean addToAnswer(AbstractObject object, float distance) {
        if (!super.addToAnswer(object, distance))
            return false;
        try { // FIXME: the RankedAbstractObject is created twice (encap is needed)
            if (currentPartition != null)
                currentPartition.add(new RankedAbstractObject(answerType.update(object), distance));
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
        return true;
    }

    /**
     * Returns the partial answer for the specified partition.
     * @param partitionIdentifier the idetifier to select a particular partition
     * @return the partial answer for the specified partition
     */
    public Iterator<RankedAbstractObject> getPartitionAnswer(Object partitionIdentifier) {
        return partitionedAnswer.get(partitionIdentifier).iterator();
    }

    /**
     * Returns the whole answer divided by partitions.
     * @return the whole answer divided by partitions
     */
    public Map<Object, SortedCollection<RankedAbstractObject>> getAllPartitionsAnswer() {
        return Collections.unmodifiableMap(partitionedAnswer);
    }

    /**
     * Update all answer data of this operation from the another operation.
     *
     * @param operation the operation to update answer from
     */
    @Override
    protected void updateFrom(RankingQueryOperation operation) {
        super.updateFrom(operation);
        if (operation instanceof PartitionedRangeQueryOperation) {
            Map<Object, SortedCollection<RankedAbstractObject>> sourceAnswer = ((PartitionedRangeQueryOperation)operation).partitionedAnswer;
            for (Map.Entry<Object, SortedCollection<RankedAbstractObject>> entry : sourceAnswer.entrySet()) {
                SortedCollection<RankedAbstractObject> actualList = partitionedAnswer.get(entry.getKey());
                if (actualList == null) {
                    actualList = new SortedCollection<RankedAbstractObject>();
                    partitionedAnswer.put(entry.getKey(), actualList);
                }
                actualList.addAll(entry.getValue());
            }
        }
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("Partitioned range query <").append(queryObject).append(',').append(radius).append("> returned ").append(getAnswerCount()).append(" objects:");
        for (Map.Entry<Object, SortedCollection<RankedAbstractObject>> entry : partitionedAnswer.entrySet()) {
            buffer.append("\n").append(entry.getKey()).append(": ").append(entry.getValue().size());
        }
        return buffer.toString();
    }

}
