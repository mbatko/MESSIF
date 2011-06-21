/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
     * {@link #next()} and makes it comparable using the comparator of the 
     * encapsulating class on the current items.
     */
    private class Item implements Iterator<T>, Comparable<Item> {
        /** Actual item that is retrieved from the iterator and being comparable */
        private T current;
        /** Iterator used to get additional items */
        private final Iterator<? extends T> iterator;

        /**
         * Creates a new instance of Item using the given iterator.
         * @param iterator the iterator wrapped by this item iterator
         * @throws NoSuchElementException if the iterator does not have any items
         */
        private Item(Iterator<? extends T> iterator) throws NoSuchElementException {
            this.current = iterator.next();
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return current = iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
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
    private Item[] createItemArray(int size) {
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

    @Override
    public boolean hasNext() {
        return iteratorsLastIndex != -1;
    }

    @Override
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

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
