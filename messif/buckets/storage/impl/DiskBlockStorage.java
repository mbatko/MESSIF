/*
 *  DiskBlockStorage
 * 
 */

package messif.buckets.storage.impl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import messif.buckets.BucketStorageException;
import messif.buckets.CapacityFullException;
import messif.buckets.StorageFailureException;
import messif.buckets.storage.LongAddress;
import messif.buckets.storage.LongStorage;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.BufferInputStream;
import messif.objects.nio.BufferOutputStream;
import messif.objects.nio.FileChannelOutputStream;

/**
 * Disk based storage.
 * The objects in this storage are stored in a file in the order
 * of insertion. The address is the position within the file.
 * Objects are serialized using the provided {@link BinarySerializator}.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public class DiskBlockStorage<T> implements LongStorage<T>, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** The prefix for auto-generated filenames */
    protected static final String FILENAME_PREFIX = "disk_storage_";

    /** The suffix for auto-generated filenames */
    protected static final String FILENAME_SUFFIX = ".ds";

    /** Header flag constant for indication whether the file was correctly closed */
    protected static final int FLAG_CLOSED = 0x00000003; // lower two bits


    //****************** Attributes ******************//

    /** The file with data */
    protected final File file;

    /** The position in the file where this storage starts (the real data starts at startPosition + headerSize) */
    protected final long startPosition;

    /** The maximal position that can be accessed in the file */
    protected final long endPosition;

    /** Serializator responsible for storing (and restoring) binary objects in the file */
    protected final BinarySerializator serializator;

    /** Class of objects that the this storage works with */
    protected final Class<? extends T> storedObjectsClass;


    /** The channel on the file with data */
    private transient final FileChannel fileChannel;

    
    /** Flag whether the file is modified */
    private transient boolean modified;

    /** Buffer for reading the mapped file */
    private transient MappedByteBuffer buffer;

    /** Stream for writing data */
    protected transient final FileChannelOutputStream outputStream;

    /** The number of objects currently stored in the file */
    private transient int objectCount;

    /** The number of deleted objects - the file space fragmentation is the ratio between this and objectCount */
    private transient int deletedFragments;

    /** The number of bytes currently stored in the file (excluding headers) */
    protected transient long fileOccupation;


    //****************** Constructors ******************//

    /**
     * Creates a new DiskStreamStorage instance.
     * 
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param file the file in which to create the bucket
     * @param bufferSize the size of the buffer used for reading/writing
     * @param bufferDirect the bucket is either direct (<tt>true</tt>) or array-backed (<tt>false</tt>)
     * @param startPosition the position in the file where this storage starts
     * @param maximalLength the maximal length of the file
     * @param serializator the object responsible for storing (and restoring) binary objects
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskBlockStorage(Class<? extends T> storedObjectsClass, File file, int bufferSize, boolean bufferDirect, long startPosition, long maximalLength, BinarySerializator serializator) throws IOException {
        this.storedObjectsClass = storedObjectsClass;
        this.file = file;
        this.startPosition = startPosition;
        this.endPosition = startPosition + maximalLength;
        this.serializator = serializator;
        this.fileChannel = openFileChannel(file);
        this.outputStream = null;
    }

    /**
     * Flush file data before garbage collection.
     * The method updates header and closes the file.
     * 
     * @throws Throwable if there was an error during releasing resources
     */
    public void destroy() throws Throwable {
        if (modified) {
            writeHeader(fileChannel, startPosition, FLAG_CLOSED);
            flush();
        }
        destroyBuffer();
        fileChannel.close();
        super.finalize();
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }


    //****************** Buffer functions ******************//

    /**
     * Returns the buffer with the file's data.
     * @param fileChannel the file channel for which to get the buffer
     * @return the buffer with the file's data
     * @throws IOException if there was a problem reading the file
     */
    private ByteBuffer getBuffer(FileChannel fileChannel) throws IOException {
        // Locking...

        // Create memory mapped file if not ready
        if (buffer == null) {
            buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,
                startPosition + headerSize,
                Math.min(fileChannel.size(), endPosition) - startPosition - headerSize
            );
            buffer.load();
        }
            
        return buffer;
    }

    /**
     * Returns the buffer with the file's data.
     * @return the buffer with the file's data
     * @throws IOException if there was a problem reading the file
     */
    ByteBuffer getBuffer() throws IOException {
        return getBuffer(fileChannel);
    }

    /**
     * Invalidates the current buffer.
     * Next read operation should create a new one using {@link #getBuffer()}.
     */
    void destroyBuffer() {
        buffer = null;
    }

    /**
     * Converts the absolute position to relative position inside the buffer.
     * @param position the absolute position in the file
     * @return the relative position in this storage's {@link #getBuffer() buffer}
     */
    int toBufferPosition(long position) {
        return (int)(position - startPosition - headerSize);
    }

    /**
     * Flushes this output stream and forces any buffered output bytes 
     * to be written out to the underlying file.
     * 
     * @throws IOException if there was an I/O error
     */
    public void flush() throws IOException {
        if (modified && buffer != null)
            buffer.force();
    }


    //****************** Header functions ******************//

    /** The size of the header - must match the {@link #writeHeader} and {@link #readHeader} methods */
    protected static final int headerSize = (3*Long.SIZE + 4*Integer.SIZE)/8;

    /**
     * Write header information to the file.
     * The {@link #objectCount}, {@link #fileOccupation} and {@link #deletedFragments}
     * are stored. The write is two-phase, with synchronized flag for opened/closed file.
     * 
     * @param fileChannel the file channel to write the header to
     * @param position the position in the file channel to write the header to
     * @param flags the flags accompanied with the bucket; 
     *          currently only "opened/closed" flag is used to resolve validity of the header
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected synchronized void writeHeader(FileChannel fileChannel, long position, int flags) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(headerSize);
        buf.putLong(serialVersionUID);
        buf.putLong(endPosition - startPosition);
        buf.putInt(serializator.hashCode()); // Hash of the serializator (hashes of the UUIDs of the cached objects)
        buf.putInt(flags & ~FLAG_CLOSED); // Closed bits are set to zero for the first time even if closing
        buf.putInt(objectCount);
        buf.putInt(deletedFragments);
        buf.flip();
        fileChannel.write(buf, position);
        if ((flags & FLAG_CLOSED) == FLAG_CLOSED) {
            // Replace flag with closed bit
            buf.putInt(20, flags); // !!!! WARNING !!!! Don't forget to change the position here !!!!
            buf.rewind();
            fileChannel.force(true);
            fileChannel.write(buf, position);
            modified = false;
        } else {
            modified = true;
        }
    }

    /**
     * Read header information from the file.
     * The {@link #objectCount}, {@link #fileOccupation} and {@link #deletedFragments}
     * variables are replaced by the values from the header.
     * 
     * @param fileChannel the file channel to read the header from
     * @param position the position in the file channel to read the header from
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected synchronized void readHeader(FileChannel fileChannel, long position) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(headerSize);

        // Read header bytes
        fileChannel.read(buf, position); // If small amount of data is read, the exception is thrown later
        buf.flip();

        try {
            // Check header magic number
            long tmpLong = buf.getLong();
            if (tmpLong != serialVersionUID)
                throw new IOException("Wrong bucket serial version UID: " + tmpLong + " should be " + serialVersionUID);

            // Check if capacity matches
            tmpLong = startPosition + buf.getLong();
            if (tmpLong != endPosition)
                throw new IOException("Wrong end position in the file: " + tmpLong + " should be " + endPosition);

            // Check if the stored class matches
            tmpLong = buf.getInt();
            if (tmpLong != serializator.hashCode())
                throw new IOException("Hash codes for the serializator do not match");

            // Read flags
            int flags = buf.getInt();
            if ((flags & FLAG_CLOSED) == FLAG_CLOSED) {
                // The file was closed correctly (lower two bits are set), we can be sure the header is OK
                objectCount = buf.getInt();
                deletedFragments = buf.getInt();
                modified = false;
            } else {
                // Header indicates pending close, so it is probably incorrect - reconstruct it from the file
                reconstructHeader(fileChannel, position + headerSize);
                writeHeader(fileChannel, position, FLAG_CLOSED);
            }
        } catch (BufferUnderflowException e) {
            throw new IOException("Header is corrupted, consider removing the file " + file);
        }
    }

    /**
     * Reconstruct header information by seeking through the whole file.
     * The {@link #objectCount}, {@link #fileOccupation} and {@link #deletedFragments}
     * variables are replaced by the reconstructed values.
     * 
     * @param fileChannel the file channel to read the header from
     * @param position the position in the file channel to read the header from
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected synchronized void reconstructHeader(FileChannel fileChannel, long position) throws IOException {
        // Reset header values
        objectCount = 0;
        deletedFragments = 0;

        // Read all objects (by seeking)
        BinaryInput reader = new BufferInputStream(getBuffer(fileChannel));
        try {
            // End iterating one an "null" object is found
            for (int objectSize = serializator.skipObject(reader, false); objectSize != 0; objectSize = serializator.skipObject(reader, false)) {
                if (objectSize > 0) {
                    objectCount++;
                } else {
                    // Negative size means deleted object
                    deletedFragments++;
                }
            }
        } catch (EOFException ignore) {
        }
    }


    //****************** Construction methods ******************//

    /**
     * Opens the file channel on <code>file</code> and reads the header.
     * @param file the file to open the channel on
     * @return the new file channel
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected FileChannel openFileChannel(File file) throws IOException {
        // If file does not exist before, it is auto-created by the RandomAccessFile constructor
        boolean fileExists = file.length() > startPosition;

        // Open the channel
        FileChannel chan = new RandomAccessFile(file, "rw").getChannel();

        // Read the occupation and number of objects
        if (fileExists)
            readHeader(chan, startPosition);
        else
            writeHeader(chan, startPosition, FLAG_CLOSED);

        return chan;
    }


    //****************** Serialization ******************//

    /**
     * Read the serialized disk storage from an object stream.
     * @param in the object stream from which to read the disk storage
     * @throws IOException if there was an I/O error during deserialization
     * @throws ClassNotFoundException if there was an unknown object in the stream
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        try {
            // Proceed with standard deserialization first
            in.defaultReadObject();

            // Reopen file channel (set it through reflection to overcome the "final" flag)
            Field field = DiskBlockStorage.class.getDeclaredField("fileChannel");
            field.setAccessible(true);
            field.set(this, openFileChannel(file));
        } catch (NoSuchFieldException e) {
            throw new ClassNotFoundException(e.toString());
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("This should never happen!", e);
        }
    }


    //****************** Implementation of the storage ******************//

    /**
     * Returns the number of objects stored in this storage.
     * @return the number of objects stored in this storage
     */
    public int size() {
        return objectCount;
    }

    public LongAddress<T> store(T object) throws BucketStorageException {
        try {
            // Write the data into a prepared buffer
            BufferOutputStream buf = serializator.write(object, true);

            synchronized (this) {
                // Check maximal size
                if (fileChannel.size() + buf.bufferedSize() > endPosition)
                    throw new CapacityFullException("Stored object is too big to fit into the storage");

                // Set modified flag
                if (!modified)
                    writeHeader(fileChannel, startPosition, 0);

                // Remember address
                LongAddress<T> address = new LongAddress<T>(this, fileChannel.size());

                // Write object
                buf.write(fileChannel, fileChannel.size());

                // Update internal counters
                objectCount++;

                // Invalidate buffer
                destroyBuffer();

                return address;
            }
        } catch (IOException e) {
            throw new StorageFailureException("Cannot store object into disk storage", e);
        }
    }

    public synchronized void remove(long position) throws BucketStorageException {
        try {
            ByteBuffer bufferView = getBuffer();
            int objectSize = bufferView.getInt(toBufferPosition(position));

            if (objectSize > 0) {
                // Set modified flag
                if (!modified)
                    writeHeader(fileChannel, startPosition, 0);

                bufferView.putInt(toBufferPosition(position), -objectSize);

                // Update internal counters
                objectCount--;
                deletedFragments++;
            }
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot remove object from position " + position, e);
        }
    }

    public T read(long position) throws BucketStorageException {
        try {
            ByteBuffer bufferView = getBuffer().duplicate();
            bufferView.position(toBufferPosition(position));
            return serializator.readObject(new BufferInputStream(bufferView), storedObjectsClass);
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot read object from position " + position, e);
        }
    }
}
