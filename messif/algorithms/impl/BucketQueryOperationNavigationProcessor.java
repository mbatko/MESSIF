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
import java.util.Collections;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
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
 * @param <O> the type of the operation that are processed by this navigator processor
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class BucketQueryOperationNavigationProcessor<O extends QueryOperation<?>> implements NavigationProcessor<O> {
    /** Operation for which this navigator was created */
    private final O operation;
    /** Internal queue of buckets to process */
    private final Queue<Bucket> buckets;
    /** Number of buckets processed so far */
    private int processed;
    /** Flag whether the queue is closed, if <tt>false</tt>, i.e. getting a next element from the queue blocks and waits for the queue to fill */
    private boolean queueClosed;

    /**
     * Create a new bucket navigation processor.
     * The processor is {@link #isClosed() closed} and contains only the specified buckets.
     * No additional buckets can be added.
     * @param operation the operation to process
     * @param buckets the buckets on which to process
     */
    public BucketQueryOperationNavigationProcessor(O operation, Collection<Bucket> buckets) {
        this.operation = operation;
        this.buckets = new LinkedList<Bucket>(buckets);
        this.queueClosed = true;
    }

    /**
     * Create a new bucket navigation processor.
     * The processor does not contain any buckets and the processing
     * will block in method {@link #processNext()}. Additional buckets must be
     * added via {@link #addBucket} methods and then {@link #close() closed}
     * in order to be able to finish the processing.
     * 
     * @param operation the operation to process
     */
    public BucketQueryOperationNavigationProcessor(O operation) {
        this.operation = operation;
        this.buckets = new LinkedList<Bucket>();
        this.queueClosed = false;
    }

    /**
     * Adds a collection of buckets to this processor.
     * Note that buckets can be added only if this processor is not {@link #isClosed() closed}.
     * 
     * @param buckets the collection of buckets to add
     * @throws IllegalStateException if this processor is already {@link #isClosed() closed}.
     */
    public void addBuckets(Collection<Bucket> buckets) throws IllegalStateException {
        synchronized (this.buckets) {
            if (queueClosed)
                throw new IllegalStateException();
            this.buckets.addAll(buckets);
            this.buckets.notifyAll();
        }
    }

    /**
     * Adds a bucket to this processor.
     * Note that bucket can be added only if this processor is not {@link #isClosed() closed}.
     * 
     * @param bucket the bucket to add
     * @throws IllegalStateException if this processor is already {@link #isClosed() closed}.
     */
    public void addBucket(Bucket bucket) throws IllegalStateException {
        addBuckets(Collections.singletonList(bucket));
    }

    /**
     * Closes this processor.
     * That means that no additional buckets can be added and the {@link #processNext()}
     * method will block and wait for additional buckets.
     * 
     * @throws IllegalStateException if this processor is already {@link #isClosed() closed}.
     */
    public void close() throws IllegalStateException {
        synchronized (buckets) {
            if (queueClosed)
                throw new IllegalStateException();
            queueClosed = true;
            buckets.notifyAll();
        }
    }

    /**
     * Returns whether additional buckets can be added to this processor (<tt>false</tt>)
     * or this processor is closed (<tt>true</tt>).
     * @return <tt>true</tt> if this processor is closed and no additional buckets can be added for processing
     */
    public boolean isClosed() {
        return queueClosed;
    }

    @Override
    public O getOperation() {
        return operation;
    }

    @Override
    public boolean hasProcessNext() {
        return !queueClosed || !buckets.isEmpty();
    }

    @Override
    public int processNext() {
        Bucket bucket;
        synchronized (buckets) {
            bucket = buckets.poll();
            while (bucket == null) {
                if (queueClosed)
                    throw new NoSuchElementException();
                try {
                    buckets.wait();
                    bucket = buckets.poll();
                } catch (InterruptedException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
            }
        }
        int ret = bucket.processQuery(operation);
        processed++;
        return ret;
    }

    @Override
    public int getProcessedCount() {
        return processed;
    }

    @Override
    public int getRemainingCount() {
        return buckets.size();
    }
    
}
