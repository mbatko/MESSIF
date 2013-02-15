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
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

public class ObjectRectangle2D extends LocalAbstractObject implements BinarySerializable {

    /** class id for serialization */
    private static final long serialVersionUID = 987654L;
    
    /** x component of spatial coordinates */
    protected float minx;
    /** y component of spatial coordinates */
    protected float miny;
    /** Orintation of the gravity vector */
    protected float maxx;
    /** Scale of the gravity vector */
    protected float maxy;

    public ObjectRectangle2D() {
    }

    public ObjectRectangle2D (float minx, float miny, float maxx, float maxy) {
        this.minx = minx; this.miny = miny; this.maxx = maxx; this.maxy = maxy;
    }

    public ObjectRectangle2D(BufferedReader stream) throws IOException, NumberFormatException {
        String line = readObjectComments(stream);
        try {
            String[] params = line.trim().split("[, ]+");
            this.minx = Float.parseFloat(params[0]);
            this.miny = Float.parseFloat(params[1]);
            this.maxx = Float.parseFloat(params[2]);
            this.maxy = Float.parseFloat(params[3]);
        } catch (NumberFormatException numberFormatException) {
            Logger.getLogger(getClass().getName()).warning("error while parsing line '"+ line+ "' for 4 floats, locator: " + getLocatorURI());
            throw numberFormatException;
        }
    }

    @Override
    public void writeData(OutputStream stream) throws IOException {
        stream.write(String.format("%1$f, %2$f, %3$f, %4$f", this.minx, this.miny, this.maxx, this.maxy).getBytes());
        stream.write('\n');
    }

    //****************** Equality comparing function ******************

    @Override
    public boolean dataEquals(Object obj) {
        return (((ObjectRectangle2D)obj).minx == this.minx && ((ObjectRectangle2D)obj).miny == this.miny
                && ((ObjectRectangle2D)obj).maxx == this.maxx && ((ObjectRectangle2D)obj).maxy == this.maxy);
    }

    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    @Override
    public int getSize() {
        return 4 * Float.SIZE; // minx, miny, maxx, maxy * sizeof (4)
    }

    /**
     * Returns minx component of spatial coordinates
     * @return minx component of spatial coordinates
     */
    public float getMinX () {
        return minx;
    }

    /**
     * Returns miny component of spatial coordinates
     * @return miny component of spatial coordinates
     */
    public float getMinY() {
        return miny;
    }

    /**
     * Returns the maxx component of spatial coordinates
     * @return maxx of spatial coordinates
     */
    public float getMaxX () {
        return maxx;
    }

    /**
     * Returns the maxy component of spatial coordinates
     * @return maxy component of spacial coordinates
     */
    public float getMaxY() {
        return maxy;
    }
    
    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeature loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFeature from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectRectangle2D (BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.minx = serializator.readFloat(input);
        this.miny = serializator.readFloat(input);
        this.maxx = serializator.readFloat(input);
        this.maxy = serializator.readFloat(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, this.minx) + serializator.write(output, this.miny) +
               + serializator.write(output, this.maxx) + serializator.write(output, this.maxy);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 16;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new CloneNotSupportedException("cloneRandomlyModify not supported yet");
    }

    @Override
    public int dataHashCode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    // @Override toString()
}
