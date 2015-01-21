/*
 *  BinarySerializatorTest
 * 
 */

package messif.objects.nio;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import junit.framework.TestCase;
import messif.objects.LocalAbstractObject;
import test.TestConstants;

/**
 *
 * @author xbatko
 */
public class BinarySerializatorTest extends TestCase {
    
    private final List<LocalAbstractObject> fillObjects;
    private FileChannel file;
    private final int bufferSize = 16*1024;
    private final boolean directBuffer = true;

    public BinarySerializatorTest(String testName) throws IOException {
        super(testName);
        fillObjects = TestConstants.createObjects();
    }

    @Override
    protected void setUp() throws Exception {
        TestConstants.serializatorTestFile.delete();
        file = new RandomAccessFile(TestConstants.serializatorTestFile, "rw").getChannel();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        file.close();
        if (!TestConstants.serializatorTestFile.delete())
            System.out.println("Cannot delete serializator file: " + TestConstants.serializatorTestFile);
        super.tearDown();
    }

    private BinaryOutput createOutput(long position) throws IOException {
        return new FileChannelOutputStream(bufferSize, directBuffer, file, position, 200*1024*1024);
    }

    private BinaryInput createInput() throws IOException {
        //return new MappedFileChannelInputStream(file, 0, 200*1024*1024);
        return new FileChannelInputStream(bufferSize, directBuffer, file, 0, 200*1024*1024);
        //return new ChannelInputStream(file, bufferSize, directBuffer);
    }

    private void writeData(BinarySerializator serializator, long position, Collection<Long> addresses) throws IOException {
        BinaryOutput output = createOutput(position);

        for (LocalAbstractObject localAbstractObject : fillObjects) {
            int bytes = serializator.write(output, localAbstractObject);
            if (addresses != null) {
                addresses.add(position);
                position += bytes;
            }
        }

        output.flush();
    }

    public void testObjectWrite() throws IOException {
        writeData(TestConstants.createSerializator(), 0, null);
    }

    public void testObjectRead() throws IOException {
        BinarySerializator serializator = TestConstants.createSerializator();
        writeData(serializator, 0, null);

        // Read what was written
        BinaryInput input = createInput();
        for (LocalAbstractObject localAbstractObject : fillObjects) {
            LocalAbstractObject obj = serializator.readObject(input, LocalAbstractObject.class);
            assertTrue("Data-equal objects", localAbstractObject.dataEquals(obj));
        }

        // There should be no next object
        try {
            LocalAbstractObject obj = serializator.readObject(input, LocalAbstractObject.class);
            fail("Additional objects found");
        } catch (EOFException ignore) {
        }
    }

    public void testObjectRandomRead() throws IOException {
        BinarySerializator serializator = TestConstants.createSerializator();
        List<Long> addresses = new ArrayList<Long>();
        writeData(serializator, 0, addresses);

        // Read what was written
        BinaryInput input = createInput();
        if (!(input instanceof FileChannelInputStream)) // Random access can only be done using file channel
            return;

        for (int i = fillObjects.size() - 1; i >= 0; i--) {
            ((FileChannelInputStream)input).setPosition(addresses.get(i));
            LocalAbstractObject obj = serializator.readObject(input, LocalAbstractObject.class);
            assertTrue("Data-equal objects", fillObjects.get(i).dataEquals(obj));
        }
    }

}
