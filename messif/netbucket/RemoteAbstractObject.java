/*
 * RemoteObject.java
 *
 * Created on 4. kveten 2003, 22:19
 */

package messif.netbucket;

import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;

/**
 * Object of this class represents an AbstractObject only by its URI locator.
 * It does not contain any data.
 *
 * @see messif.objects.AbstractObject
 * @see messif.objects.LocalAbstractObject
 * @author  xbatko
 */
public class RemoteAbstractObject extends AbstractObject {

    /** Class version id for serialization. */
    private static final long serialVersionUID = 2L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of RemoteAbstractObject using the specified locator.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    public RemoteAbstractObject(String locatorURI) {
        super(locatorURI);
    }

    /**
     * Creates a new instance of RemoteAbstractObject from the specified LocalAbstractObject.
     * @param object the local object from which to create the new one
     */
    public RemoteAbstractObject(LocalAbstractObject object) {
        super(object); // Copy object ID and key
    }


    //****************** Remote object converter ******************//

    /**
     * Returns the RemoteAbstractObject that contains only the URI locator of this object.
     * Ror RemoteAbstractObject return itself.
     * @return this object.
     */
    public RemoteAbstractObject getRemoteAbstractObject() {
        return this;
    }


    //****************** Local object converter ******************//

    /**
     * Returns the actual object - this method is not implemented.
     * It should download the object and return it.
     *
     * @return the local object this remote object represents
     * @throws UnsupportedOperationException this exception is always thrown in this version
     */
    public LocalAbstractObject getLocalAbstractObject() {
        throw new UnsupportedOperationException("RemoteAbstractObject.getLocalAbstractObject not implemented");
    }

    /**
     * Returns the actual full-data object.
     * The object is downloaded from the remote bucket.
     *
     * @param bucket the remote bucket to get the object from
     * @return the local object this remote object represents
     */
    public LocalAbstractObject getLocalAbstractObject(RemoteBucket bucket) {
        return bucket.getObject(this);
    }
}
