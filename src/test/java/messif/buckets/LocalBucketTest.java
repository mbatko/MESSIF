/*
 *  LocalBucketTest
 * 
 */

package messif.buckets;

import java.util.List;
import junit.framework.TestCase;
import messif.buckets.impl.MemoryStorageNoDupsBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class LocalBucketTest extends TestCase {

    private final List<LocalAbstractObject> fillObjects;

    public LocalBucketTest(String testName) {
        super(testName);
        fillObjects = TestConstants.createObjects();
    }

    private LocalBucket createFilledBucket() throws Exception {
        LocalBucket bucket = TestConstants.createBucket();
        for (LocalAbstractObject object : fillObjects)
            bucket.addObject(object);
        return bucket;
    }

    public void testCreateBucket() throws Throwable {
        LocalBucket instance = TestConstants.createBucket();
        instance.destroy();
    }

    /**
     * Test of addObject method, of class LocalBucket.
     */
    public void testAddObject() throws Throwable {
        LocalBucket instance = TestConstants.createBucket();

        try {
            for (int i = 1; i <= fillObjects.size(); i++) {
                LocalAbstractObject object = fillObjects.get(i - 1);
                instance.addObject(object);
                assertEquals("Number of objects after insert", i, instance.getObjectCount());
                assertEquals("Inserted object has the same key", object.getObjectKey(), instance.getObject(object.getObjectKey()).getObjectKey());
                assertTrue("Inserted object has the same data", object.dataEquals(instance.getObject(object.getObjectKey())));
            }
        } finally {
            instance.destroy();
        }
    }

    /**
     * Test of addObject method when adding duplicate object, of class LocalBucket.
     */
    public void testAddObjectDuplicate() throws Throwable {
        LocalBucket instance = TestConstants.createBucket();

        try {
            if (instance instanceof MemoryStorageNoDupsBucket)
                for (int i = 1; i <= fillObjects.size(); i++) {
                    LocalAbstractObject object = fillObjects.get(i - 1);
                    instance.addObject(object);
                    try {
                        instance.addObject(object);
                        fail("Duplicate object should throw exception");
                    } catch (DuplicateObjectException e) {
                    }
                }
        } finally {
            instance.destroy();
        }
    }

    /**
     * Test of getAllObjects method, of class LocalBucket.
     */
    public void testGetAllObjects() throws Throwable {
        LocalBucket instance = createFilledBucket();
        try {
            AbstractObjectIterator<LocalAbstractObject> allObjects = instance.getAllObjects();
            assertTrue("At least one object returned", allObjects.hasNext());
            int count = 0;
            while (allObjects.hasNext()) {
                assertNotNull("Returned object is not null", allObjects.next());
                count++;
            }
            assertEquals("Number of objects in the bucket", fillObjects.size(), count);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            instance.destroy();
        }
    }

    /**
     * Test of getAllObjects method, of class LocalBucket.
     */
    public void testGetAllObjects_remove() throws Throwable {
        LocalBucket instance = createFilledBucket();
        try {
            AbstractObjectIterator<LocalAbstractObject> allObjects = instance.getAllObjects();
            while (allObjects.hasNext()) {
                allObjects.next();
                allObjects.remove();
            }
            assertEquals("Bucket is empty", 0, instance.getObjectCount());
            assertFalse("No objects returned from empty bucket", instance.getAllObjects().hasNext());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            instance.destroy();
        }
    }

    /**
     * Test of deleteAllObjects method, of class LocalBucket.
     */
    public void testDeleteAllObjects() throws Throwable {
        LocalBucket instance = createFilledBucket();
        try {
            instance.deleteAllObjects();
            assertEquals("Bucket is empty", 0, instance.getObjectCount());
            assertFalse("No objects returned from empty bucket", instance.getAllObjects().hasNext());
        } finally {
            instance.destroy();
        }
    }

    /**
     * Test of deleteObject method, of class LocalBucket.
     */
    public void testDeleteObject() throws Throwable {
        LocalBucket instance = createFilledBucket();
        try {
            LocalAbstractObject delObject = fillObjects.get(fillObjects.size() - 1);
            int i = 1;
            do {
                instance.deleteObject(delObject); // Remove first object
                assertEquals("Bucket has less objects", fillObjects.size() - i, instance.getObjectCount());
                AbstractObjectIterator<LocalAbstractObject> allObjects = instance.getAllObjects();
                while (allObjects.hasNext())
                    assertFalse("Object in the bucket is the deleted one", delObject.equals(allObjects.next()));

                delObject = fillObjects.get(i - 1);
                i++;
            } while (i < fillObjects.size());
        } finally {
            instance.destroy();
        }
    }
}
