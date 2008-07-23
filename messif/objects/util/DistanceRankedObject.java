/*
 * DistanceRankedObject
 */

package messif.objects.util;

/**
 * Ranked object where the rank is based on distances.
 * 
 * @param <T> the encapsulated object class
 * @author xbatko
 */
public class DistanceRankedObject<T> implements DistanceRanked, Comparable<DistanceRankedObject<?>> {

    //****************** Attributes ******************//

    /** Encapsulated object */
    protected final T object;

    /** Distance that represents the rank of this object */
    protected final float distance;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of DistanceRankedObject that encapsulates a given object.
     * The specified distance is used as object's rank.
     * @param object the encapsulated object
     * @param distance the distance specifying object's rank
     */
    public DistanceRankedObject(T object, float distance) {
        this.object = object;
        this.distance = distance;
    }


    //****************** Attribute access ******************//

    /**
     * Returns the encapsulated object.
     * @return the encapsulated object
     */
    public T getObject() {
        return object;
    }

    /**
     * Returns the measured distance.
     * @return the measured distance
     */
    public float getDistance() {
        return distance;
    }


    //****************** Overrides ******************//

    /**
     * Returns <tt>true</tt> if this encapsulated object is equal to the specified 
     * <code>DistanceRankedObject</code> encapsulated object. <tt>Null</tt> values
     * are handled correctly.
     * <p>
     * Note that the equality defined by this method is <b>inconsistent</b> with {@link #compareTo}.
     * </p>
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DistanceRankedObject))
            return false;
        Object objEncap = ((DistanceRankedObject)obj).object;
        if (object == null || objEncap == null)
            return object == objEncap;
        else
            return object.equals(objEncap);
    }

    /**
     * Returns a hash code value for the stored distance.
     * @return a hash code value for the stored distance
     */
    @Override
    public int hashCode() {
        return (object == null)?0:object.hashCode();
    }

    /**
     * Compares this object with the specified object for order.
     * <p>
     * Note that the natural order defined by this method is <b>inconsistent</b> with {@link #equals}.
     * </p>
     * @param   o the object to be compared
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object
     */
    public int compareTo(DistanceRankedObject<?> o) {
        return Float.compare(distance, o.distance);
    }


    //****************** Textual representation ******************//

    @Override
    public String toString() {
        return "<" + distance + ": " + object + ">";
    }

}
