
package messif.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
 *    PartitionedKNNQueryOperation<Integer> query;
 *    for (LocalBucket bucket : (LocalBucket[])null) {
 *        query.setCurrentObject(bucket.getBucketID());
 *        bucket.processQuery(query);
 *    }
 * </pre>
 * 
 * @author David Novak, david.novak@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
@AbstractOperation.OperationName("Partitioned range query")
public class PartitionedKNNQueryOperation extends kNNQueryOperation {
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
     * Creates a new instance of kNNQueryOperation given the query object and radius.
     * @param queryObject the query object
     * @param k the query radius
     */
    @AbstractOperation.OperationConstructor({"Query object", "Query radius"})
    public PartitionedKNNQueryOperation(LocalAbstractObject queryObject, int k) {
        super(queryObject, k);
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
                currentPartition = new SortedCollection<RankedAbstractObject>(k, k, null);
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
    public RankedAbstractObject addToAnswer(LocalAbstractObject queryObject, LocalAbstractObject object, float distThreshold) {
        // Remember the last object in the answer, if the answer is full
        RankedAbstractObject lastObject = isAnswerFull()?getLastAnswer():null;

        // Call the actuall add-to-answer
        RankedAbstractObject addedObject = super.addToAnswer(queryObject, object, distThreshold);

        // If there was an object inserted
        if (addedObject != null) {
            // If the last object was removed from the answer
            if (lastObject != null) {
                // Remove the last object in the current answer from the partition information
                for (Map.Entry<Object, SortedCollection<RankedAbstractObject>> entry : partitionedAnswer.entrySet()) {
                    if (entry.getValue().remove(lastObject)) {
                        if (entry.getValue().isEmpty()) {
                            partitionedAnswer.remove(entry.getKey());
                        }
                        break;
                    }
                }
            }
            
            // Add object to current partition
            if (currentPartition != null)
                currentPartition.add(addedObject);
        }

        return addedObject;
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
        if (operation instanceof PartitionedKNNQueryOperation) {
            Map<Object, SortedCollection<RankedAbstractObject>> sourceAnswer = ((PartitionedKNNQueryOperation)operation).partitionedAnswer;
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
     * This method goes over objects in the partitioned answer and leaves there only those which
     *  are also stored in the answer itself.
     */
    public void consolidatePartitionsWithAnswer() {
        Set<RankedAbstractObject> answerSet = new HashSet<RankedAbstractObject>(getAnswerCount());
        Iterator<RankedAbstractObject> iterator = getAnswer();
        while (iterator.hasNext())
            answerSet.add(iterator.next());
        for (Iterator<Map.Entry<Object, SortedCollection<RankedAbstractObject>>> itt = partitionedAnswer.entrySet().iterator(); itt.hasNext(); ) {            
            Map.Entry<Object, SortedCollection<RankedAbstractObject>> entry = itt.next();            
            for (Iterator<RankedAbstractObject> it = entry.getValue().iterator(); it.hasNext();) {
                if (! answerSet.contains(it.next())) {
                    it.remove();
                }
            }
            if (entry.getValue().isEmpty()) {
                itt.remove();
            }
        }
    }
    
    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("Partitioned kNN query <").append(queryObject).append(',').append(k).append("> returned ").append(getAnswerCount()).append(" objects:");
        for (Map.Entry<Object, SortedCollection<RankedAbstractObject>> entry : partitionedAnswer.entrySet()) {
            buffer.append("\n").append(entry.getKey()).append(": ").append(entry.getValue().size()).append("\t").append(entry.getValue().toString());
        }
        return buffer.toString();
    }

}
