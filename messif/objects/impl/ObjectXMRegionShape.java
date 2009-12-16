
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
 * This is the MPEG-7 Region Shape descriptor.
 *
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:david.novak@fi.muni.cz">david.novak@fi.muni.cz</a>
 */
public class ObjectXMRegionShape extends LocalAbstractObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 26501L;

    //****************** Coefficients ******************//

    /** Internal quantization table */
    protected static double [] quantTable = {0.000000000, 0.003585473, 0.007418411, 0.011535520, 0.015982337, 0.020816302, 0.026111312,
            0.031964674, 0.038508176, 0.045926586, 0.054490513, 0.064619488, 0.077016351, 0.092998687, 0.115524524, 0.154032694, 1.000000000};
    /** Internal inverse quantization table */
    protected static double [] iQuantTable = {0.001763817, 0.005468893, 0.009438835, 0.013714449, 0.018346760, 0.023400748, 0.028960940,
            0.035140141, 0.042093649, 0.050043696, 0.059324478, 0.070472849, 0.084434761, 0.103127662, 0.131506859, 0.192540857};

    /** Number of angular items */
    protected static int ART_ANGULAR = 12;
    /** Number of radial items */
    protected static int ART_RADIAL = 3;
    

    /****************** Attributes ******************/

    /** An array of RegionShape coefficients - these are indexes to the static tables above */
    private final byte[] artDE;


    /****************** Constructors ******************/

    /**
     * Create new instance of ObjectRegionShape.
     * The descriptor is initialized to zeros.
     */
    public ObjectXMRegionShape() {
        artDE = new byte [ART_ANGULAR*ART_RADIAL];
    }

    /**
     * Read the object from the text file representation: single-dimentional array of bytes.
     * @param stream input strem
     * @throws java.io.IOException if reading from the stream goes wrong
     * @throws java.lang.NumberFormatException if any of the number is not a number
     */
    public ObjectXMRegionShape(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing ObjectRegionShape.");
        } while (processObjectComment(line));

        String[] numbers = line.trim().split("[, ]+");

        this.artDE = new byte [ART_ANGULAR*ART_RADIAL];
        for (int i = 0; i < this.artDE.length; i++)
            this.artDE[i] = Byte.parseByte(numbers[i]);
    }

    /**
     * Write to the text file as a single-dimensional array
     * @param stream the stream to write the data to
     * @throws java.io.IOException if writing to the stream goes wrong
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        for (int i = 0; i < this.artDE.length; i++) {
            if (i != 0) {
                stream.write(',');
                stream.write(' ');
            }
            stream.write(String.valueOf(artDE[i]).getBytes());
        }
        stream.write('\n');
    }

    // **********************************    Advanced  getters and setters    ********************************** //

    /**
     * Get element from the array - index to the quantization arrays.
     * @param p index1
     * @param r index2
     * @return index to the quantization arrays
     */
    public byte getElement(int p, int r) {
        return artDE[(p * ART_RADIAL) + r];
    }

    /**
     * Get element from the array value from the quantization array
     * @param p index1
     * @param r index2
     * @return value from the quantization array
     */
    public double getRealValue(int p, int r) {
        // Inverse Quantization
        return iQuantTable[getElement(p, r)];
    }


    // ***********************************    Implementation of abstract methods     *************************** //

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectXMRegionShape object = (ObjectXMRegionShape) obj;

        // perform matching
        float distance = 0;

        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                if (i != 0 || j != 0) {
                    distance += Math.abs(getRealValue(i, j) - object.getRealValue(i, j));
                }
            }
        }

        return distance;
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectXMRegionShape))
            return false;

        return Arrays.equals(((ObjectXMRegionShape)obj).artDE, artDE);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(artDE);
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSize() {
        return ART_ANGULAR * ART_RADIAL * (Byte.SIZE / 8);
    }


    // ********************   BinarySerializable interface   ******************************* //

    /**
     * Creates a new instance of ObjectRegionShape loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectByteVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectXMRegionShape(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.artDE = serializator.readByteArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int retval = super.binarySerialize(output, serializator);
        retval += serializator.write(output, artDE);
        return retval;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(artDE);
    }
    
}
