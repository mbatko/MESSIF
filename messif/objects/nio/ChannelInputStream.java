/*
 *  BinaryInput
 * 
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Buffered binary input stream.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several {@link ChannelInputStream ChannelInputStreams} over the
 * same channel (even if synchronized). For file channels, the {@link ByteBufferFileInputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see ChannelOutputStream
 * @author xbatko
 */
public class ChannelInputStream extends BufferInputStream implements BinaryInput {

    //****************** Constants ******************//

    /** Time to wait for additional data (in miliseconds) */
    private final long WAIT_DATA_TIME = 100;


    //****************** Attributes ******************//

    /** The channel used to read data */
    private final ReadableByteChannel readChannel;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of BinaryInput.
     * @param readChannel the channel used to read data
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link java.nio.ByteBuffer#allocateDirect direct}
     */
    public ChannelInputStream(ReadableByteChannel readChannel, int bufferSize, boolean bufferDirect) {
        super(bufferSize, bufferDirect);
        this.readChannel = readChannel;
    }


    //****************** Overriden methods ******************//

    @Override
    protected void read(ByteBuffer buffer) throws EOFException, IOException {
        int readBytes = readChannel.read(buffer);

        // If there are no data, wait a little while and retry
        while (readBytes == 0) {
            try {
                Thread.sleep(WAIT_DATA_TIME);
                readBytes = readChannel.read(buffer);
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            }
        }

        if (readBytes == -1)
            throw new EOFException("Cannot read more bytes - end of file encountered");
    }

}
