/*
 *  StorageTest
 * 
 */

package messif.buckets.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import messif.buckets.TemporaryCloseable;
import messif.buckets.TemporaryCloseableThread;
import messif.objects.LocalAbstractObject;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class StorageTest extends TestCase {
    private static final File serfile = new File("disk-storage.ser");
    private static final File storfile = new File("disk-storage.ds");

    private final List<LocalAbstractObject> objects;

    public StorageTest(String testName) throws Exception {
        super(testName);
        objects = TestConstants.createObjects();
    }

    /**
     * Test of store method, of class Storage.
     */
    @SuppressWarnings("unchecked")
    public void testStore() throws Throwable {
        Storage<LocalAbstractObject> instance = TestConstants.createStorage();
        List<Address<LocalAbstractObject>> addrs = new ArrayList<Address<LocalAbstractObject>>();
        for (LocalAbstractObject obj : objects) {
            Address<LocalAbstractObject> addr = instance.store(obj);
            assertNotNull(addr);
            addrs.add(addr);
        }

        // Test read
        for (int i = 0; i < objects.size(); i++) {
            LocalAbstractObject origObj = objects.get(i);
            LocalAbstractObject storageObj = addrs.get(i).read();
            assertEquals("Inserted object has the same key", origObj.getObjectKey(), storageObj.getObjectKey());
            assertTrue("Inserted object has the same data", origObj.dataEquals(storageObj));
        }

        // Serialize both storage and addresses
        instance.finalize();
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(serfile));
        out.writeObject(instance);
        out.writeObject(addrs);
        out.close();

        // Restore storage and addresses
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(serfile));
        instance = (Storage)in.readObject();
        addrs = (List)in.readObject();
        in.close();
        serfile.delete();

        // Test read
        for (int i = 0; i < objects.size(); i++) {
            LocalAbstractObject origObj = objects.get(i);
            LocalAbstractObject storageObj = addrs.get(i).read();
            assertEquals("Inserted object has the same key", origObj.getObjectKey(), storageObj.getObjectKey());
            assertTrue("Inserted object has the same data", origObj.dataEquals(storageObj));
        }
        instance.destroy();
    }

    /**
     * Test of store method, of class Storage.
     */
    @SuppressWarnings("unchecked")
    public void testTemporaryClose() throws Throwable {
        Storage<LocalAbstractObject> instance = TestConstants.createStorage();
        if (!(instance instanceof TemporaryCloseable)) {
            System.out.println("Storage " + instance.getClass() + " is not temporarily closeable, skipping test");
            return;
        }
        
        TemporaryCloseableThread temporaryCloseableThread = new TemporaryCloseableThread(5);
        temporaryCloseableThread.add((TemporaryCloseable)instance);

        List<Address<LocalAbstractObject>> addrs = new ArrayList<Address<LocalAbstractObject>>();
        for (LocalAbstractObject obj : objects) {
            Address<LocalAbstractObject> addr = instance.store(obj);
            assertNotNull(addr);
            addrs.add(addr);
            Thread.sleep(1);
        }

        // Test read
        for (int i = 0; i < objects.size(); i++) {
            LocalAbstractObject origObj = objects.get(i);
            LocalAbstractObject storageObj = addrs.get(i).read();
            assertEquals("Inserted object has the same key", origObj.getObjectKey(), storageObj.getObjectKey());
            assertTrue("Inserted object has the same data", origObj.dataEquals(storageObj));
        }

        instance.destroy();
    }

}
