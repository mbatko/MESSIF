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

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Output stream that implements the {@link BinaryOutput} using
 * an internal {@link ByteBuffer buffer}.
 * 
 * <p>
 * If multiple threads use the same instance of this class, the access to the
 * instance must be synchronized.
 * </p>
 *
 * @see BufferInputStream
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class BufferOutputStream extends OutputStream implements BinaryOutput {

    //****************** Constants ******************//

    /** Minimal buffer size in bytes */
    public static final int MINIMAL_BUFFER_SIZE = 32;


    //****************** Attributes ******************//

    /** The buffer where data is stored */
    private final ByteBuffer byteBuffer;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of BufferOutputStream.
     * The output operates on a newly created buffer with the specified size.
     * @param bufferSize the size of the internal buffer
     * @param bufferDirect allocate the internal buffer as {@link ByteBuffer#allocateDirect direct}
     * @throws IllegalArgumentException if there specified buffer size is not valid
     */
    public BufferOutputStream(int bufferSize, boolean bufferDirect) throws IllegalArgumentException {
        if (bufferSize < MINIMAL_BUFFER_SIZE)
            throw new IllegalArgumentException("Buffer must be at least " + MINIMAL_BUFFER_SIZE + " bytes long");
        if (bufferDirect)
            this.byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        else
            this.byteBuffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Creates a new instance of BufferOutputStream.
     * The output operates on the the given buffer.
     * @param buffer the internal buffer this stream
     */
    public BufferOutputStream(ByteBuffer buffer) {
        this.byteBuffer = buffer;
    }


    //****************** Buffered data info ******************//

    /**
     * Returns <tt>true</tt> if there are some bytes pending in the buffer.
     * @return <tt>true</tt> if there are some bytes pending in the buffer
     * @see #flush
     */
    public boolean isDirty() {
        return byteBuffer.position() > 0;
    }

    /**
     * Returns the number of bytes currently in the buffer.
     * @return the number of bytes currently in the buffer
     */
    public int bufferedSize() {
        return byteBuffer.position();
    }

    /**
     * Limit the buffer to the specified size.
     * Note that the buffer size is only update if the actual limit is greater that the size.
     * The size computation includes the actually buffered data.
     * 
     * @param size the number of bytes the buffer can accommodate (including the currently stored data)
     */
    protected void setBufferedSizeLimit(long size) {
        if (byteBuffer.limit() > size) { // This ensures that size is actually smaller than int
            int newLimit = (int)(size - byteBuffer.position());
            if (newLimit < 0)
                throw new InternalError("Attemping to discard buffered data: " + byteBuffer.position() + "B buffered, but setting size to: " + size);
            byteBuffer.limit(newLimit);
        }
    }


    //****************** Output stream implementation ******************//

    /**
     * Writes the specified byte to this output stream. 
     * @param b the byte to be written
     * @throws IOException if there was an error using writeChannel
     */
    @Override
    public void write(int b) throws IOException {
        prepareOutput(1).put((byte)b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream.
     *
     * @param bytes the data
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException if there was an error using writeChannel
     */
    @Override
    public void write(byte bytes[], int off, int len) throws IOException {
        while (len > 0) {
            ByteBuffer buffer = prepareOutput(1);
            int lenToWrite = Math.min(len, buffer.remaining());
            buffer.put(bytes, off, lenToWrite);
            off += lenToWrite;
            len -= lenToWrite;
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes 
     * to be written out.
     * 
     * @throws IOException if there was an error using writeChannel
     */
    @Override
    public void flush() throws IOException {
        // Must empty the whole buffer, so require to free the whole capacity of the buffer
        prepareOutput(byteBuffer.limit());
    }

    @Override
    public void close() throws IOException {
        flush();
    }


    //****************** Binary output implementation ******************//

    @Override
    public ByteBuffer prepareOutput(int minBytes) throws IOException {
        // If there is enough space in the buffer, do nothing
        if (minBytes <= byteBuffer.remaining())
            return byteBuffer;

        // Try to free some space
        write(byteBuffer);

        // Check the remaining size
        if (minBytes > byteBuffer.remaining())
            throw new EOFException("Buffer is too small to provide " + minBytes + " bytes");

        return byteBuffer;
    }

    /** 
     * Writes the buffered data out.
     * This method is responsible for writing all the buffered data from the
     * specified buffer. That is, the data from the beginning of the buffer
     * up to the current position.
     * 
     * @param buffer the buffer from which to write data
     * @throws IOException if there was an error writing the data
     */
    protected void write(ByteBuffer buffer) throws IOException {
        throw new IOException("The buffer is full");
    }

    /**
     * Writes the buffered data to a byte array.
     * Note that this <i>will consume</i> all the buffered data as with the other write methods.
     * @return the buffered data
     */
    public byte[] write() {
        byte[] ret = new byte[bufferedSize()];
        try {
            byteBuffer.flip();
            byteBuffer.get(ret);
        } finally {
            byteBuffer.compact();
        }
        return ret;
    }

    /**
     * Writes the buffered data to the specified channel.
     * The {@link WritableByteChannel#write(java.nio.ByteBuffer) write}
     * method is called on the channel.
     * 
     * @param channel the writable channel that the buffered data is written to
     * @return number of bytes actually written
     * @throws IOException if there was an I/O error writing to the channel
     */
    public int write(WritableByteChannel channel) throws IOException {
        try {
            byteBuffer.flip();
            return channel.write(byteBuffer);
        } finally {
            byteBuffer.compact();
        }
    }

    /**
     * Writes the buffered data to the specified file channel.
     * The {@link FileChannel#write(java.nio.ByteBuffer, long) write}
     * method is called on the channel.
     * 
     * @param channel the file channel that the buffered data is written to
     * @param position the position in the file where to write the data
     * @return number of bytes actually written
     * @throws IOException if there was an I/O error writing to the channel
     */
    public int write(FileChannel channel, long position) throws IOException {
        try {
            byteBuffer.flip();
            return channel.write(byteBuffer, position);
        } finally {
            byteBuffer.compact();
        }
    }
}
