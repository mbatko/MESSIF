/*
 * AnswerType
 * 
 */

package messif.operations;

import messif.objects.AbstractObject;

/**
 * Enumeration of types a query operation can return.
 * 
 * @author xbatko
 * @see QueryOperation
 */
public enum AnswerType {
    //****************** Constants ******************//

    /** Answer contains the original objects as is */
    ORIGINAL_OBJECTS,
    /** Answer contains clones of the original objects */
    CLONNED_OBJECTS,
    /** Answer contains clones of the original objects with {@link messif.objects.AbstractObject#clearSurplusData() cleared surplus data} */
    CLEARED_OBJECTS,
    /** Answer contains only {@link messif.netbucket.RemoteAbstractObject remote objects} */
    REMOTE_OBJECTS;


    //****************** Methods for updating objects according to answer type ******************//

    /**
     * Updates a {@link AbstractObject} so that it conforms to this answer type.
     * That means, the object is clonned/cleared/changed to remote object.
     * @param object the object to update
     * @return an updated object
     * @throws CloneNotSupportedException if a clone was requested but the specified object cannot be clonned
     */
    public AbstractObject update(AbstractObject object) throws CloneNotSupportedException {
        switch (this) {
            case ORIGINAL_OBJECTS:
                return object;
            case CLONNED_OBJECTS:
                return object.clone();
            case CLEARED_OBJECTS:
                object = object.clone();
                object.clearSurplusData();
                return object;
            case REMOTE_OBJECTS:
            default:
                return object.getRemoteAbstractObject();
        }
    }

}
