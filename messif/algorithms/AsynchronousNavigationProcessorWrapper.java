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
 * Wrapper class for any {@link AsynchronousNavigationProcessor} that delegates the methods
 * to an encapsulated  {@link AsynchronousNavigationProcessor}.
 * 
 * @param <O> the type of the operation that are processed by this asynchronous navigator processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class AsynchronousNavigationProcessorWrapper<O extends AbstractOperation> extends NavigationProcessorWrapper<O> implements AsynchronousNavigationProcessor<O> {
    /**
     * Creates a new wrapper for the given asynchronous navigation processor.
     * @param asynchronousNavigationProcessor the processor to wrap
     */
    public AsynchronousNavigationProcessorWrapper(AsynchronousNavigationProcessor<O> asynchronousNavigationProcessor) {
        super(asynchronousNavigationProcessor);
    }

    @Override
    public Callable<O> processStepAsynchronously() throws InterruptedException {
        return ((AsynchronousNavigationProcessor<O>)navigationProcessor).processStepAsynchronously();
    }

}
