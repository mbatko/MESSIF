/*
 * BinaryInputStream
 * 
 */

package messif.objects.nio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Buffered input stream for binary serializator.
 * This is an abstract input stream that provides access to a 
 * {@link ByteBuffer} that is used in subclasses to fill the
 * data in. There are also two methods for requesting additional
 * buffered data.
 * 
 * 
 * @see BinarySerializator
 * @see BinarySerializable
 * @author xbatko
 */
public abstract class BinaryInputStream extends InputStream {

    /**
     * Returns the buffer for binary operations.
     * @return the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected abstract ByteBuffer getBuffer() throws IOException;

    /**
     * Checks if there is enough data in the buffer. If the <code>minimalSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is compacted
     * and aditional chunk of data is read from the readChannel.
     * Returns either the number of bytes actually available in the buffer or the <code>checkSize</code>
     * whichever is smaller.
     * 
     * @param checkSize the checked number of bytes that should be available in the buffer
     * @param minimalSize the minimal number of bytes that must be available in the buffer
     * @return either the number of bytes actually available or the <code>checkSize</code> whichever is smaller
     * @throws IOException if there was an error using the buffer
     */
    protected abstract int checkBufferSize(int checkSize, int minimalSize) throws IOException;

    /**
     * Checks if there is enough data in the buffer. If the <code>minimalSize</code>
     * is bigger than the actual remaining size of the buffer, the buffer is compacted
     * and aditional chunk of data is read from the readChannel.
     * 
     * @param minimalSize the minimal number of bytes that must be available in the buffer
     * @throws IOException if there was an error using the buffer
     */
    protected abstract void ensureBufferSize(int minimalSize) throws IOException;

    /**
     * Returns the <code>short</code> view of the buffer for binary operations.
     * The number of shorts in the view depends on the state of the underlying
     * buffer. Maximally <code>maxLength</code> and minimaly one short will be
     * available - the actual number of shorts available can be checked using
     * the {@link java.nio.Buffer#remaining remaining} method of the {@link ShortBuffer}.
     * 
     * @param maxLength the maximal number of shorts the view should provide
     * @return the <code>short</code> view of the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected ShortBuffer getBufferShortView(int maxLength) throws IOException {
        int readyItems = checkBufferSize(maxLength << 1, 2) >> 1;
        ByteBuffer byteBuffer = getBuffer();
        ShortBuffer view = byteBuffer.asShortBuffer();
        view.limit(readyItems);
        byteBuffer.position(byteBuffer.position() + (readyItems << 1));
        return view;
    }

    /**
     * Returns the <code>char</code> view of the buffer for binary operations.
     * The number of chars in the view depends on the state of the underlying
     * buffer. Maximally <code>maxLength</code> and minimaly one char will be
     * available - the actual number of chars available can be checked using
     * the {@link java.nio.Buffer#remaining remaining} method of the {@link CharBuffer}.
     * 
     * @param maxLength the maximal number of chars the view should provide
     * @return the <code>char</code> view of the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected CharBuffer getBufferCharView(int maxLength) throws IOException {
        int readyItems = checkBufferSize(maxLength << 1, 2) >> 1;
        ByteBuffer byteBuffer = getBuffer();
        CharBuffer view = byteBuffer.asCharBuffer();
        view.limit(readyItems);
        byteBuffer.position(byteBuffer.position() + (readyItems << 1));
        return view;
    }

    /**
     * Returns the <code>int</code> view of the buffer for binary operations.
     * The number of ints in the view depends on the state of the underlying
     * buffer. Maximally <code>maxLength</code> and minimaly one int will be
     * available - the actual number of ints available can be checked using
     * the {@link java.nio.Buffer#remaining remaining} method of the {@link IntBuffer}.
     * 
     * @param maxLength the maximal number of ints the view should provide
     * @return the <code>int</code> view of the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected IntBuffer getBufferIntView(int maxLength) throws IOException {
        int readyItems = checkBufferSize(maxLength << 2, 4) >> 2;
        ByteBuffer byteBuffer = getBuffer();
        IntBuffer view = byteBuffer.asIntBuffer();
        view.limit(readyItems);
        byteBuffer.position(byteBuffer.position() + (readyItems << 2));
        return view;
    }

    /**
     * Returns the <code>long</code> view of the buffer for binary operations.
     * The number of longs in the view depends on the state of the underlying
     * buffer. Maximally <code>maxLength</code> and minimaly one long will be
     * available - the actual number of longs available can be checked using
     * the {@link java.nio.Buffer#remaining remaining} method of the {@link LongBuffer}.
     * 
     * @param maxLength the maximal number of longs the view should provide
     * @return the <code>long</code> view of the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected LongBuffer getBufferLongView(int maxLength) throws IOException {
        int readyItems = checkBufferSize(maxLength << 3, 8) >> 3;
        ByteBuffer byteBuffer = getBuffer();
        LongBuffer view = byteBuffer.asLongBuffer();
        view.limit(readyItems);
        byteBuffer.position(byteBuffer.position() + (readyItems << 3));
        return view;
    }

    /**
     * Returns the <code>float</code> view of the buffer for binary operations.
     * The number of floats in the view depends on the state of the underlying
     * buffer. Maximally <code>maxLength</code> and minimaly one float will be
     * available - the actual number of floats available can be checked using
     * the {@link java.nio.Buffer#remaining remaining} method of the {@link FloatBuffer}.
     * 
     * @param maxLength the maximal number of floats the view should provide
     * @return the <code>float</code> view of the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected FloatBuffer getBufferFloatView(int maxLength) throws IOException {
        int readyItems = checkBufferSize(maxLength << 2, 4) >> 2;
        ByteBuffer byteBuffer = getBuffer();
        FloatBuffer view = byteBuffer.asFloatBuffer();
        view.limit(readyItems);
        byteBuffer.position(byteBuffer.position() + (readyItems << 2));
        return view;
    }

    /**
     * Returns the <code>double</code> view of the buffer for binary operations.
     * The number of doubles in the view depends on the state of the underlying
     * buffer. Maximally <code>maxLength</code> and minimaly one double will be
     * available - the actual number of doubles available can be checked using
     * the {@link java.nio.Buffer#remaining remaining} method of the {@link DoubleBuffer}.
     * 
     * @param maxLength the maximal number of doubles the view should provide
     * @return the <code>double</code> view of the buffer for binary operations
     * @throws IOException if there was an error using the buffer
     */
    protected DoubleBuffer getBufferDoubleView(int maxLength) throws IOException {
        int readyItems = checkBufferSize(maxLength << 3, 8) >> 3;
        ByteBuffer byteBuffer = getBuffer();
        DoubleBuffer view = byteBuffer.asDoubleBuffer();
        view.limit(readyItems);
        byteBuffer.position(byteBuffer.position() + (readyItems << 3));
        return view;
    }

}
