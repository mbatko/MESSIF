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
package messif.objects;

import java.io.IOException;
import java.io.Serializable;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.utility.Clearable;


/**
 * The abstract piece of data that the MESSI Framework works with.
 * This is the top-most class of the object hierarchy.
 *
 * @see LocalAbstractObject
 * @see NoDataObject
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class AbstractObject extends UniqueID implements Serializable, Clearable, Cloneable {

    /** Class version id for serialization. */
    private static final long serialVersionUID = 4L;

    //****************** Attributes ******************//

    /** The key of this object - it must contain the URI. */
    private AbstractObjectKey objectKey;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of AbstractObject.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     */
    protected AbstractObject() {
        objectKey = null;
    }

    /**
     * Creates a new instance of AbstractObject.
     * A new unique object ID is generated and the 
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     */
    protected AbstractObject(AbstractObjectKey objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * Creates a new instance of AbstractObject.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    protected AbstractObject(String locatorURI) {
        this.objectKey = new AbstractObjectKey(locatorURI);
    }

    /**
     * Creates a new instance of AbstractObject.
     * The object ID and key are copied from the source object.
     * If the key is not {@link AbstractObjectKey}, the key is
     * replaced with an instance of {@link AbstractObjectKey} that
     * copies the locator URI.
     * @param source the object from which to copy the ID
     */
    protected AbstractObject(AbstractObject source) {
        super(source);
        if ((source.objectKey == null) || (AbstractObjectKey.class.equals(source.objectKey.getClass())))
            this.objectKey = source.objectKey;
        else
            this.objectKey = new AbstractObjectKey(source.objectKey.getLocatorURI());
    }


    //****************** Object ID ******************//

    /**
     * Returns the ID of this object
     * @return the ID of this object
     */
    public UniqueID getObjectID() {
        // It is necessary to create a new instance (this object should not be used directly)
        return new UniqueID(this);
    }


    //****************** Object key and locator URI ******************//

    /**
     * Returns the object key.
     * @return the object key
     */
    public AbstractObjectKey getObjectKey() {
        return objectKey;
    }
    
    /**
     * Returns the object key corresponding to the passed class.
     * @param <E> class of object key to return
     * @param objectKeyClass class of object key to return
     * @return object key if it is an instance cof the required class or <code>null</code>
     */
    public <E extends AbstractObjectKey> E getObjectKey(Class<? extends E> objectKeyClass) {
        if (objectKeyClass.isInstance(objectKey))
            return objectKeyClass.cast(objectKey);
        else
            return null;
    }

    /**
     * Set the object key
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

    /**
     * Returns the locator URI of the given object if it is instance of {@link AbstractObject}.
     * Otherwise, <tt>null</tt> is returned.
     * @param object the object the locator URI of which to get
     * @return the locator URI or <tt>null</tt>
     */
    public static String getObjectLocatorURI(Object object) {
        if (object instanceof AbstractObject)
            return ((AbstractObject)object).getLocatorURI();
        return null;
    }


    //****************** Supplemental data cleaning ******************//

    /**
     * Clear non-messif data stored in this object.
     * That is, the object key is changed to {@link AbstractObjectKey} if
     * not <tt>null</tt>. The transformation only preserves the locator URI, all
     * additional information is lost.
     */
    @Override
    public void clearSurplusData() {
        // If object key is some extended class, remap it to the basic AbstractObjectKey
        if (objectKey != null && !objectKey.getClass().equals(AbstractObjectKey.class))
            objectKey = new AbstractObjectKey(objectKey.getLocatorURI());
    }


    //****************** No-data object converter ******************//

    /**
     * Returns this object as no-data object.
     * Only the object key and ID is preserved, any internal or supplemental data
     * are not copied.
     * @return this object as {@link NoDataObject}
     */
    public NoDataObject getNoDataObject() {
        return new NoDataObject(this);
    }


    //****************** Clonning ******************//

    /**
     * Creates and returns a shallow copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     *
     * @return a clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public AbstractObject clone() throws CloneNotSupportedException {
        return (AbstractObject)super.clone();
    }

    /**
     * Creates and returns a copy of this object with changed locatorURI.
     * The precise meaning of "copy" may depend on the class of the object.
     *
     * @param objectKey new object key
     * @return a clone of this instance
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    public final AbstractObject clone(AbstractObjectKey objectKey) throws CloneNotSupportedException {
        AbstractObject rtv = clone();
        rtv.objectKey = objectKey;
        return rtv;
    }


    //****************** String representation ******************//

    /**
     * Returns a string representation of this abstract object.
     * Basically, this method returns the object type plus object locator
     * (or ID if locator is <tt>null</tt>) in brackets.
     * @return a string representation of this abstract object
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(getClass().getSimpleName()).append(" (");
        if (objectKey != null)
            rtv.append(objectKey.toString());
        else rtv.append(super.toString());
        rtv.append(")");
        return rtv.toString();
    }


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of AbstractObject loaded from binary input.
     * 
     * @param input the input to read the AbstractObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected AbstractObject(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        objectKey = serializator.readObject(input, AbstractObjectKey.class);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    protected int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, objectKey);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    protected int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(objectKey);
    }

}
