/*
 *  BinaryInput
 * 
 */

package messif.objects.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for classes that can read a binary data.
 * Such data can be processed by a {@link BinarySerializator}.
 * 
 * @author xbatko
 */
public interface BinaryInput {

    /**
     * Returns a buffer that contains at least <code>minBytes</code> bytes.
     * If the current buffered data is not big enough, the buffer is compacted
     * and aditional data is read from the readChannel. Note that the returned
     * buffer can contain more that <code>minBytes</code> data.
     * 
     * @param minBytes the minimal number of bytes that must be available for reading from the buffer
     * @return the buffer with prepared data
     * @throws IOException if there was an error reading additional data or the requested <code>minBytes</code> is too big
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
