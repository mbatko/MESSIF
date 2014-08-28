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
package messif.algorithms.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.NavigationDirectory;
import messif.algorithms.NavigationProcessor;
import messif.operations.AbstractOperation;

/**
 * Indexing algorithm that processes operation by passing them to encapsulated collection
 * of algorithms.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MultipleOverlaysAlgorithm extends Algorithm implements NavigationDirectory<AbstractOperation> {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** Internal list of algorithms on which the operations are actually processed */
    private final List<Algorithm> algorithms;
    /** Flag whether to clone the operation for asynchronous processing */
    private final boolean cloneAsynchronousOperation;

    /**
     * Creates a new multi-algorithm overlay for the given collection of algorithms.
     * @param algName string name of this algorithm to be used instead of the standard string
     * @param algorithms the algorithms on which the operations are processed
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     */
    public MultipleOverlaysAlgorithm(String algName, Collection<? extends Algorithm> algorithms, boolean cloneAsynchronousOperation) {
        super(algName);
        this.algorithms = new ArrayList<Algorithm>(algorithms);
        this.cloneAsynchronousOperation = cloneAsynchronousOperation;
    }

    /**
     * Creates a new multi-algorithm overlay for the given collection of algorithms.
     * @param algorithms the algorithms on which the operations are processed
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     */
    public MultipleOverlaysAlgorithm(Collection<? extends Algorithm> algorithms, boolean cloneAsynchronousOperation) {
        this("Multiple overlay algorithm on: " + algorithms.size() + " algorithms", algorithms, cloneAsynchronousOperation);
    }
    
    /**
     * Creates a new multi-algorithm overlay for the given collection of algorithms.
     * @param algorithms the algorithms on which the operations are processed
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     */
    @Algorithm.AlgorithmConstructor(description = "Constructor with created algorithms", arguments = {"array of running algorithms", "t/f if operation should be clonned before running"})
    public MultipleOverlaysAlgorithm(Algorithm[] algorithms, boolean cloneAsynchronousOperation) {
        this(Arrays.asList(algorithms), cloneAsynchronousOperation);
    }

    @Override
    @SuppressWarnings({"FinalizeDeclaration", "FinalizeCalledExplicitly"})
    public void finalize() throws Throwable {
        for (Algorithm algorithm : algorithms)
            algorithm.finalize();
        super.finalize();
    }

    /**
     * The provided thread pool is set on the given encapsulated algorithm and also to all
     *  the encapsulated algorithms: see 
     *  {@link  Algorithm#setOperationsThreadPool(java.util.concurrent.ExecutorService)} for details.
     * @param algorithmIndex 
     * @param operationsThreadPool new thread pool (or null)
     */
    public void setOperationsThreadPool(int algorithmIndex, ExecutorService operationsThreadPool) {
        if ((algorithmIndex >= algorithms.size()) || (algorithmIndex < 0)) {
            log.log(Level.WARNING, "trying to set thread pool for a non-existing sub-algorithm index ''{0}''", algorithmIndex);
            return;
        }
        algorithms.get(algorithmIndex).setOperationsThreadPool(operationsThreadPool);
    }

    /**
     * If the sub-algorithms don't have their own thread pool, the top algorithm shares its thread pool with them.
     * @param operationsThreadPool 
     */
    @Override
    public void setOperationsThreadPool(ExecutorService operationsThreadPool) {
        super.setOperationsThreadPool(operationsThreadPool);
//        for (Algorithm algorithm : algorithms) {
//            if (algorithm.getOperationsThreadPool() == null) {
//                algorithm.setOperationsThreadPool(operationsThreadPool);
//            }
//        }
    }
    
    @Override
    public NavigationProcessor<? extends AbstractOperation> getNavigationProcessor(AbstractOperation operation) {
        return new AbstractNavigationProcessor<AbstractOperation, Algorithm>(operation, cloneAsynchronousOperation, algorithms) {
            @Override
            protected AbstractOperation processItem(AbstractOperation operation, Algorithm algorithm) throws AlgorithmMethodException {
                try {
                    return algorithm.executeOperation(operation);
                } catch (NoSuchMethodException e) {
                    throw new AlgorithmMethodException(e);
                }
            }
        };
    }

    /**
     * Returns the number of the currently encapsulated algorithms.
     * @return the number of the currently encapsulated algorithms
     */
    public int getAlgorithmsCount() {
        return algorithms.size();
    }

    /**
     * Returns all the currently encapsulated algorithms.
     * @return all the currently encapsulated algorithms
     */
    protected Collection<Algorithm> getAlgorithms() {
        return Collections.unmodifiableCollection(algorithms);
    }

    @Override
    public String toString() {
        return getName();
    }
}
