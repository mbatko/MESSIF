/*
 * Algorithm.java
 *
 * Created on 5. kveten 2003, 23:46
 */

package messif.algorithms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import messif.buckets.BucketErrorCode;
import messif.executor.Executable;
import messif.executor.MethodClassExecutor;
import messif.executor.MethodThreadList;
import messif.operations.AbstractOperation;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import messif.executor.MethodExecutor;
import messif.executor.MethodThread;
import messif.statistics.OperationStatistics;
import messif.statistics.StatisticCounter;
import messif.statistics.StatisticTimer;
import messif.statistics.Statistics;
import messif.utility.Convert;
import messif.utility.Logger;


/**
 *  Abstract algorithm framework - support for algorithm naming and operation executive
 *
 *  Every algorithm may suport any number of operations (subclasses of AbstractOperation).
 *  This algorithm framework automatically register all methods that have a subclass
 *  of AbstractOperation as the only argument.
 *
 *  The registered operations are executed using executeOperation method, unsupported operation
 *  will throw an AlgorithmMethodException
 *
 * @author  xbatko
 */
public abstract class Algorithm implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Logger */
    protected static Logger log = Logger.getLoggerEx("messif.algorithm");

    /** Global counter for distance computations (any purpose) */
    protected static final StatisticCounter counterDistanceComputations = StatisticCounter.getStatistics("DistanceComputations");
    
    /** Maximal number of currently executed operations */
    protected static final int maximalConcurrentOperations = 1024;

    /****************** Naming ******************/

    /** The name of this algorithm */
    protected final String algorithmName;
    
    /**
     * Returns the name of this algorithm
     * @return the name of this algorithm
     */
    public String getName() {
        return algorithmName;
    }
    
    
    /****************** Constructors******************/
    
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

    
    /****************** Destructor ******************/
    
    /**
     * Public destructor to stop the algorithm.
     * This should be overriden in order to clean up.
     */
    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }


    /****************** Serialization ******************/

    /**
     * Deserialization method.
     * Initialize the method class executor, because it is not serialized
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeExecutor();
    }


    /**
     * Load the algorithm from the specified file and return it.
     *
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
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(filepath));
        try {
            T rtv = Convert.safeGenericCast(in.readObject(), algorithmClass);
            log.info("Algorithm restored from: " + filepath);
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
     * @throws IOException if the specified filename is not writable or if an error occurrs during the serialization
     *         (see {@link java.io.ObjectOutputStream#writeObject writeObject} method for detailed description)
     */
    public void storeToFile(String filepath) throws IOException {
        // Acquire all locks, thus waiting for all currently running operations and disable additional
        if (maximalConcurrentOperations > 0)
            runningOperations.acquireUninterruptibly(maximalConcurrentOperations);
        
        try {
            // Check if the file is a regular file
            File file = new File(filepath);
            if (file.isDirectory())
                file = new File(file, getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".bin");
        
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            try {
                out.writeObject(this);
                log.info("Algorithm stored to: " + file.getAbsolutePath());
            } finally {
                out.close();
            }
        } finally {
            // Unlock operations
            if (maximalConcurrentOperations > 0)
                runningOperations.release(maximalConcurrentOperations);
        }
    }


    /****************** Operation execution ******************/

    /** Number of actually running operations */
    protected transient Semaphore runningOperations;
    /** Executor for operations */
    protected transient MethodClassExecutor operationExecutor;
    /** List of background-executed operations (per thread) */
    protected transient ThreadLocal<StatisticsEnabledMethodThreadList> bgExecutionList;

    /**
     * Returns the number of currently evaluated operations.
     * Every thread inside executeOperation is counted as well as every invocation of backgroundExecute
     * that was not yet exctracted by waitBackgroundExecuteOperation.
     * @return the number of currently evaluated operations
     */
    public int getRunningOperationsCount() {
        if (maximalConcurrentOperations == 0)
            return 0;
        return maximalConcurrentOperations - runningOperations.availablePermits();
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
     * Initialize operation executor.
     * This method is called from constructor and serialization.
     * @throws IllegalArgumentException if the prototype returned by {@link #getExecutorParamClasses getExecutorParamClasses} has no items
     */
    protected void initializeExecutor() throws IllegalArgumentException {
        runningOperations = new Semaphore(maximalConcurrentOperations, true);
        operationExecutor = new MethodClassExecutor(this, 0, null, Modifier.PUBLIC|Modifier.PROTECTED, Algorithm.class, getExecutorParamClasses());
        bgExecutionList = new ThreadLocal<StatisticsEnabledMethodThreadList>() {
            @Override
            protected synchronized StatisticsEnabledMethodThreadList initialValue() {
                return new StatisticsEnabledMethodThreadList(operationExecutor);
            }
        };
    }

    /**
     * Returns the list of operations this particular algorithm supports.
     * The operations returned can be further queried on arguments by static methods in AbstractOperation.
     * @return the list of operations this particular algorithm supports
     */
    public List<Class<AbstractOperation>> getSupportedOperations() {
        return operationExecutor.getDifferentiatingClasses(AbstractOperation.class);
    }

    /**
     * Returns the list of operations this particular algorithm supports.
     * The operations returned can be restricted to a specified subclass, such as QueryOperation, etc.
     * The operations returned can be further queried on arguments by static methods in AbstractOperation.
     * @param subclassToSearch ancestor class of the returned operations.
     * @return the list of operations this particular algorithm supports
     */
    public <E extends AbstractOperation> List<Class<E>> getSupportedOperations(Class<E> subclassToSearch) {
        return operationExecutor.getDifferentiatingClasses(subclassToSearch);
    }

    /**
     * Execute algorithm operation.
     * @param operation the operation to execute on this algorithm
     * @throws AlgorithmMethodException if the execution has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    public void executeOperation(AbstractOperation operation) throws AlgorithmMethodException, NoSuchMethodException {
        if (maximalConcurrentOperations > 0)
            runningOperations.acquireUninterruptibly();
        try {
            if (Statistics.isEnabledGlobally()) {
                // Measure time of execution (as an operation statistic)
                StatisticTimer operationTime = OperationStatistics.getOpStatistics("OperationTime", StatisticTimer.class);
                operationTime.start();
                operationExecutor.execute(operation);
                operationTime.stop();
            } else operationExecutor.execute(operation);
            // If the operation's result code has not changed, pretend that everything went fine.
            if (operation.getErrorCode().equals(BucketErrorCode.NOT_SET))
                operation.endOperation();
        } catch (InvocationTargetException e) {
            throw new AlgorithmMethodException(e.getCause());
        } finally {
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
        }
    }

    /**
     * Execute algorithm operation on background.
     * <i>Note:</i> Method {@link #waitBackgroundExecuteOperation} MUST be called in the future to release resources.
     * @param operation the operation to execute on this algorithm
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    public void backgroundExecuteOperation(AbstractOperation operation) throws NoSuchMethodException {
        backgroundExecuteOperation(operation, false);
    }

    /**
     * Execute algorithm operation on background.
     * <i>Note:</i> Method {@link #waitBackgroundExecuteOperation} MUST be called in the future to release resources.
     * @param operation the operation to execute on this algorithm
     * @param updateStatistics set to <tt>true</tt> if the operations statistic should be updated after the operation finishes its background execution
     * @throws NoSuchMethodException if the operation is unsupported (there is no method for the operation)
     */
    public void backgroundExecuteOperation(AbstractOperation operation, boolean updateStatistics) throws NoSuchMethodException {
        if (maximalConcurrentOperations > 0)
            runningOperations.acquireUninterruptibly(); // Release for this acquire is in the waitBackgroundExecuteOperation
        try {
            bgExecutionList.get().backgroundExecute(updateStatistics, operation);
        } catch (NoSuchMethodException e) {
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
            throw e;
        } catch (RuntimeException e) {
            if (maximalConcurrentOperations > 0)
                runningOperations.release();
            throw e;
        }
    }

    /**
     * Wait for all operations executed on background to finish.
     * @return the list of operations that were executed
     * @throws AlgorithmMethodException if there was an exception during the background execution
     */
    public List<AbstractOperation> waitBackgroundExecuteOperation() throws AlgorithmMethodException {
        return waitBackgroundExecuteOperation(AbstractOperation.class);
    }

    /**
     * Wait for all operations executed on background to finish.
     * Only objects (executed methods' arguments) with the specified class are returned.
     * 
     * @param argClass filter on the returned object classes
     * @return the list of operations that were executed
     * @throws AlgorithmMethodException 
     */
    public <E> List<E> waitBackgroundExecuteOperation(Class<E> argClass) throws AlgorithmMethodException {
        try {
            MethodThreadList list = bgExecutionList.get();
            
            // Wait for execution end and release locks
            int operationsCount = list.waitBackgroundExecuteOperation();
            if (maximalConcurrentOperations > 0)
                runningOperations.release(operationsCount);
            
            List<E> retList = list.getAllMethodsArgument(argClass);
            
            // clear the list of finished threads
            list.clearThreadLists();
            
            return retList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AlgorithmMethodException(e);
        }
    }

    /** This is a helper class that allows merging of operation statistics from the background-executed operations */
    protected static class StatisticsEnabledMethodThreadList extends MethodThreadList implements Executable {
        /** The marked statistics to merge */
        protected Set<OperationStatistics> statisticsToMerge = null;

        /**
         * Create a new instance of StatisticsEnabledMethodThreadList.
         * @param methodExecutor the executor to use to actually execute code
         */
        public StatisticsEnabledMethodThreadList(MethodExecutor methodExecutor) {
            super(methodExecutor);
        }

        /**
         * Mark the current thread's statistics for future merge.
         * Must be called from within the thread whose statistics are going to be merged.
         */
        public synchronized void execute() {
            if (statisticsToMerge == null)
                statisticsToMerge = new HashSet<OperationStatistics>();
            statisticsToMerge.add(OperationStatistics.getLocalThreadStatistics());
        }

        /**
         * Execute registered method by arguments on background.
         * If updateStatistics is set to <tt>true</tt>, OperationStatistics of the
         * calling thread are updated when the execution finishes and is retrieved
         * by {@link #waitBackgroundExecuteOperation}.
         * 
         * @param updateStatistics flag enabling the OperationStatistics update
         * @param arguments The array of arguments for the execution method (must be consistent with the prototype in constructor)
         * @param executeBefore method to call before registered method
         * @param executeAfter method to call after registered method
         * @return method execution thread object. Method waitExecutionEnd of this object can be used to retrieve the results
         * @throws NoSuchMethodException if there was no valid method for the specified arguments
         */
        public MethodThread backgroundExecute(boolean updateStatistics, Object[] arguments, Executable executeBefore, Executable executeAfter) throws NoSuchMethodException {
            if (!updateStatistics)
                return backgroundExecute(arguments, executeBefore, executeAfter);

            if (executeAfter == null)
                return backgroundExecute(arguments, executeBefore, this);

            // Value for executeAfter is set, a collection must be created
            List<Executable> executeAfterList = new ArrayList<Executable>();
            executeAfterList.add(executeAfter);
            executeAfterList.add(this);
            return backgroundExecute(arguments, Collections.singletonList(executeBefore), executeAfterList);
        }

        /**
         * Execute registered method by arguments on background.
         * If updateStatistics is set to <tt>true</tt>, OperationStatistics of the
         * calling thread are updated when the execution finishes and is retrieved
         * by {@link #waitBackgroundExecuteOperation}.
         * 
         * @param updateStatistics flag enabling the OperationStatistics update
         * @param arguments arguments for the execution method (must be consistent with the prototype in constructor)
         * @return method execution thread object; method waitExecutionEnd of this object can be used to retrieve the results
         * @throws NoSuchMethodException if there was no valid method for the specified arguments
         */
        public MethodThread backgroundExecute(boolean updateStatistics, Object... arguments) throws NoSuchMethodException {
            return backgroundExecute(updateStatistics, arguments, null, null);
        }

        @Override
        public int waitBackgroundExecuteOperation() throws Exception {
            int rtv = super.waitBackgroundExecuteOperation();
            synchronized (this) {
                if (statisticsToMerge != null) {
                    OperationStatistics destinationStats = OperationStatistics.getLocalThreadStatistics();
                    for (OperationStatistics stats : statisticsToMerge)
                        destinationStats.updateFrom(stats);
                    statisticsToMerge.clear();
                }
            }
            return rtv;
        }

    }


    /****************** Operation selection ******************/

    /**
     * This method should return an array of additional parameters that are needed for operation execution.
     * The list must be consistent (speaking in terms of types) with the executeOperation method.
     * 
     * @return array of additional parameters that are needed for operation execution
     */
    protected Class[] getExecutorParamClasses() {
        Class[] rtv = { AbstractOperation.class };
        return rtv;
    }


    /****************** Algorithm markers ******************/

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
         * Each parameter should have a positionally-matching
         * descriptor value.
         * @return list of descriptions for constructor parameters
         */
        String[] arguments();
    }

    /**
     * Returns all annotated constructors of the provided algorithm class.
     * @param algorithmClass the class of an algorithm for which to get constructors
     * @return all annotated constructors of the provided algorithm class
     */
    public static <E extends Algorithm> List<Constructor<E>> getAnnotatedConstructors(Class<E> algorithmClass) {
        List<Constructor<E>> rtv = new ArrayList<Constructor<E>>();
        
        // Search all its constructors for proper annotation
        for (Constructor<E> constructor : (Constructor<E>[])algorithmClass.getConstructors()) // This IS A STUPID unchecked !!!
            if (constructor.isAnnotationPresent(AlgorithmConstructor.class))
                rtv.add(constructor);
        
        return rtv;
    }

    /**
     * Returns constructor argument descriptions for the provided algorithm constuctor.
     * List of available constructors of an algorithm class can be retrieved using {@link #getAnnotatedConstructors getAnnotatedConstructors}.
     * This is used by auto-generated clients to show descriptiron during algorithm creation.
     * @param constructor an algorithm constructor to get the descriptions for
     * @return constructor argument descriptions
     */
    public static String[] getConstructorArgumentDescriptions(Constructor<? extends Algorithm> constructor) {
        AlgorithmConstructor annotation = constructor.getAnnotation(AlgorithmConstructor.class);
        return (annotation == null)?null:annotation.arguments();
    }

    /**
     * Returns constructor description (without description of arguments) for the provided algorithm constuctor.
     * List of available constructors of an algorithm class can be retrieved using {@link #getAnnotatedConstructors getAnnotatedConstructors}.
     * This is used by auto-generated clients to show descriptiron during algorithm creation.
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
     * This is used by auto-generated clients to show descriptiron during algorithm creation.
     * 
     * @param constructor an algorithm constructor to get the descriptions for
     * @return constructor description including descriptions for all its arguments
     */
    public static String getConstructorDescription(Constructor<? extends Algorithm> constructor) {
        AlgorithmConstructor annotation = constructor.getAnnotation(AlgorithmConstructor.class);
        if (annotation == null)
            return "";
        
        // Construct the following description string: <algorithm class> [<argument description> ...]\n\t...<constructor description>
        StringBuffer rtv = new StringBuffer(constructor.getDeclaringClass().getName());
        for (String arg : annotation.arguments())
            rtv.append(" <").append(arg).append(">");
        rtv.append("\n\t...").append(annotation.description());
        
        return rtv.toString();
    }

}