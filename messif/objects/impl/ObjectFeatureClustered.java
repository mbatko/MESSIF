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
import java.util.Locale;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

public abstract class ObjectFeatureClustered extends ObjectFeature  implements BinarySerializable {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data ******************

    protected double clusterid;

    public double getClusterID() {
        return this.clusterid;
    }
    //****************** Constructors ******************

    /** Creates a new instance of object with no position, orientation and scale info */
    public ObjectFeatureClustered (double clusterid) {
        this.clusterid = clusterid;
    }
    
    /** Creates a new instance of object */
    public ObjectFeatureClustered(float x,  float y,  float ori,  float scl, double clusterid) {
        super(x, y, ori, scl);
        this.clusterid = clusterid;
    }

    //****************** Text file store/retrieve methods ******************

    /** Creates a new instance of Object from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     * Throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectFeatureClustered(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        super(stream);
        String line = readObjectComments(stream);
        // precti ID clusteru
        this.clusterid = new Double(line);
    }

    /** Write object to stream */
    @Override
    public void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        // check if number of decimal places is zero (spare ".0" on the output)
        if ((long) clusterid == clusterid)
            stream.write (String.valueOf((long) clusterid).getBytes());
        else
            stream.write(String.valueOf(clusterid).getBytes());
        stream.write('\n');
    }


    /** toString
     * Converts the object to a string representation.
     * The format is the comma-separated list of coordinates enclosed in square brackets
     * and the result of <code>super.toString()</code> is appended.
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(super.toString());
        rtv.append(" cluster: ").append(clusterid);
        return rtv.toString();
    }


    //****************** Equality comparing function ******************
    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectFeatureClustered))
            return false;
        if (!super.dataEquals(obj))
            return false;
        if (clusterid != ((ObjectFeatureClustered)obj).clusterid)
            return false;
        return true;
    }

    @Override
    public int dataHashCode() {
        return 0;
    }


    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    @Override
    public int getSize() {
        return super.getSize()  + Double.SIZE;
    }

    /** Returns number of dimensions of this vector.
     */
    public int getDimensionality() {
        return 1;
    }

    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeatureType loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFeatureByte from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureClustered (BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        clusterid = serializator.readDouble(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) + serializator.write(output, clusterid);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + serializator.getBinarySize(clusterid);
    }
}