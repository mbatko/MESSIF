/*
 *  LocalBucketTest
 * 
 */

package messif.buckets;

import java.util.List;
import junit.framework.TestCase;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class LocalBucketBenchmarkTest extends TestCase {

    public LocalBucketBenchmarkTest(String testName) {
        super(testName);
    }

    public void testBenchmark() throws Throwable {
        List<LocalAbstractObject> objs = TestConstants.createBenchmarkObjects();
        if (objs == null)
            return;

        LocalBucket bucket = TestConstants.createBucket();
        try {
            // Add objects to the bucket
            long time = System.currentTimeMillis();
            for (LocalAbstractObject object : objs)
                bucket.addObject(object);
            time = System.currentTimeMillis() - time;
            System.out.println("Benchmark of adding " + objs.size() + " objects run in " + time + "ms");

            // Read all objects
            time = System.currentTimeMillis();
            AbstractObjectIterator<LocalAbstractObject> allObjects = bucket.getAllObjects();
            while (allObjects.hasNext()) {
                LocalAbstractObject obj = allObjects.next();
                obj.getLocatorURI(); // This is to actualy use the created object
            }
            time = System.currentTimeMillis() - time;
            System.out.println("Benchmark of reading: " + objs.size() + " objects run in " + time + "ms");

            // Delete all objects
            time = System.currentTimeMillis();
            bucket.deleteAllObjects();
            time = System.currentTimeMillis() - time;
            System.out.println("Benchmark of deleting: " + objs.size() + " objects run in " + time + "ms");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            bucket.destroy();
        }
    }

}
