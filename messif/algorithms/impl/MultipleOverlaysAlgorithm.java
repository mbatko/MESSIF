
package messif.algorithms.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.RMIAlgorithm;
import messif.network.NetworkNode;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.RankedAbstractMetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.query.ApproxKNNQueryOperationMIndex;
import messif.operations.query.PartitionedKNNQueryOperation;
import messif.utility.SortedCollection;

/**
 * This is a centralized algorithm which connects itself to several remote algorithm (host + RMI port)
 *  and executes operations on all of them.
 *
 * @author xnovak8
 */
public class MultipleOverlaysAlgorithm extends Algorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** Multiple remote algorithms. */
    Map<Object, RMIAlgorithm> overlays = new HashMap<Object, RMIAlgorithm>();

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
    //@Algorithm.AlgorithmConstructor(description = "Constructor with given list of host+port ids of algorithms", arguments = {"list of host+port identifications"})
    public MultipleOverlaysAlgorithm(NetworkNode [] overlays) throws IllegalArgumentException {
        this();

        try {
            for (int i = 0; i < overlays.length; i++) {
                addAlgorithm(overlays[i]);
            }
        } catch (AlgorithmMethodException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new IllegalArgumentException(e);
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
        for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
            rMIAlgorithm.finalize();
        }
    }


    // ************************************     Overlays management     *********************************** //

    /**
     * Add a new remote algorithm stub to the list of overlays - generate a random new identifier
     * @param networkNode
     * @throws messif.algorithms.AlgorithmMethodException
     */
    public void addAlgorithm(NetworkNode networkNode) throws AlgorithmMethodException {
        addAlgorithm(String.valueOf(System.currentTimeMillis()), networkNode);
    }
    /**
     * Add a new remote algorithm stub to the
     * @param identifier overlay identifier
     * @param networkNode
     * @throws messif.algorithms.AlgorithmMethodException
     */
    public void addAlgorithm(String identifier, NetworkNode networkNode) throws AlgorithmMethodException {
        try {
            log.info("to add: " + identifier + ": " + networkNode);
            RMIAlgorithm newAlgorithm = new RMIAlgorithm(networkNode);
            log.info("adding: " + identifier + ": " + networkNode);
            newAlgorithm.connect();
            log.info("... connected");
            overlays.put(identifier, newAlgorithm);
        } catch (IOException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new AlgorithmMethodException(e);
        }
    }

    /**
     * Remove given algorithm (host+RMI port) from this multiple-overlays algorithm.
     * 
     * @param networkNode host+RMI port to remove
     * @return true if the specified algorithm was managed by this alg, false otherwise
     */
    public boolean removeAlgorithm(NetworkNode networkNode) {
        for (Map.Entry<Object, RMIAlgorithm> pair : overlays.entrySet()) {
            if (pair.getValue().getHost().equals(networkNode.getHost()) && (pair.getValue().getPort() == networkNode.getPort())) {
                try {
                    pair.getValue().finalize();
                } catch (Throwable e) {
                    log.log(Level.WARNING, e.getClass().toString(), e);
                }
                overlays.remove(pair.getKey());
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
            if ((operation.suppData != null) && (processOperationSingleLayer(operation))) {
                return;
            }
            
            // execute operation at all remote algorithms (background)
            for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
                rMIAlgorithm.backgroundExecuteOperation(operation);
            }

            // merge PartitionedKNNQueryOperation in a special way
            if (operation instanceof PartitionedKNNQueryOperation) {
                List<PartitionedKNNQueryOperation> overlaysOperations = new ArrayList<PartitionedKNNQueryOperation>();
                for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
                    List<AbstractOperation> returnedOperation = rMIAlgorithm.waitBackgroundExecuteOperation();
                    for (AbstractOperation abstractOperation : returnedOperation) {
                        overlaysOperations.add((PartitionedKNNQueryOperation) abstractOperation);
                    }
                }
                mergePartitionedKNNOperation((PartitionedKNNQueryOperation) operation, overlaysOperations);
                return;
            }

            // wait for the results from all overlays and merge them
            for (RMIAlgorithm rMIAlgorithm : overlays.values()) {
                List<AbstractOperation> returnedOperation = rMIAlgorithm.waitBackgroundExecuteOperation();
                for (AbstractOperation abstractOperation : returnedOperation) {
                    operation.updateFrom(abstractOperation);
                }
            }
        } catch (NoSuchMethodException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new AlgorithmMethodException(e);
        } catch (ClassCastException e) {
            log.log(Level.WARNING, e.getClass().toString(), e);
            throw new AlgorithmMethodException(e);
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

    /**
     * Process operation on a single layer.
     * This is a HACK method...
     * @param operation the operation to process
     * @return <tt>true</tt> only if the operation is approximated KNN on MIndex and the returned value is something...
     */
    private boolean processOperationSingleLayer(AbstractOperation operation) {
        if (! (operation instanceof ApproxKNNQueryOperationMIndex)) {
            return false;
        }
        ApproxKNNQueryOperationMIndex approxOperation = (ApproxKNNQueryOperationMIndex) operation;
        for (Map.Entry<Object, RMIAlgorithm> pair : this.overlays.entrySet()) {
            if (pair.getKey().equals(operation.suppData)) {
                try {
                    Class<? extends LocalAbstractObject> objectClass = pair.getValue().getObjectClass();
                    LocalAbstractObject queryObject = objectClass.getConstructor(MetaObject.class).newInstance((MetaObject) approxOperation.getQueryObject());

                    ApproxKNNQueryOperationMIndex newOperation = new ApproxKNNQueryOperationMIndex(queryObject, approxOperation);
                    log.info("processing operation: " + newOperation.toString());
                    newOperation = pair.getValue().executeOperation(newOperation);
                    operation.updateFrom(newOperation);

                    log.info("stats: " + pair.getValue().getOperationStatistics().printStatistics());

                    return true;
                } catch (Exception ex) {
                    Logger.getLogger(MultipleOverlaysAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return false;
    }

}
