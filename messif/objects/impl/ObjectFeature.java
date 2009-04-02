package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

public abstract class ObjectFeature extends LocalAbstractObject implements BinarySerializable {

    protected float x,  y,  ori,  scl;

    public ObjectFeature() {
    }

    public ObjectFeature(byte[] data) {
    }

    public ObjectFeature(BufferedReader stream) throws IOException, NumberFormatException {
        String line;
        do {
            line = stream.readLine();
            if (line == null) {
                throw new EOFException ("EoF reached wehile initializing ObjectFeature!");
            }
        } while (processObjectComment(line));
        String[] params = line.trim().split("[, ]+");
        this.x = Float.parseFloat(params[0]);
        this.y = Float.parseFloat(params[1]);
        this.ori = Float.parseFloat(params[2]);
        this.scl = Float.parseFloat(params[3]);
    }

    public void writeData(OutputStream stream) throws IOException {
        stream.write(String.format("%1$f, %2$f, %3$f, %4$f", this.x, this.y, this.ori, this.scl).getBytes());
        stream.write('\n');
    }

    //****************** Equality comparing function ******************

    public boolean dataEquals(Object obj) {
        return (((ObjectFeature)obj).x == this.x && ((ObjectFeature)obj).y == this.y
                && ((ObjectFeature)obj).ori == this.ori && ((ObjectFeature)obj).scl == this.scl);
    }

    //****************** Size function ******************

    /** Returns the size of object in bytes
     */
    public int getSize() {
        return 4 * Float.SIZE; // x, y, scl, ori * sizeof (4)
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
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, this.x) + serializator.write(output, this.y) +
               + serializator.write(output, this.ori) + serializator.write(output, this.scl);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 16;
    }

    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new CloneNotSupportedException("cloneRandomlyModify not supported yet");
    }
}
