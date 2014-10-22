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
package messif.algorithms.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.algorithms.Algorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.QueryOperation;
import messif.operations.query.GetAlgorithmInfoOperation;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.query.BatchKNNQueryOperation;
import messif.utility.Convert;

/**
 * Implementation of the naive sequential scan algorithm over a given file of objects.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class FileSequentialScan extends Algorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Number of objects read from the data file at once for special query processing. */
    private static final int DATA_BATCH_SIZE = 1000;

    /** One instance of bucket where all objects are stored */
    protected final String file;
    protected final String clazz;
    protected final int nThreads;

    /**
     * Creates a new instance of SequantialScan access structure with the given bucket and filtering pivots.
     *
     * @param file file with text representation of objects
     * @param clazz class of the data objects in the file
     */
    @Algorithm.AlgorithmConstructor(description = "FileSequentialScan", arguments = {"data file name", "data class"})
    public FileSequentialScan(String file, String clazz, int nThreads) {
        super("SequentialScan");
        
        // Create an empty bucket (using the provided bucket class and parameters)
        this.file = file;
        this.clazz = clazz;
        this.nThreads = nThreads;
        setOperationsThreadPool(Executors.newCachedThreadPool());
    }


    //******* ALGORITHM INFO OPERATION *************************************//

    /**
     * Method for processing {@link GetAlgorithmInfoOperation}.
     * The processing will fill the algorithm info with this
     * algorithm {@link #toString() toString()} value.
     * @param operation the operation to process
     */
    public void algorithmInfo(GetAlgorithmInfoOperation operation) {
        operation.addToAnswer(toString());
        operation.endOperation();
    }
        

    //******* SEARCH ALGORITHMS ************************************//

    /**
     * Evaluates a ranking single query object operation on this algorithm.
     * Note that the operation is evaluated sequentially on all objects of this algorithm.
     * @param operation the operation to evaluate
     */
    public void singleQueryObjectSearch(RankingSingleQueryOperation operation) throws IOException {
        
        try (StreamGenericAbstractObjectIterator<LocalAbstractObject> it = new StreamGenericAbstractObjectIterator<>(
                Convert.getClassForName(clazz, LocalAbstractObject.class), file)) {
            operation.evaluate(it);
            operation.endOperation();
        } catch (IllegalArgumentException | IOException | ClassNotFoundException ex) {
            Logger.getLogger(FileSequentialScan.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Processes the batch k-NN operation; the batch is split into sub-batches which are processed 
     *  in parallel. A separate thread reads the data from the data file.
     * @param operation batch operation to be processed
     * @throws IOException if the data file cannot be read
     * @throws ClassNotFoundException if the specified data class is not valid
     */
    public void batchQuerySearch(BatchKNNQueryOperation operation) throws IOException, ClassNotFoundException {
        List<Future> futures = new ArrayList<>();

        // list of all data queues used by individual query batches
        List<BlockingQueue<List<LocalAbstractObject>>> queryDataQueues = new ArrayList<>(nThreads);
        int queryBatchSize = (int) Math.ceil((float) (operation.getNOperations()) / (float) nThreads);
        int queryCounter = 0;
        
        // iterate over all sub-operations from the batch
        while (queryCounter < operation.getNOperations()) {
            List<QueryOperation> operationBatch = new ArrayList<>(queryBatchSize);
            BlockingQueue<List<LocalAbstractObject>> dataBatch = new ArrayBlockingQueue<>(3);
            queryDataQueues.add(dataBatch);
            do {
                operationBatch.add(operation.getOperation(queryCounter));
            } while ((++ queryCounter % queryBatchSize) != 0 && queryCounter < operation.getNOperations());
            
            // start thread that will process a sub-batch of KNN operations
            futures.add(getOperationsThreadPool().submit(new EvaluationThread(operationBatch, dataBatch)));
        }
        
        futures.add(getOperationsThreadPool().submit(new DataReaderThread(queryDataQueues)));
        
        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(FileSequentialScan.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        operation.endOperation();
    }   
    
    /**
     * Thread in which a sub-batch of query operations is processed. 
     */
    private class EvaluationThread extends Thread {

        private final BlockingQueue<List<LocalAbstractObject>> dataBatches;
        private final List<QueryOperation> operationBatch;
        
        /**
         * Creates a new query-batch evaluation thread.
         * @param operationBatch batch of operations to be processed within this thread
         * @param dataBatches a queue of data batches filled from the outside and consumed by this thread
         */
        public EvaluationThread(List<QueryOperation> operationBatch, BlockingQueue<List<LocalAbstractObject>> dataBatches) {
            this.dataBatches = dataBatches;
            this.operationBatch = operationBatch;
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    List<LocalAbstractObject> currentBatch = dataBatches.take();
                    if (currentBatch.isEmpty()) {
                        return;
                    }
                    for (QueryOperation operation : operationBatch) {
                        operation.evaluate(AbstractObjectIterator.getIterator(currentBatch.iterator()));
                    }
                } catch (InterruptedException ex) {    }
            }
        }
    }
    
    /**
     * Thread that reads the data from the data file and sends this data to the operation processing threads.
     */
    private class DataReaderThread extends Thread {

        private final List<BlockingQueue<List<LocalAbstractObject>>> queryQueues;
        
        /**
         * New data reading thread.
         * @param queryQueues list of all shared queues into which this data reader should send the data.
         */
        public DataReaderThread(List<BlockingQueue<List<LocalAbstractObject>>> queryQueues) {
            this.queryQueues = queryQueues;
        }
        
        @Override
        public void run() {
            try (StreamGenericAbstractObjectIterator<LocalAbstractObject> it = new StreamGenericAbstractObjectIterator<>(
                    Convert.getClassForName(clazz, LocalAbstractObject.class), file)) {
                while (it.hasNext()) {
                    List<LocalAbstractObject> dataBatch = new ArrayList<>(DATA_BATCH_SIZE);
                    int counter = 0;
                    while (it.hasNext() && counter ++ < DATA_BATCH_SIZE) {
                        dataBatch.add(it.next());
                    }
    
                    for (BlockingQueue<List<LocalAbstractObject>> queryQueue : queryQueues) {
                        try {
                            queryQueue.put(dataBatch);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(FileSequentialScan.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                for (BlockingQueue<List<LocalAbstractObject>> queryQueue : queryQueues) {
                    try {
                        queryQueue.put(Collections.EMPTY_LIST);
                    } catch (InterruptedException ex) {   }
                }
                
            } catch (IllegalArgumentException | IOException | ClassNotFoundException ex) {
                Logger.getLogger(FileSequentialScan.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }
    }
    
    /**
     * Converts the object to a string representation
     * @return String representation of this algorithm
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer("FileSequentialScan");
        return rtv.toString();
    }
}
