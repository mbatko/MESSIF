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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.DimensionObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

public abstract class ObjectFeature extends LocalAbstractObject implements DimensionObjectKey.Point, BinarySerializable {

    /** class id for serialization */
    private static final long serialVersionUID = 2L;

    /** x component of spatial coordinates (relative number in the interval [0,1) ) */
    protected float x;
    /** y component of spatial coordinates (relative number in the interval [0,1) ) */
    protected float y;
    /** Orintation of the gravity vector */
    protected float ori;
    /** Scale of the gravity vector */
    protected float scl;

    /** List of keys of this feature (e.g. M-Index cluster numbers) */
    protected long [] keys;

    /**
     * Default constructor that sets the params to 0f and null.
     */
    public ObjectFeature() {
    }
    
    public ObjectFeature(float x,  float y,  float ori,  float scl) {
        this.x = x;
        this.y = y;
        this.ori = ori;
        this.scl = scl;
        this.keys = null;
    }
    
    public ObjectFeature (float x, float y, float ori, float scl, long [] keys) {
        this.x = x;
        this.y = y;
        this.ori = ori;
        this.scl = scl;
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
    public ObjectFeature(BufferedReader stream) throws IOException, NumberFormatException {
        String line = readObjectComments(stream);
        try {
            String[] paramsAndKeys = line.trim().split("[; ]+");
            String[] params = paramsAndKeys[0].trim().split("[, ]+");
            this.x = Float.parseFloat(params[0]);
            this.y = Float.parseFloat(params[1]);
            this.ori = Float.parseFloat(params[2]);
            this.scl = Float.parseFloat(params[3]);
            // if the keys were specified
            if (paramsAndKeys.length > 1) {
                String[] keyString = paramsAndKeys[1].trim().split("[, ]+");
                this.keys = new long [keyString.length];
                int i = 0;
                for (String string : keyString) {
                    keys[i ++] = Long.parseLong(string);
                }
            }
        } catch (NumberFormatException numberFormatException) {
            Logger.getLogger(getClass().getName()).warning("error while parsing line '"+ line+ "' for 4 floats, locator: " + getLocatorURI());
            throw numberFormatException;
        }
    }

    @Override
    public void writeData(OutputStream stream) throws IOException {
        stream.write(String.format("%1$f, %2$f, %3$f, %4$f", this.x, this.y, this.ori, this.scl).getBytes());
        if (keys != null) {
            stream.write(';');
            for (int i = 0; i < keys.length; i++ ) {
                stream.write(' ');
                stream.write(Long.toString(keys[i]).getBytes());
                if (i != keys.length - 1) {
                    stream.write(',');
                }
            }
        }
        stream.write('\n');
    }

    //****************** Equality comparing function ******************

    // Ignores the unique Key
    @Override
    public boolean dataEquals(Object obj) {
        return (((ObjectFeature)obj).x == this.x && ((ObjectFeature)obj).y == this.y
                && ((ObjectFeature)obj).ori == this.ori && ((ObjectFeature)obj).scl == this.scl);
    }

    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    @Override
    public int getSize() {
        return 4 * Float.SIZE; // x, y, scl, ori * sizeof (4)
    }

    /**
     * Returns x component of spatial coordinates
     * @return x component of spatial coordinates
     */
    @Override
    public float getX () {
        return x;
    }

    /**
     * Returns Y component of spatial coordinates
     * @return x component of spatial coordinates
     */
    @Override
    public float getY() {
        return y;
    }
    
    /**
     * Returns the Scale component of the gravity vector
     * @return Scale of the gravity vector
     */
    public float getScale () {
        return scl;
    }

    /**
     * Returns the Orientation component of the gravity vector
     * @return Orientation of the gravity vector
     */
    public float getOrientation () {
        return ori;
    }
    
    @Deprecated
    public final float getOri() {
        return getOrientation();
    }

    @Deprecated
    public final float getScl() {
        return getScale();
    }

    public long[] getKeys() {
        return Arrays.copyOf(keys, keys.length);
    }

    public void setKeys(long[] keys) {
        this.keys = keys;
    }
    
    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeature loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFeature from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeature (BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.x = serializator.readFloat(input);
        this.y = serializator.readFloat(input);
        this.ori = serializator.readFloat(input);
        this.scl = serializator.readFloat(input);
        this.keys = serializator.readLongArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, this.x) +
               serializator.write(output, this.y) +
               serializator.write(output, this.ori) +
               serializator.write(output, this.scl) +
               serializator.write(output, this.keys);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 
               serializator.getBinarySize(this.x) +
               serializator.getBinarySize(this.y) +
               serializator.getBinarySize( this.ori) +
               serializator.getBinarySize(this.scl) +
               serializator.getBinarySize(this.keys);
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new CloneNotSupportedException("cloneRandomlyModify not supported yet");
    }
}
