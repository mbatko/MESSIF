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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
