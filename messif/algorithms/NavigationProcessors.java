/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import messif.operations.AbstractOperation;

/**
 * Collection of utility methods for {@link NavigationProcessor}s and {@link NavigationDirectory}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public abstract class NavigationProcessors {

    /**
     * Returns the {@link NavigationDirectory#getNavigationProcessor(messif.operations.AbstractOperation)}
     * with type casts. If the navigationDirectory is not instance of {@link NavigationDirectory} or
     * the operation is not compatible, <tt>null</tt> is returned.
     * 
     * @param navigationDirectory an instance of {@link NavigationDirectory} as plain {@link Object}
     * @param operation an instance of operation compatible with the {@link NavigationDirectory}
     * @return the result of {@link NavigationDirectory#getNavigationProcessor(messif.operations.AbstractOperation)}
     *          or <tt>null</tt> if the objects are not compatible
     */
    @SuppressWarnings("unchecked")
    public static NavigationProcessor<?> getNavigationProcessor(Object navigationDirectory, AbstractOperation operation) {
        if (!(navigationDirectory instanceof NavigationDirectory))
            return null;
        try {
            return ((NavigationDirectory<AbstractOperation>)navigationDirectory).getNavigationProcessor(operation);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Executes a given {@link AsynchronousNavigationProcessor} using {@link ExecutorService}.
     * The navigation processor will be asked for all the asynchronous
     * {@link AsynchronousNavigationProcessor#processStepAsynchronously() processing steps}
     * that are submitted one-by-one to the executor. One no more processing steps are available,
     * this method will wait for all the started processing steps to finish.
     * 
     * <p>Note that the processor can block on providing additional processing steps
     * until some currently running steps are finished.</p>
     * 
     * @param <O> the type of operation processed by the processor
     * @param executor the executor service that provides the worker threads (depends on the executor implementation)
     * @param processor the asynchronous navigation processor that provides the processing steps
     * @throws InterruptedException if the processing thread is interrupted during the processing
     * @throws AlgorithmMethodException if there was an error during the processing
     */
    public static <O extends AbstractOperation> void execute(ExecutorService executor, AsynchronousNavigationProcessor<? extends O> processor) throws InterruptedException, AlgorithmMethodException {
        List<Future<? extends O>> futures = new ArrayList<Future<? extends O>>();
        Callable<? extends O> callable = processor.processStepAsynchronously();
        while (callable != null) {
            futures.add(executor.submit(callable));
            callable = processor.processStepAsynchronously();
        }
        try {
            for (Iterator<Future<? extends O>> it = futures.iterator(); it.hasNext();)
                it.next().get();
        } catch (ExecutionException e) {
            throw new AlgorithmMethodException(e.getCause());
        } finally {
            processor.close();
        }
    }

    /**
     * Executes a given {@link NavigationProcessor} either asynchronously using {@link ExecutorService}
     * if the processor implements {@link AsynchronousNavigationProcessor} or sequentially.
     * 
     * @param <O> the type of operation processed by the processor
     * @param executor the executor service that provides the worker threads for asynchronous execution
     * @param processor the asynchronous navigation processor that provides the processing steps
     * @throws InterruptedException if the processing thread is interrupted during the processing
     * @throws AlgorithmMethodException if there was an error during the processing
     */
    @SuppressWarnings("empty-statement")
    public static <O extends AbstractOperation> void execute(ExecutorService executor, NavigationProcessor<O> processor) throws InterruptedException, AlgorithmMethodException {
        if (executor != null && processor instanceof AsynchronousNavigationProcessor) {
            execute(executor, (AsynchronousNavigationProcessor<O>)processor);
        } else {
            try {
                while (processor.processStep()); // This empty body is intended
            } finally {
                processor.close();
            }
        }
    }

    /**
     * Executes a given {@code operation} by the processor provided by {@link NavigationDirectory}.
     * If the passed objects are not instances of {@link NavigationDirectory} or {@link AbstractOperation},
     * or the directory does not provide processor for the given operation, <tt>false</tt> is returned
     * and no processing is done. Otherwise, the sequential or asynchronous processing
     * is executed for the given operation.
     * 
     * @param executor the executor service that provides the worker threads for asynchronous execution
     * @param navigationDirectory an instance of {@link NavigationDirectory} as plain {@link Object}
     * @param operation an instance of {@link AbstractOperation} as plain {@link Object} compatible with the given {@link NavigationDirectory}
     * @return <tt>true</tt> if the {@code operation} was processed using the {@code navigationDirectory} or
     *      <tt>false</tt> if no processing was performed
     * @throws InterruptedException if the processing thread is interrupted during the processing
     * @throws AlgorithmMethodException if there was an error during the processing
     */
    public static boolean executeWithCast(ExecutorService executor, Object navigationDirectory, Object operation) throws InterruptedException, AlgorithmMethodException {
        if (!(operation instanceof AbstractOperation))
            return false;
        NavigationProcessor<? extends AbstractOperation> navigationProcessor = getNavigationProcessor(navigationDirectory, (AbstractOperation)operation);
        if (navigationProcessor == null)
            return false;
        execute(executor, navigationProcessor);
        return true;
    }

}
