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
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.BucketStorageException;
import messif.buckets.CapacityFullException;
import messif.buckets.StorageFailureException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.impl.AbstractSearch;
import messif.buckets.storage.LongAddress;
import messif.buckets.storage.LongStorageIndexed;
import messif.buckets.storage.LongStorageSearch;
import messif.buckets.storage.ReadonlyStorageException;
import messif.objects.nio.AsynchronousFileChannelInputStream;
import messif.objects.nio.AsynchronousFileChannelOutputStream;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.BufferInputStream;
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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DiskStorage<T> implements LongStorageIndexed<T>, Serializable {
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
    /** Ratio of {@link #deletedFragments} to {@link #objectCount} when the {@link #compactData()} is executed */
    protected static final float COMPACTING_FRAGMENTATION_RATIO = 0.5f;
    /** Header flag constant for indication whether the file was correctly closed */
    protected static final int FLAG_CLOSED = 0x00000003; // lower two bits
    /** Default number of asynchronous threads */
    protected static final int DEFAULT_ASYNC_THREADS = 128;
    /** Default size of the reading buffer */
    protected static final int DEFAULT_BUFFER_SIZE = 16*1024;


    //****************** Attributes ******************//

    /** Buffer sizes for read/write operations */
    private final int bufferSize;
    /** Allocate the buffers for read/write operations as {@link ByteBuffer#allocateDirect direct} */
    private final boolean bufferDirect;
    /** The number of objects currently stored in the file */
    private transient int objectCount;
    /** The number of bytes currently stored in the file (excluding headers) */
    private transient long fileOccupation;
    /** The number of deleted objects - the file space fragmentation is the ratio between this and objectCount */
    private transient int deletedFragments;
    /** The file with data */
    private final File file;
    /** The channel on the file with data */
    private transient AsynchronousFileChannel fileChannel;
    /** The position in the file where this storage starts (the real data starts at startPosition + headerSize) */
    protected final long startPosition;
    /** The maximal length of the file */
    private final long maximalLength;
    /** Serializator responsible for storing (and restoring) binary objects in the file */
    private final BinarySerializator serializator;
    /** Class of objects that the this storage works with */
    private final Class<? extends T> storedObjectsClass;
    /** Number of items that reference this disk storage */
    private int references = 0;
    /** Maximal number of input streams to use (for asynchronous reading) */
    private final int inputStreamCount;
    /** Queue of prepared buffer streams for (asynchronous) reading */
    private transient BlockingDeque<SoftReference<AsynchronousFileChannelInputStream>> inputStreams;
    /** Stream for writing data */
    private transient AsynchronousFileChannelOutputStream outputStream;
    /** Flag whether the file is modified */
    private transient boolean modified;
    /** Flag whether the file is readonly */
    private transient boolean readonly;
    /** Finalize thread that writes a modified header */
    private transient Thread modifiedThread;


    //****************** Constructors ******************//

    /**
     * Creates a new DiskStreamStorage instance.
     *
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param file the file in which to create the bucket
     * @param readonly if <tt>true</tt>, the storage will be opened in read-only mode (e.g. the store method will throw an exception)
     * @param bufferSize the size of the buffer used for reading/writing
     * @param bufferDirect the bucket is either direct (<tt>true</tt>) or array-backed (<tt>false</tt>)
     * @param asyncThreads the maximal number of threads to use (for asynchronous reading)
     * @param startPosition the position in the file where this storage starts
     * @param maximalLength the maximal length of the file
     * @param serializator the object responsible for storing (and restoring) binary objects
     * @throws IOException if there was an error opening the bucket file
     */
    public DiskStorage(Class<? extends T> storedObjectsClass, File file, boolean readonly, int bufferSize, boolean bufferDirect, int asyncThreads, long startPosition, long maximalLength, BinarySerializator serializator) throws IOException {
        this.storedObjectsClass = storedObjectsClass;
        this.file = file;
        if (bufferSize < 0)
            throw new IllegalArgumentException("Bufer size must positive");
        this.bufferSize = bufferSize == 0 ? DEFAULT_BUFFER_SIZE : bufferSize;
        this.bufferDirect = bufferDirect;
        if (asyncThreads < 0)
            throw new IllegalArgumentException("Number of asynchronous threads must positive");
        this.inputStreamCount = asyncThreads == 0 ? DEFAULT_ASYNC_THREADS : asyncThreads;
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
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public DiskStorage(final DiskStorage<? extends T> copyAttributesDiskStorage, File file) throws IOException {
        this.storedObjectsClass = copyAttributesDiskStorage.storedObjectsClass;
        this.file = file;
        this.bufferSize = copyAttributesDiskStorage.bufferSize;
        this.bufferDirect = copyAttributesDiskStorage.bufferDirect;
        this.inputStreamCount = copyAttributesDiskStorage.inputStreamCount;
        this.startPosition = copyAttributesDiskStorage.startPosition;
        this.maximalLength = copyAttributesDiskStorage.maximalLength;
        this.serializator = copyAttributesDiskStorage.serializator;
        this.readonly = copyAttributesDiskStorage.readonly;
    }

    @Override
    @SuppressWarnings("FinalizeNotProtected")
    public void finalize() throws Throwable {
        if (modifiedThread != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(modifiedThread);
            } catch (IllegalStateException ignore) {
            }
            modifiedThread = null;
        }
        closeFileChannel();
        super.finalize();
    }

    @Override
    public void destroy() throws Throwable {
        if (closeFileChannel())
            file.delete();
    }


    //****************** Factory method ******************//

    /**
     * Increment the number of references to this storage by one.
     */
    private void incrementReferences() {
        references++;
    }

    /**
     * Creates a new disk storage. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>file</em> - the path to the particular disk storage (either as File or as String)</li>
     *   <li><em>dir</em> - the path to a directory (either as File or as String) where a temporary file name is
     *       created in the format of "disk_storage_XXXX.ds"</li>
     *   <li><em>cacheClasses</em> - comma-separated list of classes that will be cached for fast serialization</li>
     *   <li><em>bufferSize</em> - the size of the buffers used for I/O operations</li>
     *   <li><em>directBuffer</em> - flag controlling whether to use faster direct buffers for I/O operations</li>
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
                storage.incrementReferences();
                return storage;
            }
        }

        // Read the parameters
        File file = Convert.getParameterValue(parameters, "file", File.class, null);
        Class[] cacheClasses = Convert.getParameterValue(parameters, "cacheClasses", Class[].class, null);
        int bufferSize = Convert.getParameterValue(parameters, "bufferSize", Integer.class, DEFAULT_BUFFER_SIZE);
        boolean directBuffer = Convert.getParameterValue(parameters, "directBuffer", Boolean.class, false);
        int asyncThreads = Convert.getParameterValue(parameters, "asyncThreads", Integer.class, DEFAULT_ASYNC_THREADS);
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
                    log.log(Level.INFO, "Creating dir: {0}", dir.toString());
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
            serializator = new MultiClassSerializator<>(storedObjectsClass);
        else
            serializator = new CachingSerializator<>(storedObjectsClass, cacheClasses);
        // Store serializator into map for further use
        if (parameters != null)
            parameters.put("serializator", serializator);

        // Finally, create the storage
        DiskStorage<T> storage = new DiskStorage<>(storedObjectsClass, file, readOnly, bufferSize, directBuffer, asyncThreads, startPosition, maximalLength, serializator);

        // Save the created storage for subsequent calls
        if (oneStorage && parameters != null)
            parameters.put("storage", storage);

        return storage;
    }

    /**
     * Cast the provided object to {@link DiskStorage} with generic typing.
     * The objects stored in the storage must be of the same type as the <code>storageObjectsClass</code>.
     *
     * @param <E> the class of objects stored in the storage
     * @param storageObjectsClass the class of objects stored in the storage
     * @param object the storage instance
     * @return the generic-typed {@link DiskStorage} object
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


    //****************** Internal methods for reading/writing from file channel ******************//

    /**
     * Opens an asynchronous file channel on the given file.
     * @param file the file to open
     * @param readonly the flag whether to open the file as read-only
     * @param asyncThreads the maximal number of threads to use for asynchronous reading
     * @return an opened asynchronous file channel
     * @throws IOException if there was an error opening the file
     */
    private static AsynchronousFileChannel openFileChannel(File file, boolean readonly, int asyncThreads) throws IOException {
        Set<? extends OpenOption> options;
        if (readonly)
            options = Collections.singleton(StandardOpenOption.READ);
        else
            options = new HashSet<>(Arrays.asList(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
        return AsynchronousFileChannel.open(file.toPath(), options, asyncThreads > 0 ? Executors.newFixedThreadPool(asyncThreads) : null);
    }

    /**
     * Internal method that allows synchronous read from an asynchronous file channel.
     * @param fileChannel the file channel to read from
     * @param buffer the buffer to store the read data to
     * @param position the position in the file channel to read from
     * @return the number of bytes read into the buffer
     * @throws IOException if there was an error reading the data
     */
    private static int readFromFileChannel(AsynchronousFileChannel fileChannel, ByteBuffer buffer, long position) throws IOException {
        try {
            return fileChannel.read(buffer, position).get();
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }

    /**
     * Internal method that allows synchronous write to an asynchronous file channel.
     * @param fileChannel the file channel to write to
     * @param buffer the buffer to read the written data from
     * @param position the position in the file channel to write to
     * @return the number of bytes written from the buffer
     * @throws IOException if there was an error writing the data
     */
    private static int writeToFileChannel(AsynchronousFileChannel fileChannel, ByteBuffer buffer, long position) throws IOException {
        try {
            return fileChannel.write(buffer, position).get();
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
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
    protected synchronized void writeHeader(AsynchronousFileChannel fileChannel, long position, int flags) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(headerSize);
        buffer.putLong(serialVersionUID);
        buffer.putLong(maximalLength);
        buffer.putInt(serializator.hashCode()); // Hash of the serializator (hashes of the UUIDs of the cached objects)
        buffer.putInt(flags & ~FLAG_CLOSED); // Closed bits are set to zero for the first time even if closing
        buffer.putLong(fileOccupation);
        buffer.putInt(objectCount);
        buffer.putInt(deletedFragments);
        buffer.flip();
        writeToFileChannel(fileChannel, buffer, position);
        if ((flags & FLAG_CLOSED) == FLAG_CLOSED) {
            // Replace flag with closed bit
            buffer.putInt(20, flags); // !!!! WARNING !!!! Don't forget to change the position here !!!!
            buffer.rewind();
            fileChannel.force(true);
            writeToFileChannel(fileChannel, buffer, position);
            modified = false;
        } else {
            modified = true;

            // Prepare shutdown thread
            if (modifiedThread == null) {
                modifiedThread = new Thread() {
                    @Override
                    @SuppressWarnings("FinalizeCalledExplicitly")
                    public void run() {
                        try {
                            DiskStorage.this.finalize();
                        } catch (Throwable e) {
                            log.log(Level.WARNING, "Error during finalization: {0}", (Object)e);
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
    protected synchronized void readHeader(AsynchronousFileChannel fileChannel, long position) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(headerSize);

        // Read header bytes
        readFromFileChannel(fileChannel, buffer, position);
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
                modified = !readonly;
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
    protected synchronized void reconstructHeader(AsynchronousFileChannel fileChannel, long position) throws IOException {
        log.log(Level.INFO, "Rebuilding header of disk storage in file {0}", file.getAbsolutePath());

        // Reset header values
        objectCount = 0;
        fileOccupation = 0;
        deletedFragments = 0;
        try (BufferInputStream reader = new AsynchronousFileChannelInputStream(bufferSize, bufferDirect, fileChannel, position, maximalLength)) {
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
            }
        }
    }

    /**
     * Compacts the deleted fragments of the disk storage.
     * All objects are read from the original file and written
     * to a new temporary file. The new file is then renamed to the original one.
     * Note that this operation is thread safe and it is also compatible with multiple
     * searches running on the file concurrently.
     *
     * @throws IOException if there was a problem with reading or writing the data
     */
    // FIXME: this operation WILL break indexes built on this storage
    protected synchronized void compactData() throws IOException {
        if (readonly || deletedFragments == 0)
            return;
        if (startPosition != 0) // Compacting is not yet implemented on one-storage, sorry
            return;

        File compactFile = new File(file.getParentFile(), file.getName() + ".compact");
        log.log(Level.INFO, "Compacting disk storage in file {0}", file.getAbsolutePath());

        if (compactFile.exists()) {
            log.log(Level.WARNING, "Cannot compact disk storage - the file {0} already exists", compactFile);
            return;
        }

        // Open compact file
        AsynchronousFileChannel compactChan = openFileChannel(compactFile, false, 0);
        long position;
        try {
            try (BufferInputStream reader = openInputStream()) {
                writeHeader(compactChan, startPosition, 0);
                position = headerSize;

                // Read all objects and write them to the compact channel
                ByteBuffer writeBuffer = bufferDirect ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
                for (int objectSize = serializator.objectToBuffer(reader, writeBuffer, bufferSize); objectSize != 0; objectSize = serializator.objectToBuffer(reader, writeBuffer, bufferSize)) {
                    writeBuffer.flip();
                    position += writeToFileChannel(compactChan, writeBuffer, position);
                    writeBuffer.compact();
                }
                // Write remaining data if any (this should never happen, since these are file channels)
                writeBuffer.flip();
                while (writeBuffer.remaining() > 0)
                    position += writeToFileChannel(compactChan, writeBuffer, position);
            }
        } finally {
            compactChan.close();
        }

        if (!compactFile.renameTo(file)) {
            log.log(Level.WARNING, "Cannot replace original disk storage file {0} with compacted data in {1}", new Object[] {file.getAbsolutePath(), compactFile.getAbsolutePath()});
            return;
        } else {
            // Update file statistics
            fileOccupation = position - headerSize;
            deletedFragments = 0;
            modified = true;
        }

        // Reopen file channel
        fileChannel.close();
        fileChannel = openFileChannel(file, readonly, 0);
    }

    /**
     * Returns the file space fragmentation, i.e. the ratio between the free and occupied disk space.
     * @return the file space fragmentation
     */
    public float getFragmentation() {
        return (float)deletedFragments / (objectCount + deletedFragments);
    }


    //****************** File open/close methods ******************//

    /**
     * Create input stream on the specified channel.
     * @return the created input stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected AsynchronousFileChannelInputStream openInputStream() throws IOException {
        if (log.isLoggable(Level.FINE)) {
            // Measure time
            long time = System.currentTimeMillis();

            // Create the input stream (copied below!)
            AsynchronousFileChannelInputStream ret = new AsynchronousFileChannelInputStream(bufferSize, bufferDirect, getFileChannel(), startPosition + headerSize, maximalLength);

            // Report time
            time = System.currentTimeMillis() - time;
            log.log(Level.FINE, "Disk storage {0} opened with {1} bytes buffered in {2}ms", new Object[]{ret.getClass().getName(), ret.bufferedSize(), time});
            return ret;
        } else {
            return new AsynchronousFileChannelInputStream(bufferSize, bufferDirect, getFileChannel(), startPosition + headerSize, maximalLength);
        }
    }

    /**
     * Retrieves an input stream for this storage's file.
     * Note that it is imperative to return the taken input stream back via {@link #returnInputStream}.
     * @param position the position on which to set the input stream
     * @return the prepared input stream
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected AsynchronousFileChannelInputStream takeInputStream(long position) throws IOException {
        // Need to store all objects currently in the store buffer, so that they are visible to the input stream
        if (outputStream != null && outputStream.isDirty()) {
            outputStream.flush();
        }

        // Take one stream from the queue, wait if there are none
        AsynchronousFileChannelInputStream stream;
        try {
            stream = inputStreams == null ? null : inputStreams.takeFirst().get();
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }

        // Set position and return the stream
        try {
            // If the stream is not initialized (due to the soft reference), prepare it
            if (stream == null)
                stream = openInputStream();

            stream.setPosition(position);
            return stream;
        } catch (IOException | RuntimeException e) {
            // Return an empty placeholder into the queue on error
            if (inputStreams != null) {
                inputStreams.offer(new SoftReference<AsynchronousFileChannelInputStream>(null));
            }
            throw e;
        }
    }

    /**
     * Return the taken stream back into the waiting queue.
     * @param stream the stream to return
     * @see #takeInputStream(long)
     */
    protected void returnInputStream(AsynchronousFileChannelInputStream stream) {
        inputStreams.offerFirst(new SoftReference<>(stream));
    }

    /**
     * Opens the output stream over the current file channel.
     * Note that the storage's modification flag is set to <tt>true</tt>.
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected synchronized void openOutputStream() throws IOException {
        // Set modified flag
        if (!modified)
            writeHeader(getFileChannel(), startPosition, 0);

        // Open output stream
        outputStream = new AsynchronousFileChannelOutputStream(Math.abs(bufferSize), bufferDirect, getFileChannel(), startPosition + headerSize, maximalLength);
        outputStream.setPosition(startPosition + headerSize + fileOccupation);
    }

    /**
     * Returns the currently opened file channel.
     * If the file channel is not open yet, the {@link #file} is opened and the header read.
     * @return the current file channel
     * @throws IOException if something goes wrong when working with the filesystem
     */
    protected final AsynchronousFileChannel getFileChannel() throws IOException {
        if (fileChannel != null)
            return fileChannel;

        synchronized (this) {
            if (fileChannel != null)
                return fileChannel;
            
            // If file does not exist before, it is auto-created by the RandomAccessFile constructor
            boolean fileExists = file.length() > startPosition;

            // Open the channel (if the file does not exist and readonly is true, IOException is thrown, otherwise the file is created)
            fileChannel = openFileChannel(file, readonly, 0);

            // Create thread buffer placeholders
            inputStreams = new LinkedBlockingDeque<>(inputStreamCount);
            for (int i = 0; i < inputStreamCount; i++) {
                inputStreams.add(new SoftReference<AsynchronousFileChannelInputStream>(null));
            }

            // Read the occupation and number of objects
            if (fileExists) {
                readHeader(fileChannel, startPosition);
                if (deletedFragments > COMPACTING_FRAGMENTATION_RATIO * objectCount)
                    compactData();
                // If the header was rebuilt, flush the header so that next open does not need to rebuild it again
                if (modified)
                    writeHeader(fileChannel, startPosition, FLAG_CLOSED);
            } else {
                writeHeader(fileChannel, startPosition, FLAG_CLOSED);
            }

            return fileChannel;
        }
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
                    for (SoftReference<?> inputStreamRef : inputStreams) {
                        inputStreamRef.clear();
                    }
                    outputStream.flush();
                }
            if (syncPhysical)
                fileChannel.force(false); // If output stream is not null, the file channel is open
        }
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
                outputStream = null;
                writeHeader(fileChannel, startPosition, FLAG_CLOSED);
            }
            fileChannel.close();
            fileChannel = null;
            return true;
        } else {
            references--;
            return false;
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
        if (inputStreamCount <= 0) {
            try {
                Field countField = DiskStorage.class.getDeclaredField("inputStreamCount");
                countField.setAccessible(true);
                countField.set(this, DEFAULT_ASYNC_THREADS);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(DiskStorage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
    @Override
    public int size() {
        try {
            getFileChannel(); // Open file channel if not opened yet (so that header is read)
            return objectCount;
        } catch (IOException e) {
            throw new IllegalStateException("Error opening disk storage " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns <tt>true</tt> if the storage was modified since last open/flush.
     * @return <tt>true</tt> if the storage was modified since last open/flush
     */
    public boolean isModified() {
        return modified;
    }

    @Override
    public synchronized LongAddress<T> store(T object) throws BucketStorageException {
        if (readonly)
            throw new ReadonlyStorageException();

        try {
            // Open output stream if not opened yet (this statement is never reached if the storage is readonly)
            if (outputStream == null)
                openOutputStream();

            // Remember address
            LongAddress<T> address = new LongAddress<>(this, outputStream.getPosition());

            // Write object
            fileOccupation += serializator.write(outputStream, object);

            // Update internal counters
            objectCount++;

            return address;
        } catch (EOFException e) {
            throw new CapacityFullException(e.getMessage());
        } catch (IOException e) {
            throw new StorageFailureException("Cannot store object into disk storage", e);
        }
    }

    @Override
    public synchronized void remove(long position) throws BucketStorageException {
        try {
            // Remove the object at given position - the size of the object is retrieved by the skip
            AsynchronousFileChannelInputStream stream = takeInputStream(position);
            try {
                remove(position, serializator.skipObject(stream, false));
            } finally {
                returnInputStream(stream);
            }
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

    @Override
    public T read(long position) throws BucketStorageException {
        try {
            AsynchronousFileChannelInputStream stream = takeInputStream(position);
            try {
                return serializator.readObject(stream, storedObjectsClass);
            } finally {
                returnInputStream(stream);
            }
        } catch (IOException e) {
            throw new StorageFailureException("Disk storage cannot read object from position " + position, e);
        }
    }

    /**
     * Read multiple objects asynchronously stored at the specified addresses in this storage.
     * Note that the method returns immediately and the read is started in the asynchronous threads.
     * The iterator will block until some objects are provided by the asynchronous read.
     * The objects in the iterator <em>will not</em> be necessarily returned in the order
     * of the given positions. If there is an I/O error during the asynchronous read,
     * the iterator will thrown {@link IllegalStateException} exception with the encapsulated error.
     *
     * @param positions the addresses of the objects to read
     * @return a blocking iterator of the objects retrieved
     */
    public Iterator<T> read(long... positions) {
        return new AsyncReadIterator(positions);
    }

    /**
     * Internal class implementing the asynchronous read iterator.
     * @see #read(long[])
     */
    private class AsyncReadIterator extends Thread implements Iterator<T>, AsynchronousFileChannelInputStream.AsynchronousReadCallback {
        /** Array of positions in the storage from which to read the objects */
        private final long[] positions;
        /** Internal queue used for storing the retrieved objects */
        private final BlockingQueue<T> queue;
        /** Number of objects returned by the iterator so far */
        private int returnedCount;
        /** Asynchronous operation exception that is returned in nearest call to {@link #next()} */
        private Throwable exc;

        /**
         * Creates a new asynchronous read iterator.
         * @param positions the array of positions in the storage from which to read the objects
         */
        @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
        private AsyncReadIterator(long... positions) {
            this.positions = positions;
            this.queue = new ArrayBlockingQueue<>(positions.length);
            start();
        }

        @Override
        public boolean hasNext() {
            return returnedCount < positions.length;
        }

        @Override
        public T next() throws IllegalStateException, NoSuchElementException {
            if (exc != null)
                throw new IllegalStateException("There was an error in asynchronous reading: " + exc, exc);
            if (!hasNext())
                throw new NoSuchElementException();
            try {
                T ret = queue.take();
                returnedCount++;
                return ret;
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted during waiting for another object to be asynchronously read", e);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Asynchronous read iterator cannot remove objects");
        }

        @Override
        public void run() {
            for (long position : positions) {
                try {
                    // Takend input stream is returned in the complete/fail handler
                    takeInputStream(position).readAsynchronously(this);
                } catch (IOException e) {
                    if (this.exc == null)
                        this.exc = e;
                    break;
                }
            }
        }

        @Override
        public void completed(AsynchronousFileChannelInputStream input) {
            try {
                queue.add(serializator.readObject(input, storedObjectsClass));
            } catch (IOException e) {
                if (this.exc == null)
                    this.exc = e;
            } finally {
                returnInputStream(input);
            }
        }

        @Override
        public void failed(AsynchronousFileChannelInputStream input, Throwable exc) {
            if (this.exc == null)
                this.exc = exc;
            returnInputStream(input);
        }
    }


    //****************** Default index implementation ******************//

    @Override
    public boolean add(T object) throws BucketStorageException {
        return store(object) != null;
    }

    @Override
    public LongStorageSearch<T> search() throws IllegalStateException {
        return new DiskStorageSearch<>(null, Collections.emptyList());
    }

    @Override
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, Collection<? extends C> keys) throws IllegalStateException {
        return new DiskStorageSearch<>(comparator, keys);
    }

    @Override
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C key) throws IllegalStateException {
        return new DiskStorageSearch<>(comparator, Collections.singletonList(key));
    }

    @Override
    public <C> LongStorageSearch<T> search(IndexComparator<? super C, ? super T> comparator, C from, C to) throws IllegalStateException {
        return new DiskStorageSearch<>(comparator, from, to);
    }

    /**
     * Implements the basic search in the disk storage.
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
                throw new IllegalStateException("Cannot initialize disk storage search: " + e, e);
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
                throw new IllegalStateException("Cannot initialize disk storage search: " + e, e);
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
                throw new StorageFailureException("Cannot read next object from disk storage: " + e, e);
            }
        }

        @Override
        protected T readPrevious() throws BucketStorageException {
            throw new UnsupportedOperationException("This is not supported by the disk storage, use index");
        }

        @Override
        public LongAddress<T> getCurrentObjectAddress() {
            return new LongAddress<>(DiskStorage.this, getCurrentObjectLongAddress());
        }

        @Override
        public long getCurrentObjectLongAddress() throws IllegalStateException {
            if (lastObjectPosition == -1)
                throw new IllegalStateException("There is no object to get address for");
            return lastObjectPosition;
        }

        @Override
        public void remove() throws IllegalStateException, BucketStorageException {
            DiskStorage.this.remove(getCurrentObjectLongAddress(), (int)(inputStream.getPosition() - lastObjectPosition - 4));
            lastObjectPosition = -1;
        }

        @Override
        public void close() {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
