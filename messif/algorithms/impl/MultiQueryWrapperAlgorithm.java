package messif.algorithms.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.QueryOperation;
import messif.operations.RankingMultiQueryOperation;

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
    private final Class<? extends QueryOperation<?>> singleQueryOperationClass;
    /** Additional parameters for the single-object query operation */
    private final Object[] operationParameters;

    /**
     * Creates a new multi-object query wrapper algorithm.
     * @param algorithm the encapsulated algorithm that handles regular queries
     * @param singleQueryOperationClass the single-object query operation class that is used while evaluating the multi-object query
     * @param operationParameters the additional parameters (starting from the second one, the first one is always the query object)
     *          for the single-object query operation
     */
    public MultiQueryWrapperAlgorithm(Algorithm algorithm, Class<? extends QueryOperation<?>> singleQueryOperationClass, Object... operationParameters) throws IllegalArgumentException {
        super("Multi-query wrapper on " + algorithm.getName());
        this.algorithm = algorithm;
        this.singleQueryOperationClass = singleQueryOperationClass;
        // Copy operation parametes and make space for the query object parameter (first argument)
        if (operationParameters == null) {
            this.operationParameters = new Object[1];
        } else {
            this.operationParameters = new Object[operationParameters.length + 1];
            System.arraycopy(operationParameters, 0, this.operationParameters, 1, operationParameters.length);
        }
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
     */
    public synchronized void processMultiObjectOperation(RankingMultiQueryOperation op) throws AlgorithmMethodException, NoSuchMethodException, InvocationTargetException, InterruptedException {
        for (LocalAbstractObject queryObject : op.getQueryObjects()) {
            Object[] params = operationParameters.clone();
            params[0] = queryObject;
            algorithm.backgroundExecuteOperation(AbstractOperation.createOperation(singleQueryOperationClass, params));
        }
        for (QueryOperation<?> executedOperation : algorithm.waitBackgroundExecuteOperation(singleQueryOperationClass)) {
            for (Iterator<AbstractObject> it = executedOperation.getAnswerObjects(); it.hasNext();) {
                op.addToAnswer((LocalAbstractObject)it.next());
            }
        }
        op.endOperation();
    }

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
