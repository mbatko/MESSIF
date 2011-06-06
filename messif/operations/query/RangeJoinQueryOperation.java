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

import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.RMIAlgorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingQueryOperation;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Similarity join query operation evaluated using range queries on an external index.
 * 
 * It works as documented at {@link #evaluateSerially(messif.objects.util.AbstractObjectIterator) }.
 * 
 * See {@link JoinQueryOperation} for details.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("similairity join query by range search")
public class RangeJoinQueryOperation extends JoinQueryOperation {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//
    
    /** Algorithm for evaluating "range" queries */
    private Algorithm algorithm;
    
    /** Number of queries to run in parallel over a passed algorithm */
    private int parallelQueries;
    
    /** Cosntructor for the operation to execute on the remote algorithm */
    private final ConstructorInstantiator<RankingQueryOperation> operConstructor;
    
    /** Paramaters to instantiate the operation {@link #operConstructor} */
    private final Object[] operParams;

    //****************** Constructors ******************//

    /**
     * Creates an instance of range join query.
     * 
     * @param mu the distance threshold
     * @param k the number of nearest pairs to retrieve
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     * @param answerType the type of objects this operation stores in pairs in its answer
     * @param host the remote algorithm's IP address
     * @param port the remote algorithm's RMI port
     * @param parallelQueries number of range queries to run in parallel (pass <tt>1</tt> to execute queries serially)
     * @param queryCls name of class of {@link RankingQueryOperation} to execute on the algorithm at host:port;
     *                 so {@link RangeQueryOperation} or {@link KNNQueryOperation} can be passed
     * @param queryParams parameters of a constructor of queryCls but the first one, which is a query object
     * @throws UnknownHostException thrown during the construction of {@link RMIAlgorithm}
     * @throws NoSuchMethodException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws IllegalArgumentException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws InvocationTargetException thrown during the construction of the passed class of {@link RankingQueryOperation}
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Number of nearest pairs", "Skip symmetric pairs", "Answer type",
                                             "Hostname of remote algorithm", "Port number of remote algorithm", "Number of range queries to run in parallel",
                                             "Class name of query to execute on remote algorithm", "Parameters of the query class..."})
    public RangeJoinQueryOperation(float mu, int k, boolean skipSymmetricPairs, AnswerType answerType,
                                   String host, int port, int parallelQueries,
                                   Class<RankingQueryOperation> queryCls, String... queryParams) throws UnknownHostException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        super(mu, k, skipSymmetricPairs, answerType);
        
        algorithm = new RMIAlgorithm(host, port);
        this.parallelQueries = parallelQueries;

        // Prepare parameters first, since they will be converted from Strings to correct type by ConstructorInstantiator below.
        operParams = new Object[queryParams.length+1];
        System.arraycopy(queryParams, 0, operParams, 1, queryParams.length);
        try {
            operConstructor = new ConstructorInstantiator<RankingQueryOperation>(queryCls, true, null, operParams);
        } catch (NoSuchInstantiatorException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed class " + queryCls.getName() + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates an instance of range join query.
     * No algorithm to evaluateSerially range queries is given, so it must be specified within 
     * 
     * @param mu the distance threshold
     * @param k the number of nearest pairs to retrieve
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     * @param answerType the type of objects this operation stores in pairs in its answer
     * @param parallelQueries number of range queries to run in parallel (pass <tt>1</tt> to execute queries serially)
     * @param queryCls name of class of {@link RankingQueryOperation} to execute on the algorithm at host:port;
     *                 so {@link RangeQueryOperation} or {@link KNNQueryOperation} can be passed
     * @param queryParams parameters of a constructor of queryCls but the first one, which is a query object
     * @throws NoSuchMethodException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws IllegalArgumentException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws InvocationTargetException thrown during the construction of the passed class of {@link RankingQueryOperation}
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Number of nearest pairs", "Skip symmetric pairs", "Answer type",
                                             "Class name of query to execute on remote algorithm", "Parameters of the query class..."})
    public RangeJoinQueryOperation(float mu, int k, boolean skipSymmetricPairs, AnswerType answerType,
                                   int parallelQueries, 
                                   Class<RankingQueryOperation> queryCls, String... queryParams) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        this(mu, k, skipSymmetricPairs, answerType, null, parallelQueries, queryCls, queryParams);
    }

    /**
     * Creates an instance of range join query.
     * 
     * @param mu the distance threshold
     * @param k the number of nearest pairs to retrieve
     * @param skipSymmetricPairs flag whether symmetric pairs should be avoided in the answer
     * @param answerType the type of objects this operation stores in pairs in its answer
     * @param alg algorithm to evaluateSerially range queries
     * @param parallelQueries number of range queries to run in parallel (pass <tt>1</tt> to execute queries serially)
     * @param queryCls name of class of {@link RankingQueryOperation} to execute on the algorithm at host:port;
     *                 so {@link RangeQueryOperation} or {@link KNNQueryOperation} can be passed
     * @param queryParams parameters of a constructor of queryCls but the first one, which is a query object
     * @throws NoSuchMethodException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws IllegalArgumentException thrown during the construction of the passed class of {@link RankingQueryOperation}
     * @throws InvocationTargetException thrown during the construction of the passed class of {@link RankingQueryOperation}
     */
    @AbstractOperation.OperationConstructor({"Distance threshold", "Number of nearest pairs", "Skip symmetric pairs", "Answer type",
                                             "Instance of algorithm to run range queries on", 
                                             "Class name of query to execute on remote algorithm", "Parameters of the query class..."})
    public RangeJoinQueryOperation(float mu, int k, boolean skipSymmetricPairs, AnswerType answerType,
                                   Algorithm alg, int parallelQueries,
                                   Class<RankingQueryOperation> queryCls, String... queryParams) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        super(mu, k, skipSymmetricPairs, answerType);
        
        algorithm = null;
        this.parallelQueries = parallelQueries;
        
        // Prepare parameters first, since they will be converted from Strings to correct type by ConstructorInstantiator below.
        operParams = new Object[queryParams.length+1];
        System.arraycopy(queryParams, 0, operParams, 1, queryParams.length);
        try {
            operConstructor = new ConstructorInstantiator<RankingQueryOperation>(queryCls, true, null, operParams);
        } catch (NoSuchInstantiatorException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed class " + queryCls.getName() + ": " + ex.getMessage(), ex);
        }
    }

    
    //****************** Implementation of query evaluation ******************//

    /**
     * Evaluate this join query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * 
     * For each object in the passed iterator, a range query is evaluated on the external index given in a constructor.
     * The reange query is instantiated from the parameters passed in the constructor.
     *
     * @param objects the collection of objects on which to evaluateSerially this query
     * @return number of objects satisfying the query
     */
    @Override
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects) {
        return evaluate(objects, algorithm);
    }
    
    /**
     * Evaluate this join query on a given set of objects and algorithm to run on.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * 
     * For each object in the passed iterator, a range query is evaluated on the external index given in a constructor.
     * The range query is instantiated from the parameters passed in the constructor.
     *
     * @param objects the collection of objects on which to evaluateSerially this query
     * @return number of objects satisfying the query
     */
    public int evaluate(AbstractObjectIterator<? extends LocalAbstractObject> objects, Algorithm alg) {
        if (parallelQueries <= 1)
            return evaluateSerially(objects, alg);
        else
            return evaluateInParallel(objects, alg, parallelQueries);
    }

    /**
     * Add the answer of a range query to this join operation
     * @param q      query object used in the range query
     * @param answer answer of the range query
     */
    private void processQueryAnswer(LocalAbstractObject q, Iterator<RankedAbstractObject> answer) {
        // Process the taks response
        while (answer.hasNext()) {
            RankedAbstractObject ranked = answer.next();
            float dist = ranked.getDistance();
            LocalAbstractObject obj = (LocalAbstractObject)ranked.getObject();
            if (dist == 0f && q.dataEquals(obj))
                continue;
            addToAnswer(q, obj, dist, getDistanceThreshold());
        }
    }
    
    /**
     * Evaluate this join query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * 
     * For each object in the passed iterator, a range query is evaluated on the external index given in a constructor.
     * The reange query is instantiated from the parameters passed in the constructor.
     *
     * @param objects the collection of objects on which to evaluateSerially this query
     * @param alg algorithm to evaluateSerially range queries
     * @return number of objects satisfying the query
     */
    public int evaluateSerially(AbstractObjectIterator<? extends LocalAbstractObject> objects, Algorithm alg) {
        int beforeCount = getAnswerCount();
        
        try {
            while (objects.hasNext()) {
                LocalAbstractObject q = objects.next();
                
                // Run the query
                operParams[0] = q;
                RankingQueryOperation op = operConstructor.instantiate(operParams);
                op = alg.executeOperation(op);
                
                processQueryAnswer(q, op.getAnswer());
            }
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed RankingQueryOperation! " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed RankingQueryOperation! " + ex.getMessage(), ex);
        } catch (AlgorithmMethodException ex) {
            throw new RuntimeException("RangeJoin: Cannot run the passed RankingQueryOperation on the remote algorithm!", ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("RangeJoin: Cannot run the passed RankingQueryOperation on the remote algorithm!", ex);
        }

        return getAnswerCount() - beforeCount;
    }
    
    /**
     * Evaluate this join query on a given set of objects.
     * The objects found by this evaluation are added to answer of this query via {@link #addToAnswer}.
     * 
     * For each object in the passed iterator, a range query is evaluated on the external index given in a constructor.
     * The reange query is instantiated from the parameters passed in the constructor.
     *
     * @param objects the collection of objects on which to evaluateSerially this query
     * @param alg algorithm to evaluateSerially range queries
     * @param threads number of range queries to run in parallel
     * @return number of objects satisfying the query
     */
    public int evaluateInParallel(AbstractObjectIterator<? extends LocalAbstractObject> objects, Algorithm alg, int threads) {
        int beforeCount = getAnswerCount();
        // Prepare the thread pool executor
        ThreadPoolExecutor pool = new MyThreadPool(this, alg, threads);
        
        try {
            while (objects.hasNext()) {
                LocalAbstractObject q = objects.next();
                
                // Prepare the query
                operParams[0] = q;
                RankingQueryOperation op = operConstructor.instantiate(operParams);

                pool.execute(new MyTask(alg, q, op));
            }
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed RankingQueryOperation! " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("RangeJoin: Cannot instantiate the passed RankingQueryOperation! " + ex.getMessage(), ex);
        }
        
        // Shutdown the pool
        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(RangeJoinQueryOperation.class.getName()).log(Level.SEVERE, null, ex);
        }

        return getAnswerCount() - beforeCount;
    }
    
    private static class MyTask implements Runnable {
        private Algorithm alg;
        private final LocalAbstractObject query;
        private RankingQueryOperation oper;

        public MyTask(Algorithm alg, LocalAbstractObject q, RankingQueryOperation oper) {
            this.alg = alg;
            this.query = q;
            this.oper = oper;
        }
        
        @Override
        public void run() {
            try {
                oper = alg.executeOperation(oper);
            } catch (AlgorithmMethodException ex) {
                throw new RuntimeException("RangeJoin: Cannot run the passed RankingQueryOperation on the remote algorithm!", ex);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("RangeJoin: Cannot run the passed RankingQueryOperation on the remote algorithm!", ex);
            }
        }
        
        public LocalAbstractObject getQuery() {
            return query;
        }

        public Iterator<RankedAbstractObject> getAnswer() {
            return oper.getAnswer();
        }

        public void setAlgorithm(Algorithm alg) {
            this.alg = alg;
        }

        public boolean isRMIAlgorithm() {
            return (alg instanceof RMIAlgorithm);
        }
        
    }        
    
    private static class MyThreadPool extends ThreadPoolExecutor implements RejectedExecutionHandler {
        private final RangeJoinQueryOperation joinOper;
        private Algorithm algorithm;

        /**
         * Constructor.
         * @param join    join operation for processing range query results
         * @param alg     algorithm to evaluate the range queries
         * @param threads number of threads to execute
         */
        public MyThreadPool(RangeJoinQueryOperation join, Algorithm alg, int threads) {

            // Sets the pool to create a queue of 3 times longer than the number of threads
            super(threads, threads, 120, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(3*threads), getThreadFactory(alg));
            setRejectedExecutionHandler(new MyCallerRunsPolicy(join));

            // Sets the pool to execute tasks directly (without queueing) and rejected taks 
            // (due to all threads being active) are resubmitted later
//            super(threads, threads, 120, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
//            setRejectedExecutionHandler(this);

            joinOper = join;
            algorithm = alg;
            prestartAllCoreThreads();
        }        
        
        private static ThreadFactory getThreadFactory(Algorithm alg) {
            if (alg instanceof RMIAlgorithm)
                return new MyThreadFactory((RMIAlgorithm)alg);
            else
                return Executors.defaultThreadFactory();
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (t instanceof MyThread && r instanceof MyTask) {
                // Set the algorithm to an internal and clonned RMIAlgorithm
                ((MyTask)r).setAlgorithm(((MyThread)t).getAlgorithm());
            }
        }
        
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            if (!(r instanceof MyTask))
                return;
            MyTask task = (MyTask)r;
            
            // Process the taks response
            joinOper.processQueryAnswer(task.getQuery(), task.getAnswer());
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // Wait until a thread finishes and reexecute the task.
//            System.out.print("Max Threads: " + getMaximumPoolSize() + ", Threads: " + getActiveCount());
            while (getActiveCount() >= getMaximumPoolSize() && !executor.isShutdown()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    // Ignore it
                }
//                System.out.print(", " + getActiveCount());
            }
//            System.out.println();
            if (!executor.isShutdown())
                execute(r);
        }

        private static class MyThreadFactory implements ThreadFactory {
            private final RMIAlgorithm algorithm;

            public MyThreadFactory(RMIAlgorithm alg) {
                algorithm = alg;
            }

            @Override
            public Thread newThread(Runnable r) {
                try {
                    return new MyThread(r, (RMIAlgorithm)algorithm.clone());
                } catch (CloneNotSupportedException ex) {
                    throw new RuntimeException("Cloning RMIAlgorithm failed.", ex);
                }
            }
        }
        
        private static class MyThread extends Thread {
            private final RMIAlgorithm privAlg;

            public MyThread(Runnable target, RMIAlgorithm alg) {
                super(target);
                privAlg = alg;
            }

            public RMIAlgorithm getAlgorithm() {
                return privAlg;
            }
        }
        
        private static class MyCallerRunsPolicy implements RejectedExecutionHandler {
            private final RangeJoinQueryOperation joinOper;

            public MyCallerRunsPolicy(RangeJoinQueryOperation joinOper) {
                this.joinOper = joinOper;
            }

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                if (!e.isShutdown()) {
                    MyTask task = (MyTask)r;
                    task.run();
                    joinOper.processQueryAnswer(task.getQuery(), task.getAnswer());
                }
            }
            
        }
    }
}
