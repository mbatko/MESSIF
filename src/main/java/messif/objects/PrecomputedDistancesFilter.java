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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * This class provides a framework for metric-distance filtering techniques.
 * Using a triangle inequality property of the metric space, some distance calculations
 * can be avoided provided there is some additional information - the precomputed
 * distances.
 * 
 * <p>
 * A filter is added to a {@link LocalAbstractObject} via its
 * {@link LocalAbstractObject#chainFilter chainFilter} method. Objects can have
 * several filters chained - if the first filter fails to avoid the computation,
 * the next is used and so on. The filters are then used automatically whenever a
 * {@link LocalAbstractObject#getDistance(LocalAbstractObject) distance computation}
 * is evaluated.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class PrecomputedDistancesFilter implements Cloneable, Serializable, BinarySerializable {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** The next filter in this chain - this is accessed only from the {@link LocalAbstractObject} */
    PrecomputedDistancesFilter nextFilter;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of PrecomputedDistancesFilter.
     */
    protected PrecomputedDistancesFilter() {
    }


    //****************** Filtering methods ******************//


    /**
     * Returns a precomputed distance to the given object.
     * If there is no precomputed distance for the object {@code obj},
     * an {@link LocalAbstractObject#UNKNOWN_DISTANCE UNKNOWN_DISTANCE} is returned.
     *
     * @param obj the object for which the precomputed distance is returned
     * @return the precomputed distance to an object
     */
    public final float getPrecomputedDistance(LocalAbstractObject obj) {
        return getPrecomputedDistance(obj, null);
    }

    /**
     * Returns a precomputed distance to the given object and the respective meta distances
     * array. If there is no precomputed distance for the object {@code obj},
     * an {@link LocalAbstractObject#UNKNOWN_DISTANCE UNKNOWN_DISTANCE} is returned.
     * The {@code metaDistances} are filled only if the array is not <tt>null</tt>
     * and this distance filter has the respective precomputed meta distances stored.
     * 
     * @param obj the object for which the precomputed distance is returned
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects, if it is not <tt>null</tt>
     * @return the precomputed distance to an object
     */
    public abstract float getPrecomputedDistance(LocalAbstractObject obj, float[] metaDistances);

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#excludeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances
     */
    public abstract boolean excludeUsingPrecompDist(PrecomputedDistancesFilter targetFilter, float radius);

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#includeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances
     */
    public abstract boolean includeUsingPrecompDist(PrecomputedDistancesFilter targetFilter, float radius);

    /**
     * Adds a precomputed distance to this filter.
     * @param obj the object the distance to which is added
     * @param distance the distance to add
     * @param metaDistances the array that is filled with the distances of the respective encapsulated objects
     *          (it is <tt>null</tt> if the object does not have meta distances)
     * @return <tt>true</tt> if the distance was added to the filter, or <tt>false</tt> if
     *          it was already there
     */
    protected abstract boolean addPrecomputedDistance(LocalAbstractObject obj, float distance, float[] metaDistances);


    //****************** Serialization ******************//

    /**
     * Writes this distances filter into the output text stream.
     * The key is written using the following format:
     * <pre>#filter filterClass filter value</pre>
     *
     * @param stream the stream to write the key to
     * @throws IOException if any problem occurs during comment writing
     */
    public final void write(OutputStream stream) throws IOException {
        if (isDataWritable()) {
            stream.write("#filter ".getBytes());
            stream.write(getClass().getName().getBytes());
            stream.write(' ');
            writeData(stream);
            stream.write('\n');
        }

        // Write the whole chain
        if (nextFilter != null)
            nextFilter.write(stream);
    }

    /**
     * Store this filter's data to a text stream.
     * This method should have the opposite deserialization in constructor.
     * Note that this method should <em>not</em> write a line separator (\n).
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    protected abstract void writeData(OutputStream stream) throws IOException;

    /**
     * Returns whether this filter's data can be written to a text stream.
     * Note that the method {@link #writeData(java.io.OutputStream)} should
     * provide a valid writing implementation.
     * 
     * @return <tt>true</tt> if this filter can be written to a text stream
     */
    protected abstract boolean isDataWritable();


    //****************** Cloning ******************//

    /**
     * Creates and returns a copy of this object.
     * @return a copy of this object
     * @throws CloneNotSupportedException if this object cannot be cloned.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        PrecomputedDistancesFilter rtv = (PrecomputedDistancesFilter)super.clone();
        if (rtv.nextFilter != null)
            rtv.nextFilter = (PrecomputedDistancesFilter)rtv.nextFilter.clone();
        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of PrecomputedDistancesFilter loaded from binary input.
     * 
     * @param input the input to read the PrecomputedDistancesFilter from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected PrecomputedDistancesFilter(BinaryInput input, BinarySerializator serializator) throws IOException {
        nextFilter = serializator.readObject(input, PrecomputedDistancesFilter.class);
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
        return serializator.write(output, nextFilter);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize(nextFilter);
    }

}
