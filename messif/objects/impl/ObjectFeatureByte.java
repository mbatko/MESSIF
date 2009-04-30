package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

public abstract class ObjectFeatureByte extends ObjectFeature  implements BinarySerializable {

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
    public ObjectFeatureByte(short[] data) {
        this.data = new short[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    /** Creates a new instance of randomly generated object */
    public ObjectFeatureByte(int dimension) {
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
    public ObjectFeatureByte(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        super(stream);
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectFeatureByte.");
        } while (processObjectComment(line));
        // precti normalizovany vektor
        String[] numbers = line.trim().split("[, ]+");

        this.data = new short[numbers.length];

        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = Short.parseShort(numbers[i]);
        }
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
    }

    /** toString
     * Converts the object to a string representation.
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

    //****************** Equality comparing function ******************
    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectByteVector))
            return false;
        if (!super.dataEquals(obj))
            return false;
        return Arrays.equals(((ObjectFeatureByte)obj).data, data);
    }

    public int dataHashCode() {
        return Arrays.hashCode(data);
    }

    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    @Override
    public int getSize() {
        return super.getSize() + this.data.length * Short.SIZE / 8;
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
    protected ObjectFeatureByte (BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        data = serializator.readShortArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, data);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + serializator.getBinarySize(data);
    }
}