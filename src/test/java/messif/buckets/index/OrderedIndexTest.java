/*
 *  OrderedIndexTest
 * 
 */

package messif.buckets.index;

import java.io.IOException;
import junit.framework.TestCase;
import messif.buckets.BucketStorageException;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectIntVectorL1;
import messif.objects.keys.AbstractObjectKey;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class OrderedIndexTest extends TestCase {

    public OrderedIndexTest(String testName) throws BucketStorageException, IOException {
        super(testName);
    }

    private static LocalAbstractObject createObject(String name) {
        LocalAbstractObject o = new ObjectIntVectorL1(5);
        o.setObjectKey(new AbstractObjectKey(name));
        return o;
    }

    private static ModifiableOrderedIndex<String, LocalAbstractObject> createFilledIndex() throws Exception {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = TestConstants.createOrderedIndex();

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

    /**
     * Test of comparator method, of class OrderedIndex.
     */
    public void testComparator() throws Throwable {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = createFilledIndex();
        try {
            assertNotNull("Comparator", index.comparator());
        } finally {
            index.destroy();
        }
    }

    private void searchCheck(final Search<LocalAbstractObject> search, final String keyCheck, final int sizeCheck) throws CloneNotSupportedException {
        assertNotNull(search);

        // Clone search
        Search<LocalAbstractObject> searchBack = search.clone();
        int count = 0;

        // Test forward search
        String lastKey = keyCheck;
        while (search.next()) {
            count++;
            LocalAbstractObject o = search.getCurrentObject();
            assertNotNull("Search found null object", o);
            if (keyCheck != null)
                assertTrue("Expected " + o.getLocatorURI() + " to be bigger or equal to " + lastKey, lastKey.compareTo(o.getLocatorURI()) <= 0);
            lastKey = o.getLocatorURI();
        }

        // Test backward search
        lastKey = keyCheck;
        while (searchBack.previous()) {
            count++;
            LocalAbstractObject o = searchBack.getCurrentObject();
            assertNotNull("Search found null object", o);
            if (keyCheck != null)
                assertTrue("Expected " + o.getLocatorURI() + " to be smaller or equal to " + lastKey, lastKey.compareTo(o.getLocatorURI()) >= 0);
            lastKey = o.getLocatorURI();
        }

        // Check size
        if (sizeCheck >= 0)
            assertEquals("Returned object count", sizeCheck, count);
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch() throws Throwable {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = createFilledIndex();
        try {
            searchCheck(index.search(), "1", index.size());
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch_key_restrictEqual() throws Throwable {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = createFilledIndex();
        try {
            searchCheck(index.search("3", false), "3", index.size());
            searchCheck(index.search("100", false), "100", index.size());
            searchCheck(index.search("3", true), "3", 3);
            searchCheck(index.search("1", true), "1", 1);
            searchCheck(index.search("100", true), "100", 0);
        } finally {
            index.destroy();
        }

        // Empty index search
        index = TestConstants.createOrderedIndex();
        try {
            searchCheck(index.search("3", false), "3", 0);
            searchCheck(index.search("3", true), "3", 0);
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch_from_to() throws Throwable {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = createFilledIndex();
        try {
            searchCheck(index.search("3", "4"), "3", 4);
            searchCheck(index.search("3", "3"), "3", 3);
            searchCheck(index.search("5", "3"), "5", 0);
            searchCheck(index.search("1", "9"), "1", index.size());
        } finally {
            index.destroy();
        }

        // Empty index search
        index = TestConstants.createOrderedIndex();
        try {
            searchCheck(index.search("3", "4"), "3", 0);
            searchCheck(index.search("4", "3"), "3", 0);
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch_key_from_to() throws Throwable {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = createFilledIndex();
        try {
            searchCheck(index.search("3", "3", "4"), "3", 4);
            searchCheck(index.search("4", "3", "4"), "4", 4);
            searchCheck(index.search("1", "3", "4"), "3", 4);
            searchCheck(index.search("5", "1", "9"), "5", index.size());
        } finally {
            index.destroy();
        }

        // Empty index search
        index = TestConstants.createOrderedIndex();
        try {
            searchCheck(index.search("1", "3", "4"), "3", 0);
            searchCheck(index.search("1", "4", "3"), "3", 0);
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of skip method of class Search.
     */
    public void testSearchSkip() throws Throwable {
        ModifiableOrderedIndex<String, LocalAbstractObject> index = createFilledIndex();
        try {
            ModifiableSearch<LocalAbstractObject> search = index.search();
            assertTrue("Skip has not found an object", search.skip(index.size()));
            LocalAbstractObject currentObject = search.getCurrentObject();
            assertNotNull("Null object from search", currentObject);
            assertEquals("Last object", "9", currentObject.getLocatorURI());
            assertFalse("No next object", search.next());

            assertTrue("Skip has not found an object", search.skip(-index.size()));
            currentObject = search.getCurrentObject();
            assertNotNull("Null object from search", currentObject);
            assertEquals("First object", "1", currentObject.getLocatorURI());
            assertFalse("No previous object", search.previous());
        } finally {
            index.destroy();
        }
    }

}
