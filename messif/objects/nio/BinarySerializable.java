/*
 * BinarySerializable
 * 
 */

package messif.objects.nio;

import java.io.IOException;

/**
 * The <code>BinarySerializable</code> interface marks the implementing
 * class to be able to serialize itself into a stream of bytes provided
 * by the {@link BinarySerializator}.
 * 
 * <p>
 * The class should be able to reconstruct itself from these data by
 * providing either a constructor or a factory method.
 * The factory method should have the following prototype:
 * <pre>
 *      <i>ObjectClass</i> binaryDeserialize({@link BinaryInput} input, {@link BinarySerializator} serializator) throws {@link IOException}
 * </pre>
 * The constructor should have the following prototype:
 * <pre>
 *      <i>ClassConstructor</i>({@link BinaryInput} input, {@link BinarySerializator} serializator) throws {@link IOException}
 * </pre>
 * The access specificator of the construtor or the factory method is not
 * important and can be even <tt>private</tt>.
 * </p>
 * 
 * @see JavaToBinarySerializable
 * @see BinarySerializator
 * @author xbatko
 */
public interface BinarySerializable {

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    public int getBinarySize(BinarySerializator serializator);

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the binary output that this object is serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes written
     * @throws IOException if there was an I/O error during serialization
     */
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException;

}
