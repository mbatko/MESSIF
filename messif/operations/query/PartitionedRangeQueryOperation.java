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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.operations.AbstractOperation;
import messif.operations.RankingQueryOperation;

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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Partitioned range query")
public class PartitionedRangeQueryOperation extends RangeQueryOperation {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** The answer holder */
    protected final Map<Object, RankedSortedCollection> partitionedAnswer;

    /** Current partition list */
    protected RankedSortedCollection currentPartition;

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
        partitionedAnswer = new HashMap<Object, RankedSortedCollection>();
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
                currentPartition = new RankedSortedCollection();
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
    public RankedAbstractObject addToAnswer(LocalAbstractObject object, float distThreshold) {
        RankedAbstractObject addedObject = super.addToAnswer(object, distThreshold);
        if (addedObject != null && currentPartition != null)
            currentPartition.add(addedObject);

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
    public Map<Object, RankedSortedCollection> getAllPartitionsAnswer() {
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
            Map<Object, RankedSortedCollection> sourceAnswer = ((PartitionedRangeQueryOperation)operation).partitionedAnswer;
            for (Map.Entry<Object, RankedSortedCollection> entry : sourceAnswer.entrySet()) {
                RankedSortedCollection actualList = partitionedAnswer.get(entry.getKey());
                if (actualList == null) {
                    actualList = new RankedSortedCollection();
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
        StringBuffer buffer = new StringBuffer("Partitioned range query <").append(getQueryObject()).append(',').append(radius).append("> returned ").append(getAnswerCount()).append(" objects:");
        for (Map.Entry<Object, RankedSortedCollection> entry : partitionedAnswer.entrySet()) {
            buffer.append("\n").append(entry.getKey()).append(": ").append(entry.getValue().size());
        }
        return buffer.toString();
    }

}
