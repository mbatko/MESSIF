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
package messif.buckets.storage.impl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.BucketStorageException;
import messif.buckets.StorageFailureException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.impl.AbstractSearch;
import messif.buckets.index.Lock;
import messif.buckets.index.Lockable;
import messif.buckets.storage.LongAddress;
import messif.buckets.storage.LongStorageIndexed;
import messif.buckets.storage.LongStorageSearch;
import messif.buckets.storage.ReadonlyStorageException;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.BufferInputStream;
import messif.objects.nio.FileChannelOutputStream;
import messif.objects.nio.CachingSerializator;
import messif.objects.nio.FileChannelInputStream;
import messif.objects.nio.MappedFileChannelInputStream;
import messif.objects.nio.MultiClassSerializator;
import messif.utility.Convert;

/**
 * Disk based storage.
 * The objects in this storage are stored in a file in the order
 * of insertion. The address is the position within the file.
 * Objects are serialized using the provided {@link BinarySerializator}.
 * 
 * @param <T> the class of objects stored in this storage
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DiskStorage<T> implements LongStorageIndexed<T>, Lockable, Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Logger ******************//

    /** Logger for the disk storage class */
    private static final Logger log = Logger.getLogger(DiskStorage.class.getName());


    //****************** Constants ******************//

    /** The prefix for auto-generated filenames */
    public static final String FILENAME_PREFIX = "disk_storage_";

    /** The suffix for auto-generated filenames */
    public static final String FILENAME_SUFFIX = ".ds";

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
    protected transient FileChannel fileChannel;

    /** The position in the file where this storage starts (the real data starts at startPosition + headerSize) */
    protected final long startPosition;

    /** The maximal length of the file */
    protected final long maximalLength;

    /** Serializator responsible for storing (and restoring) binary objects in the file */
    protected final BinarySerializator serializator;

    /** Class of objects that the this storage works with */
    protected final Class<? extends T> storedObjectsClass;

    /** Number of items that reference this disk storage */
    private int references = 0;

    /** Stream for reading data */
    protected transient WeakReference<BufferInputStream> inputStream;

    /** Stream for writing data */
    protected transient FileChannelOutputStream outputStream;

    /** Flag whether the file is modified */
    protected transient boolean modified;

    /** Flag whether the file is readonly */
    protected transient boolean readonly;

    /** Finalizer thread that writes a modified header */
    protected transient Thread modifiedThread;


    //****************** Constructors ******************//

    /**
     * Creates a new DiskStreamStorage instance.
     * 
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param file the file in which to create the bucket
     * @param readonly if <tt>true</tt>, the storage will be opened in read-only mode (e.g. the store method will throw an exception)
     * @param bufferSize the size of the buffer used for reading/writing
     * @param bufferDirect the bucket is either direct (<tt>true</tt>) or array-backed (<tt>false</tt>)
     * @param memoryMap flag whether to use memory-mapped I/O
     * @param startPosition the position in the file where this storage starts
     * @param maximalLength the maximal length of the file
     * @param serializator the object responsible for storing (and restoring) binary objects
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskStorage(Class<? extends T> storedObjectsClass, File file, boolean readonly, int bufferSize, boolean bufferDirect, boolean memoryMap, long startPosition, long maximalLength, BinarySerializator serializator) throws IOException {
        this.storedObjectsClass = storedObjectsClass;
        this.file = file;
        this.bufferSize = ((memoryMap)?-1:1)*Math.abs(bufferSize);
        this.bufferDirect = bufferDirect;
        this.startPosition = startPosition;
        this.maximalLength = maximalLength;
        this.serializator = serializator;
        this.readonly = readonly;
    }

    /**
     * Creates a new DiskStreamStorage instance.
     * All parameters are copied from the provided <code>copyAttributesDiskStorage</code>
     * except for the file name. The bucket is always opened in read-write mode.
     *
     * @param copyAttributesDiskStorage the disk storage from which to copy parameters
     * @param file the file in which to create the bucket
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskStorage(final DiskStorage<? extends T> copyAttributesDiskStorage, File file) throws IOException {
        this.storedObjectsClass = copyAttributesDiskStorage.storedObjectsClass;
        this.file = file;
        this.bufferSize = copyAttributesDiskStorage.bufferSize;
        this.bufferDirect = copyAttributesDiskStorage.bufferDirect;
        this.startPosition = copyAttributesDiskStorage.startPosition;
        this.maximalLength = copyAttributesDiskStorage.maximalLength;
        this.serializator = copyAttributesDiskStorage.serializator;
        this.readonly = copyAttributesDiskStorage.readonly;
    }

    /**
     * Close the associated file channel if this storage is no longer references
     * from any index.
     * @return <tt>true</tt> if the file channel was closed
     * @throws IOException if there was a problem closing the file channel
     */
    protected boolean closeFileChannel() throws IOException {
        if (references <= 0) {
            if (fileChannel == null)
                return true;
            if (modified) {
                flush(true);
                writeHeader(fileChannel, startPosition, FLAG_CLOSED);
            }
            fileChannel.close();
            return true;
        } else {
            references--;
            return false;
        }
    }

    @Override
    public void finalize() throws Throwable {
        if (modifiedThread != null) {
            Runtime.getRuntime().removeShutdownHook(modifiedThread);
            modifiedThread = null;
        }
        closeFileChannel();
        super.finalize();
    }

    public void destroy() throws Throwable {
        if (closeFileChannel())
            file.delete();
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
     *   <li><em>memoryMap</em> - flag controlling wether to use memory-mapped I/O</li>
     *   <li><em>readOnly</em> - if <tt>true</tt>, the storage file must be a valid storage file and the storage will support only read operations</li>
     *   <li><em>startPosition</em> - the position (in bytes) of the first block of the data within the <em>file</em></li>
     *   <li><em>maximalLength</em> - the maximal length (in bytes) of the data written to <em>file</em> by this storage</li>
     *   <li><em>oneStorage</em> - if <tt>true</tt>, the storage is created only once
     *              and this created instance is used in subsequent calls</li>
     *   <li><em>serializator</em> - instance of the serializator that is used (overrides any cacheClasses settings)</li>
     * </ul>
     * 
     * @param <T> the class of objects that the new storage will work with
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param parameters list of named parameters (see above)
     * @return a new disk storage instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws InstantiationException if the parameters specified are invalid (non existent directory, null values, etc.)
     */
    public static <T> DiskStorage<T> create(Class<T> storedObjectsClass, Map<String, Object> parameters) throws IOException, InstantiationException {
        boolean oneStorage = Convert.getParameterValue(parameters, "oneStorage", Boolean.class, false);
        
        if (oneStorage) {
            DiskStorage<T> storage = castToDiskStorage(storedObjectsClass, Convert.getParameterValue(parameters, "storage", DiskStorage.class, null));
            if (storage != null) {
                storage.references++;
                return storage;
            }
        }

        // Read the parameters
        File file = Convert.getParameterValue(parameters, "file", File.class, null);
        Class[] cacheClasses = Convert.getParameterValue(parameters, "cacheClasses", Class[].class, null);
        int bufferSize = Convert.getParameterValue(parameters, "bufferSize", Integer.class, 16384);
        boolean directBuffer = Convert.getParameterValue(parameters, "directBuffer", Boolean.class, true);
        boolean memoryMap = Convert.getParameterValue(parameters, "memoryMap", Boolean.class, false);
        boolean readOnly = Convert.getParameterValue(parameters, "readOnly", Boolean.class, false);
        long startPosition = Convert.getParameterValue(parameters, "startPosition", Long.class, 0L);
        long maximalLength = Convert.getParameterValue(parameters, "maximalLength", Long.class, Long.MAX_VALUE);

        // If a file was not specified - create a new file in given directory
        if (file == null) {
            File dir = Convert.getParameterValue(parameters, "dir", File.class, null);
            if (dir == null) {
                file = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX);
            } else {
                if (! dir.exists()) {
                    log.info("Creating dir: " + dir.toString());
                    dir.mkdirs();
                }
                file = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX, dir);
            }
        }

        // Initialize serializator
        BinarySerializator serializator;
        if (parameters != null && parameters.containsKey("serializator"))
            serializator = (BinarySerializator)parameters.get("serializator");
        else if (cacheClasses == null)
            serializator = new MultiClassSerializator<T>(storedObjectsClass);
        else
            serializator = new CachingSerializator<T>(storedObjectsClass, cacheClasses);
        // Store serializator into map for further use
        if (parameters != null)
            parameters.put("serializator", serializator);

        // Finally, create the storage
        DiskStorage<T> storage = new DiskStorage<T>(storedObjectsClass, file, readOnly, bufferSize, directBuffer, memoryMap, startPosition, maximalLength, serializator);

        // Save the created storage for subsequent calls
        if (oneStorage && parameters != null)
            parameters.put("storage", storage);

        return storage;
    }

    /**
     * Cast the provided object to {@link DiskStorage} with generics typing.
     * The objects stored in the storage must be of the same type as the <code>storageObjectsClass</code>.
     * 
     * @param <E> the class of objects stored in the storage
     * @param storageObjectsClass the class of objects stored in the storage
     * @param object the storage instance
     * @return the generics-typed {@link DiskStorage} object
     * @throws ClassCastException if passed <code>object</code> is not a {@link DiskStorage} or the storage objects are incompatible
     */
    public static <E> DiskStorage<E> castToDiskStorage(Class<E> storageObjectsClass, Object object) throws ClassCastException {
        if (object == null)
            return null;

        @SuppressWarnings("unchecked")
        DiskStorage<E> storage = (DiskStorage)object; // This IS checked on the following line
        if (storage.getStoredObjectsClass() != storageObjectsClass)
            throw new ClassCastException("Storage " + object + " works with incompatible objects");
        return storage;
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

            // Prepare shutdown thread
            if (modifiedThread == null) {
                modifiedThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            DiskStorage.this.finalize();
                        } catch (Throwable e) {
                            log.warning("Error during finalization: " + e);
                        }
                    }
                };
                Runtime.getRuntime().addShutdownHook(modifiedThread);
            }
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
                modified = true;
            }

            // Check the file indicated occupation versus real size
            if (fileChannel.size() < startPosition + headerSize + fileOccupation)
                throw new IOException("Disk storage is corrupted, file occupation indicates " +
                        fileOccupation + " (start: " + startPosition + ", headerSize: " + headerSize +
                        ") but file size is only " + fileChannel.size()
                );
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
        log.info("Rebuilding header of disk storage in file " + file.getAbsolutePath());

        // Reset header values
        objectCount = 0;
        fileOccupation = 0;
        deletedFragments = 0;

        // Read all objects (by seeking)
        BufferInputStream reader = new FileChannelInputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, maximalLength - headerSize);
        try {
            // End iterating once a "null" object is found
            for (int objectSize = serializator.skipObject(reader, false); objectSize != 0; objectSize = serializator.skipObject(reader, false)) {
                if (objectSize > 0) {
                    objectCount++;
                } else {
                    // Negative size means deleted object
                    deletedFragments++;
                }
            }
            fileOccupation = reader.getPosition() - headerSize - 4; // Ignore last object size (integer) read
        } catch (EOFException ignore) {
            fileOccupation = reader.getPosition() - headerSize; // The file end encountered
        } finally {
            reader.close();
        }
    }


    //****************** Construction methods ******************//

    /**
     * Create input stream on the specified channel.
     * @return the created input stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected BufferInputStream openInputStream() throws IOException {
        // Open file channel if not opened yet
        if (fileChannel == null)
            fileChannel = openFileChannel(file, readonly);

        if (log.isLoggable(Level.FINE)) {
            // Measure time
            long time = System.currentTimeMillis();
            BufferInputStream ret;

            // Create the input stream (copied below!)
            if (bufferSize < 0)
                ret = new MappedFileChannelInputStream(fileChannel, startPosition + headerSize, maximalLength - headerSize);
            else
                ret = new FileChannelInputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, maximalLength - headerSize);

            // Report time
            time = System.currentTimeMillis() - time;
            log.fine("Disk storage " + ret.getClass().getName() + " opened with " + ret.bufferedSize() + " bytes buffered in " + time + "ms");
            return ret;
        } else {
            // Create the input stream (copied above!)
            if (bufferSize < 0)
                return new MappedFileChannelInputStream(fileChannel, startPosition + headerSize, maximalLength - headerSize);
            else
                return new FileChannelInputStream(bufferSize, bufferDirect, fileChannel, startPosition + headerSize, maximalLength - headerSize);
        }
    }

    /**
     * Retrieves an input stream for this storage's file.
     * @param position the position on which to set the input stream
     * @return the prepared input stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected BufferInputStream getInputStream(long position) throws IOException {
        BufferInputStream stream = (inputStream == null)?null:inputStream.get();
        if (stream == null || (outputStream != null && outputStream.isDirty())) {
            if (outputStream != null)
                outputStream.flush();
            stream = openInputStream();
            inputStream = new WeakReference<BufferInputStream>(stream);
        }

        stream.setPosition(position);
        return stream;
    }

    /**
     * Opens the output stream over the current file channel.
     * Note that the storage's modification flag is set to <tt>true</tt>.
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected synchronized void openOutputStream() throws IOException {
        // Open file channel if not opened yet
        if (fileChannel == null)
            fileChannel = openFileChannel(file, readonly);

        // Set modified flag
        if (!modified)
            writeHeader(fileChannel, startPosition, 0);

        // Open output stream
        outputStream = new FileChannelOutputStream(Math.abs(bufferSize), bufferDirect, fileChannel, startPosition + headerSize, maximalLength - headerSize);
        outputStream.setPosition(startPosition + headerSize + fileOccupation);
    }

    /**
     * Opens the file channel on <code>file</code> and reads the header.
     * @param file the file to open the channel on
     * @param readonly if <tt>true</tt>, the channel will be opened in read-only mode
     * @return the new file channel
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected FileChannel openFileChannel(File file, boolean readonly) throws IOException {
        // If file does not exist before, it is auto-created by the RandomAccessFile constructor
        boolean fileExists = file.length() > startPosition;

        // Open the channel
        FileChannel chan = new RandomAccessFile(file, readonly?"r":"rw").getChannel();

        // Read the occupation and number of objects
        if (fileExists) {
            readHeader(chan, startPosition);
            // If the header was rebuilt, flush the header so that next open does not need to rebuild it again
            if (modified && !readonly)
                writeHeader(chan, startPosition, FLAG_CLOSED);
        } else {
            writeHeader(chan, startPosition, FLAG_CLOSED);
        }

        return chan;
    }

    /**
     * Internal class that provides an implementation of the {@link Lock} interface.
     */
    private class DiskStorageLock implements Lock {
        /** Locking reference on the buffered data */
        private final BufferInputStream streamReference;

        /**
         * Creates a new instance of DiskStorageLock.
         * @throws IOException if there was an error acquiring the buffered data
         */
        public DiskStorageLock() throws IOException {
            this.streamReference = getInputStream(startPosition + headerSize);
        }

        public void unlock() {
        }
    }

    public synchronized Lock lock(boolean blocking) throws IllegalStateException {
        try {
            return new DiskStorageLock();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize disk storage search", e);
        }
    }


    //****************** Serialization ******************//

    /**
     * Read the serialized disk storage from an object stream.
     * @param in the object stream from which to read the disk storage
     * @throws IOException if there was an I/O error during deserialization
     * @throws ClassNotFoundException if there was an unknown object in the stream
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        // Proceed with standard deserialization first
        in.defaultReadObject();

        this.readonly = !file.canWrite();
    }


    //****************** Overrides ******************//

    /**
     * Returns the class of objects that the this storage works with.
     * @return the class of objects that the this storage works with
     */
    public Class<? extends T> getStoredObjectsClass() {
        return storedObjectsClass;
    }

    /**
     * Returns the file where the data of this storage are stored.
     * @return the file where the data of this storage are stored
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the number of objects stored in this storage.
     * @return the number of objects stored in this storage
     */
    public int size() {
        // Open file channel if not opened yet
        if (fileChannel == null) {
            try {
                fileChannel = openFileChannel(file, readonly);
            } catch (IOException e) {
                throw new IllegalStateException("Error opening disk storage " + file, e);
            }
        }

        return objectCount;
    }

    /**
     * Returns <tt>true</tt> if the storage was modified since last open/flush.
     * @return <tt>true</tt> if the storage was modified since last open/flush
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Flushes this storage and forces any buffered data to be written out.
     * 
     * @param syncPhysical if <tt>true</tt> then also the file is flushed
     *          to be sure the data are really written to disk
     * @throws IOException if there was an I/O error
     */
    public void flush(boolean syncPhysical) throws IOException {
        if (outputStream != null) {
            if (outputStream.isDirty())
                synchronized (this) {
                    inputStream = null;
                    outputStream.flush();
                }
            if (syncPhysical)
                fileChannel.force(false); // If output stream is not null, the file channel is open
        }
    }

    public synchronized LongAddress<T> store(T object) throws BucketStorageException {
        if (readonly)
            throw new ReadonlyStorageException();

        try {
            // Open output stream if not opened yet (this statement is never reached if the storage is readonly)
            if (outputStream == null)
                openOutputStream();

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
            // Remove the object at given position - the size of the object is retrieved by the skip
            remove(position, serializator.skipObject(getInputStream(position), false));
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot remove object from position " + position, e);
        }
    }

    /**
     * Removes object with size <code>objectSize</code> at position <code>position</code>.
     * @param position the absolute position in the file
     * @param objectSize the number of bytes to remove
     * @throws BucketStorageException if there was an error writing to the file
     */
    protected synchronized void remove(long position, int objectSize) throws BucketStorageException {
        if (readonly)
            throw new ReadonlyStorageException();

        try {
            // Open output stream if not opened yet (this statement is never reached if the storage is readonly)
            if (outputStream == null)
                openOutputStream();

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
            return serializator.readObject(getInputStream(position), storedObjectsClass);
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot read object from position " + position, e);
        }
    }


    //****************** Default index implementation ******************//

    public boolean add(T object) throws BucketStorageException {
        return store(object) != null;
    }

    public LongStorageSearch<T> search() throws IllegalStateException {
        return new DiskStorageSearch<Object>(null, Collections.emptyList());
    }

    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException {
        return new DiskStorageSearch<C>(comparator, keys);
    }

    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return new DiskStorageSearch<C>(comparator, Collections.singletonList(key));
    }

    @SuppressWarnings("unchecked")
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        return new DiskStorageSearch<C>(comparator, from, to);
    }

    /**
     * Implements the basic search in the memory storage.
     * All objects in the storage are searched from the first one to the last.
     * 
     * @param <C> the type the boundaries used by the search
     */
    private class DiskStorageSearch<C> extends AbstractSearch<C, T> implements LongStorageSearch<T> {
        /** Internal stream that reads objects in this storage one by one */
        private final BufferInputStream inputStream;
        /** Position of the last returned object - used for removal */
        private long lastObjectPosition = -1;

        /**
         * Creates a new instance of DiskStorageSearch for the specified search comparator and keys.
         * If {@code keyBounds} is <tt>false</tt>, this search will look for any object
         * that equals (according to the given comparator) to any of the keys.
         * Otherwise, the objects that are within interval <code>[keys[0]; keys[1]]</code>
         * are returned.
         *
         * @param comparator the comparator that is used to compare the keys
         * @param keys list of keys to search for
         * @throws IllegalStateException if there was a problem initializing disk storage
         */
        private DiskStorageSearch(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException {
            super(comparator, keys);
            try {
                flush(false);
                this.inputStream = openInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize disk storage search", e);
            }
        }

        /**
         * Creates a new instance of DiskStorageSearch for the specified search comparator and [from,to] bounds.
         * @param comparator the comparator that compares the <code>keys</code> with the stored objects
         * @param fromKey the lower bound on the searched keys
         * @param toKey the upper bound on the searched keys
         */
        private DiskStorageSearch(IndexComparator<? super C, ? super T> comparator, C fromKey, C toKey) {
            super(comparator, fromKey, toKey);
            try {
                flush(false);
                this.inputStream = openInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize disk storage search", e);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }

        @Override
        protected T readNext() throws BucketStorageException {
            try {
                lastObjectPosition = inputStream.getPosition();
                return serializator.readObject(inputStream, storedObjectsClass);
            } catch (EOFException e) {
                return null;
            } catch (IOException e) {
                throw new StorageFailureException("Cannot read next object from disk storage", e);
            }
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            throw new UnsupportedOperationException("This is not supported by the disk storage, use index");
        }

        public LongAddress<T> getCurrentObjectAddress() {
            return new LongAddress<T>(DiskStorage.this, getCurrentObjectLongAddress());
        }

        public long getCurrentObjectLongAddress() throws IllegalStateException {
            if (lastObjectPosition == -1)
                throw new IllegalStateException("There is no object to get address for");
            return lastObjectPosition;
        }

        public void remove() throws IllegalStateException, BucketStorageException {
            DiskStorage.this.remove(getCurrentObjectLongAddress(), (int)(inputStream.getPosition() - lastObjectPosition - 4));
            lastObjectPosition = -1;
        }

        public void close() {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
