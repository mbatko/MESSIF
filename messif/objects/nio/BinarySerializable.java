/*
 * BinarySerializable
 * 
 */

package messif.objects.nio;

import java.io.IOException;

/**
 * The <code>BinarySerializable</code> interface marks the implementing
 * class to be able to serialize itself into a stream of bytes provided
 * by the {@link NativeDataOutput}.
 * 
 * <p>
 * The class should be able to reconstruct itself from these data by
 * providing either a constructor or a factory method.
 * The factory method should have the following prototype:
 * <pre>
 *      <i>ObjectClass</i> binaryDeserialize({@link NativeDataInput} input, int dataSize) throws {@link IOException}
 * </pre>
 * The constructor should have the following prototype:
 * <pre>
 *      <i>ClassConstructor</i>({@link NativeDataInput} input, int dataSize) throws {@link IOException}
 * </pre>
 * The access specificator of the construtor or the factory method is not
 * important and can be even <tt>private</tt>.
 * </p>
 * 
 * @see JavaToBinarySerializable
 * @see NativeDataOutput
 * @see NativeDataInput
 * @author xbatko
 */
public interface BinarySerializable {

    /**
     * Returns the exact size of the serialized version of this object in bytes.
     * @return size of the serialized version of this object
     */
    public int getSize();

    /**
     * Serialize this object into the <code>output</code>.
     * @param output the data output this object is serialized into
     * @throws IOException if there was an I/O error during serialization
     */
    public void binarySerialize(NativeDataOutput output) throws IOException;

}
