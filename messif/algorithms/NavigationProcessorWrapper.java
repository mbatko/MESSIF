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

import messif.operations.AbstractOperation;

/**
 * Wrapper class for any {@link NavigationProcessor} that delegates the methods
 * to an encapsulated  {@link NavigationProcessor}.
 * 
 * @param <O> the type of the operation that are processed by this navigator processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class NavigationProcessorWrapper<O extends AbstractOperation> implements NavigationProcessor<O> {
    /** Encapsulated navigation processor */
    protected final NavigationProcessor<O> navigationProcessor;

    /**
     * Creates a new wrapper for the given navigation processor.
     * @param navigationProcessor the processor to wrap
     */
    public NavigationProcessorWrapper(NavigationProcessor<O> navigationProcessor) {
        this.navigationProcessor = navigationProcessor;
    }

    @Override
    public boolean processStep() throws InterruptedException, AlgorithmMethodException, CloneNotSupportedException {
        return navigationProcessor.processStep();
    }

    @Override
    public boolean isFinished() {
        return navigationProcessor.isFinished();
    }

    @Override
    public int getRemainingCount() {
        return navigationProcessor.getRemainingCount();
    }

    @Override
    public int getProcessedCount() {
        return navigationProcessor.getProcessedCount();
    }

    @Override
    public O getOperation() {
        return navigationProcessor.getOperation();
    }

    @Override
    public void close() {
        navigationProcessor.close();
    }
}
