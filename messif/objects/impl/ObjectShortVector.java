/*
 * ObjectShortVector.java
 *
 * Created on 6. kveten 2004, 14:30
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.AbstractObjectIterator;


/**
 *
 * @author xbatko
 */
public abstract class ObjectShortVector extends LocalAbstractObject implements BinarySerializable {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Data ******************
    
    protected short[] data;
    
    /** Returns the vector of integers, which represents the contents of this object.
     *  A copy is returned, so any modifications to the returned array do not affect the original object.
     */
    public short[] getVectorData() {
        return this.data.clone();
    }
    
    //****************** Constructors ******************
    
    /** Creates a new instance of object */
    public ObjectShortVector(short[] data) {
        this.data = new short[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectShortVector(int dimension) {
        this.data = new short[dimension];
        for (; dimension > 0; dimension--)
            this.data[dimension - 1] = (short)(getRandomNormal()*256);
    }
    
    //****************** Text file store/retrieve methods ******************
    
    /** Creates a new instance of Object from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     * Throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectShortVector(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectShortVector.");
        } while (processObjectComment(line));
        
        String[] numbers = line.trim().split("[, ]+");

        this.data = new short[numbers.length];
        
        for (int i = 0; i < this.data.length; i++)
            this.data[i] = Short.parseShort(numbers[i]);
    }
    
    /** Write object to stream */
    public void writeData(OutputStream stream) throws IOException {
        for (int i = 0; i < this.data.length; i++) {
            if (i > 0)
                stream.write(", ".getBytes());
            stream.write(String.valueOf(this.data[i]).getBytes());
        }
        
        stream.write('\n');
    }
    
    
    /** toString
     * Converts the object to a string representation.
     * The format is the comma-separated list of coordinates enclosed in square brackets
     * and the result of <code>super.toString()</code> is appended.
     */
    public String toString() {
        StringBuffer rtv = new StringBuffer(super.toString()).append(" [");

        for (int i = 0; i < this.data.length; i++) {
            if (i > 0) rtv.append(", ");
            rtv.append(data[i]);
        }
        rtv.append("]");

        return rtv.toString();
    }
    
    
    //****************** Equality comparing function ******************
    
    
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectShortVector))
            return false;
        
        return Arrays.equals(((ObjectShortVector)obj).data, data);
    }
    
    public int dataHashCode() {
        return Arrays.hashCode(data);
    }
    
    
    //****************** Size function ******************
    
    /** Returns the size of object in bytes
     */
    public int getSize() {
        return this.data.length * Short.SIZE / 8;
    }
    
    /** Returns number of dimensions of this vector.
     */
    public int getDimensionality() {
        return this.data.length;
    }

    /** Computes minimum and maximum values over all coordinates of the current vector.
     *
     * @param currRange An optional parameter containing current minimum and maximum values. If null is passed
     *                  a new range with minimum and maximum is created, otherwise the passed array is updated.
     *
     * @return Returns an array of two short values for the minimum and the maximum, respectively.
     */
    protected short[] getMinMaxOverCoords(short[] currRange) {
        short[] range;
        
        if (currRange != null)
            range = currRange;
        else
            range = new short[]{Short.MAX_VALUE, Short.MIN_VALUE};
        
        for (short val : data) {
            if (val < range[0])         // Test the minimum
                range[0] = val;
            if (val > range[1])         // Test the maximum
                range[1] = val;
        }
        return range;
    }
    
    /** Computes minimum and maximum values over all coordinates of vectors in the collection's
     * iterator.
     * @param iterator Iterator of a collection containing vectors to process.
     * @return Returns an array of two short values for the minimum and the maximum per all
     *         coordinates, respectively.
     */
    static public short[] getMinMaxOverCoords(AbstractObjectIterator<? extends ObjectShortVector> iterator) {
        short[] range = {Short.MAX_VALUE, Short.MIN_VALUE};
        while (iterator.hasNext()) {
            ObjectShortVector obj = iterator.next();
            range = obj.getMinMaxOverCoords(range);
        }
        return range;
    }
    
    
    /** Computes minimum and maximum values over every coordinate of vectors in the collection's
     * iterator.
     *
     * @param iterator Iterator of a collection containing vectors to process.
     *
     * @return Returns a 2-dimensional array of short values. The first dimension distiguishes between
     *         minimum and maximum values. Index [0] contains an array of doubles which represent minimum
     *         values of individual coordinates over all vectors. Index [0] contains a respective array
     *         of maximum values. This return value can be directly passed to translateToUnitCube() method.
     */
    static public short[][] getMinMaxForEveryCoord(AbstractObjectIterator<? extends ObjectShortVector> iterator) {
        short[][] range = null;
        int dims = -1;
        
        while (iterator.hasNext()) {
            ObjectShortVector obj = iterator.next();
            
            if (range == null) {
                // Allocate result array
                dims = obj.getDimensionality();
                range = new short[2][dims];
                // Initialize it
                for (int i = 0; i < dims; i++) {
                    range[0][i] = Short.MAX_VALUE;
                    range[1][i] = Short.MIN_VALUE;
                }
            }
            
            short[] vec = obj.data;
            for (int i = 0; i < dims; i++) {
                if (vec[i] < range[0][i])         // Test the minimum
                    range[0][i] = vec[i];
                if (vec[i] > range[1][i])         // Test the maximum
                    range[1][i] = vec[i];
            }
        }
        return range;
    }
    
    /****************************** Cloning *****************************/

    /**
     * Creates and returns a randomly modified copy of this vector.
     * Selects a vector position in random and changes it - the final value stays in the given range.
     * The modification is small - only by (max-min)/1000
     *
     * @param  args  expected size of the array is 2: <b>minVector</b> vector with minimal values in all positions
     *         <b>maxVector</b> vector with maximal values in all positions
     * @return a randomly modified clone of this instance.
     */
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        ObjectShortVector rtv = (ObjectShortVector) this.clone();
        rtv.data = this.data.clone();
        
        try {
            ObjectIntVector minVector = (ObjectIntVector) args[0];
            ObjectIntVector maxVector = (ObjectIntVector) args[1];
            Random random = new Random(System.currentTimeMillis());
            
            // pick a vector position in random
            int position = random.nextInt(Math.min(rtv.data.length, Math.min(minVector.data.length, maxVector.data.length)));
            
            // calculate 1/1000 of the possible range of this value and either add or substract it from the origival value
            float smallStep = (maxVector.data[position] - minVector.data[position]) / 1000;
            if (rtv.data[position] + smallStep <= maxVector.data[position])
                rtv.data[position] += smallStep;
            else rtv.data[position] -= smallStep;
        } catch (ArrayIndexOutOfBoundsException ignore) {
        } catch (ClassCastException ignore) { }
        
        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectShortVector loaded from binary input stream.
     * 
     * @param input the stream to read the ObjectShortVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected ObjectShortVector(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        data = serializator.readShortArray(input);
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
        return super.binarySerialize(output, serializator) +
               serializator.write(output, data);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(data);
    }
    
}
