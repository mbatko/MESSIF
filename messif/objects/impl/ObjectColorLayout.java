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
import java.util.Random;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectColorLayout extends LocalAbstractObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 2L;

    /****************** Attributes ******************/

    protected byte YCoeff[];
    protected byte CbCoeff[];
    protected byte CrCoeff[];


    /****************** Constructors ******************/

    /** Creates a new instance of ObjectHomogeneousTexture */
    public ObjectColorLayout(byte[] YCoeff, byte[] CbCoeff, byte[] CrCoeff) {
        this.YCoeff = YCoeff;
        this.CbCoeff = CbCoeff;
        this.CrCoeff = CrCoeff;
    }


    //****************** Text file store/retrieve methods ******************
    
    /** Creates a new instance of ObjectHomogeneousTexture from stream.
     * Throws IOException when an error appears during reading from given stream.
     * Throws EOFException when eof of the given stream is reached.
     * Throws NumberFormatException when the line read from given stream does 
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectColorLayout(BufferedReader stream) throws IOException, NumberFormatException, IndexOutOfBoundsException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        
        String[] fields = line.trim().split(";\\p{Space}*");
        this.YCoeff = split(fields[0], ",\\p{Space}*");
        this.CbCoeff = split(fields[1], ",\\p{Space}*");
        this.CrCoeff = split(fields[2], ",\\p{Space}*");
    }

    /** Write object to text stream */
    @Override
    public void writeData(OutputStream stream) throws IOException {
        join(stream, YCoeff);
        stream.write(';');
        stream.write(' ');
        join(stream, CbCoeff);
        stream.write(';');
        stream.write(' ');
        join(stream, CrCoeff);
        stream.write('\n');
    }

    protected static byte[] split(String str, String split) {
        String[] values = str.trim().split(split);
        byte[] rtv = new byte[values.length];
        for (int i = 0; i < values.length; i++)
            rtv[i] = Byte.parseByte(values[i]);
        return rtv;
    }

    protected static void join(OutputStream stream, byte[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            stream.write(String.valueOf(values[i]).getBytes());
            if (i + 1 < values.length) {
                stream.write(',');
                stream.write(' ');
            }
        }
    }


    /****************** Size function ******************/

    @Override
    public int getSize() {
        return (YCoeff.length + CbCoeff.length + CrCoeff.length) * Byte.SIZE / 8;
    }


    /****************** Data equality functions ******************/

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectColorLayout))
            return false;
        ObjectColorLayout castObj = (ObjectColorLayout)obj;
        return
                Arrays.equals(YCoeff, castObj.YCoeff) &&
                Arrays.equals(CbCoeff, castObj.CbCoeff) &&
                Arrays.equals(CrCoeff, castObj.CrCoeff);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(YCoeff) + Arrays.hashCode(CbCoeff) + Arrays.hashCode(CrCoeff);
    }


    /****************** Distance function ******************/

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectColorLayout castObj = (ObjectColorLayout)obj;
        return (float)(Math.sqrt(sumCoeff(YWeights, YCoeff, castObj.YCoeff)) + 
               Math.sqrt(sumCoeff(CbWeights, CbCoeff, castObj.CbCoeff)) +
               Math.sqrt(sumCoeff(CrWeights, CrCoeff, castObj.CrCoeff)));
    }

    private static byte YWeights[] = {3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private static byte CbWeights[] = {2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private static byte CrWeights[] = {4, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    protected static float sumCoeff(byte[] weights, byte[] coeffs1, byte[] coeffs2) {
        float rtv = 0;
        for (int j = Math.min(coeffs1.length, coeffs2.length) - 1; j >= 0; j--) {
            int diff = coeffs1[j] - coeffs2[j];
            rtv += weights[j]*diff*diff;
        }
        return rtv;
    }
    
    /********************  Cloning ***************************/
    
    /**
     * Creates and returns a randomly modified copy of this vector.
     * Selects a position in the "energy" array in random and changes it - the final value stays in the given range.
     * The modification is small - only by (max-min)/1000
     *
     * @param  args  expected size of the array is 2: <b>minVector</b> vector with minimal values in all positions
     *         <b>maxVector</b> vector with maximal values in all positions
     * @return a randomly modified clone of this instance.
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        ObjectColorLayout rtv = (ObjectColorLayout) this.clone();
        rtv.YCoeff = this.YCoeff.clone();
        rtv.CbCoeff = this.CbCoeff.clone();
        rtv.CrCoeff = this.CrCoeff.clone();
        
        try {
            ObjectColorLayout minVector = (ObjectColorLayout) args[0];
            ObjectColorLayout maxVector = (ObjectColorLayout) args[1];
            Random random = new Random(System.currentTimeMillis());
            
            // pick one of the vectors in random
            int vecNumber = random.nextInt(3);
            int position;
            if (vecNumber == 0) {
                // pick a vector position in random
                position = random.nextInt(Math.min(rtv.YCoeff.length, Math.min(minVector.YCoeff.length, maxVector.YCoeff.length)));
                
                // calculate 1/1000 of the possible range of this value and either add or substract it from the origival value
                int smallStep = Math.max((maxVector.YCoeff[position] - minVector.YCoeff[position]) / 1000, 1);
                if (rtv.YCoeff[position] + smallStep <= maxVector.YCoeff[position])
                    rtv.YCoeff[position] += smallStep;
                else rtv.YCoeff[position] -= smallStep;
            } else if (vecNumber == 1) {
                // pick a vector position in random
                position = random.nextInt(Math.min(rtv.CbCoeff.length, Math.min(minVector.CbCoeff.length, maxVector.CbCoeff.length)));

                // calculate 1/1000 of the possible range of this value and either add or substract it from the origival value
                int smallStep = Math.max((maxVector.CbCoeff[position] - minVector.CbCoeff[position]) / 1000, 1);
                if (rtv.CbCoeff[position] + smallStep <= maxVector.CbCoeff[position])
                    rtv.CbCoeff[position] += smallStep;
                else rtv.CbCoeff[position] -= smallStep;
            } else {
                // pick a vector position in random
                position = random.nextInt(Math.min(rtv.CrCoeff.length, Math.min(minVector.CrCoeff.length, maxVector.CrCoeff.length)));
                
                // calculate 1/1000 of the possible range of this value and either add or substract it from the origival value
                int smallStep = Math.max((maxVector.CrCoeff[position] - minVector.CrCoeff[position]) / 1000, 1);
                if (rtv.CrCoeff[position] + smallStep <= maxVector.CrCoeff[position])
                    rtv.CrCoeff[position] += smallStep;
                else rtv.CrCoeff[position] -= smallStep;
            }
        } catch (ArrayIndexOutOfBoundsException ignore) {
        } catch (ClassCastException ignore) { }
        
        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectColorLayout loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectColorLayout from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectColorLayout(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        YCoeff = serializator.readByteArray(input);
        CbCoeff = serializator.readByteArray(input);
        CrCoeff = serializator.readByteArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, YCoeff) +
               serializator.write(output, CbCoeff) +
               serializator.write(output, CrCoeff);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(YCoeff) +
                serializator.getBinarySize(CbCoeff) + serializator.getBinarySize(CrCoeff);
    }
    
}
