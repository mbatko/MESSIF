/*
 *  LocalAbstractObjectOrder
 * 
 */

package messif.buckets.index;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Collection;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.keys.AbstractObjectKey;
import messif.operations.AnswerType;
import messif.operations.GetAllObjectsQueryOperation;
import messif.operations.GetObjectByLocatorOperation;
import messif.operations.GetObjectQueryOperation;
import messif.operations.GetObjectsByLocatorsOperation;
import messif.operations.QueryOperation;

/**
 * Default orders of {@link LocalAbstractObject} based on attributes.
 * Specifically, order can be defined on object IDs, locators, data or keys.
 * 
 * @author xbatko
 */
public enum LocalAbstractObjectOrder implements IndexComparator<LocalAbstractObject, LocalAbstractObject>, Serializable {
    /** Order defined by object IDs */
    UNIQUE_ID,
    /** Order defined by object locator URIs */
    LOCATOR,
    /** Order defined by object data hash codes */
    DATA,
    /** Order defined by object keys */
    KEY;

    public int indexCompare(LocalAbstractObject o1, LocalAbstractObject o2) {
        switch (this) {
            case UNIQUE_ID:
                return o1.compareTo(o2);
            case LOCATOR:
                return o1.getLocatorURI().compareTo(o2.getLocatorURI());
            case DATA:
                int cmp = o1.dataHashCode() - o2.dataHashCode();
                if (cmp == 0 && !o1.dataEquals(o2))
                    cmp = o1.compareTo(o2);     // Order object by their uniqueID
                return cmp;
            case KEY:
                return o1.getObjectKey().compareTo(o2.getObjectKey());
        }
        throw new InternalError("Compare method is not implemented for order " + this);
    }

    public int compare(LocalAbstractObject o1, LocalAbstractObject o2) {
        return indexCompare(o1, o2);
    }

    public LocalAbstractObject extractKey(LocalAbstractObject object) {
        return object;
    }

    /** Index order defined by object IDs */
    public static OperationIndexComparator<UniqueID> uniqueIDComparator = new OperationIndexComparator<UniqueID>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25102L;

        public int indexCompare(UniqueID o1, LocalAbstractObject o2) {
            return compare(o1, o2);
        }

        public int compare(UniqueID o1, UniqueID o2) {
            return o1.compareTo(o2);
        }

        public UniqueID extractKey(LocalAbstractObject object) {
            return object;
        }

        public QueryOperation<?> createIndexOperation(Collection<? extends UniqueID> ids) {
            if (ids.size() == 1)
                return new GetObjectQueryOperation(ids.iterator().next(), AnswerType.ORIGINAL_OBJECTS);
            else
                return new GetAllObjectsQueryOperation(AnswerType.ORIGINAL_OBJECTS);
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

    /** Index order defined by object locators */
    public static OperationIndexComparator<String> locatorToLocalObjectComparator = new OperationIndexComparator<String>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25103L;

        public int indexCompare(String o1, LocalAbstractObject o2) {
            return compare(o1, o2.getLocatorURI());
        }

        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }

        public String extractKey(LocalAbstractObject object) {
            return object.getLocatorURI();
        }

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

    /** Index order defined by object keys */
    public static IndexComparator<AbstractObjectKey, LocalAbstractObject> keyToLocalObjectComparator = new IndexComparator<AbstractObjectKey, LocalAbstractObject>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25104L;

        public int indexCompare(AbstractObjectKey o1, LocalAbstractObject o2) {
            return compare(o1, o2.getObjectKey());
        }

        public int compare(AbstractObjectKey o1, AbstractObjectKey o2) {
            return o1.compareTo(o2);
        }

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

    /** */
    public static IndexComparator<Comparable, Object> trivialObjectComparator = new IndexComparator<Comparable, Object>() {
        private static final long serialVersionUID = 25105L;

        @SuppressWarnings("unchecked")
        public int indexCompare(Comparable k, Object o) {
            return k.compareTo(o);
        }

        public Comparable extractKey(Object object) {
            return (Comparable)object;
        }

        @SuppressWarnings("unchecked")
        public int compare(Comparable o1, Comparable o2) {
            return o1.compareTo(o2);
        }

    };

    //****************** Search wrappers ******************//

    /**
     * Search the specified {@code index} for the object with given ID.
     * @param <T> type of objects stored in the index
     * @param index the index to search
     * @param objectID the object ID to search for
     * @return the object or <tt>null</tt> if it was not found in the index
     * @throws IllegalStateException if there was a problem reading objects from the index
     */
    public static <T extends LocalAbstractObject> T searchIndexByObjectID(Index<T> index, UniqueID objectID) throws IllegalStateException {
        Search<T> search = index.search(uniqueIDComparator, objectID);
        if (!search.next())
            return null;
        return search.getCurrentObject();
    }

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
