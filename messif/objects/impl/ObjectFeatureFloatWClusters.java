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

public abstract class ObjectFeatureFloatWClusters extends ObjectFeature  implements BinarySerializable {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data ******************

    protected float [] data;

    protected double clusterid;
    /** Returns the vector of integers, which represents the contents of this object.
     *  A copy is returned, so any modifications to the returned array do not affect the original object.
     */
    public float[] getVectorData() {
        return this.data.clone();
    }

    public double getClusterID() {
        return this.clusterid;
    }
    //****************** Constructors ******************

    /** Creates a new instance of object */
    public ObjectFeatureFloatWClusters(float x,  float y,  float ori,  float scl, float[] data, double clusterid) {
        super(x, y, ori, scl);
        this.data = new float[data.length];
        this.clusterid = clusterid;
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    //****************** Text file store/retrieve methods ******************

    /** Creates a new instance of Object from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     * Throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectFeatureFloatWClusters(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        super(stream);
        String line = readObjectComments(stream);
        // precti normalizovany vektor
        String[] numbers = line.trim().split("[, ]+");

        this.data = new float[numbers.length];

        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = Float.parseFloat(numbers[i]);
        }
        // precti ID clusteru
        line = readObjectComments(stream);
        this.clusterid = new Double(line);
    }

    /** Write object to stream */
    @Override
    public void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        for (int i = 0; i < this.data.length; i++) {
            if (i > 0)
                stream.write(", ".getBytes());
            stream.write(String.valueOf(this.data[i]).getBytes());
        }
        stream.write('\n');
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
        rtv.append(" cluster: ").append(clusterid).append(", [");
        // Formatter fmt = new Formatter();
        for (int i = 0; i < this.data.length; i++) {
            if (i > 0) rtv.append(", ");
            //rtv.append(fmt.format(Locale.US, "%.8f", data[i]));
            rtv.append(String.format(Locale.US, "%#8f", data[i]));
        }
        rtv.append("]");

        return rtv.toString();
    }


    //****************** Equality comparing function ******************
    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectByteVector))
            return false;
        if (!super.dataEquals(obj))
            return false;
        if (clusterid != ((ObjectFeatureFloatWClusters)obj).clusterid)
            return false;
        return Arrays.equals(((ObjectFeatureFloatWClusters)obj).data, data);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(data);
    }


    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    @Override
    public int getSize() {
        return super.getSize() + this.data.length * Float.SIZE / 8 + Double.SIZE;
    }

    /** Returns number of dimensions of this vector.
     */
    public int getDimensionality() {
        return this.data.length;
    }

    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeatureType loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFeatureByte from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureFloatWClusters (BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        data = serializator.readFloatArray(input);
        clusterid = serializator.readDouble(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, data) + serializator.write(output, clusterid);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + serializator.getBinarySize(data) + serializator.getBinarySize(clusterid);
    }
}
