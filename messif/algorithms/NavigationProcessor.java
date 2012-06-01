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

import java.io.Closeable;
import messif.operations.AbstractOperation;

/**
 * Interface for processing operations via {@link NavigationDirectory}.
 * The instance of {@link NavigationProcessor} is first obtained on a given {@link NavigationDirectory}
 * via {@link NavigationDirectory#getNavigationProcessor(messif.operations.AbstractOperation) getNavigationProcessor}
 * method for a given {@link AbstractOperation operation} instance.
 * The operation then can be processed iteratively by calling {@link #processNext()}
 * until there are no more directory items available. This typically means that
 * the navigation directory provides candidates where the operation
 * should be processed which are either computed before the processing (i.e. the
 * list of buckets to visit) or dynamically when the next processing
 * is requested.
 * 
 * <p>
 * The steps of the navigation processor may be independent and thus executed
 * in parallel. Every implementation of the {@link NavigationProcessor} should
 * state the conditions under which it can be executed in multiple threads.
 * </p>
 * 
 * <p>
 * The {@link #close() } method should be called after processing all the steps.
 * </p>
 * 
 * @param <O> the type of the operation that are processed by this navigator processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface NavigationProcessor<O extends AbstractOperation> extends Closeable {
    /**
     * Returns the operation for which this navigator was created.
     * @return the operation for which this navigator was created
     */
    public O getOperation();

    /**
     * Returns whether there are more processing steps available.
     * @return <tt>true</tt> if additional processing via {@link #processNext()} is possible
     */
    public boolean hasProcessNext();

    /**
     * Processes the encapsulated operation by the next processing step.
     * The returned value depends on the context of the processor,
     * it can be the number of objects inserted into or deleted from the algorithm,
     * the number of objects added to the answer of a query operation, etc.
     * 
     * @return the number of objects affected
     */
    public int processNext() throws AlgorithmMethodException;

    /**
     * Returns the number of processing steps already evaluated by this processor.
     * @return the number of finished processing steps
     */
    public int getProcessedCount();

    /**
     * Returns the number of the remaining processing steps.
     * If the count cannot be determined, a negative number is returned.
     * Note that this number may not be precise and change over time.
     * 
     * @return the number of the remaining processing steps
     */
    public int getRemainingCount();

}
