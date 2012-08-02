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
package messif.algorithms.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.AsynchronousNavigationProcessor;
import messif.algorithms.NavigationProcessor;
import messif.operations.AbstractOperation;

/**
 * Basic implementation of {@link NavigationProcessor} that processes any {@link AbstractOperation}
 * on a set of processing items (e.g. buckets).
 * 
 * <h4>Parallelism</h4>
 * The implementation is thread-safe, if operation cloning is enabled. Otherwise,
 * the operation answer needs to be synchronized.
 * 
 * @param <O> the type of the operation that are processed by this navigation processor
 * @param <T> the type of processing items used by this navigation processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public abstract class AbstractNavigationProcessor<O extends AbstractOperation, T> implements AsynchronousNavigationProcessor<O> {
    /** Operation for which this navigation processor was created */
    private final O operation;
    /** Flag whether to clone the operation for asynchronous processing */
    private final boolean cloneAsynchronousOperation;
    /** Internal queue of items used for processing */
    private final Queue<T> processingItems;
    /** Number of items processed so far */
    private volatile int processed;
    /** Flag whether the queue is closed, if <tt>false</tt>, i.e. getting a next element from the queue blocks and waits for the queue to fill */
    private boolean queueClosed;

    /**
     * Create a new navigation processor with a given queue instance.
     * The processor is {@link #isClosed() closed} and contains only the specified processing items.
     * No additional processing items can be added.
     * @param operation the operation to process
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     * @param processingItems the processing items queue
     * @param queueClosed the flag whether the given queue is closed, if <tt>false</tt>, i.e. getting a next element from the queue blocks and waits for the queue to fill
     */
    protected AbstractNavigationProcessor(O operation, boolean cloneAsynchronousOperation, Queue<T> processingItems, boolean queueClosed) {
        this.operation = operation;
        this.cloneAsynchronousOperation = cloneAsynchronousOperation;
        this.processingItems = processingItems;
        this.queueClosed = queueClosed;
    }

    /**
     * Create a new navigation processor.
     * The processor is {@link #isClosed() closed} and contains only the specified processing items.
     * No additional processing items can be added.
     * @param operation the operation to process
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     * @param processingItems the processing items for the operation
     */
    public AbstractNavigationProcessor(O operation, boolean cloneAsynchronousOperation, Collection<? extends T> processingItems) {
        this.operation = operation;
        this.cloneAsynchronousOperation = cloneAsynchronousOperation;
        this.processingItems = new LinkedList<T>(processingItems);
        this.queueClosed = true;
    }

    /**
     * Create a new navigation processor.
     * The processor does not contain any processing items and the processing
     * will block in method {@link #processNext()}. Additional processing items
     * must be added via {@link #addProcessingItem} methods and then {@link #close() closed}
     * in order to be able to finish the processing.
     * 
     * @param operation the operation to process
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     */
    public AbstractNavigationProcessor(O operation, boolean cloneAsynchronousOperation) {
        this.operation = operation;
        this.cloneAsynchronousOperation = cloneAsynchronousOperation;
        this.processingItems = new LinkedList<T>();
        this.queueClosed = false;
    }

    /**
     * Adds a collection of processing items to this processor.
     * Note that processing items can be added only if this processor is not {@link #isClosed() closed}.
     * 
     * @param processingItems the collection of processing items to add
     * @throws IllegalStateException if this processor is already {@link #isClosed() closed}.
     */
    public final void addProcessingItems(Collection<? extends T> processingItems) throws IllegalStateException {
        for (T processingItem : processingItems)
            addProcessingItem(processingItem);
    }

    /**
     * Adds a processing item to this processor.
     * Note that processing item can be added only if this processor is not {@link #isClosed() closed}.
     * 
     * @param processingItem the processing item to add
     * @return returns the added processing item
     * @throws IllegalStateException if this processor is already {@link #isClosed() closed}.
     */
    public synchronized T addProcessingItem(T processingItem) throws IllegalStateException {
        if (queueClosed)
            throw new IllegalStateException();
        processingItems.add(processingItem);
        notify();
        return processingItem;
    }

    /**
     * Closes this processor.
     * That means that no additional processing items can be added and the {@link #processNext()}
     * method will no longer block and wait for additional processing items.
     * 
     * @throws IllegalStateException if this processor is already {@link #isClosed() closed}.
     */
    public synchronized void closeQueue() throws IllegalStateException {
        if (queueClosed)
            throw new IllegalStateException();
        queueClosed = true;
        notifyAll();
    }

    @Override
    public void close() {
        abort();
    }

//    @Override
    public synchronized void abort() throws IllegalStateException {
        queueClosed = true;
        processingItems.clear();
        notifyAll();
    }

    /**
     * Returns whether additional processing items can be added to this processor (<tt>false</tt>)
     * or this processor is closed (<tt>true</tt>).
     * @return <tt>true</tt> if this processor is closed and no additional processing items can be added for processing
     */
    public boolean isClosed() {
        return queueClosed;
    }

    @Override
    public O getOperation() {
        return operation;
    }

    @Override
    public boolean isFinished() {
        return queueClosed && processingItems.isEmpty();
    }

    @Override
    public int getProcessedCount() {
        return processed;
    }

    @Override
    public int getRemainingCount() {
        return processingItems.size();
    }

    /**
     * Returns the next processing item in the queue.
     * @return the next processing item in the queue or <tt>null</tt> if the queue is empty
     * @throws InterruptedException if the waiting for the next item in the queue has been interrupted
     */
    protected synchronized T getNextProcessingItem() throws InterruptedException {
        T processingItem = processingItems.poll();
        while (processingItem == null) {
            if (queueClosed)
                return null;
            wait();
            processingItem = processingItems.poll();
        }
        return processingItem;
    }

    /**
     * Processes the encapsulated operation using the given processing item.
     * 
     * @param operation the operation that is to be processed
     * @param processingItem the processing item using which to process the operation
     * @throws AlgorithmMethodException if an error occurred during the evaluation of the processing step
     */
    protected abstract void processItem(O operation, T processingItem) throws AlgorithmMethodException;

    @Override
    public final boolean processStep() throws InterruptedException, AlgorithmMethodException {
        T processingItem = getNextProcessingItem();
        if (processingItem == null)
            return false;
        processItem(operation, processingItem);
        return true;
    }

    @Override
    public final Callable<O> processStepAsynchronously() throws InterruptedException {
        final T processingItem = getNextProcessingItem();
        if (processingItem == null)
            return null;
        return new Callable<O>() {
            @Override
            public O call() throws InterruptedException, CloneNotSupportedException, AlgorithmMethodException {
                if (cloneAsynchronousOperation) {
                    @SuppressWarnings("unchecked")
                    O clonedOperation = (O)operation.clone(); // This cast IS safe, since this is cloning
                    processItem(clonedOperation, processingItem);
                    processed++;
                    synchronized (operation) {
                        operation.updateFrom(clonedOperation);
                    }
                } else {
                    processItem(operation, processingItem);
                    processed++;
                }
                return operation;
            }
        };
    }

}
