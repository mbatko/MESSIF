
package messif.objects.util;

import java.io.Closeable;
import java.io.IOException;
import messif.objects.LocalAbstractObject;

/**
 * This class represents an iterator on {@link LocalAbstractObject}s that are read from a file.
 * The objects are instantiated one by one every time the {@link #next next} method is called.
 * The file should be created using {@link LocalAbstractObject#write} method.
 *
 * @param <E> the class of objects provided by this stream iterator (must be descendant of {@link LocalAbstractObject})
 * @author xbatko
 */
public abstract class AbstractStreamObjectIterator<E extends LocalAbstractObject> extends AbstractObjectIterator<E> implements Closeable {
    

    //****************** Attribute access methods ******************//

    /**
     * Sets the value of this stream's object constructor argument.
     * This method can be used to change object passed to <code>constructorArgs</code>.
     * 
     * @param index the parameter index to change (zero-based)
     * @param paramValue the changed value to pass to the constructor
     * @throws IllegalArgumentException when the passed object is incompatible with the constructor's parameter
     * @throws IndexOutOfBoundsException if the index parameter is out of bounds (zero parameter cannot be changed)
     * @throws InstantiationException if the value passed is string that is not convertible to the constructor class
     */
    public abstract void setConstructorParameter(int index, Object paramValue) throws IndexOutOfBoundsException, IllegalArgumentException, InstantiationException;


    /**
     * Reset the associated stream and restarts the iteration from beginning.
     * @throws IOException if there was an I/O error re-opening the file
     */
    public abstract void reset() throws IOException;

}
