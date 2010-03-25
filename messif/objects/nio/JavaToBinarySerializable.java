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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
