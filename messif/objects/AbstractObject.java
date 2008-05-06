/*
 * AbstractObject.java
 *
 * Created on 3. kveten 2003, 20:09
 */

package messif.objects;

import java.io.Serializable;
import java.util.UUID;
import messif.netbucket.RemoteAbstractObject;


/**
 * The abstract piece of data that the MESSI Framework works with.
 * This is the top-most class of the object hierarchy.
 *
 * @see LocalAbstractObject
 * @see messif.netbucket.RemoteAbstractObject
 *
 * @author  xbatko
 */
public abstract class AbstractObject extends UniqueID implements Serializable {
    
    /** Class version id for serialization. */
    private static final long serialVersionUID = 4L;
    
    
    /****************** Object ID ******************/

    /**
     * Returns the ID of this object
     * @return the ID of this object
     */
    public UniqueID getObjectID() {
        return new UniqueID(this);
    }


    /****************** Constructors ******************/

    /**
     * Creates a new instance of AbstractObject.
     * A new unique object ID is generated.
     */
    protected AbstractObject() {
    }

    /**
     * Creates a new instance of AbstractObject.
     * The object ID is copied from the source object.
     * @param source the object from which to copy the ID
     */
    protected AbstractObject(AbstractObject source) {
        super(source);
    }

    /****************** Locator URI ******************/
    
    /** The key of this object - it must contain the URI. */
    protected AbstractObjectKey objectKey = null;

    /** Returns the object key.
     * @return the object key
     */
    public AbstractObjectKey getObjectKey() {
        return objectKey;
    }

    /** Set the object key
     * @param objectKey the new object key
     */
    public void setObjectKey(AbstractObjectKey objectKey) {
        this.objectKey = objectKey;
    }        
    
    /**
     * Returns the locator URI of this object.
     * @return the locator URI of this object
     */
    public String getLocatorURI() {
        if (objectKey == null)
            return null;
        return objectKey.getLocatorURI();
    }

    
    /****************** Supplemental data cleaning ******************/
    
    /**
     * Clear non-messif data stored in this object.
     * This method is intended to be called whenever the object is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    public void clearSurplusData() {
    }
    

    /****************** Local object converter ******************/
    
    /**
     * Returns this abstract object as local object.
     * For a LocalAbstractObject it returns itself, for a RemoteAbstractObject
     * it first downloads the object and then returns it as local object.
     * @return this abstract object as local object
     */
    public abstract LocalAbstractObject getLocalAbstractObject();


    /****************** Remote object converter ******************/
    
    /**
     * Returns the RemoteAbstractObject that contains only the URI locator of this object.
     * For LocalAbstractObject creates a new object, for RemoteAbstractObject returns itself.
     * @return RemoteAbstractObject that contains only the URI locator of this object.
     */
    public abstract RemoteAbstractObject getRemoteAbstractObject();


    /****************** String representation ******************/

    /**
     * Returns a string representation of this abstract object.
     * Basically, this method returns the object type plus object locator
     * (or ID if locator is <tt>null</tt>) in brackets.
     * @return a string representation of this abstract object
     */
    public String toString() {
        StringBuffer rtv = new StringBuffer(getClass().getSimpleName()).append(" (");
        if (objectKey != null)
            rtv.append(objectKey.toString());
        else rtv.append(super.toString());
        rtv.append(")");
        return rtv.toString();
    }

}
