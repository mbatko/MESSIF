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
package messif.statistics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import messif.operations.AbstractOperation;

/**
 * Implementation of the {@link FutureWithStatistics}.
 * The instance is created and then the executed future is set
 *
 * @param <T> the result type returned by this Future's <tt>get</tt> method
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class FutureWithStatisticsImpl<T> implements FutureWithStatistics<T> {
    /** Internal list of statistics gathered during the computation */
    private final List<Statistics<?>> statistics = new ArrayList<Statistics<?>>();
    /** Encapsulated future object that is used for delegation of the waiting methods */
    private Future<T> future;
    /** Value computed without executor service */
    private T value;
    /** Exception thrown when computed without executor service */
    private Exception exception;

    /**
     * Sets the encapsulated future object.
     * @param future the encapsulated future object
     */
    protected final void setFuture(Future<T> future) {
        if (this.future != null)
            throw new IllegalStateException("Cannot set future twice");
        if (this.value != null || this.exception != null)
            throw new IllegalStateException("Cannot set future: the callable has already been set");
        this.future = future;
    }

    /**
     * Sets the callable for immediate execution.
     * @param callable the callable for immediate execution
     */
    protected final void setCallable(Callable<T> callable) {
        if (this.value != null || this.exception != null)
            throw new IllegalStateException("Cannot set callable twice");
        if (this.future != null)
            throw new IllegalStateException("Cannot set callable: the future has already been set");
        try {
            this.value = callable.call();
        } catch (Exception e) {
            this.exception = e;
        }
    }

    /**
     * Collect the gathered statistics.
     * @param stats the gathered statistics
     */
    protected final void addStats(Iterable<Statistics<?>> stats) {
        for (Statistics<?> stat : stats) {
            statistics.add(stat);
        }
    }

    /**
     * Executes the given callable and collects the {@link OperationStatistics}.
     * @param <T> the type of value returned by the given callable
     * @param executorService the executor used to run the callable
     * @param callable the callable to run
     * @return the future object that can be used to wait for the results
     * @throws RejectedExecutionException if the callable was no accepted by the executor service
     * @throws NullPointerException if the callable was null
     */
    public static <T extends AbstractOperation> FutureWithStatistics<T> submit(ExecutorService executorService, final Callable<T> callable) throws RejectedExecutionException, NullPointerException {
        final FutureWithStatisticsImpl<T> ret = new FutureWithStatisticsImpl<T>();
        if (executorService == null) {
            ret.setCallable(callable);
        } else {
            ret.setFuture(executorService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    OperationStatistics.resetLocalThreadStatistics();
                    T value = callable.call();
                    ret.addStats(OperationStatistics.getLocalThreadStatistics());
                    return value;
                }
            }));
        }
        return ret;
    }

    @Override
    public Iterator<Statistics<?>> iterator() {
        return statistics.iterator();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (future == null)
            return false;
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        if (future == null)
            return false;
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        if (future == null)
            return true;
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (future != null)
            return future.get();
        if (exception != null)
            throw new ExecutionException(exception);
        return value;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (future != null)
            return future.get(timeout, unit);
        if (exception != null)
            throw new ExecutionException(exception);
        return value;
    }
}
