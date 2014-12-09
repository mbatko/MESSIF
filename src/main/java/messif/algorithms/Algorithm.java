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
package messif.algorithms;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import static messif.algorithms.NavigationProcessors.getNavigationProcessor;
import messif.executor.MethodClassExecutor;
import messif.executor.MethodExecutor;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingQueryOperation;
import messif.operations.query.GetObjectCountOperation;
import messif.statistics.FutureWithStatistics;
import messif.statistics.FutureWithStatisticsImpl;
import messif.statistics.OperationStatistics;
import messif.statistics.StatisticCounter;
import messif.statistics.StatisticObject;
import messif.statistics.StatisticTimer;
import messif.statistics.Statistics;
import messif.utility.Convert;
import messif.utility.ModifiableParametric;
import messif.utility.ParametricBase;
import messif.utility.reflection.MethodInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;


/**
 *  Abstract algorithm framework - support for algorithm naming and operation executive
 *
 *  Every algorithm may support any number of operations (subclasses of AbstractOperation).
 *  This algorithm framework automatically register all methods that have a subclass
 *  of AbstractOperation as the only argument.
 *
 *  The registered operations are executed using executeOperation method, unsupported operation
 *  will throw an AlgorithmMethodException
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Algorithm implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Logger */
    protected static final Logger log = Logger.getLogger("messif.algorithm");

    /** Maximal number of currently executed operations */
    protected static final int maximalConcurrentOperations = 1024;


    //****************** Attributes ******************//

    /** The name of this algorithm */
    private final String algorithmName;

    /** Verbosity of the logging of the last executed operation */
    private int executedOperationsLogVerbosity;

    /** Number of actually running operations */
    private transient Semaphore runningOperationsSemaphore;

    /** List of currently running operations */
    private transient WeakHashMap<AbstractOperation, Thread> runningOperations;

    /** Executor for operations */
    private transient MethodClassExecutor operationExecutor;

    /** Thread pool service to process operations in threads. */
    private transient ExecutorService operationsThreadPool;
    

    //****************** Constructors ******************//

    /**
     * Create new instance of Algorithm and initialize the operation executor.
     * @param algorithmName the name of this algorithm
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses} has no items
     */
    public Algorithm(String algorithmName) throws IllegalArgumentException {
        // Set current algorithm name
        this.algorithmName = algorithmName;
        initializeExecutor();
    }


    //****************** Destructor ******************//

    /**
     * Finalize the algorithm. All transient resources associated with this
     * algorithm are released.
     * After this method is called, the behavior of executing any operation is unpredictable.
     *
     * @throws Throwable if there was an error finalizing
     */
    @Override
    public void finalize() throws Throwable {
        if (operationsThreadPool != null)
            operationsThreadPool.shutdown();
        super.finalize();
    }

    /**
     * Destroy this algorithm. This method releases all resources (transient and persistent)
     * associated with this algorithm.
     * After this method is called, the behavior of executing any operation is unpredictable.
     *
     * <p>
     * This implementation defaults to call {@link #finalize()}, but should be overridden
     * if the algorithm needs to differentiate between finalizing and destroying. In that case
     * the "super.destroy()" should <i>not</i> be called if finalizing is not part of destroy.
     * </p>
     *
     * @throws Throwable if there was an error while cleaning
     */
    public void destroy() throws Throwable {
        finalize();
    }


    //****************** Attribute access ******************//

    /**
     * Returns the name of this algorithm
     * @return the name of this algorithm
     */
    public String getName() {
        return algorithmName;
    }

    /**
     * Returns the class of objects indexed by this algorithm.
     * This methods returns a generic {@link LocalAbstractObject} class.
     * @return the class of objects indexed by this algorithm
     */
    public Class<? extends LocalAbstractObject> getObjectClass() {
        return LocalAbstractObject.class;
    }

    /**
     * Returns the number of objects currently stored in the algorithm.
     * This is (by default) done by executing the {@link GetObjectCountOperation}
     * but can be overridden if a more efficient method is available.
     * If the number of objects is unknown, -1 is returned.
     * 
     * @return the number of objects currently stored in the algorithm
     * @throws AlgorithmMethodException if there was an error during the execution
     */
    public int getObjectCount() throws AlgorithmMethodException {
        try {
            return executeOperation(new GetObjectCountOperation()).getAnswerCount();
        } catch (NoSuchMethodException ignore) {
            return -1;
        }
    }

    /**
     * Sets a new {@link #operationsThreadPool thread pool} used for processing operations (via {@link NavigationProcessor}).
     * The previously set {@link #operationsThreadPool} is shut down (destroyed).
     * If {@code operationsThreadPool} is not null, parallel processing is used.
     * @param operationsThreadPool the new thread pool instance to set (can be <tt>null</tt>)
     */
    public void setOperationsThreadPool(ExecutorService operationsThreadPool) {
        if (this.operationsThreadPool != null && this.operationsThreadPool != operationsThreadPool) {
            this.operationsThreadPool.shutdown();
            log.log(Level.INFO, "shutting down threadpool: {0}", this.operationsThreadPool);
        }
        this.operationsThreadPool = operationsThreadPool;
    }

    /**
     * Returns the current thread pool used for processing operations.
     * @return the current thread pool instance or <tt>null</tt> if no thread pool is set
     */
    public ExecutorService getOperationsThreadPool() {
        return operationsThreadPool;
    }

    /**
     * Set the verbosity of the logging of the last executed operation.
     * If set to zero (default), no executed operations are logged.
     * If set to 1, the executed operation is logged using INFO level.
     * If set to 2, the executed operation and all its parameters are logged using INFO level.
     * @param executedOperationsLogVerbosity the verbosity level
     */
    public void setExecutedOperationsLogVerbosity(int executedOperationsLogVerbosity) {
        this.executedOperationsLogVerbosity = executedOperationsLogVerbosity;
    }


    //****************** Serialization ******************//

    /**
     * Deserialization method.
     * Initialize the method class executor, because it is not serialized
     * @param in the input stream to deserialize from
     * @throws IOException if there was an error reading from the input stream
     * @throws ClassNotFoundException if there was an error resolving classes from the input stream
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeExecutor();
    }

    /**
     * Load the algorithm from the specified file and return it.
     *
     * @param <T> class of the stored algorithm
     * @param algorithmClass class of the stored algorithm
     * @param filepath the path to a file from which the algorithm should be restored
     * @return the loaded algorithm
     * @throws IOException if the specified filename is not a readable serialized algorithm 
     *         (see {@link java.io.ObjectInputStream#readObject readObject} method for detailed description)
     * @throws NullPointerException if the specified filename is <tt>null</tt>
     * @throws ClassNotFoundException if the class of serialized object cannot be found
     * @throws ClassCastException if the filename doesn't contain serialized algorithm
     */
    public static <T extends Algorithm> T restoreFromFile(String filepath, Class<T> algorithmClass) throws IOException, NullPointerException, ClassNotFoundException, ClassCastException {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)));
        try {
            T rtv = algorithmClass.cast(in.readObject());
            log.log(Level.INFO, "Algorithm restored from: {0}", filepath);
            return rtv;
        } finally {
            in.close();
        }
    }

    /**
     * Load the algorithm from the specified file and return it.
     *
     * @param filepath the path to file from which the algorithm should be restored
     * @return the loaded algorithm
     * @throws IOException if the specified filename is not a readable serialized algorithm 
     *         (see {@link java.io.ObjectInputStream#readObject readObject} method for detailed description)
     * @throws NullPointerException if the specified filename is <tt>null</tt>
     * @throws ClassNotFoundException if the class of serialized object cannot be found
     * @throws ClassCastException if the filename doesn't contain serialized algorithm
     */
    public static Algorithm restoreFromFile(String filepath) throws IOException, NullPointerException, ClassNotFoundException, ClassCastException {
        return restoreFromFile(filepath, Algorithm.class);
    }

    /**
     * Store the algorithm to the specified file.
     *
     * @param filepath the path to a file where the algorithm should be stored. If this path is a directory,
     *        the algorithm name (all non alphanumeric characters are replaced by underscore) with <tt>.bin</tt>
     *        extension is appended to the path.
     * @throws IOException if the specified filename is not writable or if an error occurs during the serialization
     *         (see {@link java.io.ObjectOutputStream#writeObject writeObject} method for detailed description)
     */
    public void storeToFile(String filepath) throws IOException {
        // Acquire all locks, thus waiting for all currently running operations and disable additional
        if (maximalConcurrentOperations > 0)
            runningOperationsSemaphore.acquireUninterruptibly(maximalConcurrentOperations);
        
        try {
            // Check if the file is a regular file
            File file = new File(filepath);
            if (file.isDirectory())
                file = new File(file, getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".bin");
        
            beforeStoreToFile(filepath);
            
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            try {
                out.writeObject(this);
                log.log(Level.INFO, "Algorithm stored to: {0}", file.getAbsolutePath());
                afterStoreToFile(filepath, true);
            } catch (IOException ex) {
                afterStoreToFile(filepath, false);
                throw ex;
            } finally {
                out.close();
            } 
        } finally {
            // Unlock operations
            if (maximalConcurrentOperations > 0)
                runningOperationsSemaphore.release(maximalConcurrentOperations);
        }
    }

    /**
     * This method is executed BEFORE the method {@link #storeToFile(java.lang.String)} 
     *  was called. It is empty and expected to be overridden.
     * @param filepath the path to a file where the algorithm was stored
     */
    protected void beforeStoreToFile(String filepath) {
    }

    /**
     * This method is executed after the method {@link #storeToFile(java.lang.String)} 
     *  was called. It is empty and expected to be overridden.
     * @param filepath the path to a file where the algorithm was stored
     * @param successful true, if the write to file was successful, false otherwise
     */
    protected void afterStoreToFile(String filepath, boolean successful) { 
    }
    

    //****************** Operation info methods ******************//

    /**
     * Returns the number of currently evaluated operations.
     * Every thread inside executeOperation is counted as well as every invocation of backgroundExecute
     * that was not yet extracted by waitBackgroundExecuteOperation.
     * @return the number of currently evaluated operations
     */
    public int getRunningOperationsCount() {
        if (maximalConcurrentOperations == 0)
            return 0;
        return maximalConcurrentOperations - runningOperationsSemaphore.availablePermits();
    }

    /**
     * Returns the currently executed operation with the given identifier.
     * If there is no running operation with that identifier, <tt>null</tt> is returned.
     * @param operationId the identifier of the operation to get
     * @return the executed operation or <tt>null</tt> if there was no running operation with that identifier
     */
    public AbstractOperation getRunningOperationById(UUID operationId) {
        for (AbstractOperation operation : getAllRunningOperations())
            if (operation.getOperationID().equals(operationId))
                return operation;
        return null;
    }

    /**
     * Returns all operations currently executed by this algorithm.
     * Note that the returned collection is new independent instance, thus any
     * modifications are not propagated, so it is <em>not</em> unmodifiable.
     * Note also that the thread processing the operation must be still alive
     * in order to be returned by this method.
     *
     * @return a collection of all operations
     */
    public Collection<AbstractOperation> getAllRunningOperations() {
        synchronized (algorithmName) { // We are synchronizing the access to the list using algorithmName so that the runningOperations can be set when deserializing
            // The list of operations must be copied to a serializable list
            Collection<AbstractOperation> ret = new ArrayList<AbstractOperation>(runningOperations.size());
            for (Entry<AbstractOperation, Thread> entry : runningOperations.entrySet()) { // Note that the entryset never returns a key weak-ref that is garbage collected
                if (entry.getValue().isAlive())
                    ret.add(entry.getKey());
            }
            return ret;
        }
    }

    /**
     * Returns the statistics of the executed operations.
     * Unless you call {@link #resetOperationStatistics}, the cumulative statistics
     * for all operations run in this thread are returned.
     * @return the statistics of the executed operations
     */
    public OperationStatistics getOperationStatistics() {
        return OperationStatistics.getLocalThreadStatistics();
    }

    /**
     * Resets all the statistics of the executed operations gathered so far.
     */
    public void resetOperationStatistics() {
        OperationStatistics.resetLocalThreadStatistics();
    }

    /**
     * Returns the list of operations this particular algorithm supports.
     * The operations returned can be further queried on arguments by static methods in AbstractOperation.
     * @return the list of operations this particular algorithm supports
     */
    public List<Class<? extends AbstractOperation>> getSupportedOperations() {
        return getSupportedOperations(AbstractOperation.class);
    }

    /**
     * Returns the list of operations this particular algorithm supports.
     * The operations returned can be restricted to a specified subclass, such as QueryOperation, etc.
     * The operations returned can be further queried on arguments by static methods in AbstractOperation.
     * @param <E> type of the returned operations
     * @param subclassToSearch ancestor class of the returned operations.
     * @return the list of operations this particular algorithm supports
     */
    public <E extends AbstractOperation> List<Class<? extends E>> getSupportedOperations(Class<? extends E> subclassToSearch) {
        return operationExecutor.getDifferentiatingClasses(subclassToSearch);
    }

    /**
     * Returns the first operation that is a supported by this algorithm and is a subclass of (or the same class as) {@code subclassToSearch}.
     * The operations returned can be further queried on arguments by static methods in AbstractOperation.
     *
     * @param <E> type of the returned operations
     * @param subclassToSearch ancestor class of the returned operations
     * @return the first operation of {@code subclassToSearch} that is a supported by this algorithm
     * @throws NoSuchMethodException if this algorithm does not support any operation of the given {@code subclassToSearch}
     */
    public final <E extends AbstractOperation> Class<? extends E> getFirstSupportedOperation(Class<? extends E> subclassToSearch) throws NoSuchMethodException {
        List<Class<? extends E>> supportedOperations = getSupportedOperations(subclassToSearch);
        if (supportedOperations == null || supportedOperations.isEmpty())
            throw new NoSuchMethodException("Algorithm does not support operation " + subclassToSearch.getName());
        return supportedOperations.get(0);
    }

    /**
     * Given a list of abstract operation classes and a required class,
     * this auxiliary static method returns list of all classes that are subclass of the required class.
     * @param <E> type of the returned operations
     * @param operations list of operation classes to search within
     * @param subclassToSearch ancestor class of the returned operations
     * @return the list of operations this particular algorithm supports
     */
    public static <E extends AbstractOperation> List<Class<? extends E>> getOperationSubClasses(Collection<Class<? extends AbstractOperation>> operations, Class<? extends E> subclassToSearch) {
        List<Class<? extends E>> ret = new ArrayList<>();
        for (Class<? extends AbstractOperation> operClass : operations) {
            if (subclassToSearch.isAssignableFrom(operClass)) {
                ret.add((Class<E>) operClass); // This class is checked on the previous line
            }
        }
        return ret;
    }


    //****************** Operation execution ******************//

    /**
     * Initialize operation executor.
     * This method is called from constructor and serialization.
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses} has no items
     */
    private void initializeExecutor() throws IllegalArgumentException {
        runningOperationsSemaphore = new Semaphore(maximalConcurrentOperations, true);
        runningOperations = new WeakHashMap<>(maximalConcurrentOperations);
        operationExecutor = new MethodClassExecutor(this, 0, null, Modifier.PUBLIC|Modifier.PROTECTED, Algorithm.class, getExecutorParamClasses());
    }
    
    /**
     * Executes a given {@code operation} by the processor provided by {@link NavigationDirectory}.
     * If the passed objects are not instances of {@link NavigationDirectory} or {@link AbstractOperation},
     * or the directory does not provide processor for the given operation, <tt>false</tt> is returned
     * and no processing is done. Otherwise, the sequential or asynchronous processing
     * is executed for the given operation.
     * 
     * @param navigationDirectory an instance of {@link NavigationDirectory} as plain {@link Object}
     * @param operation an instance of {@link AbstractOperation} as plain {@link Object} compatible with the given {@link NavigationDirectory}
     * @param statisticsOn run the before/after operation statistics, see {@link #statisticsBeforeOperation()} and {@link #statisticsAfterOperation(messif.operations.AbstractOperation)}
     * @return <tt>true</tt> if the {@code operation} was processed using the {@code navigationDirectory} or
     *      <tt>false</tt> if no processing was performed
     * @throws InterruptedException if the processing thread is interrupted during the processing
     * @throws AlgorithmMethodException if there was an error during the processing
     * @throws CloneNotSupportedException if there was a need for cloning (due to asynchronous access) but cloning was not supported
     */
    public boolean executeUsingNavDir(Object navigationDirectory, Object operation, boolean statisticsOn) throws InterruptedException, AlgorithmMethodException, CloneNotSupportedException {
        if (!(operation instanceof AbstractOperation))
            return false;
        NavigationProcessor<? extends AbstractOperation> navigationProcessor = getNavigationProcessor(navigationDirectory, (AbstractOperation)operation);
        if (navigationProcessor == null)
            return false;
        if (statisticsOn) {
            statisticsBeforeOperation();            
        }
        NavigationProcessors.execute(operationsThreadPool, navigationProcessor);
        if (statisticsOn) {
            statisticsAfterOperation((AbstractOperation) operation);            
        }
        return true;
    }
    
    
    /**
     * Execute operation with additional parameters.
     * This is a synchronized wrapper around {@link MethodExecutor#execute}.
     * @param statisticsOn add the execution time statistics to {@link OperationStatistics}
     * @param params the parameters compatible with {@link #getExecutorParamClasses()}
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    protected final void execute(final boolean statisticsOn, Object... params) throws AlgorithmMethodException, NoSuchMethodException {
        if (maximalConcurrentOperations > 0)
            runningOperationsSemaphore.acquireUninterruptibly();
        synchronized (algorithmName) { // We are synchronizing the access to the list using algorithmName so that the runningOperations can be set when deserializing
            runningOperations.put(getExecutorOperationParam(params), Thread.currentThread());
        }
        // log the operation processing information 
        long startTimeStamp = System.currentTimeMillis();
        try {
            StatisticTimer operationTime = null;
            // Measure time of execution (as an operation statistic)
            if (statisticsOn) {
                operationTime = OperationStatistics.getOpStatistics("OperationTime", StatisticTimer.class);
                operationTime.start();
            }
            if (!executeUsingNavDir(this, params[0], statisticsOn))
                operationExecutor.execute(params);
            if (statisticsOn) {
                operationTime.stop();
            }
        } catch (CloneNotSupportedException e) {
            throw new AlgorithmMethodException(e);
        } catch (ClassCastException e) { // This can occur when OperationTime statistics exists, but has a wrong class
            throw new AlgorithmMethodException(e);
        } catch (InterruptedException e) {
            throw new AlgorithmMethodException(e);
        } catch (InvocationTargetException e) {
            throw new AlgorithmMethodException(e.getCause());
        } finally {
            long runningTime = System.currentTimeMillis() - startTimeStamp;
            if (executedOperationsLogVerbosity > 0) {
                Object paramString = params[0]; // This will be automatically converted to string by the logger
                if (executedOperationsLogVerbosity > 1) {
                    paramString = ParametricBase.toStringWithCast(params[0], "\n", ": ", ", ");
                }
                log.log(Level.INFO, "{0} processed: {1}; Time: {2}", new Object[]{this.getName(), paramString, runningTime});
            }
            if (! statisticsOn && (params[0] instanceof ModifiableParametric)) {
                ((ModifiableParametric) params[0]).setParameter("OperationTime", runningTime);
            }
            if (maximalConcurrentOperations > 0)
                runningOperationsSemaphore.release();
        }
    }

    /**
     * Execute operation on this algorithm.
     * @param <T> the type of executed operation
     * @param operation the operation to execute on this algorithm
     * @return the executed operation (same as the argument)
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    public <T extends AbstractOperation> T executeOperation(T operation) throws AlgorithmMethodException, NoSuchMethodException {
        execute(Statistics.isEnabledGlobally(), operation);
        return operation;
    }

    /**
     * Reset {@link #resetOperationStatistics() operation statistics},
     * bind the operation statistics according to the given regular expression, 
     * and execute operation on this algorithm.
     * @param <T> the type of executed operation
     * @param operation the operation to execute on this algorithm
     * @param operationStatsRegexp regular expression matching the statistics to bind
     * @return the executed operation (same as the argument)
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    public <T extends AbstractOperation> T setupStatsAndExecuteOperation(T operation, String operationStatsRegexp) throws AlgorithmMethodException, NoSuchMethodException {
        resetOperationStatistics();
        if (operationStatsRegexp != null)
            OperationStatistics.getLocalThreadStatistics().registerBoundAllStats(operationStatsRegexp);
        T rtv = executeOperation(operation);
        if (operationStatsRegexp != null)
            OperationStatistics.getLocalThreadStatistics().unbindAllStats(operationStatsRegexp);
        return rtv;
    }

    /**
     * Execute query operation on this algorithm and return the answer.
     * This is a shortcut method for calling {@link #executeOperation(messif.operations.AbstractOperation)} and
     * {@link QueryOperation#getAnswer()}.
     *
     * @param <T> the type of query operation answer
     * @param operation the operation to execute on this algorithm
     * @return iterator for the answer of the executed query
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    public final <T> Iterator<? extends T> getQueryAnswer(QueryOperation<? extends T> operation) throws AlgorithmMethodException, NoSuchMethodException {
        return executeOperation(operation).getAnswer();
    }

    /**
     * Execute query operation on this algorithm and return the answer.
     * The operation to execute is created according to the given class and arguments.
     * This is a shortcut method for calling {@link AbstractOperation#createOperation(java.lang.Class, java.lang.Object[])} and
     * {@link #getQueryAnswer(messif.operations.QueryOperation)}.
     *
     * @param <T> the type of query operation answer
     * @param operationClass the class of the operation to execute on this algorithm
     * @param arguments the arguments for the operation constructor
     * @return iterator for the answer of the executed query
     * @throws InvocationTargetException if the operation constructor has thrown an exception
     * @throws NoSuchMethodException if the operation is unknown or unsupported by this algorithm
     * @throws AlgorithmMethodException if the execution has thrown an exception
     */
    public final <T> Iterator<? extends T> getQueryAnswer(Class<? extends QueryOperation<? extends T>> operationClass, Object... arguments) throws InvocationTargetException, AlgorithmMethodException, NoSuchMethodException {
        return getQueryAnswer(AbstractOperation.createOperation(operationClass, arguments));
    }

    /**
     * Creates a new {@link Callable} that simply runs the {@link #executeOperation} method on the given operation.
     * @param <T> the type of operation
     * @param operation the operation to run
     * @return the created {@link Callable}
     */
    protected <T extends AbstractOperation> Callable<T> createBackgroundExecutionCallable(final T operation) {
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                return executeOperation(operation);
            }
        };
    }

    /**
     * Execute algorithm operation on background.
     * @param <T> the type of the executed operation
     * @param operation the operation to execute on this algorithm
     * @return a {@link Future} that can be used to wait for the execution to finish and retrieve the resulting executed operation;
     *      note that a {@link NoSuchMethodException} exception can be thrown if the operation is unsupported (there is no method for the operation)
     */
    public <T extends AbstractOperation> Future<T> backgroundExecuteOperation(T operation) {
        return operationsThreadPool.submit(createBackgroundExecutionCallable(operation));
    }

    /**
     * Execute algorithm operation on background.
     * @param <T> the type of the executed operation
     * @param operation the operation to execute on this algorithm
     * @return a {@link Future} that can be used to wait for the execution to finish and retrieve the resulting executed operation;
     *      note that a {@link NoSuchMethodException} exception can be thrown if the operation is unsupported (there is no method for the operation)
     */
    public <T extends AbstractOperation> FutureWithStatistics<T> backgroundExecuteOperationWithStatistics(T operation) {
        return FutureWithStatisticsImpl.submit(operationsThreadPool, createBackgroundExecutionCallable(operation));
    }

    /**
     * Helper method for waiting for an operation executed on background.
     * Note that the operation statistics are automatically updated if the given future
     * implements {@link FutureWithStatistics}.
     * @param <T> the type of the executed operation
     * @param future a future of the previously executed operation
     * @return the executed operation
     * @throws AlgorithmMethodException if there was an exception during the background execution
     * @throws InterruptedException if the waiting was interrupted
     * @throws NoSuchMethodException if the operation is not supported
     */
    @SuppressWarnings("ThrowableResultIgnored")
    public static <T extends AbstractOperation> T waitBackgroundExecution(Future<? extends T> future) throws InterruptedException, AlgorithmMethodException, NoSuchMethodException {
        try {
            T ret = future.get();
            if (future instanceof FutureWithStatistics) {
                OperationStatistics.getLocalThreadStatistics().updateFrom((FutureWithStatistics<? extends T>)future);
            }
            return ret;
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof AlgorithmMethodException)
                throw (AlgorithmMethodException)ex.getCause();
            if (ex.getCause() instanceof NoSuchMethodException)
                throw (NoSuchMethodException)ex.getCause();
            throw new AlgorithmMethodException(ex.getCause());
        }
    }

    /**
     * Execute algorithm operation on background independently, i.e. without the
     * possibility to wait for its finish. Use the {@link #getAllRunningOperations()}
     * or {@link #getRunningOperationById(java.util.UUID)} to access the operation.
     * Method {@link #terminateOperation(java.util.UUID)} can be used to stop the operation.
     * After the operation is finished, there is no way to access it.
     * @param operation the operation to execute on this algorithm
     */
    public void backgroundExecuteOperationIndependent(AbstractOperation operation) {
        backgroundExecuteOperation(operation);
    }

    /**
     * Terminates processing of the operation with given identifier.
     * Note that the thread that executes the operation is interrupted leaving
     * the decision on how to finish cleanly to the processing method.
     * Each processing method thus should check the {@link Thread#isInterrupted()}
     * regularly and act accordingly if it is set.
     * @param operationId the identifier of the operation to terminate
     * @return <tt>true</tt> if there was an operation for that identifier and it was interrupted;
     *          if there was no operation or the thread executing it has already finished, <tt>false</tt> is returned
     */
    public boolean terminateOperation(UUID operationId) {
        synchronized (algorithmName) { // We are synchronizing the access to the list using algorithmName so that the runningOperations can be set when deserializing
            for (Entry<AbstractOperation, Thread> entry : runningOperations.entrySet()) {
                if (entry.getKey().getOperationID().equals(operationId)) {
                    entry.getValue().interrupt();
                    return entry.getValue().isAlive();
                }
            }
            return false;
        }
    }

    /**
     * Terminates processing of the given operation.
     * Note that the thread that executes the operation is interrupted leaving
     * the decision on how to finish cleanly to the processing method.
     * Each processing method thus should check the {@link Thread#isInterrupted()}
     * regularly and act accordingly if it is set.
     * @param operation the operation to terminate
     * @return <tt>true</tt> if the operation was processed by a thread and it was interrupted;
     *          if there was no thread currently processing the operation, <tt>false</tt> is returned
     */
    public boolean terminateOperation(AbstractOperation operation) {
        return terminateOperation(operation.getOperationID());
    }

    /**
     * This method can be used by all algorithms before processing any operation to set default (operation) statistics.
     * @throws ClassCastException if new statistic cannot be created
     */
    public void statisticsBeforeOperation() throws ClassCastException {
            OperationStatistics.getLocalThreadStatistics().registerBoundStat(StatisticCounter.class, "DistanceComputations", "DistanceComputations");
            OperationStatistics.getLocalThreadStatistics().registerBoundStat(StatisticCounter.class, "DistanceComputations.Savings", "DistanceComputations.Savings");
            OperationStatistics.getLocalThreadStatistics().registerBoundStat(StatisticCounter.class, "BlockReads", "BlockReads");
        }

    /**
     * This method can be used by all algorithms after processing any operation to set default (operation) statistics.
     * The results are right only when the {@link #statisticsBeforeOperation() } method was used before.
     * @param operation (typically query) operation that was just processed 
     */
    public void statisticsAfterOperation(AbstractOperation operation) {
        OperationStatistics.getLocalThreadStatistics().unbindAllStats();
        StatisticCounter accessedObjects = OperationStatistics.getOpStatisticCounter("AccessedObjects");
        accessedObjects.set(OperationStatistics.getOpStatisticCounter("DistanceComputations").get()
                + OperationStatistics.getOpStatisticCounter("DistanceComputations.Savings").get());
        if (operation instanceof QueryOperation) {
            OperationStatistics.getOpStatisticCounter("AnswerCount").set(((QueryOperation) operation).getAnswerCount());
            if ((operation instanceof RankingQueryOperation) && (((QueryOperation) operation).getAnswerCount() > 0)) {
                OperationStatistics.getLocalThreadStatistics().getStatistics("AnswerDistance", StatisticObject.class).set(((RankingQueryOperation) operation).getAnswerDistance());
            }
        }
    }    


    //****************** Operation method specifier ******************//

    /**
     * This method should return an array of additional parameters that are needed for operation execution.
     * The list must be consistent with the parameters array passed to {@link #execute} and {@link #backgroundExecute}.
     * @return array of additional parameters that are needed for operation execution
     */
    protected Class<?>[] getExecutorParamClasses() {
        Class<?>[] rtv = { AbstractOperation.class };
        return rtv;
    }

    /**
     * Returns the instance of {@link AbstractOperation} from the parameters for the executor.
     * @param params the parameters for the executor
     * @return the instance of {@link AbstractOperation}
     */
    private AbstractOperation getExecutorOperationParam(Object... params) {
        Class<?>[] executorParamClasses = getExecutorParamClasses();
        for (int i = 0; i < executorParamClasses.length; i++)
            if (executorParamClasses[i] == AbstractOperation.class) {
                if (params[i] instanceof AbstractOperation)
                    return (AbstractOperation)params[i];
                throw new InternalError("Method getExecutorParamClasses is not compatible with the actual operation call");
            }
        throw new InternalError("Method getExecutorParamClasses does not have an AbstractOperation class");
    }


    //****************** Method execute ******************//

    /**
     * Executes a given method on this algorithm and returns the result.
     * @param methodName the name of the method to execute on the remote algorithm
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted to proper types if possible
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param methodArguments the arguments for the method
     * @return the method return value
     * @throws InvocationTargetException if the executed method throws an exception
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    Object methodExecute(String methodName, boolean convertStringArguments, Map<String, Object> namedInstances, Object... methodArguments) throws InvocationTargetException, NoSuchInstantiatorException, IllegalArgumentException {
        try {
            return MethodInstantiator.getMethod(getClass(), methodName, convertStringArguments, true, namedInstances, methodArguments).invoke(this, methodArguments);
        } catch (IllegalAccessException e) {
            throw new InternalError("Method cannot be invoked even though it is public"); // This should never happen
        }
    }

    /**
     * Executes a given method on this algorithm and returns the result.
     * This method is a convenience method for APIs and should not be called from a native Java application.
     *
     * @param methodNameAndArguments the array that contains a method name and its arguments as string
     * @param methodNameIndex the index in the {@code methodNameAndArguments} array where the method name is (the following arguments are considered to be the arguments)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @return the method return value
     * @throws InvocationTargetException if the executed method throws an exception
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    public final Object executeMethodWithStringArguments(String[] methodNameAndArguments, int methodNameIndex, Map<String, Object> namedInstances) throws InvocationTargetException, NoSuchInstantiatorException, IllegalArgumentException {
        Object[] arguments = new Object[methodNameAndArguments.length - methodNameIndex - 1];
        System.arraycopy(methodNameAndArguments, methodNameIndex + 1, arguments, 0, arguments.length);
        return methodExecute(methodNameAndArguments[methodNameIndex], true, namedInstances, arguments);
    }

    /**
     * Executes a given method on this algorithm and returns the result.
     * @param methodName the name of the method to execute on the remote algorithm
     * @param methodArguments the arguments for the method
     * @return the method result or exception
     * @throws InvocationTargetException if the executed method throws an exception
     * @throws NoSuchInstantiatorException if the there is no method for the given name and prototype
     * @throws IllegalArgumentException if there was a problem reading the class in the remote algorithm's result
     */
    public final Object methodExecute(String methodName, Object[] methodArguments) throws InvocationTargetException, NoSuchInstantiatorException, IllegalArgumentException {
        return methodExecute(methodName, false, null, methodArguments);
    }


    //****************** Algorithm markers ******************//

    /**
     * Annotation for algorithm constructors.
     * Each constructor, that should be accessible by auto-generated clients
     * must be annotated. Such constructor can only have parameters that can
     * be converted from a string by {@link messif.utility.Convert#stringToType stringToType}
     * method. Each constructor parameter should be annotated by a description
     * using this annotations values.
     */    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    public @interface AlgorithmConstructor {
        /**
         * Description of an algorithm constructor.
         * @return description of algorithm constructor
         */
        String description();
        /**
         * A list of descriptions for constructor parameters.
         * Each parameter should have a position-matching
         * descriptor value.
         * @return list of descriptions for constructor parameters
         */
        String[] arguments();
    }

    /**
     * Returns all annotated constructors of the provided algorithm class.
     * @param <E> class of algorithm for which to get constructors
     * @param algorithmClass the class of an algorithm for which to get constructors
     * @return all annotated constructors of the provided algorithm class
     */
    public static <E extends Algorithm> List<Constructor<E>> getAnnotatedConstructors(Class<? extends E> algorithmClass) {
        List<Constructor<E>> rtv = new ArrayList<Constructor<E>>();
        
        // Search all its constructors for proper annotation
        for (Constructor<E> constructor : Convert.getConstructors(algorithmClass))
            if (constructor.isAnnotationPresent(AlgorithmConstructor.class))
                rtv.add(constructor);
        
        return rtv;
    }

    /**
     * Returns all annotated constructors of the provided algorithm class as array.
     * @param <E> class of algorithm for which to get constructors
     * @param algorithmClass the class of an algorithm for which to get constructors
     * @return all annotated constructors of the provided algorithm class
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <E extends Algorithm> Constructor<E>[] getAnnotatedConstructorsArray(Class<? extends E> algorithmClass) {
        return getAnnotatedConstructors(algorithmClass).toArray(new Constructor[1]);
    }

    /**
     * Returns constructor argument descriptions for the provided algorithm constructor.
     * List of available constructors of an algorithm class can be retrieved using {@link #getAnnotatedConstructors getAnnotatedConstructors}.
     * This is used by auto-generated clients to show description during algorithm creation.
     * @param constructor an algorithm constructor to get the descriptions for
     * @return constructor argument descriptions
     */
    public static String[] getConstructorArgumentDescriptions(Constructor<? extends Algorithm> constructor) {
        AlgorithmConstructor annotation = constructor.getAnnotation(AlgorithmConstructor.class);
        return (annotation == null)?null:annotation.arguments();
    }

    /**
     * Returns constructor description (without description of arguments) for the provided algorithm constructor.
     * List of available constructors of an algorithm class can be retrieved using {@link #getAnnotatedConstructors getAnnotatedConstructors}.
     * This is used by auto-generated clients to show description during algorithm creation.
     * @param constructor an algorithm constructor to get the descriptions for
     * @return constructor description
     */
    public static String getConstructorDescriptionSimple(Constructor<? extends Algorithm> constructor) {
        AlgorithmConstructor annotation = constructor.getAnnotation(AlgorithmConstructor.class);
        if (annotation == null)
            return "";
        
        // Return only the description of the constructor
        return annotation.description();
    }

    /**
     * Returns algorithm constructor description including descriptions for all its arguments.
     * List of available constructors of an algorithm class can be retrieved using {@link #getAnnotatedConstructors getAnnotatedConstructors}.
     * This is used by auto-generated clients to show description during algorithm creation.
     * 
     * @param constructor an algorithm constructor to get the descriptions for
     * @return constructor description including descriptions for all its arguments
     */
    public static String getConstructorDescription(Constructor<? extends Algorithm> constructor) {
        AlgorithmConstructor annotation = constructor.getAnnotation(AlgorithmConstructor.class);
        if (annotation == null)
            return "";
        
        // Construct the following description string: <algorithm class> [<argument description> ...]\n\t...<constructor description>
        StringBuilder rtv = new StringBuilder(constructor.getDeclaringClass().getName());
        for (String arg : annotation.arguments())
            rtv.append(" <").append(arg).append(">");
        rtv.append("\n\t...").append(annotation.description());
        
        return rtv.toString();
    }

}
