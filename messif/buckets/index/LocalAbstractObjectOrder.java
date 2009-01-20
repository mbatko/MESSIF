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

    public int compare(LocalAbstractObject o1, LocalAbstractObject o2) {
        switch (this) {
            case UNIQUE_ID:
            default:
                return o1.compareTo(o2);
            case LOCATOR:
                return o1.getLocatorURI().compareTo(o2.getLocatorURI());
            case DATA:
                return o1.dataHashCode() - o2.dataHashCode();
            case KEY:
                return o1.getObjectKey().compareTo(o2.getObjectKey());
        }
    }

    public LocalAbstractObject extractKey(LocalAbstractObject object) {
        return object;
    }

    public static OperationIndexComparator<UniqueID> uniqueIDComparator = new OperationIndexComparator<UniqueID>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25102L;

        public int compare(UniqueID o1, LocalAbstractObject o2) {
            return o1.compareTo(o2);
        }

        public UniqueID extractKey(LocalAbstractObject object) {
            return object;
        }

        public GetObjectQueryOperation createIndexOperation(UniqueID key) {
            return new GetObjectQueryOperation(key, AnswerType.ORIGINAL_OBJECTS);
        }

    };

    public static OperationIndexComparator<String> locatorToLocalObjectComparator = new OperationIndexComparator<String>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25103L;

        public int compare(String o1, LocalAbstractObject o2) {
            return o1.compareTo(o2.getLocatorURI());
        }

        public String extractKey(LocalAbstractObject object) {
            return object.getLocatorURI();
        }

        public GetObjectByLocatorOperation createIndexOperation(String key) {
            return new GetObjectByLocatorOperation(key, AnswerType.ORIGINAL_OBJECTS);
        }

    };

    public static IndexComparator<AbstractObjectKey, LocalAbstractObject> keyToLocalObjectComparator = new IndexComparator<AbstractObjectKey, LocalAbstractObject>() {
        /** Class serial id for serialization. */
        private static final long serialVersionUID = 25104L;

        public int compare(AbstractObjectKey o1, LocalAbstractObject o2) {
            return o1.compareTo(o2.getObjectKey());
        }

        public AbstractObjectKey extractKey(LocalAbstractObject object) {
            return object.getObjectKey();
        }

    };

}
