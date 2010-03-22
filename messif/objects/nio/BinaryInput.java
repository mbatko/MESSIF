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
 * Interface for classes that can read a binary data.
 * Such data can be processed by a {@link BinarySerializator}.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface BinaryInput {

    /**
     * Returns a buffer that contains at least <code>minBytes</code> bytes.
     * If the buffer with the required number of bytes cannot be provided, an
     * {@link IOException} is thrown. Note that the returned
     * buffer can provide more than <code>minBytes</code>.
     * 
     * @param minBytes the minimal number of bytes that must be available for reading from the buffer
     * @return the buffer prepared for reading
     * @throws IOException if there was an error while preparing a buffer with <code>minBytes</code> bytes
     */
    ByteBuffer readInput(int minBytes) throws IOException;

    /**
     * Skips over and discards <code>n</code> bytes of data from this input.
     * The <code>skip</code> method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly <code>0</code>.
     * This may result from any of a number of conditions; reaching end of file
     * before <code>n</code> bytes have been skipped is only one possibility.
     * The actual number of bytes skipped is returned.
     *
     * @param n the number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @exception IOException if the input does not support seek or some other I/O error occurs
     */
    long skip(long n) throws IOException;
}
