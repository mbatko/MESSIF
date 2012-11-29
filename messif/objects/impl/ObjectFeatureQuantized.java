/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.text.StringDataProvider;

/**
 * Class for local image feature that is quantized to an array of long integers.
 * 
 * Distance function is implemented trivially -- it test equality of all correspoding keys.
 * 
 * @author Vlastislav Dohnal, dohnal@fi.muni.cz
 * @author Tomáš Homola, xhomola@fi.muni.cz
 */
public class ObjectFeatureQuantized extends ObjectFeature implements BinarySerializable, StringDataProvider {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** List of quantized keys of this feature (e.g. M-Index cluster numbers) */
    protected long keys[];

    /**
     * Default constructor that sets the params to 0f and null.
     */
    public ObjectFeatureQuantized() {
    }
    
    public ObjectFeatureQuantized(float x,  float y,  float ori,  float scl) {
        this(x, y, ori, scl, null);
    }
    
    public ObjectFeatureQuantized(float x, float y, float ori, float scl, long keys[]) {
        super(x, y, ori, scl);
        this.keys = keys;
    }
    
    /**
     * Expected format: 
     * #key locator
     * x, y, orientation, scale; key1, key2, key3...
     * data vector
     * 
     * @param stream
     * @throws IOException
     * @throws NumberFormatException 
     */
    public ObjectFeatureQuantized(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        super(stream);
        String line = readObjectComments(stream);
        // precti normalizovany vektor
        String[] numbers = line.trim().split("[, ]+");

        try {
            this.keys = new long[numbers.length];
            for (int i = 0; i < this.keys.length; i++) {
                this.keys[i] = Long.parseLong(numbers[i]);
            }
        } catch (NumberFormatException numberFormatException) {
            Logger.getLogger(getClass().getName()).warning("error while parsing keys '" + line+ "', locator: " + getLocatorURI());
            throw numberFormatException;
        }
    }

    @Override
    public void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        if (keys == null) {
            stream.write("null".getBytes());
        } else {
            for (int i = 0; i < keys.length; i++ ) {
                stream.write(Long.toString(keys[i]).getBytes());
                if (i != keys.length - 1)
                    stream.write(',');
            }
        }
        stream.write('\n');
    }

    //****************** Equality comparing function ******************

    // Ignores the unique Key
    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectFeatureQuantized))
            return false;
        if (!super.dataEquals(obj))
            return false;
        return Arrays.equals(((ObjectFeatureQuantized)obj).keys, keys);
    }

    @Override
    public int dataHashCode() {
        if (keys != null)
            return 97 * super.dataHashCode() + Arrays.hashCode(keys);
        else
            return super.dataHashCode();
    }
    
    //****************** Distance function ******************

    /** 
     * Trivial Metric function implemented as equality on all the keys.
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        if (Arrays.equals(((ObjectFeatureQuantized)obj).keys, keys))
            return 0;
        else
            return LocalAbstractObject.MAX_DISTANCE;
    }

    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    @Override
    public int getSize() {
        return super.getSize() + ((keys == null) ? 0 : (keys.length * Long.SIZE / 8));
    }

    //****************** Access to quantized keys ******************

    public long[] getKeys() {
        return Arrays.copyOf(keys, keys.length);
    }

    public void setKeys(long[] keys) {
        this.keys = keys;
    }

    public void addKey(long key) {
        if (keys == null) {
            keys = new long [1];
        } else {
            keys = Arrays.copyOf(keys, keys.length + 1);
        }
        keys[keys.length - 1] = key;
    }

    @Override
    public String getStringData() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i != 0)
                sb.append(":");
            sb.append(Long.toString(keys[i], Character.MAX_RADIX));
        }
        return sb.toString();
    }
    
    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeature loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFeature from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureQuantized(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.keys = serializator.readLongArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, this.keys);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 
               serializator.getBinarySize(this.keys);
    }
}
