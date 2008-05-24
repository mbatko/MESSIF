/*
 * DiskBlockBucket.java
 *
 * Created on 12. kveten 2008, 16:49
 */

package messif.buckets;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import messif.objects.LocalAbstractObject;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializingFileInputStream;
import messif.objects.nio.BinarySerializingFileOutputStream;
import messif.objects.nio.JavaToBinarySerializable;
import messif.utility.Convert;

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

    /** The prefix for auto-generated filenames */
    protected static final String FILENAME_PREFIX = "disk_block_bucket_";

    /** The suffix for auto-generated filenames */
    protected static final String FILENAME_SUFFIX = ".dbb";

    /** Bucket header flag constant for indication whether the bucket file was correctly closed */
    protected static final int FLAG_CLOSED = 0x00000003; // lower two bits

    /** Buffer sizes for read/write operations */
    protected static final int bufferSize = 4096;

    /** Allocate the buffers for read/write operations as {@link ByteBuffer#allocateDirect direct} */
    protected static final boolean bufferDirect = false;

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

    /** The class of objects stored in this bucket; mixed classes can be stored if <tt>null</tt> */
    protected final Class<? extends LocalAbstractObject> storedObjectsClass;

    /** Constructor or factory method used for deserializing objects */
    protected transient final Object objectsConstructorOrFactory;

    /** Stream for writing data */
    protected transient final BinarySerializingFileOutputStream outputStream;


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
     * @param startPosition the position in the file where the bucket starts
     * @param storedObjectsClass the class of objects stored in this bucket
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, File file, long startPosition, Class<? extends LocalAbstractObject> storedObjectsClass) throws IOException {
        super(capacity, softCapacity, lowOccupation, true);

        this.file = file;
        this.startPosition = startPosition;
        this.storedObjectsClass = storedObjectsClass;
        this.fileChannel = openFileChannel(file);
        this.objectsConstructorOrFactory = createConstructorOrFactory(storedObjectsClass);
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
     * @param startPosition the position in the file where the bucket starts
     * @param storedObjectsClass the class of objects stored in this bucket
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskBlockBucket(long capacity, long softCapacity, long lowOccupation, String fileName, long startPosition, Class<? extends LocalAbstractObject> storedObjectsClass) throws IOException {
        this(capacity, softCapacity, lowOccupation, new File(fileName), startPosition, storedObjectsClass);
    }

    /**
     * Clean up bucket internals before deletion.
     * This method is called by bucket dispatcher when this bucket is removed.
     * 
     * The method updates header and closes the file.
     * 
     * @throws Exception if there was an error during releasing resources
     */
    @Override
    public void cleanUp() throws Exception {
        writeHeader(fileChannel, startPosition, FLAG_CLOSED);
        flush(true);
        fileChannel.close();
        super.cleanUp();
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a bucket. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>file</em> - the path to the particular bucket (either as File or as String)</li>
     *   <li><em>dir</em> - the path to a directory (either as File or as String) where a temporary file name is
     *       created in the format of "disk_block_bucket_XXXX.dbb"</li>
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

        // Read file name from the parameters
        File file = null;
        if (parameters != null) // This will throw ClassCast exception, if the "file" is neither File nor String
            try {
                file = (File)parameters.get("file"); // if file is unspecified, this will NOT throw exception (it will be null)
            } catch (ClassCastException ignore) {
                file = new File((String)parameters.get("file"));
            }

        // Read class from the parameters
        Class<? extends LocalAbstractObject> objectClass = null;
        if (parameters != null)
            try {
                // If class is unspecified, this will NOT throw exception, but the objectClass will be null instead
                objectClass = Convert.genericCastToClass(parameters.get("class"), LocalAbstractObject.class);
            } catch (ClassCastException ignore) {
                objectClass = Convert.getClassForName((String)parameters.get("class"), LocalAbstractObject.class);
            }

        // If a file was not specified - create a new file in given directory
        if (file == null) {
            File dir = null;
            if (parameters != null)
                try {
                    dir = (File)parameters.get("dir");
                } catch (ClassCastException ignore) {
                    dir = new File((String)parameters.get("dir"));
                }
            if (dir == null)
                file = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX);
            else file = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX, dir);
        }

        // Finally, create the bucket
        return new DiskBlockBucket(capacity, softCapacity, lowOccupation, file, 0, objectClass);
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
        buffer.putInt((storedObjectsClass == null)?-1:storedObjectsClass.getName().hashCode()); // Hash of the stored class name
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
            if (tmpLong != ((storedObjectsClass == null)?-1:storedObjectsClass.getName().hashCode()))
                throw new IOException("Hash codes for the stored object classes do not match");

            // Read flags
            int flags = buffer.getInt();
            if ((flags & FLAG_CLOSED) == FLAG_CLOSED) {
                // The file was closed correctly (lower two bits are set), we can be sure the header is OK
                fileOccupation = buffer.getLong();
                objectCount = buffer.getInt();
                deletedFragments = buffer.getInt();
                occupation = buffer.getLong();
            } else {
                // Header indicates pending close, so it is probably incorrect - reconstruct it from the file
                reconstructHeader(fileChannel, position + headerSize);
            }
        } catch (BufferUnderflowException e) {
            throw new IOException("Header is corrupted, consider removing the file " + file);
        }

        // Mark the bucket as open
        writeHeader(fileChannel, position, 0);
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
        occupation = 0;

        // Read all objects (by seeking)
        BinarySerializingFileInputStream reader = new BinarySerializingFileInputStream(bufferSize, bufferDirect, fileChannel, position, getCapacity());
        try {
            // End iterating one an "null" object is found
            for (int objectSize = reader.skipObject(false); objectSize != 0; objectSize = reader.skipObject(false)) {
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
     * Create constructor or factory method for creating BinarySerializable objects of the specified class.
     * @param objectsClass the class to create instances for
     * @return a {@link Constructor}, a {@link Method} or <tt>null</tt>
     */
    protected static Object createConstructorOrFactory(Class<?> objectsClass) {
        // The stored objects' class is not limited or the stored objects are not BinarySerializable
        if (objectsClass == null || !BinarySerializable.class.isAssignableFrom(objectsClass))
            return null;

        // Try the BinarySerializable constructor then factory method
        try {
            return JavaToBinarySerializable.getNativeSerializableConstructor(objectsClass);
        } catch (NoSuchMethodException ignore) {
            try {
                return JavaToBinarySerializable.getNativeSerializableFactoryMethod(objectsClass);
            } catch (NoSuchMethodException ignoretoo) {
                return null;
            }
        }
    }

    /**
     * Create output stream over the current file channel.
     * @return the created output stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected BinarySerializingFileOutputStream openOutputStream() throws IOException {
        BinarySerializingFileOutputStream stream = new BinarySerializingFileOutputStream(bufferSize, bufferDirect, fileChannel, headerSize, capacity);
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
        boolean fileExists = file.length() > 0;

        // Open the channel
        FileChannel chan = new RandomAccessFile(file, "rw").getChannel();

        // Read the occupation and number of objects
        if (fileExists)
            readHeader(chan, startPosition);
        else
            writeHeader(chan, startPosition, 0);

        return chan;
    }


    //****************** Serialization ******************//

    /** Read this bucket from the object stream */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        try {
            // Proceed with standard deserialization first
            in.defaultReadObject();

            // Reopen file channel (set it through reflection to overcome the "final" flag)
            Field field = getClass().getDeclaredField("fileChannel");
            field.setAccessible(true);
            field.set(this, openFileChannel(file));

            // Reset the constructor / factory method
            field = getClass().getDeclaredField("objectsConstructorOrFactory");
            field.setAccessible(true);
            field.set(this, createConstructorOrFactory(storedObjectsClass));
            
            // Reopen the output stream
            field = getClass().getDeclaredField("outputStream");
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
     * @return OBJECT_INSERTED if the object was successfuly inserted,
     *         OBJECT_REFUSED if the object is incompatible with the {@link #storedObjectsClass},
     *         otherwise an exception is thrown (usually OutOfMemoryError)
     * @throws IllegalStateException if there was an I/O error that prevented to the object to be stored
     */
    protected synchronized BucketErrorCode storeObject(LocalAbstractObject object) throws IllegalStateException {
        try {
            if (storedObjectsClass != null && !storedObjectsClass.isInstance(object))
                return BucketErrorCode.OBJECT_REFUSED;

            if (objectsConstructorOrFactory != null)
                fileOccupation += outputStream.write((BinarySerializable)object);
            else
                fileOccupation += outputStream.write(new JavaToBinarySerializable(object));
            objectCount++;

            return BucketErrorCode.OBJECT_INSERTED;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
     * @param T the type of the bucket this iterator operates on
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
        protected final BinarySerializingFileInputStream inputStream;

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
                inputStream = new BinarySerializingFileInputStream(bufferSize, bufferDirect, bucket.fileChannel, bucket.startPosition + headerSize, bucket.capacity);
                nextObjectPosition = 0;
                nextObject = (LocalAbstractObject)inputStream.read(bucket.objectsConstructorOrFactory);
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
                 * object position. The size is 4 bytes that must be subtracted.
                 * If there were any deleted objects in between the space will be merged
                 */
                synchronized (bucket) {
                    bucket.outputStream.writeIntAt(currentObjectPosition, (int)(currentObjectPosition - nextObjectPosition + 4));
                    currentObjectPosition = -1;
                    bucket.objectCount--;
                    bucket.deletedFragments++;
                }
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
                nextObject = (LocalAbstractObject)inputStream.read(bucket.objectsConstructorOrFactory);
            } catch (EOFException e) {
                nextObject = null; // EOF is a correct exit
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
            return nextObject != null;
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
