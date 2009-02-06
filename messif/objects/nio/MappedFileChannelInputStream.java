/*
 * FileChannelInputStream
 *
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Extending class for a {@link ChannelInputStream} that operates on a
 * file. The position is restored before every read operation, so it is safe
 * to use multiple instances of this class over the same file channel. However,
 * if multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * 
 * @author xbatko
 */
public class MappedFileChannelInputStream extends BufferInputStream {

    /** The file from which to read data */
    private final FileChannel fileChannel;

    /** Starting position of the file */
    private final long startPosition;

    /** The maximal amount of data that can be accessed in the file */
    private final long maxLength;

    /** Internal buffer for the whole file */
    private transient MappedByteBuffer byteBuffer;


    /**
     * Creates a new instance of FileChannelInputStream.
     * @param fileChannel the file channel from which to read data
     * @param position the starting position of the file
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using readChannel
     */
    public MappedFileChannelInputStream(FileChannel fileChannel, long position, long maxLength) throws IOException {
        super(null);
        this.fileChannel = fileChannel;
        this.startPosition = position;
        this.maxLength = maxLength;
    }

    /**
     * Repositions this stream to the starting position.
     */
    @Override
    public void reset() throws IOException {
        setPosition(startPosition);
    }

    @Override
    public void discard() {
        byteBuffer = null;
    }

    /**
     * Returns the current position in the file.
     * @return the current position in the file
     */
    @Override
    public long getPosition() {
        return startPosition + getBuffer().position();
    }

    /**
     * Set the position from which the data will be read.
     * @param position the new position
     * @throws IOException if the specified position is outside the boundaries
     */
    @Override
    public void setPosition(long position) throws IOException {
        try {
            getBuffer().position((int)(position - startPosition));
        } catch (IllegalArgumentException e) {
            throw new IOException("Position " + position + " is outside the allowed range");
        }
    }

    @Override
    protected ByteBuffer getBuffer() {
        if (byteBuffer != null)
            return byteBuffer;
        try {
            byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startPosition, Math.min(fileChannel.size(), maxLength));
            byteBuffer.load();
            return byteBuffer;
        } catch (IOException e) {
            throw new InternalError(e.toString());
        }
    }

    @Override
    public ByteBuffer readInput(int minBytes) throws IOException {
        ByteBuffer buffer = getBuffer();
        if (buffer.remaining() < minBytes)
            throw new EOFException("Cannot read more bytes - end of file encountered");
        return buffer;
    }
}
