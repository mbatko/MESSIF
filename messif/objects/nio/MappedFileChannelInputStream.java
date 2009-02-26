/*
 * FileChannelInputStream
 *
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
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

    /** Starting position of the file */
    private final long startPosition;

    /**
     * Creates a new instance of FileChannelInputStream.
     * @param fileChannel the file channel from which to read data
     * @param position the starting position of the file
     * @param maxLength the maximal length of data
     * @throws IOException if there was an error using readChannel
     */
    public MappedFileChannelInputStream(FileChannel fileChannel, long position, long maxLength) throws IOException {
        super(bufferFile(fileChannel, position, maxLength));
        this.startPosition = position;
    }

    /**
     * Repositions this stream to the starting position.
     */
    @Override
    public void reset() throws IOException {
        setPosition(startPosition);
    }

    /**
     * Returns the current position in the file.
     * @return the current position in the file
     */
    @Override
    public long getPosition() {
        return startPosition + super.getPosition();
    }

    /**
     * Set the position from which the data will be read.
     * @param position the new position
     * @throws IOException if the specified position is outside the boundaries
     */
    @Override
    public void setPosition(long position) throws IOException {
        super.setPosition(position - startPosition);
    }

    @Override
    public ByteBuffer readInput(int minBytes) throws IOException {
        // There is enough data remaining in the buffer
        if (minBytes <= byteBuffer.remaining())
            return byteBuffer;
        else
            throw new EOFException("Cannot read more bytes - end of buffer reached");
    }

    /**
     * Reads the whole file into a buffer.
     * @param fileChannel the file to read the data from
     * @param position the starting position in the file
     * @param maxLength the maximal number of bytes to read
     * @return a buffer with file's data
     * @throws java.io.IOException
     */
    private static ByteBuffer bufferFile(FileChannel fileChannel, long position, long maxLength) throws IOException {
        long mappingBytes = Math.min(fileChannel.size() - position, maxLength);

        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, mappingBytes);
        buffer.load();

        /*
        if (mappingBytes >= Integer.MAX_VALUE)
            throw new IOException("Buffer for " + mappingBytes + " bytes cannot be allocated");
        ByteBuffer buffer = ByteBuffer.allocateDirect((int)mappingBytes);
        if (fileChannel.read(buffer, position) < mappingBytes)
            throw new IOException("File channel provided less than " + mappingBytes + " bytes");
        buffer.flip();
        */

        return buffer;
    }
}
