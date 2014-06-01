package messif.algorithms.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingMultiQueryOperation;
import messif.statistics.FutureWithStatistics;
import messif.utility.reflection.Instantiators;

/**
 * Wrapper algorithm that processes {@link RankingMultiQueryOperation}s by executing
 * multiple single-object queries and add them to the result.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class MultiQueryWrapperAlgorithm extends Algorithm {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;

    /** Encapsulated algorithm that handles regular queries */
    private final Algorithm algorithm;
    /** Class of the single-object query operation class that is used while evaluating the multi-object query */
    private final Constructor<? extends QueryOperation<?>> queryOperationConstructor;
    /** Additional parameters for the single-object query operation */
    private final Object[] queryOperationArguments;

    /**
     * Creates a new multi-object query wrapper algorithm.
     * @param algorithm the encapsulated algorithm that handles regular queries
     * @param singleQueryOperationClass the single-object query operation class that is used while evaluating the multi-object query
     * @param operationParameters the additional parameters (starting from the second one, the first one is always the query object)
     *          for the single-object query operation
     * @throws NoSuchMethodException  if the single-query operation constructor was not found for the given number of arguments
     */
    @AlgorithmConstructor(description = "creates a multi-query wrapper for the given algorithm", arguments = {"algorithm to encapsulate", "query to run at the algorithm", "parameters of the query (except for the query object)..."})
    public MultiQueryWrapperAlgorithm(Algorithm algorithm, Class<? extends QueryOperation<?>> singleQueryOperationClass, String... operationParameters) throws IllegalArgumentException, NoSuchMethodException {
        super("Multi-query wrapper on " + algorithm.getName());
        this.algorithm = algorithm;
        this.queryOperationConstructor = AbstractOperation.getAnnotatedConstructor(singleQueryOperationClass, operationParameters == null ? 1 : operationParameters.length + 1);
        Class<?>[] operationPrototype = queryOperationConstructor.getParameterTypes();
        this.queryOperationArguments = new Object[operationPrototype.length];
        if (operationParameters != null)
            System.arraycopy(operationParameters, 0, this.queryOperationArguments, 1, operationParameters.length);
        String error = Instantiators.isPrototypeMatching(operationPrototype, this.queryOperationArguments, true, null);
        if (error != null)
            throw new IllegalArgumentException(error);
        setOperationsThreadPool(Executors.newCachedThreadPool());
    }

    @Override
    @SuppressWarnings({"FinalizeNotProtected", "FinalizeCalledExplicitly"})
    public void finalize() throws Throwable {
        algorithm.finalize();
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy() throws Throwable {
        algorithm.destroy();
        super.destroy(); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Returns the encapsulated algorithm.
     * @return the encapsulated algorithm
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Implementation of multi-object query operation.
     *
     * @param op the generic operation to execute
     * @throws AlgorithmMethodException if the operation execution on the encapsulated algorithm has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported by the encapsulated algorithm
     * @throws InvocationTargetException if the specified operation cannot be created for the given parameters
     * @throws InterruptedException if the computation has been interrupted while processing
     * @throws InstantiationException if the single-query operation cannot be created
     * @throws IllegalAccessException if the single-query operation constructors is not accessible
     */
    public synchronized void processMultiObjectOperation(RankingMultiQueryOperation op) throws AlgorithmMethodException, NoSuchMethodException, InvocationTargetException, InterruptedException, InstantiationException, IllegalAccessException {
        Collection<? extends LocalAbstractObject> queryObjects = op.getQueryObjects();
        List<FutureWithStatistics<? extends QueryOperation<?>>> futures = new ArrayList<FutureWithStatistics<? extends QueryOperation<?>>>(queryObjects.size());
        for (LocalAbstractObject queryObject : queryObjects) {
            Object[] params = queryOperationArguments.clone();
            params[0] = queryObject;
            futures.add(algorithm.backgroundExecuteOperationWithStatistics(queryOperationConstructor.newInstance(params)));
        }

        op.setAnswerIgnoringDuplicates(true);
        for (Future<? extends QueryOperation<?>> future : futures) {
            for (Iterator<AbstractObject> it = Algorithm.waitBackgroundExecution(future).getAnswerObjects(); it.hasNext();) {
                op.addToAnswer((LocalAbstractObject)it.next());
            }
        }
        op.endOperation();
    }

    /**
     * Implementation of multi-object query operation.
     *
     * @param op the generic operation to execute
     * @throws AlgorithmMethodException if the operation execution on the encapsulated algorithm has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported by the encapsulated algorithm
     * @throws InvocationTargetException if the specified operation cannot be created for the given parameters
     * @throws InterruptedException if the computation has been interrupted while processing
     * @throws InstantiationException if the single-query operation cannot be created
     * @throws IllegalAccessException if the single-query operation constructors is not accessible
     */
//    public synchronized void processMultiObjectOperation(RankingMultiQueryOperation op) throws AlgorithmMethodException, NoSuchMethodException, InvocationTargetException, InterruptedException, InstantiationException, IllegalAccessException {
//        for (LocalAbstractObject queryObject : op.getQueryObjects()) {
//            Object[] params = queryOperationArguments.clone();
//            params[0] = queryObject;
//            QueryOperation<?> executedOperation = algorithm.executeOperation(queryOperationConstructor.newInstance(params));
//            for (Iterator<AbstractObject> it = executedOperation.getAnswerObjects(); it.hasNext();) {
//                op.addToAnswer((LocalAbstractObject)it.next());
//            }
//        }
//        op.endOperation();
//    }

    /**
     * Implementation of a generic operation.
     * The the operation is passed the encapsulated algorithm for processing.
     * @param op the generic operation to execute
     * @throws AlgorithmMethodException if the operation execution on the encapsulated algorithm has thrown an exception
     * @throws NoSuchMethodException if the operation is unsupported by the encapsulated algorithm
     */
    public void processOperation(AbstractOperation op) throws AlgorithmMethodException, NoSuchMethodException {
        AbstractOperation executedOp = algorithm.executeOperation(op);
        if (op != executedOp) // Instance check is correct
            op.updateFrom(executedOp);
    }
}
