/*
 * DiskBlockBucket.java
 *
 * Created on 12. kveten 2008, 16:49
 */

package messif.buckets.impl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.LocalBucket;
import messif.buckets.LocalFilteredBucket;
import messif.buckets.OccupationLowException;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.ByteBufferFileInputStream;
import messif.objects.nio.ByteBufferFileOutputStream;
import messif.objects.nio.CachingSerializator;
import messif.objects.nio.MultiClassSerializator;
import messif.utility.Convert;
import messif.utility.Logger;

/**
 * A disk-oriented implementation of {@link LocalBucket}.
 * It stores all objects in a specified blocks of a file.
 *
 * The storage is persistent, even if the process using this bucket
 * quits, the bucket can be opened afterwards.
 * Note that this bucket only saves the name of the file when serialized,
 * thus the file must exist when the bucket is deserialized.
 *
 * @author  xbatko
 * @see BucketDispatcher
 * @see LocalBucket
 * @see DiskBucket
 * @see SimpleDiskBucket
 */
public class DiskBlockBucket extends LocalFilteredBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 0xffa891035bcf0001L;

    /** Logger for this bucket */
    private static Logger log = Logger.getLoggerEx("messif.buckets.impl.DiskBlockBucket");

    /** The prefix for auto-generated filenames */
    protected static final String FILENAME_PREFIX = "disk_block_bucket_";

    /** The suffix for auto-generated filenames */
    protected static final String FILENAME_SUFFIX = ".dbb";

    /** Bucket header flag constant for indication whether the bucket file was correctly closed */
    protected static final int FLAG_CLOSED = 0x00000003; // lower two bits

    /** Buffer sizes for read/write operations */
    protected final int bufferSize;

    /** Allocate the buffers for read/write operations as {@link ByteBuffer#allocateDirect direct} */
    protected final boolean bufferDirect;

    /** The number of objects currently stored in the bucket file */
    protected transient int objectCount;

    /** The number of bytes currently stored in the file (excluding headers) */
    protected transient long fileOccupation;

    /** The number of deleted objects - the file space fragmentation is the ratio between this and objectCount */
    protected transient int deletedFragments;

    /** The file with bucket data */
    protected final File file;

    /** The channel on the file with bucket data */
    protected transient final FileChannel fileChannel;

    /** The position in the file where the bucket starts */
    protected final long startPosition;

    /** Serializator responsible for storing (and restoring) binary objects in this bucket */
    protected final BinarySerializator serializator;

    /** Stream for writing data */
    protected transient final ByteBufferFileOutputStream outputStream;

    /** Flag whether the bucket file is modified */
    protected transient boolean modified;

    //****************** Constructors ******************//

    /**
     * Creates a new DiskBlockBucket instance.
     * The occupation and the limits are always set to bytes. This bucket doesn't
     * support limits in objects.
     * 
     * <p>
     * If a valid class is provided as <code>storedObjectsClass</code>, this bucket
     * will store only instances of this class. Moreover, if the <code>storedObjectsClass</code>
     * also implements {@link BinarySerializable} it will be used to physically store
     * the objects.<br/>
     * Otherwise, if <tt>null</tt> is provided, this bucket will store any {@link LocalAbstractObject},
     * but will use standard Java {@link Serializable serialization} even for objects that support {@link BinarySerializable}.
     * </p>
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param file the file in which to create the bucket
     * @param bufferSize the size of the buffer used for reading/writing
     * @param bufferDirect the bucket is either direct (<tt>true</tt>) or array-backed (<tt>false</tt>)
     * @param startPosition the position in the file where the bucket starts
     * @param serializator the object responsible for storing (and restoring) binary objects
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, File file, int bufferSize, boolean bufferDirect, long startPosition, BinarySerializator serializator) throws IOException {
        super(capacity, softCapacity, lowOccupation, true);

        this.file = file;
        this.bufferSize = bufferSize;
        this.bufferDirect = bufferDirect;
        this.startPosition = startPosition;
        this.serializator = serializator;
        this.fileChannel = openFileChannel(file);
        this.outputStream = openOutputStream();
    }

    /**
     * Creates a new DiskBlockBucket instance.
     * The occupation and the limits are always set to bytes. This bucket doesn't
     * support limits in objects.
     * 
     * <p>
     * If a valid class is provided as <code>storedObjectsClass</code>, this bucket
     * will store only instances of this class. Moreover, if the <code>storedObjectsClass</code>
     * also implements {@link BinarySerializable} it will be used to physically store
     * the objects.<br/>
     * Otherwise, if <tt>null</tt> is provided, this bucket will store any {@link LocalAbstractObject},
     * but will use standard Java {@link Serializable serialization} even for objects that support {@link BinarySerializable}.
     * </p>
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param fileName the name of the file in which to create the bucket
     * @param bufferSize the size of the buffer used for reading/writing
     * @param bufferDirect the bucket is either direct (<tt>true</tt>) or array-backed (<tt>false</tt>)
     * @param startPosition the position in the file where the bucket starts
     * @param serializator the object responsible for storing (and restoring) binary objects
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, String fileName, int bufferSize, boolean bufferDirect, long startPosition, BinarySerializator serializator) throws IOException {
        this(capacity, softCapacity, lowOccupation, new File(fileName), bufferSize, bufferDirect, startPosition, serializator);
    }

    /**
     * Clean up bucket internals before deletion.
     * This method is called by bucket dispatcher when this bucket is removed.
     * 
     * The method updates header and closes the file.
     * 
     * @throws Throwable if there was an error during releasing resources
     */
    @Override
    public void finalize() throws Throwable {
        try {
            if (modified) {
                writeHeader(fileChannel, startPosition, FLAG_CLOSED);
                flush(true);
            }
            fileChannel.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Error during bucket clean-up, continuing", e);
        }
        super.finalize();
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a bucket. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>file</em> - the path to the particular bucket (either as File or as String)</li>
     *   <li><em>dir</em> - the path to a directory (either as File or as String) where a temporary file name is
     *       created in the format of "disk_block_bucket_XXXX.dbb"</li>
     *   <li><em>cacheClasses</em> - comma-separated list of classes that will be cached for fast serialization</li>
     *   <li><em>bufferSize</em> - the size of the buffers used for I/O operations</li>
     *   <li><em>startPosition</em> - the position (in bytes) of the first block of the new bucket within the <em>file</em></li>
     *   <li><em>oneFile</em> - if <tt>true</tt>, the startPosition will be automatically computed so that all the buckets are within one file</li>
     * </ul>
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters - this bucket supports "file" and "path" (see above)
     * @return a new SimpleDiskBucket instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    protected static DiskBlockBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IOException, InstantiationException, ClassNotFoundException {
        if (!occupationAsBytes)
            throw new InstantiationException("DiskBlockBucket cannot count occupation in objects");

        // Read the parameters
        File file = Convert.getParameterValue(parameters, "file", File.class, null);
        Class[] cacheClasses = Convert.getParameterValue(parameters, "cacheClasses", Class[].class, null);
        int bufferSize = Convert.getParameterValue(parameters, "bufferSize", Integer.class, 16384);
        long startPosition = Convert.getParameterValue(parameters, "startPosition", Long.class, 0L);

        // Capacity override hack...
        Long capacityOverrideHack = Convert.getParameterValue(parameters, "capacityOverrideHack", Long.class, null);
        if (capacityOverrideHack != null)
            capacity = capacityOverrideHack;
        
        // Compute next extent if "oneFile" was requested
        if (file == null) {
            startPosition = 0;
        } else {
            if (Convert.getParameterValue(parameters, "oneFile", Boolean.class, Boolean.FALSE) && file != null)
                parameters.put("startPosition", startPosition + capacity + headerSize);
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
            serializator = new MultiClassSerializator(LocalAbstractObject.class);
        else
            serializator = new CachingSerializator(LocalAbstractObject.class, cacheClasses);

        // Finally, create the bucket
        return new DiskBlockBucket(capacity, softCapacity, lowOccupation, file, bufferSize, true, startPosition, serializator);
    }


    //****************** Header functions ******************//

    /** The size of the header - must match the {@link #writeHeader} and {@link #readHeader} methods */
    private static final int headerSize = (4*Long.SIZE + 4*Integer.SIZE)/8;

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
        buffer.putLong(getCapacity());
        buffer.putInt(serializator.hashCode()); // Hash of the serializator (hashes of the UUIDs of the cached objects)
        buffer.putInt(flags & ~FLAG_CLOSED); // Closed bits are set to zero for the first time even if closing
        buffer.putLong(fileOccupation);
        buffer.putInt(objectCount);
        buffer.putInt(deletedFragments);
        buffer.putLong(occupation);
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
            if (tmpLong != getCapacity())
                throw new IOException("Wrong capacity of the bucket: " + tmpLong + " should be " + getCapacity());

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
                occupation = buffer.getLong();
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
        log.info("Rebuilding header of bucket ID " + getBucketID());

        // Reset header values
        objectCount = 0;
        fileOccupation = 0;
        deletedFragments = 0;
        occupation = 0;

        // Read all objects (by seeking)
        ByteBufferFileInputStream reader = new ByteBufferFileInputStream(bufferSize, bufferDirect, fileChannel, position, getCapacity());
        try {
            // End iterating one an "null" object is found
            for (int objectSize = serializator.skipObject(reader, false); objectSize != 0; objectSize = serializator.skipObject(reader, false)) {
                if (objectSize > 0) {
                    objectCount++;
                    occupation += objectSize;
                } else {
                    // Negative size means deleted object
                    deletedFragments++;
                }
            }
            fileOccupation = reader.position() - 4; // Ignore last integer read
        } catch (EOFException ignore) {
            fileOccupation = reader.position(); // The file ends prematurely
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
    protected ByteBufferFileInputStream openInputStream() throws IOException {
        return new ByteBufferFileInputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, capacity);
    }

    /**
     * Create output stream over the current file channel.
     * @return the created output stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected ByteBufferFileOutputStream openOutputStream() throws IOException {
        ByteBufferFileOutputStream stream = new ByteBufferFileOutputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, capacity);
        stream.position(fileOccupation);
        return stream;
    }

    /**
     * Opens the file channel on <code>file</code> and reads the header.
     * @param file the file to open the channel on
     * @return the new file channel
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected FileChannel openFileChannel(File file) throws IOException {
        // Remember if file exists before it is auto-created by the RandomAccessFile constructor
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
     * Read the serialized bucket from an object stream.
     * @param in the object stream from which to read the bucket
     * @throws IOException if there was an I/O error during deserialization
     * @throws ClassNotFoundException if there was an unknown object in the stream
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        try {
            // Proceed with standard deserialization first
            in.defaultReadObject();

            // Reopen file channel (set it through reflection to overcome the "final" flag)
            Field field = DiskBlockBucket.class.getDeclaredField("fileChannel");
            field.setAccessible(true);
            field.set(this, openFileChannel(file));

            // Reopen the output stream
            field = DiskBlockBucket.class.getDeclaredField("outputStream");
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

    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount() {
        return objectCount;
    }

    /**
     * Stores the specified object in a the hash table.
     *
     * @param object the new object to be inserted
     * @return {@link BucketErrorCode#OBJECT_INSERTED} if the object was successfuly inserted
     * @throws IllegalStateException if there was an I/O error that prevented to the object to be stored
     */
    protected synchronized BucketErrorCode storeObject(LocalAbstractObject object) throws IllegalStateException {
        try {
            // Set modified flag
            if (!modified)
                writeHeader(fileChannel, startPosition, 0);

            // Write object
            fileOccupation += serializator.write(outputStream, object);

            // Update internal counters
            objectCount++;

            return BucketErrorCode.OBJECT_INSERTED;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Delete all objects from this bucket.
     * @return the number of deleted objects
     * @throws OccupationLowException if the low occupation limit is reached when deleting objects
     */
    @Override
    public synchronized int deleteAllObjects() throws OccupationLowException {
        // If the bucket has some required lowest occupation, this method cannot be used
        if (lowOccupation > 0)
            throw new OccupationLowException();

        int deleted = 0;
        try {
            // Reset the output stream (and also clear the buffer)
            outputStream.position(0);

            // Reset header values
            deleted = objectCount;
            objectCount = 0;
            fileOccupation = 0;
            deletedFragments = 0;
            occupation = 0;

            // Update statistics
            counterBucketDelObject.add(this, deleted);

            // Unset modified flag
            writeHeader(fileChannel, startPosition, FLAG_CLOSED);
        } catch (IOException e) {
            log.warning("Cannot delete all objects from disk bucket: " + e);
        }

        return deleted;
    }

    /**
     * Deletes the object at specified position.
     * Practically, this method writes negative size of the deleted object into position.
     * @param position the relative address of the begining of the object
     * @param size the size of the object
     * @throws IOException if there was an I/O error
     */
    protected synchronized void deleteObject(long position, int size) throws IOException {
        // Set modified flag
        if (!modified)
            writeHeader(fileChannel, startPosition, 0);

        // Remember position to be able to restore it after the write
        long currentPosition = outputStream.position();
        try {
            outputStream.position(position);
            // Write the negative object size to indicate deleted object
            serializator.write(outputStream, -size);
        } finally {
            outputStream.position(currentPosition);
        }

        // Update internal counters
        objectCount--;
        deletedFragments++;
    }

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected synchronized LocalBucketIterator<? extends DiskBlockBucket> iterator() {
        return new DiskBlockBucketIterator<DiskBlockBucket>(this);
    }


    //****************** Iterator object ******************//

    /**
     * Internal class for iterator implementation
     * @param <T> the type of the bucket this iterator operates on
     */
    protected static class DiskBlockBucketIterator<T extends DiskBlockBucket> extends LocalBucket.LocalBucketIterator<T> {

        /** Last returned object */
        protected LocalAbstractObject currentObject = null;

        /** Next prepared object */
        protected LocalAbstractObject nextObject;

        /** The exception thrown when getting next object (will be thrown in nearest call to next() */
        protected RuntimeException nextException = null;

        /** The starting position of the current object */
        protected long currentObjectPosition = -1;

        /** The starting position of the next object */
        protected long nextObjectPosition;
        
        /** Input stream for reading objects */
        protected final ByteBufferFileInputStream inputStream;

        /** Hash value for detection of concurrent modification */
        protected long detectConcurrent;

        /**
         * Creates a new instance of DiskBlockBucketIterator with the DiskBlockBucket.
         * This constructor is intended to be called only from DiskBlockBucketIterator class.
         *
         * @param bucket actual instance of DiskBlockBucket on which this iterator should work
         * @throws RuntimeException if there was an error reading the bucket's file
         */
        protected DiskBlockBucketIterator(T bucket) throws RuntimeException {
            super(bucket);
            detectConcurrent = getDetectConcurrent();

            try {
                bucket.flush(false); // This is very fast unless there are some data pending
                inputStream = bucket.openInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                nextObjectPosition = 0;
                nextObject = bucket.serializator.readObject(inputStream, LocalAbstractObject.class);
            } catch (EOFException e) {
                nextObject = null; // EOF is a correct exit
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns a hash value for detection of concurrent modification.
         * @return XORed occupation & fragmentation of the bucket
         */
        protected long getDetectConcurrent() {
            return bucket.fileOccupation ^ ((long)bucket.deletedFragments << 32);
        }

        /**
         * Physically remove the current object.
         * @throws IllegalStateException if either next was not called yet or the object was already deleted
         */
        protected void removeInternal() throws IllegalStateException {
            // The current object position is not set (either next was not called, or the object is already deleted)
            if (currentObjectPosition < 0)
                throw new IllegalStateException("There is no object for deletion");

            try {
                /*
                 * Write negative object size to indicate deleted object. The length
                 * of the deleted space is the space between the current and the next
                 * object position. The size integer is 4 bytes and must be subtracted.
                 * If there were any deleted objects in between, the space will be merged
                 */
                bucket.deleteObject(currentObjectPosition, (int)(nextObjectPosition - currentObjectPosition - 4));
                currentObjectPosition = -1;
                detectConcurrent = getDetectConcurrent();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException iteration has no more elements.
         * @throws ConcurrentModificationException if the bucket was modified (object added or deleted) during iteration
         */
        public LocalAbstractObject next() throws NoSuchElementException, ConcurrentModificationException {
            // If there was an exception getting next object, throw it now
            if (nextException != null)
                throw nextException;
            // If there is no next object
            if (nextObject == null)
                throw new NoSuchElementException();
            // If the bucket was modified since this iterator was created
            if (detectConcurrent != getDetectConcurrent())
                throw new ConcurrentModificationException("Bucket was modified while reading");

            // Set current object to "pre"-read next one
            currentObject = nextObject;
            currentObjectPosition = nextObjectPosition;

            // Read next object and preserve any exception thrown (will throw it in the next call)
            try {
                nextObjectPosition = inputStream.position();
                // Check end-of-file to avoid exception
                if (nextObjectPosition == bucket.fileOccupation)
                    nextObject = null;
                else
                    nextObject = bucket.serializator.readObject(inputStream, LocalAbstractObject.class);
            } catch (IOException e) {
                nextException = new NoSuchElementException(e.toString());
            } catch (RuntimeException e) {
                nextException = e;
            }
            return currentObject;
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return nextObject != null || nextException != null; // The exception means that there are probably more objects, but something went wrong...
        }

        /**
         * Returns the object returned by the last call to next().
         * @return the object returned by the last call to next()
         * @throws NoSuchElementException if next() has not been called yet
         */
        public LocalAbstractObject getCurrentObject() throws NoSuchElementException {
            if (currentObject == null)
                throw new NoSuchElementException("Can't call getCurrentObject before next was called");
            
            return currentObject;
        }

    }    

}
