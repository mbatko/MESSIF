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

import java.util.concurrent.Callable;
import messif.operations.AbstractOperation;

/**
 * Extension of the {@link NavigationProcessor} that supports asynchronous execution
 * of the processing steps via {@link Callable}s. If this interface is implemented,
 * each step of the processor must be independent and thread-safe so that
 * any parallelization can be used to process the steps. Synchronization can be used
 * when implementing by blocking the {@link #processStepAsynchronously()} method.
 * 
 * <p>
 * Implementation of this interface must ensure equivalency of the thread processing
 * by calling {@link #processStepAsynchronously()} and sequential processing by calling
 * {@link #processStep()}.
 * </p>
 * 
 * @param <O> the type of the operation that are processed by this navigator processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public interface AsynchronousNavigationProcessor<O extends AbstractOperation> extends NavigationProcessor<O> {
    /**
     * Returns a {@link Callable} that allows to execute the next processing step asynchronously.
     * Note that this method may block if necessary.
     * @return a {@link Callable} for next step or <tt>null</tt> if there are no more steps
     * @throws InterruptedException if the processing thread was interrupted
     */
    public Callable<O> processStepAsynchronously() throws InterruptedException;
}
