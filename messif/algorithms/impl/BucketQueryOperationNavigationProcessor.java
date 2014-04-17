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
import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.NavigationProcessor;
import messif.buckets.Bucket;
import messif.operations.QueryOperation;

/**
 * Implementation of {@link NavigationProcessor} that processes any {@link QueryOperation}
 * on a set of {@link Bucket}s.
 * The buckets where the operation should be processed is provided via constructor.
 * 
 * <h4>Parallelism</h4>
 * The implementation is thread-safe, if operation cloning is enabled. Otherwise,
 * the operation answer needs to be synchronized.
 * 
 * @param <O> the type of the operation that are processed by this navigation processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class BucketQueryOperationNavigationProcessor<O extends QueryOperation<?>> extends AbstractNavigationProcessor<O, Bucket> {

    /**
     * Create a new bucket navigation processor.
     * The processor is {@link #isQueueClosed() closed} and contains only the specified buckets.
     * No additional buckets can be added.
     * @param operation the operation to process
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     * @param buckets the buckets on which to process
     */
    public BucketQueryOperationNavigationProcessor(O operation, boolean cloneAsynchronousOperation, Collection<? extends Bucket> buckets) {
        super(operation, cloneAsynchronousOperation, buckets);
    }

    /**
     * Create a new bucket navigation processor.
     * The processor does not contain any buckets and the processing
     * will block in method {@link #processStep()}. Additional buckets must be
     * added via {@link #addProcessingItem} methods and then {@link #queueClose() closed}
     * in order to be able to finish the processing.
     * 
     * @param operation the operation to process
     * @param cloneAsynchronousOperation the flag whether to clone the operation for asynchronous processing
     */
    public BucketQueryOperationNavigationProcessor(O operation, boolean cloneAsynchronousOperation) {
        super(operation, cloneAsynchronousOperation);
    }

    @Override
    protected O processItem(O operation, Bucket processingItem) throws AlgorithmMethodException {
        processingItem.processQuery(operation);
        return operation;
    }

}
