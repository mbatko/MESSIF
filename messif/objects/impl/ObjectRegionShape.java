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
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;


/**
 * This is the MPEG-7 Region Shape descriptor.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectRegionShape extends LocalAbstractObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 26501L;

    /****************** Coefficients ******************/

	protected static double [] quantTable = {0.000000000, 0.003585473, 0.007418411, 0.011535520, 0.015982337, 0.020816302, 0.026111312,
            0.031964674, 0.038508176, 0.045926586, 0.054490513, 0.064619488, 0.077016351, 0.092998687, 0.115524524, 0.154032694, 1.000000000};
	protected static double [] iQuantTable = {0.001763817, 0.005468893, 0.009438835, 0.013714449, 0.018346760, 0.023400748, 0.028960940,
            0.035140141, 0.042093649, 0.050043696, 0.059324478, 0.070472849, 0.084434761, 0.103127662, 0.131506859, 0.192540857};

    protected static int ART_ANGULAR = 12;
    protected static int ART_RADIAL = 3;
    

    /****************** Attributes ******************/

    /** An array of RegionShape coefficients - these are indexes to the static tables above */
	private byte [] [] m_ArtDE = new byte [ART_ANGULAR][ART_RADIAL];


    /****************** Constructors ******************/

    /**
     * Initialize all coefficients to "0".
     */
    public ObjectRegionShape() {
        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                m_ArtDE[i][j] = 0;
            }
        }
    }

    /**
     * Read the object from the text file representation: single-dimentional array of bytes.
     * @param stream input strem
     * @throws java.io.IOException if reading from the stream goes wrong
     * @throws java.lang.NumberFormatException if any of the number is not a number
     */
    public ObjectRegionShape(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);

        String[] numbers = line.trim().split("[, ]+");

        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                this.m_ArtDE[i][j] = Byte.parseByte(numbers[(i * ART_RADIAL) + j]);
            }
        }
    }

    /**
     * Write to the text file as a single-dimensional array
     * @param stream the stream to write the data to
     * @throws java.io.IOException if writing to the stream goes wrong
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                stream.write(String.valueOf(m_ArtDE[i][j]).getBytes());
                if (i + 1 < ART_ANGULAR || j + 1 < ART_RADIAL) {
                    stream.write(',');
                    stream.write(' ');
                }
            }
        }
        stream.write('\n');
    }

    // **********************************    Advanced  getters and setters    ********************************** //

    /**
     * Classical setter to the m_ArtDE table
     * @param p index1
     * @param r index2
     * @param value value to store (byte)
     * @return true if set, false if the indexes are out of dimensions
     */
    boolean setElement(int p, int r, byte value) {
        if (p <0 || p >= ART_ANGULAR || r < 0 || r >= ART_RADIAL) {
            return false;
        }
        m_ArtDE[p][r] = value;
        return true;
    }

    /**
     * Set the elelement according to quantization in the static fields.
     * @param p index1
     * @param r index2
     * @param value double value to find in the quantization fields
     * @return true if set, false if the indexes are out of dimensions
     */
    boolean setElement(int p, int r, double value) {
        if (p < 0 || p >= ART_ANGULAR || r < 0 || r >= ART_RADIAL ||
                value > 1.0 || value < 0.0) {
            return false;
        }

        // Quantization
        int high = 17;
        int low = 0;
        int middle;

        while (high - low > 1) {
            middle = (high + low) / 2;

            if (quantTable[middle] < value) {
                low = middle;
            } else {
                high = middle;
            }
        }

        m_ArtDE[p][r] = (byte) low;
        return true;
    }

    /**
     * Get element from the array - index to the quantization arrays.
     * @param p index1
     * @param r index2
     * @return index to the quantization arrays
     */
    public byte getElement(int p, int r) {
        if (p < 0 || p >= ART_ANGULAR || r < 0 || r >= ART_RADIAL) {
            return -1;
        }

        return m_ArtDE[p][r];		// Always positive value
    }

    /**
     * Get element from the array value from the quantization array
     * @param p index1
     * @param r index2
     * @return value from the quantization array
     */
    public double getRealValue(int p, int r) {
        if (p < 0 || p >= ART_ANGULAR || r < 0 || r >= ART_RADIAL) {
            return -1;
        }

        // Inverse Quantization
        return iQuantTable[m_ArtDE[p][r]];	// Always positive value
    }

    /**
     * Get element from the array value from the quantization array - without checking the boundaries
     *   and inlined
     * @param p index1
     * @param r index2
     * @return value from the quantization array
     */
    private final double getRealValueFast(int p, int r) {
        // Inverse Quantization
        return iQuantTable[m_ArtDE[p][r]];	// Always positive value
    }


    // ***********************************    Implementation of abstract methods     *************************** //

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {

        ObjectRegionShape object = (ObjectRegionShape) obj;

        // perform matching
        float distance = 0;

        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                if (i != 0 || j != 0) {
                    distance += Math.abs(getRealValueFast(i, j) - object.getRealValueFast(i, j));
                }
            }
        }

        return distance;
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectRegionShape))
            return false;

        return Arrays.deepEquals(((ObjectRegionShape)obj).m_ArtDE, m_ArtDE);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(m_ArtDE);
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
    protected ObjectRegionShape(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                this.m_ArtDE[i][j] = serializator.readByte(input);
            }
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int retval = super.binarySerialize(output, serializator);
        for (int i = 0; i < ART_ANGULAR; i++) {
            for (int j = 0; j < ART_RADIAL; j++) {
                retval += serializator.write(output, m_ArtDE[i][j]);
            }
        }
        return retval;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return  super.getBinarySize(serializator) + (ART_ANGULAR * ART_RADIAL);
    }
    
}
