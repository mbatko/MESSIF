package messif.objects.keys;

import java.io.IOException;
import java.io.Serializable;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author xnovak8
 */
public class DoubleKeyInterval extends KeyInterval<DoubleKey> implements Serializable, BinarySerializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;    
    
    /**
     * Lower bound (inclusive).
     */
    protected final DoubleKey from;
    
    /**
     * Upeer bound (inclusive).
     */
    protected final DoubleKey to;
    
    /**
     * Returns the lower bound.
     * @return the lower bound.
     */
    @Override
    public DoubleKey getFrom() {
        return from;
    }

    /**
     * Returns the upper bound.
     * @return the upper bound.
     */
    @Override
    public DoubleKey getTo() {
        return to;
    }

    /**
     * Constructor for this interval.
     * @param from lower bound (inclusive)
     * @param to upper bound (inclusive)
     */
    public DoubleKeyInterval(DoubleKey from, DoubleKey to) {
        this.from = from;
        this.to = to;
    }

    public int compareTo(KeyInterval<DoubleKey> o) {
        return from.compareTo(o.getFrom());
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of DoubleKeyInterval loaded from binary input.
     * 
     * @param input the input to read the DoubleKeyInterval from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected DoubleKeyInterval(BinaryInput input, BinarySerializator serializator) throws IOException {
        from = serializator.readObject(input, DoubleKey.class);
        to = serializator.readObject(input, DoubleKey.class);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return serializator.write(output, from) + serializator.write(output, to);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize(from) + serializator.getBinarySize(to);
    }
}
