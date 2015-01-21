/*
 * TestConstants
 *
 */

package test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.buckets.impl.DiskBlockBucket;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.impl.IntStorageIndex;
import messif.buckets.index.impl.LongStorageIndex;
import messif.buckets.storage.IntStorage;
import messif.buckets.storage.LongStorage;
import messif.buckets.storage.Storage;
import messif.buckets.storage.impl.DiskStorage;
import messif.objects.LocalAbstractObject;
import messif.objects.PrecomputedDistancesFixedArrayFilter;
import messif.objects.impl.ObjectIntVectorL1;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.CachingSerializator;
import messif.objects.nio.MultiClassSerializator;
import messif.objects.util.StreamGenericAbstractObjectIterator;

/**
 *
 * @author xbatko
 */
public class TestConstants {

    public static final File directoryForInputStreamTest = null;// new File("_directory_test");
    public static final String storfilePrefix = "test-storfile-";
    public static final File diskBucketFile = new File("test-disk-bucket.dbb");
    public static final File serializatorTestFile = new File("test-serializator.file");

    public static final int objectsCount = 100;
    public static final int benchmarkObjectCount = -1; // Use -1 to disable benchmark test

    public static Storage<LocalAbstractObject> createStorage() throws Exception {
        BinarySerializator serializator = new MultiClassSerializator<LocalAbstractObject>(LocalAbstractObject.class);
/**
        return new MemoryStorage<LocalAbstractObject>(LocalAbstractObject.class);
/**/
/**/
        File storfile = File.createTempFile(storfilePrefix, ".stor", new File("."));
        return new DiskStorage<LocalAbstractObject>(LocalAbstractObject.class, storfile, false, 16*1024, true, 0, 0, Long.MAX_VALUE, serializator);
/**/
/*
        create database pokus;
        grant all privileges on pokus.* to pokus@'%' identified by 'pokus';
        create table pokus.pokus(id int auto_increment primary key, data blob, locator varchar(255));

        drop database pokus;
        drop user pokus;
*/
/**
        Properties props = new Properties();
        props.setProperty("user", "pokus");
        props.setProperty("password", "pokus");

        return new DatabaseStorage<LocalAbstractObject>(
                LocalAbstractObject.class,
                "jdbc:mysql://andromeda.fi.muni.cz/pokus", props,
                "pokus", "id",
                new String[] {
                    "data",
                    "locator"
                },
                new ColumnConvertor[] {
                    new DatabaseStorage.BinarySerializableColumnConvertor<LocalAbstractObject>(LocalAbstractObject.class, serializator),
                    DatabaseStorage.locatorColumnConvertor
                }
        );
/**/
    }

    public static ModifiableIndex<LocalAbstractObject> createIndex() throws Exception {
        return (ModifiableIndex<LocalAbstractObject>)createStorage();
    }

    public static ModifiableOrderedIndex<String, LocalAbstractObject> createOrderedIndex() throws Exception {
        Storage<LocalAbstractObject> storage = createStorage();
        if (storage instanceof IntStorage) {
            return new IntStorageIndex<String, LocalAbstractObject>((IntStorage<LocalAbstractObject>)storage, LocalAbstractObjectOrder.locatorToLocalObjectComparator);
        } else if (storage instanceof LongStorage) {
            return new LongStorageIndex<String, LocalAbstractObject>((LongStorage<LocalAbstractObject>)storage, LocalAbstractObjectOrder.locatorToLocalObjectComparator);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static LocalBucket createBucket() throws Exception {
        //return new MemoryStorageBucket(100, 80, 0, false);
        //return new MemoryStorageNoDupsBucket(100, 80, 0, false);
        //return new MemoryStorageNoDupsBucket(100, 80, 0, false);
        return new DiskBlockBucket(Long.MAX_VALUE, 80, 0, diskBucketFile, 24000, false, 0, createSerializator());
    }

    public static LocalAbstractObject createObject(String name) {
        LocalAbstractObject o = new ObjectIntVectorL1(5);
        o.setObjectKey(new AbstractObjectKey(name));
        return o;
    }

    public static BinarySerializator createSerializator() {
        return new CachingSerializator<LocalAbstractObject>(
                LocalAbstractObject.class, AbstractObjectKey.class, ObjectIntVectorL1.class,
                PrecomputedDistancesFixedArrayFilter.class
        );
//        return new CachingSerializator<LocalAbstractObject>(
//                LocalAbstractObject.class, AbstractObjectKey.class, MetaObjectSAPIRWeightedDist2.class,
//                ObjectIntVectorL1.class, ObjectShortVectorL1.class, ObjectColorLayout.class,
//                ObjectVectorEdgecomp.class, ObjectHomogeneousTexture.class, ObjectGPSCoordinate.class,
//                PrecomputedDistancesFixedArrayFilter.class
//        );
    }

    public static List<LocalAbstractObject> createObjects() {
        return createRandomObjects(objectsCount);
    }

    public static List<LocalAbstractObject> createBenchmarkObjects() {
        if (benchmarkObjectCount <= 0)
            return null;
        return createRandomObjects(benchmarkObjectCount);
    }

    private static List<LocalAbstractObject> createRandomObjects(int count) {
        List<LocalAbstractObject> list = new ArrayList<LocalAbstractObject>();
        for (int i = 0; i < count; i++)
            list.add(createObject(Integer.toString(i + 1)));
        return list;
    }

    private static List<LocalAbstractObject> createFileObjects(String file, Class<? extends LocalAbstractObject> clazz, int count) throws IOException {
        List<LocalAbstractObject> list = new ArrayList<LocalAbstractObject>();
        StreamGenericAbstractObjectIterator<LocalAbstractObject> it = new StreamGenericAbstractObjectIterator<LocalAbstractObject>(clazz, file);
        for (int i = 0; i < count && it.hasNext(); i++)
            list.add(it.next());
        it.close();

        return list;
    }

    public static ModifiableIndex<LocalAbstractObject> fillIndex(ModifiableIndex<LocalAbstractObject> index) throws IOException, BucketStorageException {
        // Create objects and fill the index
        for (int i = 1; i <= 9; i++) {
            LocalAbstractObject o = createObject(Integer.toString(i));
            index.add(o);
            // Insert duplicates (for object 3)
            if (i == 3) {
                index.add(o);
                index.add(o);
            }
        }
        return index;
    }

    public static ModifiableIndex<LocalAbstractObject> createFilledIndex() throws Exception {
        return fillIndex(createIndex());
    }

}
