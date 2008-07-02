/*
 * MeasuredAbstractObject
 * 
 */
package messif.objects;

import java.io.Serializable;

/**
 * Encapsulation of an object-distance pair.
 * This class holds an {@link AbstractObject} and its distance.
 * It is used as a return value for all the {@link messif.operations.QueryOperation query operations}.
 * 
 * @param <T> the encapsulated object class
 * @author xbatko
 */
public class MeasuredAbstractObject<T extends AbstractObject> implements Comparable<MeasuredAbstractObject<?>>, Serializable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Actual measured object */
    protected final T object;

    /** Distance of the object */
    protected final float distance;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of MeasuredAbstractObject for the object its measured distance.
     * @param object the measured object
     * @param distance the measured distance
     */
    public MeasuredAbstractObject(T object, float distance) {
        this.object = object;
        this.distance = distance;
    }


    //****************** Attribute getters ******************//

    /**
     * Returns the measured object.
     * @return the measured object
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


    //****************** Comparator/equality overrides ******************//

    /**
     * Compares this measured object's distance with the specified measured
     * object's distance for order. Returns a negative integer or a
     * positive integer as this object's distance is less than
     * or greater than the specified object's distance. If the distances
     * are equal, the objects are compared using {@link AbstractObject#compareTo their IDs}.
     * @param o the object to be compared
     * @return a negative integer, zero, or a positive integer as this object's distance
     *		is less than, equal to, or greater than the specified object's distance.
     */
    public int compareTo(MeasuredAbstractObject<?> o) {
        if (distance < o.distance)
            return -1;
        if (distance > o.distance)
            return 1;
        if (object == null)
            return (o.object == null) ? 0 : -1;
        if (o.object == null)
            return 1;

        // Distances are equal and both objects are not null
        return object.compareTo(o.object);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MeasuredAbstractObject))
            return false;
        MeasuredAbstractObject castObj = (MeasuredAbstractObject)obj;
        if (castObj.distance != this.distance)
            return false;
        if (object == null)
            return castObj.object == null;
        if (castObj.object == null)
            return false;

        // Distances are equal and both objects are not null
        return this.object.equals(castObj.object);
    }

    @Override
    public int hashCode() {
        return 67 * 7 + Float.floatToIntBits(distance);
    }


    //****************** Textual representation ******************//

    @Override
    public String toString() {
        return "<" + distance + ": " + object + ">";
    }

}
