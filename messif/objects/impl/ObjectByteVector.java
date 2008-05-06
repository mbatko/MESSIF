/*
 * ObjectByteVector.java
 *
 * Created on 6. kveten 2004, 14:30
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import messif.objects.GenericAbstractObjectIterator;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author xbatko
 */
public abstract class ObjectByteVector extends LocalAbstractObject {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Data ******************
    
    protected byte[] data;
    
    /** Returns the vector of integers, which represents the contents of this object.
     *  A copy is returned, so any modifications to the returned array do not affect the original object.
     */
    public byte[] getVectorData() {
        return this.data.clone();
    }
    
    //****************** Constructors ******************
    
    /** Creates a new instance of object */
    public ObjectByteVector(byte[] data) {
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectByteVector(int dimension) {
        this.data = new byte[dimension];
        for (; dimension > 0; dimension--)
            this.data[dimension - 1] = (byte)(getRandomNormal()*256);
    }
    
    //****************** Text file store/retrieve methods ******************
    
    /** Creates a new instance of Object from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     * Throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectByteVector(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectVector.");
        } while (processObjectComment(line));
        
        String[] numbers = line.trim().split("[, ]+");

        this.data = new byte[numbers.length];
        
        for (int i = 0; i < this.data.length; i++)
            this.data[i] = Byte.parseByte(numbers[i]);
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
        if (!(obj instanceof ObjectByteVector))
            return false;
        
        return Arrays.equals(((ObjectByteVector)obj).data, data);
    }
    
    public int dataHashCode() {
        return Arrays.hashCode(data);
    }
    
    
    //****************** Size function ******************
    
    /** Returns the size of object in bytes
     */
    public int getSize() {
        return this.data.length * Byte.SIZE / 8;
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
     * @return Returns an array of two byte values for the minimum and the maximum, respectively.
     */
    protected byte[] getMinMaxOverCoords(byte[] currRange) {
        byte[] range;
        
        if (currRange != null)
            range = currRange;
        else
            range = new byte[]{Byte.MAX_VALUE, Byte.MIN_VALUE};
        
        for (byte val : data) {
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
     * @return Returns an array of two byte values for the minimum and the maximum per all
     *         coordinates, respectively.
     */
    static public byte[] getMinMaxOverCoords(GenericAbstractObjectIterator<? extends ObjectByteVector> iterator) {
        byte[] range = {Byte.MAX_VALUE, Byte.MIN_VALUE};
        while (iterator.hasNext()) {
            ObjectByteVector obj = iterator.next();
            range = obj.getMinMaxOverCoords(range);
        }
        return range;
    }
    
    
    /** Computes minimum and maximum values over every coordinate of vectors in the collection's
     * iterator.
     *
     * @param iterator Iterator of a collection containing vectors to process.
     *
     * @return Returns a 2-dimensional array of byte values. The first dimension distiguishes between
     *         minimum and maximum values. Index [0] contains an array of doubles which represent minimum
     *         values of individual coordinates over all vectors. Index [0] contains a respective array
     *         of maximum values. This return value can be directly passed to translateToUnitCube() method.
     */
    static public byte[][] getMinMaxForEveryCoord(GenericAbstractObjectIterator<? extends ObjectByteVector> iterator) {
        byte[][] range = null;
        int dims = -1;
        
        while (iterator.hasNext()) {
            ObjectByteVector obj = iterator.next();
            
            if (range == null) {
                // Allocate result array
                dims = obj.getDimensionality();
                range = new byte[2][dims];
                // Initialize it
                for (int i = 0; i < dims; i++) {
                    range[0][i] = Byte.MAX_VALUE;
                    range[1][i] = Byte.MIN_VALUE;
                }
            }
            
            byte[] vec = obj.data;
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
        ObjectByteVector rtv = (ObjectByteVector) this.clone();
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
    
    
}
