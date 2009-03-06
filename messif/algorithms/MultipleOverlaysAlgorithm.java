
package messif.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import messif.network.NetworkNode;
import messif.objects.util.RankedAbstractMetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.PartitionedKNNQueryOperation;
import messif.utility.SortedCollection;

/**
 * This is a centralized algorithm which connects itself to several remote algorithm (host + RMI port)
 *  and executes operations on all of them.
 *
 * @author xnovak8
 */
public class MultipleOverlaysAlgorithm extends Algorithm {

    /** Multiple remote algorithms. */
    Collection<RMIAlgorithm> overlays = new ArrayList<RMIAlgorithm>();

    /**
     * Constructs the algorithm with empty set of overlays.
     * 
     * @throws java.lang.IllegalArgumentException
     */
    @Algorithm.AlgorithmConstructor(description = "Basic empty constructor", arguments = {})
    public MultipleOverlaysAlgorithm() throws IllegalArgumentException {
        super("Multiple-overlays Algorithm");
    }

    /**
     * Constructs the algorithm for a given list of overlays.
     * 
     * @param overlays array of host+RMI port values to connect to
     * @throws java.lang.IllegalArgumentException
     */
    @Algorithm.AlgorithmConstructor(description = "Constructor with given list of host+port ids of algorithms", arguments = {"list of host+port identifications"})
    public MultipleOverlaysAlgorithm(NetworkNode [] overlays) throws IllegalArgumentException {
        this();

        try {
            for (int i = 0; i < overlays.length; i++) {
                addAlgorithm(overlays[i]);
            }
        } catch (AlgorithmMethodException ex) {
            log.warning(ex);
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Disconnect from all overlays.
     * 
     * @throws java.lang.Throwable if any disconnection goes wrong.
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        for (RMIAlgorithm rMIAlgorithm : overlays) {
            rMIAlgorithm.finalize();
        }
    }


    // ************************************     Overlays management     *********************************** //

    /**
     * Add a new remote algorithm stub to the
     * @param networkNode
     * @throws messif.algorithms.AlgorithmMethodException
     */
    public void addAlgorithm(NetworkNode networkNode) throws AlgorithmMethodException {
        try {
            RMIAlgorithm newAlgorithm = new RMIAlgorithm(networkNode);
            newAlgorithm.connect();
            overlays.add(newAlgorithm);
        } catch (IOException ex) {
            log.warning(ex);
            throw new AlgorithmMethodException(ex);
        }
    }

    /**
     * Remove given algorithm (host+RMI port) from this multiple-overlays algorithm.
     * 
     * @param networkNode host+RMI port to remove
     * @return true if the specified algorithm was managed by this alg, false otherwise
     */
    public boolean removeAlgorithm(NetworkNode networkNode) {
        for (RMIAlgorithm rMIAlgorithm : overlays) {
            if (rMIAlgorithm.getHost().equals(networkNode.getHost()) && (rMIAlgorithm.getPort() == networkNode.getPort())) {
                try {
                    rMIAlgorithm.finalize();
                } catch (Throwable ex) {
                    log.warning(ex);
                }
                overlays.remove(rMIAlgorithm);
                return true;
            }
        }
        return false;
    }

    // *******************************     Operations     *********************************** //

    /**
     * This method processes the operation on each of the remote algorithms (running each of it in a separate thread)
     *   and merges the answers.
     * @param operation operation to execute on each of the overlays
     * @throws messif.algorithms.AlgorithmMethodException
     */
    public void processOperation(AbstractOperation operation) throws AlgorithmMethodException {
        try {
            // execute operation at all remote algorithms (background)
            for (RMIAlgorithm rMIAlgorithm : overlays) {
                rMIAlgorithm.backgroundExecuteOperation(operation);
            }

            // merge PartitionedKNNQueryOperation in a special way
            if (operation instanceof PartitionedKNNQueryOperation) {
                List<PartitionedKNNQueryOperation> overlaysOperations = new ArrayList<PartitionedKNNQueryOperation>();
                for (RMIAlgorithm rMIAlgorithm : overlays) {
                    List<AbstractOperation> returnedOperation = rMIAlgorithm.waitBackgroundExecuteOperation();
                    for (AbstractOperation abstractOperation : returnedOperation) {
                        overlaysOperations.add((PartitionedKNNQueryOperation) abstractOperation);
                    }
                }
                mergePartitionedKNNOperation((PartitionedKNNQueryOperation) operation, overlaysOperations);
                return;
            }

            // wait for the results from all overlays and merge them
            for (RMIAlgorithm rMIAlgorithm : overlays) {
                List<AbstractOperation> returnedOperation = rMIAlgorithm.waitBackgroundExecuteOperation();
                for (AbstractOperation abstractOperation : returnedOperation) {
                    operation.updateFrom(abstractOperation);
                }
            }
        } catch (NoSuchMethodException ex) {
            log.warning(ex);
            throw new AlgorithmMethodException(ex);
        } catch (ClassCastException ex) {
            log.warning(ex);
            throw new AlgorithmMethodException(ex);
        }
    }

    /**
     * Merging of partitioned kNN queries from multiple overlays - merge the "best" partitions from
     *   all overlays, merge the "second best" partitions from all overlays, etc.
     * @param originalOperation
     * @param overlaysOperations
     * @throws AlgorithmMethodException
     */
    protected void mergePartitionedKNNOperation(PartitionedKNNQueryOperation originalOperation, List<PartitionedKNNQueryOperation> overlaysOperations) throws AlgorithmMethodException {
        if (originalOperation.getAnswerCount() > 0) {
            throw new AlgorithmMethodException("The orginal PartitionedKNNQueryOperation operation must be empty when merging partial answer from overlays");
        }

        // sort individual partitioned answer from all overlays
        List<SortedSet<SortedCollection<RankedAbstractObject>>> sortedOverlayAnswers = new ArrayList<SortedSet<SortedCollection<RankedAbstractObject>>>();
        for (PartitionedKNNQueryOperation partitionedKNNQueryOperation : overlaysOperations) {
            SortedSet<SortedCollection<RankedAbstractObject>> sortedAnswer = new TreeSet<SortedCollection<RankedAbstractObject>>(
                    new Comparator<SortedCollection<RankedAbstractObject>> () {
                        @Override
                        public int compare(SortedCollection<RankedAbstractObject> o1, SortedCollection<RankedAbstractObject> o2) {
                            return ((Integer) o2.size()).compareTo(o1.size());
                        }
            });
            for (SortedCollection<RankedAbstractObject> partition : partitionedKNNQueryOperation.getAllPartitionsAnswer().values()) {
                sortedAnswer.add(partition);
            }
            sortedOverlayAnswers.add(sortedAnswer);
        }

        // merge corresponding part-answers from all the overlays
        int mergedPartitionNumber = 1; // this counter identifies the global partition created by merging the N-th partitions from all overlays
        while (! sortedOverlayAnswers.isEmpty()) {
            String mergedPartitionId = "answer_from_buckets_" + mergedPartitionNumber;
            originalOperation.setCurrentPartition(mergedPartitionId);
            for (Iterator<SortedSet<SortedCollection<RankedAbstractObject>>> it = sortedOverlayAnswers.iterator(); it.hasNext(); ) {
                SortedSet<SortedCollection<RankedAbstractObject>> overlaysPartitions = it.next();
                SortedCollection<RankedAbstractObject> partition = overlaysPartitions.first();
                for (RankedAbstractObject rankedAbstractObject : partition) {
                    float [] subdistances = (rankedAbstractObject instanceof RankedAbstractMetaObject) ? (((RankedAbstractMetaObject) rankedAbstractObject).getSubDistances()) : null;
                    originalOperation.addToAnswer(rankedAbstractObject.getObject(), rankedAbstractObject.getDistance(), subdistances);
                }
                overlaysPartitions.remove(partition);
                if (overlaysOperations.isEmpty()) {
                    it.remove();
                }
            }
            mergedPartitionNumber ++;
        }
    }

}
