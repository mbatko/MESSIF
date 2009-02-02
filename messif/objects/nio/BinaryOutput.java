/*
 *  BinaryOutput
 * 
 */

package messif.objects.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for classes that can write a binary data.
 * Such data can be provided by a {@link BinarySerializator}.
 * 
 * @author xbatko
 */
public interface BinaryOutput {

    /**
     * Returns a buffer has at least <code>minBytes</code> bytes remaining.
     * If the current buffered data is not big enough, the buffer is flushed
     * to the flushChannel and some space is freed. Note that the returned
     * buffer can provide more than <code>minBytes</code> space.
     * 
     * @param minBytes the minimal number of bytes that must be available for writing into the buffer
     * @return the buffer with prepared writing
     * @throws IOException if there was an error writing data or the requested <code>minBytes</code> is too big
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
