/*
 *  OrderedIndexTest
 * 
 */

package messif.buckets.index;

import java.io.IOException;
import junit.framework.TestCase;
import messif.buckets.BucketStorageException;
import messif.buckets.storage.impl.DiskStorage;
import messif.objects.LocalAbstractObject;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class ModifiableIndexTest extends TestCase {

    public ModifiableIndexTest(String testName) throws BucketStorageException, IOException {
        super(testName);
    }

    private void searchCheck(final Search<LocalAbstractObject> search, final String keyCheck, final int sizeCheck) throws CloneNotSupportedException {
        assertNotNull(search);

        // Test forward search
        int count = 0;
        String lastKey = keyCheck;
        while (search.next()) {
            count++;
            LocalAbstractObject o = search.getCurrentObject();
            assertNotNull("Search found null object", o);
            if (keyCheck != null)
                assertTrue("Expected " + o.getLocatorURI() + " to be bigger or equal to " + lastKey, lastKey.compareTo(o.getLocatorURI()) <= 0);
            lastKey = o.getLocatorURI();
        }
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch() throws Throwable {
        ModifiableIndex<LocalAbstractObject> index = TestConstants.createFilledIndex();
        try {
            searchCheck(index.search(), null, index.size());
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch_key() throws Throwable {
        ModifiableIndex<LocalAbstractObject> index = TestConstants.createFilledIndex();
        try {
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "1"), "1", 1);
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "3"), "3", 3);
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "100"), "100", 0);
        } finally {
            index.destroy();
        }

        // Empty index search
        index = TestConstants.createIndex();
        try {
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "3"), "3", 0);
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of search method, of class OrderedIndex.
     */
    public void testSearch_from_to() throws Throwable {
        ModifiableIndex<LocalAbstractObject> index = TestConstants.createFilledIndex();
        try {
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "1", "2"), "1", 2);
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "3", "4"), "3", 4);
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "3", "3"), "3", 3);
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "1", Integer.toString(index.size())), "1", index.size() + 2);
        } finally {
            index.destroy();
        }

        // Empty index search
        index = TestConstants.createIndex();
        try {
            searchCheck(index.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, "1", "3"), "3", 0);
        } finally {
            index.destroy();
        }
    }

    /**
     * Test of skip method of class Search.
     */
    public void testSearchForward() throws Throwable {
        ModifiableIndex<LocalAbstractObject> index = TestConstants.createFilledIndex();
        try {
            ModifiableSearch<LocalAbstractObject> search = index.search();
            assertTrue("Skip cannot go to last object", search.skip(index.size()));
            LocalAbstractObject currentObject = search.getCurrentObject();
            assertNotNull("Null object from search", currentObject);
            assertEquals("Last object", "9", currentObject.getLocatorURI());
            assertFalse("Skip cannot go after last object", search.skip(1));
            assertFalse("No next object", search.next());
        } finally {
            index.destroy();
        }
    }
    /**
     * Test of skip method of class Search.
     */
    public void testSearchBackward() throws Throwable {
        ModifiableIndex<LocalAbstractObject> index = TestConstants.createFilledIndex();
        try {
            if (! (index instanceof DiskStorage)) {// Backward search is not supported by disk storage
                ModifiableSearch<LocalAbstractObject> search = index.search();
                assertTrue("Skip cannot go to last object", search.skip(index.size()));
                assertTrue("Skip cannot go to first object", search.skip(-index.size() + 1));
                LocalAbstractObject currentObject = search.getCurrentObject();
                assertNotNull("Null object from search", currentObject);
                assertEquals("First object", "1", currentObject.getLocatorURI());
                assertFalse("Skip cannot go before first object", search.skip(-1));
                assertFalse("No previous object", search.previous());
            }
        } finally {
            index.destroy();
        }
    }
}
