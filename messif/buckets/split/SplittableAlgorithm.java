/*
 * SplittableAlgorithm.java
 *
 * Created on 12. listopad 2007, 10:40
 *
 */

package messif.buckets.split;

import java.util.Collection;
import messif.algorithms.Algorithm;
import messif.buckets.BucketStorageException;
import messif.buckets.CapacityFullException;
import messif.buckets.FilterRejectException;
import messif.buckets.OccupationLowException;
import messif.objects.LocalAbstractObject;


/**
 * Implement this interface on an {@link messif.algorithms.Algorithm Algorithm}
 * if it supports "clever" splitting. If this interface is not implemented for an algorithm,
 * the standard way through {@link messif.operations.GetAllObjectsQueryOperation get all objects} and 
 * {@link messif.operations.DeleteOperation delete} operations will be used in case of splitting.
 * 
 * @see messif.algorithms.Algorithm
 * @see SplitPolicy
 * @author xbatko
 */
public interface SplittableAlgorithm {

    /**
     * This is helper class that allows the split method to control the creation of algorithms.
     * When the {@link #split} method is called, an instance of this class is provided and
     * the split method should mark all the objects moved through either the
     * {@link #markMovedObject} or {@link #markMovedObjects} method.
     *
     * <p>Example of a split method implementation:
     * <pre>
     *   class SomeAlgorithm extends Algorithm {
     *       ...
     *       public void split(SplitPolicy policy, SplittableAlgorithmResult result) throws OccupationLowException, IllegalArgumentException, CapacityFullException {
     *           Algorithm newAlgorithm = new SomeAlgorithm(...);
     *           // Move some objects from this algorithm to another one and get the number of bytes or number of objects moved
     *           for (...moving one object <i>o</i>...) {
     *               result.markMovedObject(newAlgorithm, <i>o</i>);
     *           }
     *           // Or a batch move
     *           result.markMovedObjects(newAlgorithm, ...collection of objects...);
     *           ... batch move of collection of objects to algorithm <i>newAlgorithm</i> ...
     *       }
     *   }
     * </pre>
     * </p>
     */
    public static interface SplittableAlgorithmResult {
        /**
         * Registers a move of objects into the result.
         * @param algorithm the created algorithm that is the destination for the move
         * @param objects the objects moved
         * @throws BucketStorageException if the move can't be performed due to the capacity or filtering reasons
         * @throws InstantiationException if encapsulating bucket for the algorithm cannot be created
         */
        public void markMovedObjects(Algorithm algorithm, Collection<? extends LocalAbstractObject> objects) throws BucketStorageException, InstantiationException;

        /**
         * Registers a move of one object into the result.
         * @param algorithm the created algorithm that is the destination for the move
         * @param object the object moved
         * @throws BucketStorageException if the move can't be performed due to the capacity or filtering reasons
         * @throws InstantiationException if encapsulating bucket for the algorithm cannot be created
         */
        public void markMovedObject(Algorithm algorithm, LocalAbstractObject object) throws BucketStorageException, InstantiationException;
    }

    /**
     * Split this algorithm according to the specified policy.
     * The newly created algorithms should be returned by adding them to the <code>splitAlgorithms</code>.
     * @param policy the splitting policy to use
     * @param result object used to return results of the split
     * @param whoStays identification of a partition whose objects stay in this bucket.
     * @throws IllegalArgumentException if there are too few target buckets
     * @throws BucketStorageException if there was a storage error during split
     */
    public void split(SplitPolicy policy, SplittableAlgorithmResult result, int whoStays) throws BucketStorageException, IllegalArgumentException;
}
