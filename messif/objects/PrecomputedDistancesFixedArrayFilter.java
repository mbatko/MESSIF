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
package messif.objects;

import messif.objects.util.AbstractObjectList;
import java.io.IOException;
import java.io.OutputStream;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * Precomputed distance filter that has a fixed array of distances.
 * While filtering, this filter uses one stored distance after the other
 * and matches it against the opposite object's distance.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class PrecomputedDistancesFixedArrayFilter extends PrecomputedDistancesFilter {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The list of precomputed distances */
    protected float[] precompDist = null;

    /** The actual size of precompDist (if it was pre-buffered) */
    protected int actualSize = 0;

    /**
     * Creates a new instance of PrecomputedDistancesFixedArrayFilter.
     */
    public PrecomputedDistancesFixedArrayFilter() {
    }

    /**
     * Creates a new instance of PrecomputedDistancesFixedArrayFilter.
     * @param object the object to which to add this filter
     */
    public PrecomputedDistancesFixedArrayFilter(LocalAbstractObject object) {
        object.chainFilter(this, true);
    }

    /**
     * Creates a new instance of PrecomputedDistancesFixedArrayFilter.
     * @param initialSize the initial size for this filter's internal array of distances
     */
    public PrecomputedDistancesFixedArrayFilter(int initialSize) {
        precompDist = new float[initialSize];
    }

    /**
     * Creates a new instance of PrecomputedDistancesFixedArrayFilter
     * @param object the object to which to add this filter
     * @param initialSize the initial size for this filter's internal array of distances
     */
    public PrecomputedDistancesFixedArrayFilter(LocalAbstractObject object, int initialSize) {
        this(initialSize);
        object.chainFilter(this, true);
    }

    /**
     * Creates a new instance of PrecomputedDistancesFixedArrayFilter from a string.
     * The string must be of format "dist1 dist2 dist3...".
     * @param distancesString string to create the filter from
     * @throws java.lang.IllegalArgumentException if the string is of inappropriate format
     */
    public PrecomputedDistancesFixedArrayFilter(String distancesString) throws IllegalArgumentException {
        String[] distStrings = distancesString.split(" ");
        precompDist = new float[distStrings.length];
        try {
            for (String dist : distStrings) {
                precompDist[actualSize++] = Float.valueOf(dist);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("string must be of format 'dist1 dist2 dist3...': "+distancesString);
        }
    }

    @Override
    protected boolean isDataWritable() {
        return true;
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        for (int i = 0; i < precompDist.length; i++) {
            if (i > 0)
                stream.write(' ');
            stream.write(Float.toString(precompDist[i]).getBytes());
        }
    }


    //****************** Manipulation methods ******************//

    @Override
    protected boolean addPrecomputedDistance(LocalAbstractObject obj, float distance, float[] metaDistances) {
        insertPrecompDist(actualSize, distance);
        return true;
    }

    /** Add distance at the end of internal list of precomputed distances.
     * @param  dist   distance to append
     * @return The total number of precomputed distances stored.
     */
    public synchronized int addPrecompDist(float dist) {
        insertPrecompDist(actualSize, dist);
        return actualSize;
    }

    /** Add the passed distances at the end of internal list of precomputed distances.
     * @param  dists   array of distances to append
     * @return The total number of precomputed distances stored.
     */
    public synchronized int addPrecompDist(float[] dists) {
        // Resize the internal array if necessary
        resizePrecompDistArray(dists.length + actualSize);

        // Copy the array
        System.arraycopy(dists, 0, precompDist, actualSize, dists.length);
        actualSize += dists.length;

        return actualSize;
    }

    /** Add distance at the end of internal list of precomputed distances. The distance is computed between the objects passed.
     * @param p   first object (usually pivot)
     * @param o   second object
     * @return The distance computed between the object passed in arguments. The <code>LocalAbstractObject.UNKNOWN_DISTANCE</code> is returned if any of objects passed is null.
     */
    public synchronized float addPrecompDist(LocalAbstractObject p, LocalAbstractObject o) {
        if (p == null || o == null)
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        float d = p.getDistance(o);
        addPrecompDist(d);
        return d;
    }

    /** Add distances at the end of internal list of precomputed distances. 
     * The distances appended are computed between all pivots passed in an iterator and the objects <code>o</code>.
     * If <code>pivots</code> or <code>o</code> is null, no change is done.
     * @param pivots   list of objects (usually pivots)
     * @param obj      second object
     * @return The total number of precomputed distances stored.
     */
    public synchronized int addPrecompDist(AbstractObjectList<LocalAbstractObject> pivots, LocalAbstractObject obj) {
        if (pivots == null || obj == null)
            return actualSize;
        
        // Resize the internal array if necessary
        resizePrecompDistArray(pivots.size() + actualSize);

        // Go through all pivots
        for (LocalAbstractObject pvt : pivots)
            precompDist[actualSize++] = pvt.getDistance(obj);
        
        return actualSize;
    }
    
    /** Add distances at the end of internal list of precomputed distances. 
     * The distances appended are computed between all pivots passed in an iterator and the objects <code>o</code>.
     * If <code>pivots</code> or <code>o</code> is null, no change is done.
     * @param pivots   list of objects (usually pivots)
     * @param obj      second object
     * @return The total number of precomputed distances stored.
     */
    public synchronized int addPrecompDist(LocalAbstractObject[] pivots, LocalAbstractObject obj) {
        if (pivots == null || obj == null)
            return actualSize;
        
        // Resize the internal array if necessary
        resizePrecompDistArray(pivots.length + actualSize);

        // Go through all pivots
        for (LocalAbstractObject pvt : pivots)
            precompDist[actualSize++] = pvt.getDistance(obj);
        
        return actualSize;
    }
    
    /** Insert distance into internal list of precomputed distances at the specified position.
     * @param pos    the index to insert the distance at
     * @param dist   the distance to insert
     * @throws IndexOutOfBoundsException is thrown when <code>pos</code> is out of bounds. 
     */
    public synchronized void insertPrecompDist(int pos, float dist) throws IndexOutOfBoundsException {
        if (pos > actualSize || pos < 0)
            throw new IndexOutOfBoundsException("Position '" + pos + "' is out of bounds");

        if (precompDist == null)
            precompDist = new float[1];
        
        float[] newArray;
        if (actualSize >= precompDist.length) {
            newArray = new float[actualSize + 1];
            System.arraycopy(precompDist, 0, newArray, 0, pos);
        } else newArray = precompDist;
        
        if (actualSize != pos)
            System.arraycopy(precompDist, pos, newArray, pos + 1, actualSize - pos);

        precompDist = newArray;
        precompDist[pos] = dist;
        actualSize++;
    }
    
    /** Insert distance into internal list of precomputed distances at the specified position.
     * @param pos    the index to insert the distance at
     * @param p      first object (usually pivot)
     * @param o      second object
     * @return The distance computed between the object passed in arguments. The <code>LocalAbstractObject.UNKNOWN_DISTANCE</code> is returned if any of objects passed is null.
     * @throws IndexOutOfBoundsException is thrown when <code>pos</code> is out of bounds. 
     */
    public synchronized float insertPrecompDist(int pos, LocalAbstractObject p, LocalAbstractObject o) throws IndexOutOfBoundsException {
        if (p == null || o == null)
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        float d = p.getDistance(o);
        insertPrecompDist(pos, d);
        return d;
    }

    /** Set distance in the internal list of precomputed distances at the specified position. 
     * @param pos    the index to set the distance at
     * @param dist   the distance to set
     * @throws IndexOutOfBoundsException is thrown when <code>pos</code> is out of bounds. 
     */
    public void setPrecompDist(int pos, float dist) throws IndexOutOfBoundsException {
        if (pos >= actualSize || pos < 0)
            throw new IndexOutOfBoundsException("Position '" + pos + "' is out of bounds");
        precompDist[pos] = dist;
    }

    /** Set distance at the end of internal list of precomputed distances. The distance is computed between the objects passed.
     * @param pos    the index to insert the distance at
     * @param p      first object (usually pivot)
     * @param o      second object
     * @return The distance computed between the object passed in arguments. The <code>LocalAbstractObject.UNKNOWN_DISTANCE</code> is returned if any of objects passed is null.
     * @throws IndexOutOfBoundsException is thrown when <code>pos</code> is out of bounds. 
     */
    public float setPrecompDist(int pos, LocalAbstractObject p, LocalAbstractObject o) throws IndexOutOfBoundsException {
        if (p == null || o == null)
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        float d = p.getDistance(o);
        setPrecompDist(pos, d);
        return d;
    }

    /** Remove distance at the specified index from the internal list of precomputed distances. 
     * The elements behind the deleted index are shifted down and the array truncated.
     * @param pos    the index to remove the distance at
     * @throws IndexOutOfBoundsException is thrown when <code>pos</code> is out of bounds. 
     */
    public synchronized void removePrecompDist(int pos) throws IndexOutOfBoundsException {
        if (precompDist == null || actualSize <= pos)
            throw new IndexOutOfBoundsException("There are no precomputed distance at the passed position to remove");
        
        System.arraycopy(precompDist, pos + 1, precompDist, pos, actualSize - 1 - pos);
        actualSize--;
    }

    /** Removes the requested number of distances from the end of the array.
     * @param cnt    the number of distances to remove
     * @throws IndexOutOfBoundsException is thrown when the list of precomputed distance is already empty.
     */
    public synchronized void removeLastPrecompDists(int cnt) throws IndexOutOfBoundsException {
        if (precompDist == null || actualSize == 0)
            throw new IndexOutOfBoundsException("There are no precomputed distances to remove");

        actualSize-=cnt;
        if (actualSize < 0)
            actualSize = 0;
    }

    /** Replaces the current array of precomputed distances with the values passed in the argument.
     * @param precompDist   an array of new distances
     */
    public synchronized void setFixedPivotsPrecompDist(float[] precompDist) {
        this.precompDist = precompDist;
        actualSize = (precompDist == null)?0:precompDist.length;
    }

    /** Returns the number of stored precomputed distance.
     * @return The size of array.
     */
    public int getPrecompDistSize() {
        return actualSize;
    }

    /** Removes all precomputed distances and sets the actual array size to zero (the maximal size stays).
     */
    public void resetAllPrecompDist() {
        actualSize = 0;
    }

    /**
     * Returns the precomputed distance at the specified index.
     * If there is no distance associated with the index <code>position</code>
     * the function returns {@link LocalAbstractObject#UNKNOWN_DISTANCE}.
     *
     * @param position the index to retrieve the distance from
     * @return the precomputed distance at the specified index
     */
    public float getPrecompDist(int position) {
        if (position < 0 || position >= actualSize)
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        return precompDist[position];
    }

    /**
     * Return the whole array of precomputed distances.
     * @return The whole array of precomputed distances is returned. null is returned if no precomputed distances are stored.
     */
    public float[] getPrecompDist() {
        if (actualSize == 0)
            return null;
        float[] retArr = new float[actualSize];
        System.arraycopy(precompDist, 0, retArr, 0, actualSize);
        return retArr;
    }
    
    //****************** Cloning ******************//

    /**
     * Creates and returns a copy of this object.
     * @return a copy of this object
     * @throws CloneNotSupportedException if this object cannot be cloned.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        PrecomputedDistancesFixedArrayFilter rtv = (PrecomputedDistancesFixedArrayFilter)super.clone();
        if (rtv.precompDist != null) {
            float[] origArray = rtv.precompDist;
            rtv.precompDist = new float[origArray.length];
            System.arraycopy(origArray, 0, rtv.precompDist, 0, origArray.length);
        }
        return rtv;
    }


    //****************** Filtering methods ******************//

    @Override
    public final boolean excludeUsingPrecompDist(PrecomputedDistancesFilter targetFilter, float radius) {
        try {
            return excludeUsingPrecompDist((PrecomputedDistancesFixedArrayFilter)targetFilter, radius);
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Return true if the obj has been filtered out using stored precomputed distance.
     * Otherwise returns false, i.e. when obj must be checked using original distance (getDistance()).
     *
     * In other words, method returns true if this object and obj are more distant than radius. By
     * analogy, returns false if this object and obj are within distance radius. However, both this cases
     * use only precomputed distances! Thus, the real distance between this object and obj can be greater
     * than radius although the method returned false!!!
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances
     */
    public boolean excludeUsingPrecompDist(PrecomputedDistancesFixedArrayFilter targetFilter, float radius) {
        // We have no precomputed distances either in the query or this object
        if (precompDist == null || targetFilter.precompDist == null)
            return false;
        
        // Traverse the precomputed distances by array
        int maxIndex = Math.min(actualSize, targetFilter.actualSize);
        for (int i = 0; i < maxIndex; i++)
            if (Math.abs(precompDist[i] - targetFilter.precompDist[i]) > radius)
                return true;
        return false;
    }

    @Override
    public final boolean includeUsingPrecompDist(PrecomputedDistancesFilter targetFilter, float radius) {
        try {
            return includeUsingPrecompDist((PrecomputedDistancesFixedArrayFilter)targetFilter, radius);
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#includeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances
     */
    public boolean includeUsingPrecompDist(PrecomputedDistancesFixedArrayFilter targetFilter, float radius) {
        // We have no precomputed distances either in the query or this object
        if (precompDist == null || targetFilter.precompDist == null)
            return false;
        
        // Traverse the precomputed distances by array
        int maxIndex = Math.min(actualSize, targetFilter.actualSize);
        for (int i = 0; i < maxIndex; i++)
            if (precompDist[i] + targetFilter.precompDist[i] <= radius)
                return true;
        return false;
    }

    @Override
    public float getPrecomputedDistance(LocalAbstractObject obj, float[] metaDistances) {
        return LocalAbstractObject.UNKNOWN_DISTANCE;
    }


    /**
     * Resize the internal precomputed distances array to the newSize. 
     * The array can only be enlarged, i.e., the array cannot be truncated by calling this method.
     * @param newSize the new size for the precomputed array
     */
    protected synchronized void resizePrecompDistArray(int newSize) {
        if (precompDist != null) {
            if (precompDist.length >= newSize)
                return;
        
            float[] newArray = new float[newSize];
            System.arraycopy(precompDist, 0, newArray, 0, actualSize);        
            precompDist = newArray;
        } else precompDist = new float[newSize];
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of PrecomputedDistancesFixedArrayFilter loaded from binary input.
     * 
     * @param input the input to read the PrecomputedDistancesFixedArrayFilter from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected PrecomputedDistancesFixedArrayFilter(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        actualSize = serializator.readInt(input);
        precompDist = serializator.readFloatArray(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, actualSize) +
               serializator.write(output, precompDist);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 4 + serializator.getBinarySize(precompDist);
    }

}
