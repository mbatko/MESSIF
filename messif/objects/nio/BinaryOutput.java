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
import java.nio.ByteBuffer;

/**
 * Interface for classes that can write a binary data.
 * Such data can be provided by a {@link BinarySerializator}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface BinaryOutput {

    /**
     * Returns a buffer that allows to write at least <code>minBytes</code>.
     * If the buffer with the required space cannot be provided, an
     * {@link IOException} is thrown. Note that the returned
     * buffer can provide more than <code>minBytes</code>.
     * 
     * @param minBytes the minimal number of bytes that must be available for writing into the buffer
     * @return the buffer prepared for writing
     * @throws IOException if there was an error while preparing a buffer for <code>minBytes</code> bytes
     */
    ByteBuffer prepareOutput(int minBytes) throws IOException;

    /**
     * Flushes this output and forces any buffered output bytes 
     * to be written out to the flushChannel.
     * 
     * @throws IOException if there was an error using flushChannel
     */
    void flush() throws IOException;
}
