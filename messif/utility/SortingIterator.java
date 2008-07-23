/*
 * SortingIterator
 * 
 */

package messif.utility;


import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator that retrieves the smallest object from a collection of iterators when
 * its {@link #next() next} method is called.
 * @param <T> type of objects provided by the encapsulated collection of iterators
 * @author xbatko
 */
public class SortingIterator<T> implements Iterator<T> {

    /** The collection of encapsulated iterators */
    private final Item[] iterators;

    /** The index of last iterator */
    private int iteratorsLastIndex;

    /** The comparator used to sort the provided objects */
    protected final Comparator<? super T> comparator;

    /**
     * Internal class that encapsulates iterator, holds the last element got from
     * {@link #next()} and makes it comparable using the encapsulating class's
     * comparator on the current items.
     */
    private class Item implements Iterator<T>, Comparable<Item> {
        protected T current;
        protected final Iterator<? extends T> iterator;

        public Item(Iterator<? extends T> iterator) throws NoSuchElementException {
            this.current = iterator.next();
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public T next() {
            return current = iterator.next();
        }

        public void remove() {
            iterator.remove();
        }

        public int compareTo(Item item) {
            int ret = comparator.compare(current, item.current);
            if (ret == 0)
                return hashCode() - item.hashCode();
            else
                return ret;
        }
    }

    /**
     * Creates a new array of Items, since classical new Item[size] DOES NOT WORK!
     * @param size the size of the array
     * @return a new Item[] array
     */
    @SuppressWarnings("unchecked")
    private final Item[] createItemArray(int size) {
        return (Item[])Array.newInstance(Item.class, size); // This cast IS A STUPID BUG
    }

    /**
     * Creates a new instance of SortingIterator.
     * @param iterators the collection of iterators to get the objects from
     * @param comparator the comparator used to sort the objects provided by iterators
     */
    public SortingIterator(Collection<? extends Iterator<? extends T>> iterators, final Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.iterators = createItemArray(iterators.size());
        this.iteratorsLastIndex = -1;
        for (Iterator<? extends T> iterator : iterators) {
            try {
                this.iterators[iteratorsLastIndex + 1] = new Item(iterator);
                iteratorsLastIndex++; // This must be done after the new Item is called, since it can throw exception
            } catch (NoSuchElementException ignore) {} // This means that the iterator is empty and thus ignored
        }
        // Sort the array, so that the iterator having the smallest number is last
        if (iteratorsLastIndex != -1)
            Arrays.sort(this.iterators, 0, iteratorsLastIndex + 1, Collections.reverseOrder());
    }

    /**
     * Insert-sort the <code>item</code> into iterators according to ordering.
     * @param item the inserted item
     */
    private void insertSort(Item item) {
	int low = 0;
	int high = iteratorsLastIndex;

	while (low <= high) {
	    int mid = (low + high) >>> 1;
	    int cmp = item.compareTo(iterators[mid]); // This is reverse sort add

	    if (cmp < 0) {
		low = mid + 1;
            } else if (cmp > 0) {
		high = mid - 1;
            } else {
                // key found, insert position is after
		low = mid + 1;
                break;
            }
	}

        // insertion positon is low
        iteratorsLastIndex++;
        System.arraycopy(iterators, low, iterators, low + 1, iteratorsLastIndex - low);
        iterators[low] = item;	
    }

    public boolean hasNext() {
        return iteratorsLastIndex != -1;
    }

    public T next() {
        // Pop the last iterator (which has the smallest current object)
        Item item = iterators[iteratorsLastIndex--];

        // Retrieve the current object from the iterator
        T ret = item.current;

        // If the iterator is not empty, get next object and insert-sort it back into iterators
        if (item.hasNext()) {
            item.next();
            insertSort(item);
        }

        return ret;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
