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
