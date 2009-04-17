/*
 * ChannelOutputStream
 * 
 */

package messif.objects.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Buffered output stream for operating over channels.
 * 
 * <p>
 * Note that it is <em>not safe</em> to use several {@link ChannelOutputStream ChannelOutputStreams} over the
 * same channel (even if synchronized). For file channels, the {@link FileChannelOutputStream}
 * can be used if you need this functionality. Use copy-pipes if you need it
 * on other channel types.
 * </p>
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 * 
 * @see ChannelInputStream
 * @author xbatko
 */
public class ChannelOutputStream extends BufferOutputStream implements BinaryOutput {

    //****************** Attributes ******************//

    /** The channel used to write data */
    private final WritableByteChannel writeChannel;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of ChannelOutputStream.
     * @param bufferSize the size of the internal buffer used for flushing
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @param writeChannel the channel into which to write data
     * @throws IOException if there was an error using writeChannel
     */
    public ChannelOutputStream(int bufferSize, boolean bufferDirect, WritableByteChannel writeChannel) throws IOException {
        super(bufferSize, bufferDirect);
        this.writeChannel = writeChannel;
    }

    /** 
     * Writes the buffered data to the write channel.
     * 
     * @param buffer the buffer from which to write data
     * @throws IOException if there was an error writing the data
     */
    @Override
    protected void write(ByteBuffer buffer) throws IOException {
        writeChannel.write(buffer);
    }

}
