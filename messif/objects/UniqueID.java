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
import java.io.Serializable;
import java.util.UUID;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * A class that represents an immutable universally unique identifier (UUID).
 * It represents a 128-bit value.
 * It is generated using {@link java.util.UUID} class's static constructor.
 *
 * Note: we decided not to use the UUID class directly, because it is memory intensive.
 *
 * @see java.util.UUID
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class UniqueID implements Serializable, Comparable<UniqueID> {

    /** Class serial id for serialization */
    private static final long serialVersionUID = -1584641682281614360L;

    /** The most significant 64 bits of this UniqueID. */
    private long mostSigBits;

    /** The least significant 64 bits of this UniqueID. */
    private long leastSigBits;   

    /** Creates a new instance of UniqueID with newly generated ID. */
    protected UniqueID() {
        UUID newID = UUID.randomUUID();
        this.mostSigBits = newID.getMostSignificantBits();
        this.leastSigBits = newID.getLeastSignificantBits();
    }

    /**
     * Creates a new instance of UniqueID from an existing UniqueID object.
     * @param source the unique ID to copy from
     */
    protected UniqueID(UniqueID source) {
        this.mostSigBits = source.mostSigBits;
        this.leastSigBits = source.leastSigBits;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Returns <tt>true</tt> if and only if the obj is descendant of UniqueID and its
     * internal 128-bit IDs are equal. Otherwise <tt>false</tt> is returned.
     * @param obj the object to compare with.
     * @return <tt>true</tt> if the objects are the same; <tt>false</tt> otherwise
     */
    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof UniqueID))
            return false;
        UniqueID castObj = (UniqueID)obj;
        return this.leastSigBits == castObj.leastSigBits && this.mostSigBits == castObj.mostSigBits;
    }

    /**
     * Returns a hash code value for this unique ID.
     * @return a hash code value for this unique ID
     */
    @Override
    public final int hashCode() {
        return (int)((mostSigBits >> 32) ^
                mostSigBits ^
                (leastSigBits >> 32) ^
                leastSigBits);
    }

    /**
     * Returns long value <code>val</code> represented by the specified number of hex digits.
     * @param val the value to convert to hex digits
     * @param digits the number of digits the convert
     * @return a string containing the hex-digit representation
     */
    private static String digits(long val, int digits) {
	long hi = 1L << (digits * 4);
	return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    /**
     * Returns a string representation of this unique ID.
     * @return a string representation of this unique ID
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(digits(mostSigBits >> 32, 8)).append('-');
        rtv.append(digits(mostSigBits >> 16, 4)).append('-');
	rtv.append(digits(mostSigBits, 4)).append('-');
	rtv.append(digits(leastSigBits >> 48, 4)).append('-');
	rtv.append(digits(leastSigBits, 12));
        return rtv.toString();
    }

    /**
     * Creates and returns a copy of this object.
     * The unique ID of the copy will be different from this object's ID.
     *
     * Note: This method should be called only from clone method of this class's descendants.
     *       Thus it is protected and the class doesn't implement {@link java.lang.Cloneable} interface.
     * @return a copy of this object
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        UniqueID rtv = (UniqueID)super.clone();

        // Clonning defines a new ID for this object
        UUID newID = UUID.randomUUID();
        rtv.mostSigBits = newID.getMostSignificantBits();
        rtv.leastSigBits = newID.getLeastSignificantBits();

        return rtv;
    }

    /**
     * Compares this unique ID with the specified unique ID.
     * 
     * <p>The first of two unique IDs follows the second if the most significant
     * field in which the unique IDs differ is greater for the first unique ID.
     *
     * @param  val the unique ID to which this unique ID is to be compared.
     * @return -1, 0 or 1 as this unique ID is less than, equal
     *         to, or greater than <tt>val</tt>.
     */
    @Override
    public final int compareTo(UniqueID val) {
        // The ordering is intentionally set up so that the unique IDs
        // can simply be numerically compared as two numbers
        return (this.mostSigBits < val.mostSigBits ? -1 : 
                (this.mostSigBits > val.mostSigBits ? 1 :
                 (this.leastSigBits < val.leastSigBits ? -1 :
                  (this.leastSigBits > val.leastSigBits ? 1 :
                   0))));
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of UniqueID loaded from binary input.
     * 
     * @param input the input to read the ID from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected UniqueID(BinaryInput input, BinarySerializator serializator) throws IOException {
        this.mostSigBits = serializator.readLong(input);
        this.leastSigBits = serializator.readLong(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    protected int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return serializator.write(output, mostSigBits) +
               serializator.write(output, leastSigBits);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    protected int getBinarySize(BinarySerializator serializator) {
        return 16;
    }
}
