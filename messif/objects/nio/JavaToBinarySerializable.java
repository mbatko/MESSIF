/*
 * JavaToBinarySerializable
 * 
 */

package messif.objects.nio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This is a helper class to provide the {@link BinarySerializable} wrapping
 * of the native {@link java.io.Serializable serialization} of Java.
 * 
 * @author xbatko
 */
public class JavaToBinarySerializable extends ByteArrayOutputStream implements BinarySerializable {

    /**
     * Creates an instance of a {@link java.io.Serializable serialized} version of the <code>object</code>.
     * 
     * @param object the object from which to create a serialized version
     * @throws IOException if there was an I/O error during serialization
     */
    public JavaToBinarySerializable(Object object) throws IOException {
        ObjectOutputStream objectStream = new ObjectOutputStream(this);
        objectStream.writeObject(object);
        objectStream.close();
    }

    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        serializator.write(output, buf, 0, count);
        return count;
    }

    public int getBinarySize(BinarySerializator serializator) {
        return size();
    }


    //************************ Factory method for deserializing ************************//

    /**
     * Deserialize a previously {@link java.io.Serializable stored} object from input buffer.
     * @param input the buffer from which to read the object
     * @param serializator the serializator used to read the data
     * @return the previously {@link java.io.Serializable stored} object
     * @throws IOException if there was an I/O error during deserialization
     */
    public static Object binaryDeserialize(BinaryInput input, BinarySerializator serializator) throws IOException {
        try {
            byte[] buffer = serializator.readByteArray(input);
            return new ObjectInputStream(new ByteArrayInputStream(buffer)).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.toString());
        }
    }

}
