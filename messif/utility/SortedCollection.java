/*
 * SortedCollection
 * 
 */

package messif.utility;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementation of a sorted collection.
 * The order is maintained using the comparator specified in the constructor.
 * Complexity of insertion is O(log n).
 * 
 * @param <T> the type of objects stored in this collection
 * @author xbatko
 */
public class SortedCollection<T> implements Collection<T>, Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Percentage of the capacity that is added when internal array is resized */
    private static final float SIZE_INCREASE_FACTOR = 0.3f;


    //****************** Attributes ******************//

    /** Comparator used to maintain order in this collection */
    private final Comparator comparator;

    /** Array buffer into which the elements of the SortedCollection are stored */
    private Object[] items;

    /** Size of the SortedCollection (the number of elements it contains) */
    private int size;

    /** Maximal capacity of the collection */
    private final int capacity;

    /** Modifications counter, used for detecting iterator concurrent modification */
    private transient int modCount;



    //****************** Constructor ******************//

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capatity of the collection
     * @param comparator the comparator that defines ordering
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public SortedCollection(int initialCapacity, int maximalCapacity, Comparator<? super T> comparator) throws IllegalArgumentException {
        if (initialCapacity < 1)
            throw new IllegalArgumentException("Illegal capacity: " + initialCapacity);
        if (maximalCapacity < initialCapacity)
            throw new IllegalArgumentException("Illegal maximal capacity: " + maximalCapacity);

        this.comparator = comparator;
        this.capacity = maximalCapacity;
        this.items = new Object[initialCapacity];
    }

    /**
     * Constructs an empty collection with the specified initial capacity.
     * The capacity of this collection is not limited.
     * @param initialCapacity the initial capacity of the collection
     * @param comparator the comparator that defines ordering
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public SortedCollection(int initialCapacity, Comparator<? super T> comparator) throws IllegalArgumentException {
        this(initialCapacity, Integer.MAX_VALUE, comparator);
    }

    /**
     * Constructs an empty collection.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * @param comparator the comparator that defines ordering
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public SortedCollection(Comparator<? super T> comparator) throws IllegalArgumentException {
        this(16, comparator);
    }

    /**
     * Constructs an empty collection.
     * The order is defined using the natural order of items.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public SortedCollection() throws IllegalArgumentException {
        this(null);
    }


    //****************** Data access methods ******************//

    /**
     * Returns the number of elements in this collection.
     * @return the number of elements in this collection
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this collection contains no elements.
     * @return <tt>true</tt> if this collection contains no elements
     */
    public boolean isEmpty() {
	return size == 0;
    }

    /**
     * Returns <tt>true</tt> if this collection contains the maximal number of elements.
     * @return <tt>true</tt> if this collection contains the maximal number of elements
     */
    public boolean isFull() {
	return size == capacity;
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified collection.
     *
     * @param  c collection to be checked for containment in this collection
     * @return <tt>true</tt> if this collection contains all of the elements
     *	       in the specified collection
     * @throws NullPointerException if the specified collection is null
     * @see    #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        for (Object item : c)
            if (indexOf(item) < 0)
                return false;
        return true;
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
    private int indexOf(Object o) {
	if (o == null) {
	    for (int i = 0; i < size; i++)
		if (items[i] == null)
		    return i;
	} else {
	    for (int i = 0; i < size; i++)
		if (o.equals(items[i]))
		    return i;
	}
	return -1;
    }

    /**
     * Returns the element at the specified position in this collection.
     * @param  index index of the element to return
     * @return the element at the specified position in this collection
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    @SuppressWarnings("unchecked")
    private final T get(int index) {
        return (T)items[index];
    }

    /**
     * Returns the last element of this collection according to the order
     * specified by the comparator.
     * @return the element at the specified position in this collection
     * @throws NoSuchElementException if the collection is empty
     */
    public T last() throws NoSuchElementException {
        if (isEmpty())
            throw new NoSuchElementException();
        return get(size - 1);
    }


    //****************** Copy methods ******************//

    /**
     * Returns a shallow copy of this <tt>SortedCollection</tt> instance.
     * The elements themselves are not copied and the comparator is shared.
     * @return a clone of this <tt>SortedCollection</tt> instance
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        SortedCollection ret = (SortedCollection)super.clone();
        ret.items = new Object[size];
        System.arraycopy(items, 0, ret.items, 0, size);
        ret.modCount = 0;
        return ret;
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list in
     *         proper sequence
     */
    public Object[] toArray() {
        Object[] ret = new Object[size];
        System.arraycopy(items, 0, ret, 0, size);
        return ret;
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); the runtime type of the returned
     * array is that of the specified array.  If the list fits in the
     * specified array, it is returned therein.  Otherwise, a new array is
     * allocated with the runtime type of the specified array and the size of
     * this list.
     *
     * <p>If the list fits in the specified array with room to spare
     * (i.e., the array has more elements than the list), the element in
     * the array immediately following the end of the collection is set to
     * <tt>null</tt>.  (This is useful in determining the length of the
     * list <i>only</i> if the caller knows that the list does not contain
     * any null elements.)
     *
     * @param <E> the type of array components
     * @param array the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    public <E> E[] toArray(E[] array) {
        if (array.length < size)
            array = Convert.createGenericArray(array, size);
	System.arraycopy(items, 0, array, 0, size);
        if (array.length > size)
            array[size] = null;
        return array;
    }


    //****************** Modification methods ******************//

    /**
     * Returns the index where the object <code>e</code> should be inserted.
     * The order defined by the comparator is used.
     * @param e the object to get the insertion point for
     * @return the index where the object <code>e</code> should be inserted
     */
    @SuppressWarnings("unchecked")
    private int insertionPoint(T e) {
	int low = 0;
	int high = size - 1;

	while (low <= high) {
	    int mid = (low + high) >>> 1;
	    int cmp = comparator.compare(e, items[mid]); // This IS checked, since this collection contains only T

	    if (cmp < 0) // Inserted object is smaller than the current middle
		high = mid - 1;
            else // Inserted object is greater than or equal to the current middle
		low = mid + 1;
	}
	return low;  // key not found.
    }

    /**
     * Returns the index where the object <code>e</code> should be inserted.
     * The order defined by the inserted object's {@link Comparable#compareTo} method.
     * @param e the object to get the insertion point for
     * @return the index where the object <code>e</code> should be inserted
     */
    @SuppressWarnings("unchecked")
    private int insertionPoint(Comparable e) {
	int low = 0;
	int high = size - 1;

	while (low <= high) {
	    int mid = (low + high) >>> 1;
	    int cmp = e.compareTo(items[mid]); // This IS checked, since this collection contains only T

	    if (cmp < 0) // Inserted object is smaller than the current middle
		high = mid - 1;
            else // Inserted object is greater than or equal to the current middle
		low = mid + 1;
	}
	return low;  // key not found.
    }

    /**
     * Adds the specified element to this list.
     * The element is added according the to order defined by the comparator.
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean add(T e) {
        modCount++;
        int i = (comparator == null)?insertionPoint((Comparable)e):insertionPoint(e);

        // If array is too small to hold new item
        if (size == items.length) {
            // If the capacity limit is not reached yet
            if (items.length < capacity) {
                // Compute new capacity
                int newSize = items.length + 1 + (int)(items.length * SIZE_INCREASE_FACTOR);
                if (newSize > capacity)
                    newSize = capacity;

                // Create new array
                Object[] newItems = new Object[newSize];
                System.arraycopy(items, 0, newItems, 0, i);

                // Add object
                if (i < size)
                    System.arraycopy(items, i, newItems, i + 1, size - i);
                newItems[i] = e;
                items = newItems;
                size++;
            } else { // No capacity left for storing object
                // If insertion point is after last object
                if (i >= size)
                    return false;

                // Insert in the middle (effectively destroying the last object)
                System.arraycopy(items, i, items, i + 1, size - i - 1);
                items[i] = e;
            }
        } else {
            // There is enough space
            if (i < size)
                System.arraycopy(items, i, items, i + 1, size - i);
            items[i] = e;
            size++;
        }

        return true;
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If the list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns <tt>true</tt> if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     */
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index < 0)
            return false;
        remove(index);
	return true;
    }

    /**
     * Removes an element at the specified position from this list.
     * @param index the position of the element to remove
     */
    private void remove(int index) {
        modCount++;
        if (index < size - 1)
            System.arraycopy(items, index + 1, items, index, size - index - 1);
        items[--size] = null; // Let gc do its work
    }


    //****************** Bulk modification methods ******************//

    /**
     * Add all of the elements in the specified collection to this list.
     * The elements are added according the to order defined by the comparator.
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(Collection<? extends T> c) {
	boolean ret = false;
        for (T item : c)
            if (add(item))
                ret = true;
        return ret;
    }

    /**
     * Merge sorts two item arrays into the destination array using the specified comparator.
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
    @SuppressWarnings("unchecked")
    private static void mergeSort(Object[] items1, int from1, int to1, Object[] items2, int from2, int to2, Comparator comparator, Object[] destination, int fromDest, int toDest) {
        // Merge sort data
        while (from1 < to1 && from2 < to2 && fromDest < toDest) {
            // Compare the first elements in items1 and items2
            int cmp = (comparator == null)?
                ((Comparable)items1[from1]).compareTo(items2[from2]):
                comparator.compare(items1[from1], items2[from2]);

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

    /**
     * Add all of the elements in the specified collection to this list.
     * The elements are added according the to order defined by the comparator.
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(SortedCollection<? extends T> c) {
        // If source array is empty, we are done
        if (c.isEmpty())
            return false;

	// Check if the comparators are equal
        if ((comparator != c.comparator) && (comparator == null || !comparator.equals(c.comparator)))
            return addAll((Collection<? extends T>)c); // Fallback to normal addAll method

        // More effective implementation is available, because these two collections maintain the same order

        // Prepare new array to hold items (always will have at least one element)
        Object[] newItems = new Object[Math.min(this.size + c.size, capacity)];

        // Merge sort arrays
        if (isEmpty())
            System.arraycopy(c.items, 0, newItems, 0, c.size);
        else
            mergeSort(items, 0, size, c.items, 0, c.size, comparator, newItems, 0, newItems.length);

        modCount++;
        this.items = newItems;
        this.size = newItems.length;

        return true;
    }

    /**
     * Removes all of this collection's elements that are also contained in the
     * specified collection. After this call returns, this collection will contain
     * no elements in common with the specified collection.
     *
     * @param c collection containing elements to be removed from this collection
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     * @throws NullPointerException if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object item : c)
            if (remove(item))
                ret = true;
        return ret;
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection. In other words, removes from this collection all
     * of its elements that are not contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this collection
     * @return <tt>true</tt> if this collection changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        Iterator<T> iterator = iterator();
        while (iterator.hasNext()) {
            if (!c.contains(iterator.next())) {
                ret = true;
                iterator.remove();
            }
        }

        return ret;
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     */
    public void clear() {
	modCount++;

	// Let gc do its work
	for (int i = 0; i < size; i++)
	    items[i] = null;

	size = 0;
    }


    //****************** String conversion ******************//

    /**
     * Returns a string representation of this collection. The string
     * representation consists of a list of the collection's elements in the
     * order defined by the comparator, enclosed in square brackets
     * (<tt>"[]"</tt>).  Adjacent elements are separated by the characters
     * <tt>", "</tt> (comma and space).  Elements are converted to strings as
     * by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this collection
     */
    @Override
    public String toString() {
        if (isEmpty())
            return "[]";

	StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(items[0]); // This is correct since collection is not empty
	for (int i = 1; i < size; i++)
            sb.append(", ").append(items[i]);
        return sb.append(']').toString();
    }


    //****************** Iterator ******************//

    /**
     * Returns an iterator over the elements in this collection. Their order
     * is defined by the comparator.
     *
     * @return an iterator over the elements in this collection
     */
    public Iterator<T> iterator() {
        return new Itr();
    }

    /** Internal class that implements iterator for this collection */
    private class Itr implements Iterator<T> {
	/** Index of an element to be returned by subsequent call to next */
	private int curIndex = 0;

	/**
	 * Index of element returned by most recent call to next or
	 * previous.  Reset to -1 if this element is deleted by a call
	 * to remove.
	 */
	private int lastRet = -1;

	/**
	 * The modCount value that the iterator believes that the backing
	 * List should have.  If this expectation is violated, the iterator
	 * has detected concurrent modification.
	 */
	private int expectedModCount = modCount;

	public boolean hasNext() {
            return curIndex < SortedCollection.this.size;
	}

	public T next() {
            checkForComodification();
	    try {
		T next = SortedCollection.this.get(curIndex);
		lastRet = curIndex++;
		return next;
	    } catch (IndexOutOfBoundsException e) {
		checkForComodification();
		throw new NoSuchElementException();
	    }
	}

	public void remove() {
	    if (lastRet == -1)
		throw new IllegalStateException();
            checkForComodification();

	    try {
		SortedCollection.this.remove(lastRet);
		if (lastRet < curIndex)
		    curIndex--;
		lastRet = -1;
		expectedModCount = modCount;
	    } catch (IndexOutOfBoundsException e) {
		throw new ConcurrentModificationException();
	    }
	}

	private final void checkForComodification() {
	    if (modCount != expectedModCount)
		throw new ConcurrentModificationException();
	}
    }

}
