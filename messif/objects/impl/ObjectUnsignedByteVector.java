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
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;


/**
 * This object represents an array of unsigned bytes represented in memory as short integers.
 * No implementation of distance function is provided.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectUnsignedByteVector extends LocalAbstractObject implements BinarySerializable {
    
    /** class id for serialization */
    private static final long serialVersionUID = 320201L;

    //****************** Attributes ******************//

    /** Data array */
    protected short[] data;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectUnsignedByteVectorL2.
     * @param data the data content of the new object
     */
    public ObjectUnsignedByteVector(short[] data) {
        this.data = data.clone();
    }

    /**
     * Creates a new instance of ObjectUnsignedByteVectorL2 with randomly generated content data.
     * Content will be generated using normal distribution of random short integer numbers
     * from interval [0;max short int).
     *
     * @param dimension number of dimensions to generate
     */
    public ObjectUnsignedByteVector(int dimension) {
        this(dimension, (short)0, (short)(Short.MAX_VALUE - 1));
    }

    /**
     * Creates a new instance of ObjectUnsignedByteVectorL2 with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [min;max).
     *
     * @param dimension number of dimensions to generate
     * @param min lower bound of the random generated values (inclusive)
     * @param max upper bound of the random generated values (exclusive)
     */
    public ObjectUnsignedByteVector(int dimension, short min, short max) {
        this.data = ObjectShortVector.randomData(dimension, min, max);
    }

    /**
     * Creates a new instance of ObjectUnsignedByteVectorL2 from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectUnsignedByteVector(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        this.data = ObjectShortVector.parseShortVector(line);
    }


    //****************** Text file store/retrieve methods ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        ObjectShortVector.writeShortVector(data, stream, ',', '\n');
    }



    //****************** Equality comparing function ******************//

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectUnsignedByteVector))
            return false;
        
        return Arrays.equals(((ObjectUnsignedByteVector)obj).data, data);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(data);
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the vector of short integer values, which represents the contents of this object.
     * A copy is returned, so any modifications to the returned array do not affect the original object.
     * @return the data contents of this object
     */
    public short[] getVectorData() {
        return this.data.clone();
    }

    @Override
    public int getSize() {
        return this.data.length * Short.SIZE / 8;
    }

    /**
     * Returns the number of dimensions of this vector.
     * @return the number of dimensions of this vector
     */
    public int getDimensionality() {
        return this.data.length;
    }
    

    //************ String representation ************//

    /**
     * Converts this object to a string representation.
     * The format is the comma-separated list of coordinates enclosed in square brackets
     * and the result of <code>super.toString()</code> is appended.
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(super.toString()).append(" [");

        for (int i = 0; i < this.data.length; i++) {
            if (i > 0) rtv.append(", ");
            rtv.append(data[i]);
        }
        rtv.append("]");

        return rtv.toString();
    }

    
    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectUnsignedByteVectorL2 loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectUnsignedByteVectorL2 from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectUnsignedByteVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        
        byte[] byteArray = serializator.readByteArray(input);
        data = new short[byteArray.length];        
        for (int i = 0; i < byteArray.length; i++) {
            data[i] = (short) (0x000000FF & ((int) byteArray[i]));
        }
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the data output this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        byte[] byteArray = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            byteArray[i] = (byte) (0x000000FF & data[i]);
        }
        return super.binarySerialize(output, serializator) +
               serializator.write(output, byteArray);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(1) + ((data != null) ? data.length : 0);
    }
    
}
