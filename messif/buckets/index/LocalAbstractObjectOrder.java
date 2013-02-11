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
package messif.buckets.index;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.AbstractObjectKey;
import messif.operations.AnswerType;
import messif.operations.QueryOperation;
import messif.operations.query.GetObjectByLocatorOperation;
import messif.operations.query.GetObjectsByLocatorPrefixOperation;
import messif.operations.query.GetObjectsByLocatorsOperation;

/**
 * Default orders of {@link LocalAbstractObject} based on attributes.
 * Specifically, order can be defined on object IDs, locators, data or keys.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public enum LocalAbstractObjectOrder implements IndexComparator<LocalAbstractObject, LocalAbstractObject>, Serializable {
    /** Order defined by object locator URIs */
    LOCATOR,
    /** Order defined by object data hash codes */
    DATA,
    /** Order defined by object keys */
    KEY;

    @Override
    public int indexCompare(LocalAbstractObject o1, LocalAbstractObject o2) {
        switch (this) {
            case LOCATOR:
                return o1.getLocatorURI().compareTo(o2.getLocatorURI());
            case DATA:
                int cmp = o1.dataHashCode() - o2.dataHashCode();
                if (cmp == 0 && !o1.dataEquals(o2))
                    cmp = o1.getObjectKey().compareTo(o2.getObjectKey()); // Order object by their keys
                return cmp;
            case KEY:
                return o1.getObjectKey().compareTo(o2.getObjectKey());
            default:
                throw new InternalError("Compare method is not implemented for order " + this);
        }
    }

    @Override
    public int compare(LocalAbstractObject o1, LocalAbstractObject o2) {
        return indexCompare(o1, o2);
    }

    @Override
    public LocalAbstractObject extractKey(LocalAbstractObject object) {
        return object;
    }

    /** Index order defined by object locators */
    public static final OperationIndexComparator<String> locatorToLocalObjectComparator = new OperationIndexComparator<String>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25103L;

        @Override
        public int indexCompare(String o1, LocalAbstractObject o2) {
            return compare(o1, o2.getLocatorURI());
        }

        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }

        @Override
        public String extractKey(LocalAbstractObject object) {
            return object.getLocatorURI();
        }

        @Override
        public QueryOperation<?> createIndexOperation(Collection<? extends String> locators) {
            if (locators.size() == 1)
                return new GetObjectByLocatorOperation(locators.iterator().next(), AnswerType.ORIGINAL_OBJECTS);
            else
                return new GetObjectsByLocatorsOperation(new HashSet<String>(locators), null, AnswerType.ORIGINAL_OBJECTS);
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == this.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    };

    /** Index order defined by object locator prefixes */
    public static final OperationIndexComparator<String> locatorPrefixToLocalObjectComparator = new OperationIndexComparator<String>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25104L;

        @Override
        public int indexCompare(String o1, LocalAbstractObject o2) {
            return compare(o1, o2.getLocatorURI());
        }

        @Override
        public int compare(String o1, String o2) {
            if (o2.startsWith(o1))
                return 0;
            return o1.compareTo(o2);
        }

        @Override
        public String extractKey(LocalAbstractObject object) {
            return object.getLocatorURI();
        }

        @Override
        public QueryOperation<?> createIndexOperation(Collection<? extends String> locators) {
            if (locators.size() != 1)
                throw new UnsupportedOperationException("Prefix locator operation works only on one locator");
            return new GetObjectsByLocatorPrefixOperation(locators.iterator().next());
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == this.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    };

    /** Index order defined by object keys */
    public static final IndexComparator<AbstractObjectKey, LocalAbstractObject> keyToLocalObjectComparator = new IndexComparator<AbstractObjectKey, LocalAbstractObject>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25104L;

        @Override
        public int indexCompare(AbstractObjectKey o1, LocalAbstractObject o2) {
            return compare(o1, o2.getObjectKey());
        }

        @Override
        public int compare(AbstractObjectKey o1, AbstractObjectKey o2) {
            return o1.compareTo(o2);
        }

        @Override
        public AbstractObjectKey extractKey(LocalAbstractObject object) {
            return object.getObjectKey();
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass() == this.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    };

    /**
     * Index order defined by the object itself via {@link Comparable} interface.
     * Note that the compare methods can throw {@link ClassCastException}s.
     */
    public static final IndexComparator<Comparable<?>, Object> trivialObjectComparator = new IndexComparator<Comparable<?>, Object>() {
        private static final long serialVersionUID = 25105L;

        @SuppressWarnings("unchecked")
        @Override
        public int indexCompare(Comparable k, Object o) {
            return k.compareTo(o);
        }

        @Override
        public Comparable<?> extractKey(Object object) {
            return (Comparable<?>)object;
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compare(Comparable o1, Comparable o2) {
            return o1.compareTo(o2);
        }

    };

    //****************** Search wrappers ******************//

    /**
     * Search the specified {@code index} for the object with given locator.
     * @param <T> type of objects stored in the index
     * @param index the index to search
     * @param locator the locator to search for
     * @return the object or <tt>null</tt> if it was not found in the index
     * @throws IllegalStateException if there was a problem reading objects from the index
     */
    public static <T extends LocalAbstractObject> T searchIndexByLocator(Index<T> index, String locator) throws IllegalStateException {
        Search<T> search = index.search(locatorToLocalObjectComparator, locator);
        if (!search.next())
            return null;
        return search.getCurrentObject();
    }

    /**
     * Search the specified {@code index} for the object with given key.
     * @param <T> type of objects stored in the index
     * @param index the index to search
     * @param key the key to search for
     * @return the object or <tt>null</tt> if it was not found in the index
     * @throws IllegalStateException if there was a problem reading objects from the index
     */
    public static <T extends LocalAbstractObject> T searchIndexByKey(Index<T> index, AbstractObjectKey key) throws IllegalStateException {
        Search<T> search = index.search(keyToLocalObjectComparator, key);
        if (!search.next())
            return null;
        return search.getCurrentObject();
    }
}
