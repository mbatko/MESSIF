/*
 *  SortedCollectionTest
 * 
 */

package messif.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author xbatko
 */
public class SortedCollectionTest extends TestCase {

    private final Collection<String> data = Arrays.asList(new String[] { "ahoj", "nazdar", "cau1", "ahoj", "nazdar", "cau0" });
    private final List<String> sortedData;

    public SortedCollectionTest(String testName) {
        super(testName);
        sortedData = new ArrayList<String>(data);
        Collections.sort(sortedData);
    }            

    private String[] benchmarkData(int count) {
        String[] data = new String[count];
        for (int i = 0; i < count; i++)
            data[i] = Integer.toString(i);
        return data;
    }

    /**
     * Test of size method, of class SortedCollection.
     */
    public void testSize() {
        SortedCollection<String> instance = new SortedCollection<String>();
        assertEquals(0, instance.size());
        instance.addAll(data);
        assertEquals(data.size(), instance.size());
    }

    /**
     * Test of add method, of class SortedCollection.
     */
    public void testAdd() {
        SortedCollection<String> instance = new SortedCollection<String>();
        for (String s : data)
            assertTrue(instance.add(s));
        Iterator<String> i = sortedData.iterator();
        Iterator<String> j = instance.iterator();
        while (i.hasNext()) {
            assertTrue(j.hasNext());
            assertEquals(i.next(), j.next());
        }
        assertFalse(j.hasNext());
    }

    /**
     * Test of iterator method, of class SortedCollection.
     */
    public void testIterator() {
        SortedCollection<String> instance = new SortedCollection<String>();
        instance.addAll(data);
        Iterator<String> i = sortedData.iterator();
        Iterator<String> j = instance.iterator();
        while (i.hasNext()) {
            assertTrue(j.hasNext());
            assertEquals(i.next(), j.next());
        }
        assertFalse(j.hasNext());
    }

    /**
     * Test of listIterator method, of class SortedCollection.
     *
    public void testListIterator_0args() {
        SortedCollection<String> instance = new SortedCollection<String>();
        instance.addAll(data);
        ListIterator<String> i = sortedData.listIterator();
        ListIterator<String> j = instance.listIterator();
        while (i.hasNext()) {
            assertTrue(j.hasNext());
            assertEquals(i.next(), j.next());
        }
        assertFalse(j.hasNext());
        while (i.hasPrevious()) {
            assertTrue(j.hasPrevious());
            assertEquals(i.previous(), j.previous());
        }
        assertFalse(j.hasPrevious());
    }*/

    private long benchmarkAdd(SortedCollection<String> instance, String[] benchData) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < benchData.length; i++)
            instance.add(benchData[i]);
        return System.currentTimeMillis() - time;
    }

    private long benchmarkSearchBinary(SortedCollection<String> instance, String[] benchData) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < benchData.length; i++)
            instance.contains(benchData[i]);
        return System.currentTimeMillis() - time;
    }

    private long benchmarkSearchFull(SortedCollection<String> instance, String[] benchData) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < benchData.length; i++)
            for (String string : instance)
                if (benchData[i].equals(string))
                    break;
        return System.currentTimeMillis() - time;        
    }

    public void testBenchmark() {
        final int count = 100000;
        final String[] benchData = benchmarkData(count);
        final int repeats = 10;

        long totalTimeO = 0;
        //long totalTimeC = 0;
        for (int i = 0; i < repeats; i++) {
            totalTimeO += benchmarkAdd(new SortedCollection<String>(), benchData);
            //totalTimeC += benchmarkAdd(new SortedCollection<String>(new TestComparator()), benchData);
        }
        System.out.println(
                "Time for adding " + benchData.length + " objects (comparator/comparable): " +
                //(totalTimeC/repeats) + "ms/" +
                (totalTimeO/repeats) + "ms"
        );

        totalTimeO = 0;
        //totalTimeC = 0;
        SortedCollection<String> instanceO = new SortedCollection<String>();
        //SortedCollection<String> instanceC = new SortedCollection<String>(new TestComparator());
        benchmarkAdd(instanceO, benchData);
        //benchmarkAdd(instanceC, benchData);
        for (int i = 0; i < repeats; i++) {
            totalTimeO += benchmarkSearchBinary(instanceO, benchData);
            //totalTimeC += benchmarkSearchBinary(instanceC, benchData);
        }
        System.out.println(
                "Time for searching " + benchData.length + " objects (comparator/comparable): " +
                //(totalTimeC/repeats) + "ms/" +
                (totalTimeO/repeats) + "ms"
        );

    }
}
