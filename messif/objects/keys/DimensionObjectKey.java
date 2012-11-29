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
package messif.objects.keys;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * This class adds to the standard object key {@link AbstractObjectKey} dimensions of the object.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DimensionObjectKey extends AbstractObjectKey {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    //************ Attributes ************//

    /** Dimensions */
    private final int[] dimensions;


    //************ Constructor ************//

    /** 
     * Creates a new instance for a string value.
     * @param value the string representaion of the key
     */
    public DimensionObjectKey(String value) {
        super(AbstractObjectKey.getKeyStringPart(value, " ", 1));
        // Dimensions:
        try {
            String d = AbstractObjectKey.getKeyStringPart(value, " ", 0).substring(value.indexOf('[') + 1, value.indexOf(']'));
            String[] dims = d.split("[,:]");
            this.dimensions = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                dimensions[i] = Integer.parseInt(dims[i]);
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("Incorrect dimension object key format! Expected: \"[x,y,...] URI\"  Passed: " + value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Incorrect dimension object key format! Expected: \"[x,y,...] URI\"  Passed: " + value);
        }
    }

    /** 
     * Creates a new instance.
     * @param locatorURI the URI locator
     * @param width x-axis dimension
     * @param height y-axis dimension
     */
    public DimensionObjectKey(String locatorURI, int width, int height) {
        super(locatorURI);
        this.dimensions = new int[] {width, height};
    }

    /** 
     * Creates a new instance.
     * @param locatorURI the URI locator
     * @param dimensions dimensions of the object
     */
    public DimensionObjectKey(String locatorURI, int[] dimensions) {
        super(locatorURI);
        this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
    }


    //************ Attribute access methods ************//

    /**
     * Size of the object in the required dimension.
     * @param di dimension index
     * @return value of the required dimension
     */
    public int getDimension(int di) {
        return dimensions[di];
    }
    
    /**
     * Number of dimensions.
     * @return number of dimensions
     */
    public int getDimensionCount() {
        return dimensions.length;
    }

    /**
     * Width of the object.
     * @return value of first dimension
     */
    public int getWidth() {
        return dimensions[0];
    }

    /**
     * Height of the object.
     * @return value of second dimension
     */
    public int getHeight() {
        return dimensions[1];
    }
    
    //****************** Serialization ******************//

    /**
     * Store this key's data to a text stream.
     * This method should have the opposite deserialization in constructor.
     * Note that this method should <em>not</em> write a line separator (\n).
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        stream.write(Arrays.toString(dimensions).getBytes());
    }


    //************ Comparator and equality methods ************//

    /**
     * Compare the keys according to their locators.
     * @param o the key to compare this key with
     * @return a negative integer, zero, or a positive integer if this object
     *         is less than, equal to, or greater than the specified object
     */
    @Override
    public int compareTo(AbstractObjectKey o) {
        if (o == null || !(o.getClass().equals(DimensionObjectKey.class)))
            return 3;
        int cmp = super.compareTo(o);
        if (cmp != 0)
            return cmp;
        
        DimensionObjectKey obj = (DimensionObjectKey)o;
        if (this.dimensions.length < obj.dimensions.length)
            return -1;
        else if (this.dimensions.length > obj.dimensions.length)
            return 1;
        for (int i = 0; i < this.dimensions.length; i++) {
            if (this.dimensions[i] < obj.dimensions[i])
                return -1;
            else if (this.dimensions[i] > obj.dimensions[i])
                return 1;
        }
        return 0;
    }

    /**
     * Return the hashCode of the locator URI or 0, if it is null.
     * @return the hashCode of the locator URI or 0, if it is null
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        
        for (int d : dimensions)
            hash = hash * 47 + d;
        
        return hash;
    }

    /**
     * Returns whether this key is equal to the <code>obj</code>.
     * It is only and only if the <code>obj</code> is descendant of
     * {@link AbstractObjectKey} and has an equal locator URI.
     * 
     * @param obj the object to compare this object to
     * @return <tt>true</tt> if this object is the same as the <code>obj</code> argument; <tt>false</tt> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        if (! (obj.getClass().equals(DimensionObjectKey.class)))
            return false;
        
        DimensionObjectKey o = (DimensionObjectKey)obj;
        if (this.dimensions.length != o.dimensions.length)
            return false;
        for (int i = 0; i < this.dimensions.length; i++) {
            if (this.dimensions[i] != o.dimensions[i])
                return false;
        }
        return true;
    }


    //************ String representation ************//

    /**
     * Returns the URI string.
     * @return the URI string
     */
    @Override
    public String toString() {
        return super.toString() + " " + Arrays.toString(dimensions);
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of AbstractObjectKey loaded from binary input.
     * 
     * @param input the input to read the AbstractObjectKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected DimensionObjectKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.dimensions = serializator.readIntArray(input);
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
        return super.binarySerialize(output, serializator) + serializator.write(output, dimensions);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(dimensions);
    }


    //************ Geometric conversion methods ************//

    /**
     * Convert the passed absolute value in the passed dimension to a relative value in the interval [0,1).
     * @param value absolute value in the passed dimension
     * @param dimension zero-based dimension index (0 = x-axis, 1 = y-axis, ...)
     * @return relative value
     */
    public float convertToRelative(int value, int dimension) {
        return convertToRelative((float)value, dimension);
    }

    /**
     * Convert the passed absolute value in the passed dimension to a relative value in the interval [0,1).
     * @param value absolute value in the passed dimension
     * @param dimension zero-based dimension index (0 = x-axis, 1 = y-axis, ...)
     * @return relative value
     */
    public float convertToRelative(float value, int dimension) {
        return value / (float)getDimension(dimension);
    }

    /**
     * Convert the passed relative value in the passed dimension to an absolute value in the interval [0,<code>this.getDimension(dimension)</code>).
     * @param value relative value in the passed dimension
     * @param dimension zero-based dimension index (0 = x-axis, 1 = y-axis, ...)
     * @return absolute value
     */
    public int convertToAbsolute(float value, int dimension) {
        return (int) (value * getDimension(dimension));
    }
    
    //************ Static geometric methods and interfaces ************//

    /** Interface that marks a class that it offers its position (x,y) */
    public static interface Point {
        /**
         * x-axis coordinate of the position.
         * @return value of x-axis.
         */
        public float getX();

        /**
         * y-axis coordinate of the position.
         * @return value of y-axis.
         */
        public float getY();
    }
    
    @Deprecated
    public static interface Range {
        public float getMin();
        public float getMax();
    }
}
