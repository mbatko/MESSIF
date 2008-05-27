/*
 * PrecomputedDistancesPivotMapFilter.java
 *
 * Created on 10. cervenec 2007, 11:29
 *
 */

package messif.objects;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author xbatko
 */
public class PrecomputedDistancesPivotMapFilter extends PrecomputedDistancesFilter {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The hash table of precomputed distances */
    protected final Map<LocalAbstractObject, Float> precompDistMapping;

    /** Creates a new instance of PrecomputedDistancesPivotMapFilter */
    public PrecomputedDistancesPivotMapFilter() {
        precompDistMapping = Collections.synchronizedMap(new HashMap<LocalAbstractObject, Float>());
    }

    /** Creates a new instance of PrecomputedDistancesPivotMapFilter */
    public PrecomputedDistancesPivotMapFilter(LocalAbstractObject object) {
        this();
        object.chainFilter(this, true);
    }

    /**
     * Return the string value of this filter.
     * @return the string value of this filter
     */
    @Override
    public String getText(){
        throw new UnsupportedOperationException("The pivot map filter is not to be written into the text file");
    }

    /**
     * Associates a precomputed distance to an object with this object
     *      Function appends the new distance 'dist' from the object 'obj'
     *      or replaces the old value of distance.
     *      If 'dist == LocalAbstractObject.UNKNOWN_DISTANCE' then the precomputed distance is
     *      removed.
     * 
     * 
     * @return If there is no distance associated with the object obj
     *         the function returns false. Otherwise, returns true.
     */
    public boolean setPrecompDist(LocalAbstractObject obj, float dist) {
        synchronized (precompDistMapping) {
            // Removing the object if "dist" argument is UNKNOWN_DISTANCE
            if (dist == LocalAbstractObject.UNKNOWN_DISTANCE)
                // Removing object
                return precompDistMapping.remove(obj) != null;

            precompDistMapping.put(obj, dist);
        }

        return true;
    }

    /** Resets the precomputed distance to given object (pivot).
     *
     * @param obj Object to which the precomputed distance is stored.
     *
     * @return If there is no distance associated with the object obj
     *         the function returns false. Otherwise, returns true.
     */
    public boolean resetPrecompDist(LocalAbstractObject obj) {
        return setPrecompDist(obj, LocalAbstractObject.UNKNOWN_DISTANCE);
    }

    /** Removes all precomputed distances.
     */
    public void resetAllPrecompDist() {
        precompDistMapping.clear();
    }

    /** Returns the number of stored precomputed distance.
     */
    public int getPrecompDistSize() {
        return precompDistMapping.size();
    }


    /****************** Filtering methods ******************/

    /** Return true if the obj has been filtered out using stored precomputed distance.
     * Otherwise returns false, i.e. when obj must be checked using original distance (getDistance()).
     *
     * In other words, method returns true if this object and obj are more distant than radius. By
     * analogy, returns false if this object and obj are within distance radius. However, both this cases
     * use only precomputed distances! Thus, the real distance between this object and obj can be greater
     * than radius although the method returned false!!!
     */
    protected boolean excludeUsingPrecompDistImpl(PrecomputedDistancesFilter targetFilter, float radius) {
        try {
            return excludeUsingPrecompDistImpl((PrecomputedDistancesPivotMapFilter)targetFilter, radius);
        } catch (ClassCastException e) {
            return false;
        }
    }

    protected boolean excludeUsingPrecompDistImpl(PrecomputedDistancesPivotMapFilter targetFilter, float radius) {
        for (Map.Entry<LocalAbstractObject, Float> entry : precompDistMapping.entrySet()) {
            Float targetDistance = targetFilter.precompDistMapping.get(entry.getKey());
            if (targetDistance != null)
                continue;
            if (Math.abs(entry.getValue().floatValue() - targetDistance.floatValue()) > radius)
                return true;
        }

        return false;
    }

    protected boolean includeUsingPrecompDistImpl(PrecomputedDistancesFilter targetFilter, float radius) {
        try {
            return includeUsingPrecompDistImpl((PrecomputedDistancesPivotMapFilter)targetFilter, radius);
        } catch (ClassCastException e) {
            return false;
        }
    }

    protected boolean includeUsingPrecompDistImpl(PrecomputedDistancesPivotMapFilter targetFilter, float radius) {
        for (Map.Entry<LocalAbstractObject, Float> entry : precompDistMapping.entrySet()) {
            Float targetDistance = targetFilter.precompDistMapping.get(entry.getKey());
            if (targetDistance != null)
                continue;
            if (entry.getValue().floatValue() + targetDistance.floatValue() <= radius)
                return true;
        }

        return false;
    }

    public boolean isGetterSupported() {
        return true;
    }

    /**
     * Returns the precomputed distance to an object
     *      If there is no distance associated with the object 'obj'
     *      the function returns LocalAbstractObject.UNKNOWN_DISTANCE.
     */
    protected float getPrecomputedDistanceImpl(LocalAbstractObject obj) {
        Float distance = precompDistMapping.get(obj);

        if (distance == null)
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        
        return distance.floatValue();
    }

    /** Return all objects to which this object has precomputed distances */
    public Set<LocalAbstractObject> getPrecompObjects() {
        if (precompDistMapping == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(precompDistMapping.keySet());
    }


    /****************** Clonning ******************/

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("PrecomputedDistancesPivotMapFilter can't be clonned");
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of PrecomputedDistancesPivotMapFilter loaded from binary input stream.
     * 
     * @param input the stream to read the PrecomputedDistancesPivotMapFilter from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected PrecomputedDistancesPivotMapFilter(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        int items = serializator.readInt(input);
        precompDistMapping = Collections.synchronizedMap(new HashMap<LocalAbstractObject, Float>(items));
        for (int i = 0; i < items; i++)
            precompDistMapping.put(serializator.readObject(input, LocalAbstractObject.class), serializator.readFloat(input));
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the data output this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, precompDistMapping.size());
        for (Entry<LocalAbstractObject, Float> entry : precompDistMapping.entrySet()) {
            size += serializator.write(output, entry.getKey());
            size += serializator.write(output, entry.getValue().floatValue());
        }
        return size;
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) + 4;
        for (LocalAbstractObject object : precompDistMapping.keySet())
            size += serializator.getBinarySize(object) + 4;
        return size;
    }

}
