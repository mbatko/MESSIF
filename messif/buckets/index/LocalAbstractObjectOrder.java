/*
 *  LocalAbstractObjectOrder
 * 
 */

package messif.buckets.index;

import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.keys.AbstractObjectKey;
import messif.operations.AnswerType;
import messif.operations.GetAllObjectsQueryOperation;
import messif.operations.GetObjectByLocatorOperation;
import messif.operations.GetObjectQueryOperation;
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
            default:
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

        public QueryOperation<?> createIndexOperation(UniqueID from, UniqueID to) {
            if (to == null || from.equals(to))
                return new GetObjectQueryOperation(from, AnswerType.ORIGINAL_OBJECTS);
            else
                return new GetAllObjectsQueryOperation(AnswerType.ORIGINAL_OBJECTS);
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

        public QueryOperation<?> createIndexOperation(String from, String to) {
            if (to == null || from.equals(to))
                return new GetObjectByLocatorOperation(from, AnswerType.ORIGINAL_OBJECTS);
            else
                return new GetAllObjectsQueryOperation(AnswerType.ORIGINAL_OBJECTS);
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
    };

}
