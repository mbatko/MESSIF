/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.utility;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import junit.framework.TestCase;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class DirectoryInputStreamTest extends TestCase {

    public DirectoryInputStreamTest(String testName) {
        super(testName);
    }

    /**
     * Test of searchFiles method, of class DirectoryInputStream.
     */
    public void testSearchFiles() throws Exception {
        if (TestConstants.directoryForInputStreamTest == null || !TestConstants.directoryForInputStreamTest.exists())
            return;
        Collection<File> files = DirectoryInputStream.searchFiles(null, TestConstants.directoryForInputStreamTest, null, false);
        assertEquals("Shallow files found", 3, files.size());
        files = DirectoryInputStream.searchFiles(null, TestConstants.directoryForInputStreamTest, null, true);
        assertEquals("Deep files found", 7, files.size());
        for (File file : files) {
            assertNotNull("File is null", file);
            assertFalse("File is dir", file.isDirectory());
        }
    }

    /**
     * Test of read method, of class DirectoryInputStream.
     */
    public void testOpen() throws Exception {
        if (TestConstants.directoryForInputStreamTest == null || !TestConstants.directoryForInputStreamTest.exists())
            return;
        InputStream instance = DirectoryInputStream.open(TestConstants.directoryForInputStreamTest.getPath() + File.separatorChar + "*.txt");
        int result = instance.read();
        int bytes = 0;
        while (result >= 0) {
            bytes++;
            assertTrue("Correct data", result < 256);
            assertTrue("Correct data", result == 'a' || result == 'b' || result == 'c' || result == 'd' || result == '\r' || result == '\n');
            result = instance.read();
        }
        assertEquals("Last read", -1, result);
        assertEquals("Number of bytes", 7*10, bytes);
    }

    /**
     * Test of read method, of class DirectoryInputStream.
     */
    public void testRead_0args() throws Exception {
        if (TestConstants.directoryForInputStreamTest == null || !TestConstants.directoryForInputStreamTest.exists())
            return;
        DirectoryInputStream instance = new DirectoryInputStream(TestConstants.directoryForInputStreamTest, null, true);
        int result = instance.read();
        int bytes = 0;
        while (result >= 0) {
            bytes++;
            assertTrue("Correct data", result < 256);
            assertTrue("Correct data", result == 'a' || result == 'b' || result == 'c' || result == 'd' || result == '\r' || result == '\n');
            result = instance.read();
        }
        assertEquals("Last read", -1, result);
        assertEquals("Number of bytes", 7*10, bytes);
    }

}
