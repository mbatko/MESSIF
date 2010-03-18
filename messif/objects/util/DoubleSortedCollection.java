
package messif.objects.util;

import java.util.HashMap;
import java.util.Map;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;

/**
 * This class resorts the ranked objects according to new distances (returned by method implemented in the
 *   child classes). It keeps the original distances in order to return correctly the threshold distance.
 *
 * @author David Novak david.novak(at)fi.muni.cz
 */
public abstract class DoubleSortedCollection extends RankedSortedCollection {

    /**
     * Distances to be used for pivot filtering - the original distances without the new sorting distances.
     */
    protected final Map<RankedAbstractObject, Float> originalDistances;
    
    /**
     * Object from the collectio with the threshold distance for pivot filtering - this distance cannot be simply the last
     *  distance including the keywords distance, because the keywords ditance cannot be considered for filtering.
     */
    protected RankedAbstractObject thresholdObject = null;

    /**
     * Creates new sorted collection sorted according to pixmac shape+color distance + weighted keywords distance
     *
     * @param initialCapacity capacity of the collection to allocate initially
     * @param maximalCapacity max capacity of the collection
     * @throws IllegalArgumentException
     */
    public DoubleSortedCollection(int initialCapacity, int maximalCapacity) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity);
        this.originalDistances = new HashMap<RankedAbstractObject, Float>(2 * initialCapacity);
    }

    /**
     * Given an object, this method should return the new distance this collection is sorted according to.
     * @param origObject the original object
     * @param origDistance the original distance
     * @return new sorting distance
     */
    public abstract float getNewDistance(AbstractObject origObject, float origDistance);

    @Override
    public boolean add(RankedAbstractObject e) {
        return add((RankedAbstractObject) e.clone(getNewDistance(e.getObject(), e.getDistance())), e.getDistance());
    }

    /**
     * Adds the specified element to this list.
     * The element is added according the to order defined by the comparator.
     *
     * @param newObject the element to add to this list
     * @param oldDistance old ranking distance (used for keeping the threshold)
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(RankedAbstractObject newObject, float oldDistance) {
        // if the last object would "fall off" the collection
        if (isFull() && (!isEmpty()) && (getLastDistance() > newObject.getDistance())) {
            removeFromOrigDistances(get(getMaximalCapacity() - 1));
        }

        // add also to the map with original distances
        if (!isFull() || (getLastDistance() > newObject.getDistance())) {
            originalDistances.put(newObject, oldDistance);
            if ((thresholdObject == null) || (originalDistances.get(thresholdObject) < oldDistance)) {
                thresholdObject = newObject;
            }
        }
        
        return super.add(newObject);
    }

    @Override
    protected boolean remove(int index) {
        removeFromOrigDistances(get(index));
        return super.remove(index);
    }

    @Override
    public float getThresholdDistance() {
        if (isFull() && (thresholdObject != null)) {
            return originalDistances.get(thresholdObject);
        } else {
            return LocalAbstractObject.MAX_DISTANCE;
        }
    }

    /**
     * Internal method to remove given object from the map with original distances.
     *  This method updates the thresholdObject correctly - it is set to null, if the map is empty after the removal.
     * @param object object to remove
     */
    private void removeFromOrigDistances(RankedAbstractObject object) {
        originalDistances.remove(object);
        // if the removed object is the last object in the original ordering, then find the new threshold object & value
        if ((thresholdObject == object) && (!originalDistances.isEmpty())) {
            float thresholdValue = Float.MIN_VALUE;
            for (Map.Entry<RankedAbstractObject, Float> objectOrigDist : originalDistances.entrySet()) {
                if (objectOrigDist.getValue() > thresholdValue) {
                    thresholdObject = objectOrigDist.getKey();
                    thresholdValue = objectOrigDist.getValue();
                }
            }
        }
        if (originalDistances.isEmpty()) {
            thresholdObject = null;
        }
    }
}
