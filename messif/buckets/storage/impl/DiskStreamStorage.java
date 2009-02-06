/*
 *  DiskStreamStorage
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
import java.nio.channels.FileChannel;
import java.util.Map;
import messif.buckets.BucketStorageException;
import messif.buckets.StorageFailureException;
import messif.buckets.storage.LongAddress;
import messif.buckets.storage.LongStorage;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.FileChannelInputStream;
import messif.objects.nio.FileChannelOutputStream;
import messif.objects.nio.CachingSerializator;
import messif.objects.nio.MultiClassSerializator;
import messif.utility.Convert;

/**
 * Disk based storage.
 * The objects in this storage are stored in a file in the order
 * of insertion. The address is the position within the file.
 * Objects are serialized using the provided {@link BinarySerializator}.
 * 
 * @param <T> the class of objects stored in this storage
 * @author xbatko
 */
public class DiskStreamStorage<T> implements LongStorage<T>, Serializable {
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

    /** Buffer sizes for read/write operations */
    protected final int bufferSize;

    /** Allocate the buffers for read/write operations as {@link ByteBuffer#allocateDirect direct} */
    protected final boolean bufferDirect;

    /** The number of objects currently stored in the file */
    protected transient int objectCount;

    /** The number of bytes currently stored in the file (excluding headers) */
    protected transient long fileOccupation;

    /** The number of deleted objects - the file space fragmentation is the ratio between this and objectCount */
    protected transient int deletedFragments;

    /** The file with data */
    protected final File file;

    /** The channel on the file with data */
    protected transient final FileChannel fileChannel;

    /** The position in the file where this storage starts (the real data starts at startPosition + headerSize) */
    protected final long startPosition;

    /** The maximal length of the file */
    protected final long maximalLength;

    /** Serializator responsible for storing (and restoring) binary objects in the file */
    protected final BinarySerializator serializator;

    /** Class of objects that the this storage works with */
    protected final Class<? extends T> storedObjectsClass;

    /** Stream for reading data */
    protected transient final FileChannelInputStream inputStream;

    /** Stream for writing data */
    protected transient final FileChannelOutputStream outputStream;

    /** Flag whether the file is modified */
    protected transient boolean modified;


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
    public DiskStreamStorage(Class<? extends T> storedObjectsClass, File file, int bufferSize, boolean bufferDirect, long startPosition, long maximalLength, BinarySerializator serializator) throws IOException {
        this.storedObjectsClass = storedObjectsClass;
        this.file = file;
        this.bufferSize = bufferSize;
        this.bufferDirect = bufferDirect;
        this.startPosition = startPosition;
        this.maximalLength = maximalLength;
        this.serializator = serializator;
        this.fileChannel = openFileChannel(file);
        this.outputStream = openOutputStream();
        this.inputStream = openInputStream();
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
            flush(true);
        }
        fileChannel.close();
        super.finalize();
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a new disk storage. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>file</em> - the path to the particular disk storage (either as File or as String)</li>
     *   <li><em>dir</em> - the path to a directory (either as File or as String) where a temporary file name is
     *       created in the format of "disk_storage_XXXX.ds"</li>
     *   <li><em>cacheClasses</em> - comma-separated list of classes that will be cached for fast serialization</li>
     *   <li><em>bufferSize</em> - the size of the buffers used for I/O operations</li>
     *   <li><em>directBuffer</em> - flag controlling wether to use faster direct buffers for I/O operations</li>
     *   <li><em>startPosition</em> - the position (in bytes) of the first block of the data within the <em>file</em></li>
     *   <li><em>maximalLength</em> - the maximal length (in bytes) of the data written to <em>file</em> by this storage</li>
     *   <li><em>oneFile</em> - if <tt>true</tt>, the startPosition will be automatically computed so that all the storages are within one file</li>
     * </ul>
     * 
     * @param <T> the class of objects that the new storage will work with
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param parameters list of named parameters (see above)
     * @return a new disk storage instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     */
    public static <T> DiskStreamStorage<T> create(Class<T> storedObjectsClass, Map<String, Object> parameters) throws IOException, InstantiationException {
        // Read the parameters
        File file = Convert.getParameterValue(parameters, "file", File.class, null);
        Class[] cacheClasses = Convert.getParameterValue(parameters, "cacheClasses", Class[].class, null);
        int bufferSize = Convert.getParameterValue(parameters, "bufferSize", Integer.class, 16384);
        boolean directBuffer = Convert.getParameterValue(parameters, "directBuffer", Boolean.class, true);
        long startPosition = Convert.getParameterValue(parameters, "startPosition", Long.class, 0L);
        long maximalLength = Convert.getParameterValue(parameters, "maximalLength", Long.class, Long.MAX_VALUE);

        // Compute next extent if "oneFile" was requested
        if (Convert.getParameterValue(parameters, "oneFile", Boolean.class, Boolean.FALSE) && file != null) {
            if (maximalLength == Long.MAX_VALUE)
                throw new IOException("Maximal length must be specified if oneFile flag is used");
            parameters.put("startPosition", startPosition + maximalLength);
        }

        // If a file was not specified - create a new file in given directory
        if (file == null) {
            File dir = Convert.getParameterValue(parameters, "dir", File.class, null);
            if (dir == null)
                file = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX);
            else file = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX, dir);
        }

        // Initialize serializator
        BinarySerializator serializator;
        if (cacheClasses == null)
            serializator = new MultiClassSerializator<T>(storedObjectsClass);
        else
            serializator = new CachingSerializator<T>(storedObjectsClass, cacheClasses);

        // Finally, create the bucket
        return new DiskStreamStorage<T>(storedObjectsClass, file, bufferSize, directBuffer, startPosition, maximalLength, serializator);
    }


    //****************** Header functions ******************//

    /** The size of the header - must match the {@link #writeHeader} and {@link #readHeader} methods */
    private static final int headerSize = (3*Long.SIZE + 4*Integer.SIZE)/8;

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
        ByteBuffer buffer = ByteBuffer.allocate(headerSize);
        buffer.putLong(serialVersionUID);
        buffer.putLong(maximalLength);
        buffer.putInt(serializator.hashCode()); // Hash of the serializator (hashes of the UUIDs of the cached objects)
        buffer.putInt(flags & ~FLAG_CLOSED); // Closed bits are set to zero for the first time even if closing
        buffer.putLong(fileOccupation);
        buffer.putInt(objectCount);
        buffer.putInt(deletedFragments);
        buffer.flip();
        fileChannel.write(buffer, position);
        if ((flags & FLAG_CLOSED) == FLAG_CLOSED) {
            // Replace flag with closed bit
            buffer.putInt(20, flags); // !!!! WARNING !!!! Don't forget to change the position here !!!!
            buffer.rewind();
            fileChannel.force(true);
            fileChannel.write(buffer, position);
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
        ByteBuffer buffer = ByteBuffer.allocate(headerSize);

        // Read header bytes
        fileChannel.read(buffer, position);
        buffer.flip();

        try {
            // Check header magic number
            long tmpLong = buffer.getLong();
            if (tmpLong != serialVersionUID)
                throw new IOException("Wrong bucket serial version UID: " + tmpLong + " should be " + serialVersionUID);

            // Check if capacity matches
            tmpLong = buffer.getLong();
            if (tmpLong != maximalLength)
                throw new IOException("Wrong maximal length of the file: " + tmpLong + " should be " + maximalLength);

            // Check if the stored class matches
            tmpLong = buffer.getInt();
            if (tmpLong != serializator.hashCode())
                throw new IOException("Hash codes for the serializator do not match");

            // Read flags
            int flags = buffer.getInt();
            if ((flags & FLAG_CLOSED) == FLAG_CLOSED) {
                // The file was closed correctly (lower two bits are set), we can be sure the header is OK
                fileOccupation = buffer.getLong();
                objectCount = buffer.getInt();
                deletedFragments = buffer.getInt();
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
        fileOccupation = 0;
        deletedFragments = 0;

        // Read all objects (by seeking)
        FileChannelInputStream reader = new FileChannelInputStream(bufferSize, bufferDirect, fileChannel, position, maximalLength - headerSize);
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
            fileOccupation = reader.getPosition() - 4; // Ignore last object size (integer) read
        } catch (EOFException ignore) {
            fileOccupation = reader.getPosition(); // The file ends
        } finally {
            reader.close();
        }
    }


    //****************** Construction methods ******************//

    /**
     * Create input stream on the current file channel.
     * @return the created input stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected FileChannelInputStream openInputStream() throws IOException {
        flush(false); // This is very fast unless there are some data pending
        return new FileChannelInputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, maximalLength - headerSize);
    }

    /**
     * Create output stream over the current file channel.
     * @return the created output stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected FileChannelOutputStream openOutputStream() throws IOException {
        FileChannelOutputStream stream = new FileChannelOutputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, maximalLength - headerSize);
        stream.setPosition(startPosition + headerSize + fileOccupation);
        return stream;
    }

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
            Field field = DiskStreamStorage.class.getDeclaredField("fileChannel");
            field.setAccessible(true);
            field.set(this, openFileChannel(file));

            // Reopen the input stream
            field = DiskStreamStorage.class.getDeclaredField("inputStream");
            field.setAccessible(true);
            field.set(this, openInputStream());

            // Reopen the output stream
            field = DiskStreamStorage.class.getDeclaredField("outputStream");
            field.setAccessible(true);
            field.set(this, openOutputStream());
        } catch (NoSuchFieldException e) {
            throw new ClassNotFoundException(e.toString());
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException("This should never happen!", e);
        }
    }


    //****************** Overrides ******************//

    /**
     * Flushes this output stream and forces any buffered output bytes 
     * to be written out to the underlying file.
     * 
     * @param syncPhysical if <tt>true</tt> then also the file is flushed
     *          to be sure the data are really written to disk
     * @throws IOException if there was an I/O error
     */
    public void flush(boolean syncPhysical) throws IOException {
        if (outputStream.isDirty()) {
            synchronized (this) {
                outputStream.flush();
            }
        }
        if (syncPhysical) {
            fileChannel.force(false);
        }
    }

    public void flush() throws IOException {
        flush(true);
    }

    public synchronized LongAddress<T> store(T object) throws BucketStorageException {
        try {
            // Set modified flag
            if (!modified)
                writeHeader(fileChannel, startPosition, 0);

            // Remember address
            LongAddress<T> address = new LongAddress<T>(this, outputStream.getPosition());

            // Write object
            fileOccupation += serializator.write(outputStream, object);

            // Update internal counters
            objectCount++;

            return address;
        } catch (IOException e) {
            throw new StorageFailureException("Cannot store object into disk storage", e);
        }
    }

    public synchronized void remove(long position) throws BucketStorageException {
        try {
            // Read stored object size
            inputStream.setPosition(position);
            remove(position, serializator.skipObject(inputStream, false));
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot remove object from position " + position, e);
        }
    }

    protected synchronized void remove(long position, int objectSize) throws BucketStorageException {
        try {
            // Set modified flag
            if (!modified)
                writeHeader(fileChannel, startPosition, 0);

            // Remember position to be able to restore it after the write
            long currentPosition = outputStream.getPosition();
            try {
                outputStream.setPosition(position);
                // Write the negative object size to indicate deleted object
                serializator.write(outputStream, -objectSize);
            } finally {
                outputStream.setPosition(currentPosition);
            }

            // Update internal counters
            objectCount--;
            deletedFragments++;
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot remove object from position " + position, e);
        }
    }

    public synchronized T read(long position) throws BucketStorageException {
        try {
            inputStream.setPosition(position);
            return serializator.readObject(inputStream, storedObjectsClass);
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot read object from position " + position, e);
        }
    }
}
