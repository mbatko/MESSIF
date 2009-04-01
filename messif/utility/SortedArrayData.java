/*
 * SortedArrayData
 * 
 */

package messif.utility;

import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Abstract implementation of a basic sorted array data.
 * The order is maintained by the {@link #compare} method.
 * Methods for binary search with O(log n) complexity
 * as well as some basic collection operations are provided.
 * 
 * @param <K> the type of keys this array is ordered by
 * @param <T> the type of objects stored in this array
 * @author xbatko
 */
public abstract class SortedArrayData<K, T> {

    //****************** Data access methods ******************//

    /**
     * Returns the number of elements in this collection.
     * @return the number of elements in this collection
     */
    public abstract int size();

    /**
     * Returns the element at the specified position in this collection.
     * @param index index of the element to return
     * @return the element at the specified position in this collection
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     * @throws IllegalStateException if the object at position <code>index</code> cannot be accessed
     */
    protected abstract T get(int index) throws IndexOutOfBoundsException, IllegalStateException;

    /**
     * Compares its two arguments for order. Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     *
     * @param key the key to compare
     * @param object the object to be compared
     * @return a negative integer, zero, or a positive integer as the
     * 	       first argument is less than, equal to, or greater than the
     *	       second.
     * @throws ClassCastException if the arguments' types prevent them from
     * 	       being compared by this comparator.
     */
    protected abstract int compare(K key, T object) throws ClassCastException;


    //****************** Search methods ******************//

    /**
     * Searches a range in this collection of objects for the specified value
     * using the binary search algorithm.
     * 
     * <p>
     * If the range contains multiple elements with the specified value,
     * there is no guarantee which one will be found.
     * </p>
     *
     * @param low the index of the first element (inclusive) to be searched
     * @param high the index of the last element (inclusive) to be searched
     * @param key the key value to be searched for
     * @param indicateFailure flag that controls the return value when the key is not found;
     *          if {@code indicateFailure == true}, a negative number {@code -(insertionPoint + 1)}
     *          is returned, otherwise, the index of the nearest bigger key is returned
     * @return index of the search key, if it is contained in this collection
     *	       within the specified range;
     *	       otherwise, the nearest (higher) index is returned or -1
     *         according to the value of <code>indicateFailure</code>
     * @throws IndexOutOfBoundsException if the specified boundaries are outside range
     * @throws IllegalStateException if there was an error accessing object via {@link #get}
     */
    protected int binarySearch(K key, int low, int high, boolean indicateFailure) throws IndexOutOfBoundsException, IllegalStateException {
	while (low <= high) {
	    int mid = (low + high) >>> 1;
            int cmp = compare(key, get(mid));

	    if (cmp > 0)
		low = mid + 1;
	    else if (cmp < 0)
		high = mid - 1;
	    else
		return mid; // key found
	}

        // Key not found
        if (indicateFailure)
            return -(low + 1);
        else
            return low;
    }

    /**
     * Searches a range in this collection for objects that are equal to the specified key.
     * If the comparator is not <tt>null</tt> it is used to check for equality.
     * Otherwise, the {@link java.lang.Object#euals} is used.
     * 
     * @param <C> type of keys for the comparator
     * @param comparator the comparator used to check for equality of objects (can be <tt>null</tt>)
     * @param low the index of the first element (inclusive) to be searched
     * @param high the index of the last element (inclusive) to be searched
     * @param key the key value to be searched for
     * @return index of the first object that is equal to key or -1
     * @throws IndexOutOfBoundsException if the specified boundaries are outside range
     * @throws IllegalStateException if there was an error accessing object via {@link #get}
     */
    protected <C> int fullSearch(IndexComparator<C, T> comparator, C key, int low, int high) throws IndexOutOfBoundsException, IllegalStateException {
        // Null key search
        if (key == null) {
            for (int i = low; i <= high; i++)
                if (get(i) == null)
                    return i;
            return -1;
        }

        // Equality search (comparator is null but key is not)
        if (comparator == null) {
            for (int i = low; i <= high; i++)
                if (key.equals(get(i)))
                    return i;
            return -1;
        }

        // Comparator search
        for (int i = low; i <= high; i++)
            if (comparator.compare(key, get(i)) == 0)
                return i;
        return -1;
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     *         this list, or -1 if this list does not contain the element
     */
    protected int indexOf(Object o) {
        if (o != null)
            try {
                @SuppressWarnings("unchecked")
                int pos = binarySearch((K)o, 0, size() - 1, true); // This cast is not checked, but will throw a class-cast exception
                return (pos < 0)?-1:pos;
            } catch (ClassCastException ignore) { // will fall back to full search
            }
        return fullSearch(null, o, 0, size() - 1);
    }

    /**
     * Returns the last element of this collection according to the order
     * specified by the comparator.
     * @return the element at the specified position in this collection
     * @throws NoSuchElementException if the collection is empty
     * @throws IllegalStateException if there was an error accessing object via {@link #get}
     */
    public T first() throws NoSuchElementException, IllegalStateException {
        try {
            return get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the last element of this collection according to the order
     * specified by the comparator.
     * @return the element at the specified position in this collection
     * @throws NoSuchElementException if the collection is empty
     * @throws IllegalStateException if there was an error accessing object via {@link #get}
     */
    public T last() throws NoSuchElementException, IllegalStateException {
        try {
            return get(size() - 1);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
    }


    //****************** Bulk modification methods ******************//

    /**
     * Merges two sorted arrays into the destination array using the specified comparator.
     * 
     * @param <T> type of array elements
     * @param items1 the first array to merge
     * @param from1 the starting index in the first array
     * @param to1 the last index (exclusive) in the first array
     * @param items2 the second array to merge
     * @param from2 the starting index in the second array
     * @param to2 the last index (exclusive) in the second array
     * @param comparator the comparator used to compare objects from the collections;
     *        its type check is supressed, the array items are expected to be compatible
     * @param destination the destination array
     * @param fromDest the starting index in the destination array
     * @param toDest the last index (exclusive) in the destination array
     */
    public static <T> void mergeSort(T[] items1, int from1, int to1, T[] items2, int from2, int to2, Comparator<T> comparator, T[] destination, int fromDest, int toDest) {
        // Merge sort data
        while (from1 < to1 && from2 < to2 && fromDest < toDest) {
            // Compare the first elements in items1 and items2
            int cmp = comparator.compare(items1[from1], items2[from2]);

            // If the current item of this collection is bigger than the current item of the added collection
            if (cmp > 0)
                destination[fromDest++] = items2[from2++];
            else
                destination[fromDest++] = items1[from1++];
        }

        // Copy the remaining data
        if (from2 >= to2)
            System.arraycopy(items1, from1, destination, fromDest, Math.min(toDest - fromDest, to1 - from1));
        else if (from1 >= to1)
            System.arraycopy(items2, from2, destination, fromDest, Math.min(toDest - fromDest, to2 - from2));
    }

}
