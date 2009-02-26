/*
 *  VirtualStorageBucket
 * 
 */

package messif.buckets.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import messif.buckets.OrderedLocalBucket;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.impl.AddressStorageIndex;
import messif.buckets.index.impl.IntStorageIndex;
import messif.buckets.index.impl.LongStorageIndex;
import messif.buckets.storage.IntStorage;
import messif.buckets.storage.LongStorage;
import messif.buckets.storage.Storage;
import messif.objects.LocalAbstractObject;
import messif.utility.Convert;

/**
 * Encapsulating bucket for generic indices and storages.
 * 
 * @param <C> type of the keys that this bucket's objects are ordered by
 * @author xbatko
 */
public final class VirtualStorageBucket<C> extends OrderedLocalBucket<C> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Attributes ******************//

    /** Internal index with encapsulated storage */
    private final ModifiableOrderedIndex<C, LocalAbstractObject> index;


    //****************** Constructor ******************//

    /**
     * Constructs a new MemoryStorageBucket instance
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param index the index to encapsulate
     */
    public VirtualStorageBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, ModifiableOrderedIndex<C, LocalAbstractObject> index) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
        this.index = index;
    }

    @Override
    public void finalize() throws Throwable {
        index.destroy();
        super.finalize();
    }
    
    @Override
    protected ModifiableOrderedIndex<C, LocalAbstractObject> getModifiableIndex() {
        return index;
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a bucket. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>storageClass</em> - the class of the storage that this bucket operates on</li>
     *   <li><em>comparatorInstance</em> - a fully specified constructor signature with arguments of a class that implements the {@link IndexComparator}</li>
     *   <li><em>comparatorClass</em> - a fully specified class name with empty public constructor that implements the {@link IndexComparator}</li>
     *   <li><em>localAbstractObjectOrder</em> - the name of an enum constant or a static field of the {@link LocalAbstractObjectOrder}</li>
     * </ul>
     * <p>
     * If any of the <em>comparatorInstance</em>, <em>comparatorClass</em>, <em>localAbstractObjectOrder</em> is specified,
     * an index is created using this comparator. Otherwise, the storage is used in internal order.
     * </p>
     * <p>
     * Note that additional parameters may be required according to the specified <em>storageClass</em>.
     * See the documentation of that storage.
     * </p>
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters (see above)
     * @return a new SimpleDiskBucket instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws IllegalArgumentException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    public static VirtualStorageBucket<?> getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IOException, IllegalArgumentException, ClassNotFoundException {
        try {
            // Create storage - retrieve class from parameter and use "create" factory method
            Class<? extends Storage> storageClass = Convert.genericCastToClass(parameters.get("storageClass"), Storage.class);
            @SuppressWarnings("unchecked")
            Storage<LocalAbstractObject> storage = Convert.createInstanceUsingFactoryMethod(storageClass, "create", LocalAbstractObject.class, parameters);

            // Create the comparator from the parameters, encapsulate it with an index and then by the virtual bucket
            return getBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, storage, createComparator(parameters));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Creates a bucket for the given storage and comparator.
     * 
     * @param <T> type of the keys that the new bucket's objects will be ordered by
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param storage the underlying storage for object persistence
     * @param comparator the comparator that imposes order on the indexed objects
     * @return a new SimpleDiskBucket instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws IllegalArgumentException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    public static <T> VirtualStorageBucket<T> getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Storage<LocalAbstractObject> storage, IndexComparator<T, LocalAbstractObject> comparator) throws IOException, IllegalArgumentException, ClassNotFoundException {
        return new VirtualStorageBucket<T>(capacity, softCapacity, lowOccupation, occupationAsBytes, createIndex(storage, comparator));
    }

    /**
     * Creates a comparator for the new indexed virtual bucket.
     * The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>comparatorInstance</em> - a fully specified constructor signature with arguments of a class that implements the {@link IndexComparator}</li>
     *   <li><em>comparatorClass</em> - a fully specified class name with empty public constructor that implements the {@link IndexComparator}</li>
     *   <li><em>localAbstractObjectOrder</em> - the name of an enum constant or a static field of the {@link LocalAbstractObjectOrder}</li>
     * </ul>
     * @param parameters list of named parameters (see above)
     * @return a new instance of the {@link IndexComparator} or <tt>null</tt> if the specified parameters does not contain a comparator specification
     * @throws IllegalArgumentException if there was an error creating comparator - see the encapsulated exception for details
     */
    @SuppressWarnings("unchecked")
    private static IndexComparator<?, LocalAbstractObject> createComparator(Map<String, Object> parameters) throws IllegalArgumentException {
        try {
            String comparatorInstance = Convert.getParameterValue(parameters, "comparatorInstance", String.class, null);
            if (comparatorInstance != null)
                return Convert.createInstanceWithStringArgs(comparatorInstance, IndexComparator.class, null); // This is unchecked, but it cannot be checked until two objects are compared
            Class<? extends IndexComparator> comparatorClass = Convert.genericCastToClass(parameters.get("comparatorClass"), IndexComparator.class);
            if (comparatorClass != null)
                return comparatorClass.newInstance(); // This is unchecked, but it cannot be checked until two objects are compared
            String localAbstractObjectOrder = Convert.getParameterValue(parameters, "localAbstractObjectOrder", String.class, null);
            if (localAbstractObjectOrder != null)
                try {
                    // Try enum constant
                    return LocalAbstractObjectOrder.valueOf(localAbstractObjectOrder.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Try static attribute
                    return (IndexComparator)LocalAbstractObjectOrder.class.getField(localAbstractObjectOrder).get(null); // This is unchecked, but the class does not contain wrong objects
                }
            return null;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getCause().toString(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString(), e);
        }
    }

    /**
     * Creates an index for the given storage and comparator.
     * 
     * @param <T> the type of the keys the objects in the index will be ordered by
     * @param storage the storage over which the new index will operate
     * @param comparator the comparator that imposes order of keys in the index
     * @return a new instance of index
     * @throws IllegalArgumentException if the comparator is <tt>null</tt>
     */
    @SuppressWarnings("unchecked")
    private static <T> ModifiableOrderedIndex<T, LocalAbstractObject> createIndex(Storage<LocalAbstractObject> storage, IndexComparator<T, LocalAbstractObject> comparator) throws IllegalArgumentException {
        // All the conversions here are unchecked, but if the storage operates on LocalAbstractObject, everything is correct
        if (comparator == null) {
            throw new IllegalArgumentException("Cannot create index for null comparator");
        } else if (storage instanceof IntStorage) {
            return new IntStorageIndex((IntStorage)storage, comparator);
        } else if (storage instanceof LongStorage) {
            return new LongStorageIndex((LongStorage)storage, comparator);
        } else {
            return new AddressStorageIndex(storage, comparator);
        }
    }
}
