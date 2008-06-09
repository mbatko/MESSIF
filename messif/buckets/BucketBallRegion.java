/*
 * BucketBallRegion.java
 *
 * Created on 10. listopad 2007, 23:50
 *
 */

package messif.buckets;

import messif.objects.BallRegion;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author xbatko
 */
public class BucketBallRegion extends BallRegion implements BucketFilterInterface {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    /****************** Attributes ******************/

    /** The bucket on which this ball region should be maintained */
    protected final LocalBucket bucket;

    /** The flag if there was a modification to bucket */
    protected boolean needsAdjusting;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of BucketBallRegion.
     * @param bucket the bucket on which the ball region should be maintained
     */
    public BucketBallRegion(LocalBucket bucket) {
        this(bucket, true);
    }

    /**
     * Creates a new instance of BucketBallRegion.
     * @param bucket the bucket on which the ball region should be maintained
     * @param registerAsFilter specifies if the automatic registration as {@link messif.buckets.BucketFilterInterface bucket filter} is desirable
     */
    public BucketBallRegion(LocalBucket bucket, boolean registerAsFilter) {
        this(bucket, registerAsFilter, null, LocalAbstractObject.MIN_DISTANCE);
    }

    /**
     * Creates a new instance of BucketBallRegion.
     * @param bucket the bucket on which the ball region should be maintained
     * @param registerAsFilter specifies if the automatic registration as {@link messif.buckets.BucketFilterInterface bucket filter} is desirable
     * @param pivot the pivot for the new ball region
     */
    public BucketBallRegion(LocalBucket bucket, boolean registerAsFilter, LocalAbstractObject pivot) {
        this(bucket, registerAsFilter, pivot, LocalAbstractObject.MIN_DISTANCE);
    }

    /**
     * Creates a new instance of BucketBallRegion with specified pivot and radius.
     * @param bucket the bucket on which the ball region should be maintained
     * @param registerAsFilter specifies if the automatic registration as {@link messif.buckets.BucketFilterInterface bucket filter} is desirable
     * @param pivot the pivot for the new ball region
     * @param radius the radius for the new ball region
     */
    public BucketBallRegion(LocalBucket bucket, boolean registerAsFilter, LocalAbstractObject pivot, float radius) {
        super(pivot, radius);
        this.bucket = bucket;
        if (registerAsFilter && (bucket instanceof LocalFilteredBucket))
            ((LocalFilteredBucket)bucket).registerFilter(this);
        this.needsAdjusting = true;
    }


    /****************** Data getter/setter methods ******************/

    /**
     * Returns the bucket associated with this ball region.
     * @return the bucket associated with this ball region
     */
    public LocalBucket getBucket() {
        return bucket;
    }

    /**
     * Returns current radius of this ball region.
     * @return current radius of this ball region
     */
    @Override
    public synchronized float getRadius() {
        return radius;
    }

    /**
     * Sets the radius for this ball region.
     * @param radius the new radius
     * @throws IllegalArgumentException if the specified radius is negative
     */
    @Override
    public void setRadius(float radius) throws IllegalArgumentException {
        if (radius < 0)
            throw new IllegalArgumentException("Radius must be non-negative");
        synchronized (this) {
            this.radius = radius;
            this.needsAdjusting = false;
        }
    }

    /**
     * Sets the pivot for this ball region.
     * Note, that the radius is set to {@link messif.objects.LocalAbstractObject#MAX_DISTANCE} if <code>updateRadius</code> is <tt>true</tt>.
     *
     * @param pivot the new pivot
     * @param updateRadius specifies whether to update the region's radius or not
     */
    @Override
    public synchronized void setPivot(LocalAbstractObject pivot, boolean updateRadius) {
        this.pivot = pivot;
        if (updateRadius)
            adjustRadius();
        else needsAdjusting = true;
    }

    /**
     * Returns <tt>true</tt> if the underlying bucket has changed without adjusting the radius.
     * However, the returned value is just a hint and can be inaccurate.
     * @return <tt>true</tt> if the underlying bucket has changed without adjusting the radius
     */
    public boolean needsAdjusting() {
        return needsAdjusting;
    }


    /****************** Adjusting radius ******************/

    /**
     * Adjust the radius according to objects in the underlying bucket.
     */
    public void adjustRadius() {
        synchronized (bucket) {
            synchronized (this) {
                if (pivot != null) {
                    radius = LocalAbstractObject.MIN_DISTANCE;
                    GenericObjectIterator<LocalAbstractObject> iterator = bucket.getAllObjects();
                    while (iterator.hasNext()) {
                        float distance = pivot.getDistance(iterator.next());
                        if (distance > radius)
                            radius = distance;
                    }
                } else {
                    radius = LocalAbstractObject.MIN_DISTANCE;
                }
                needsAdjusting = false;
            }
        }
    }


    /****************** Bucket filter interface method ******************/

    /**
     * Adjust this ball region whenever an object is inserted into a bucket.
     *
     * @param object the inserted object
     * @param situation actual situation in which the method is called (see FilterSituations constants for detailed description)
     * @param inBucket bucket, where the object will be/was stored
     * @throws FilterRejectException if the current operation should be aborted (should be thrown only in BEFORE situations)
     */
    public void filterObject(LocalAbstractObject object, BucketFilterInterface.FilterSituations situation, LocalFilteredBucket inBucket) throws FilterRejectException {
        // Only adjust while adding objects
        switch (situation) {
            case AFTER_ADD:
                break;
            case AFTER_DEL:
                needsAdjusting = true;
                return;
            default:
                return;
        }

        synchronized (this) {
            if (pivot == null) {
                // First object is set as the pivot
                pivot = object;
            } else {
                float distance = pivot.getDistance(object);
                if (radius < distance)
                    radius = distance;
            }
        }
    }    
}
