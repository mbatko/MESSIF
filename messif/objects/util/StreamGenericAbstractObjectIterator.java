/*
 * StreamGenericAbstractObjectIterator.java
 *
 * Created on 4. listopad 2005, 9:23
 *
 */

package messif.objects.util;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.utility.Convert;
import messif.utility.DirectoryInputStream;
import messif.utility.reflection.Instantiators;

/**
 * This class represents an iterator on {@link LocalAbstractObject}s that are read from a file.
 * The objects are instantiated one by one every time the {@link #next next} method is called.
 * The file should be created using {@link LocalAbstractObject#write} method.
 *
 * @param <E> the class of objects provided by this stream iterator (must be descendant of {@link LocalAbstractObject})
 * @author xbatko
 */
public class StreamGenericAbstractObjectIterator<E extends LocalAbstractObject> extends AbstractStreamObjectIterator<E> {
    
    //****************** Attributes ******************//

    /** An input stream for reading objects of this iterator from */
    protected BufferedReader stream;
    /** Remembered name of opened file to provide reset capability */
    protected String fileName;
    /** Instance of a next object. This is needed for implementing reading objects from a stream */
    protected E nextObject;
    /** Instance of the current object */
    protected E currentObject;
    /** Number of objects read from the stream */
    protected int objectsRead;
    /** Class instance of objects of type E needed for instantiating objects read from a stream */
    protected final Constructor<? extends E> constructor;
    /** Arguments for the constructor (first will always be the stream) */
    protected final Object[] constructorArgs;


    //****************** Constructors ******************//


    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The constructor args must be compatible with the constructor and the first
     * argument must be a {@link BufferedReader}.
     *
     * @param constructor the constructor used to create instances of objects in this stream
     * @param constructorArgs constructor arguments
     */
    public StreamGenericAbstractObjectIterator(Constructor<? extends E> constructor, Object[] constructorArgs) {
        this.constructor = constructor;
        this.stream = (BufferedReader)constructorArgs[0];
        this.constructorArgs = constructorArgs;

        // Read first object from the stream (hasNext is set automatically)
        this.nextObject = nextStreamObject();
    }

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from the given stream on the fly as this iterator is iterated.
     * The constructor of <code>objClass</code> that acceps {@link BufferedReader}
     * as the first argument and all the arguments from the <code>constructorArgs</code>
     * is used to read objects from the stream.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param stream stream from which objects are read and instantiated
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param constructorArgs additional constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IllegalStateException if there was an error reading from the stream
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, BufferedReader stream, Map<String, Object> namedInstances, Collection<?> constructorArgs) throws IllegalArgumentException, IllegalStateException {
        this.stream = stream;

        // Read constructor arguments
        this.constructorArgs = convertArguments(constructorArgs, stream);

        // Get constructor for arguments
        try {
            this.constructor = Instantiators.getConstructor(objClass, true, namedInstances, this.constructorArgs);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Object " + objClass + " lacks proper constructor: " + e.getMessage());
        }

        // Read first object from the stream (hasNext is set automatically)
        this.nextObject = nextStreamObject();
    }

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from the given file on the fly as this iterator is iterated.
     * If the <code>fileName</code> is empty, <tt>null</tt> or dash, standard input is used.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param fileName the path to a file from which objects are read;
     *          if it is a directory, all files that match the glob pattern are loaded
     *          (see {@link DirectoryInputStream#open(java.lang.String) DirectoryInputStream} for more informations)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param constructorArgs additional constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, String fileName, Map<String, Object> namedInstances, Collection<?> constructorArgs) throws IllegalArgumentException, IOException {
        this(objClass, new BufferedReader(new InputStreamReader(DirectoryInputStream.open(fileName))), namedInstances, constructorArgs);

        // Set file name to provided value - it is used in reset functionality
        if (fileName == null || fileName.length() == 0 || fileName.equals("-"))
            this.fileName = null;
        else
            this.fileName = fileName;
    }

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from the given file on the fly as this iterator is iterated.
     * If the <code>fileName</code> is empty, <tt>null</tt> or dash, standard input is used.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param fileName the path to a file from which objects are read
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, String fileName) throws IllegalArgumentException, IOException {
        this(objClass, fileName, null, null);
    }

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from the given stream on the fly as this iterator is iterated.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param stream stream from which objects are read and instantiated
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, BufferedReader stream) throws IllegalArgumentException {
        this(objClass, stream, null, null);
    }


    //****************** Constructor helper method ******************//

    /**
     * Convert the constructor arguments from collection to array.
     * The {@code stream} is added as the first argument in any case.
     *
     * @param constructorArgs the additional arguments
     * @param stream the stream argument
     * @return the resulting argument array
     */
    private static Object[] convertArguments(Collection<?> constructorArgs, BufferedReader stream) {
        // Read constructor arguments
        Object[] ret = new Object[(constructorArgs == null)?1:(1+constructorArgs.size())];

        // First argument of the prototype is always BufferedReader
        ret[0] = stream;

        // Next arguments
        if (constructorArgs != null) {
            int i = 1;
            for (Object arg : constructorArgs)
                ret[i++] = arg;
        }

        return ret;
    }


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
    public void setConstructorParameter(int index, Object paramValue) throws IndexOutOfBoundsException, IllegalArgumentException, InstantiationException {
        if (index++ < 0 || index >= constructorArgs.length) // index is incremented because the first argument is always the stream
            throw new IndexOutOfBoundsException("Invalid index (" + index + ") for " + constructor.toString());
        Class<?>[] argTypes = constructor.getParameterTypes();
        if (!argTypes[index].isInstance(paramValue)) {
            if (paramValue instanceof String)
                paramValue = Convert.stringToType((String)paramValue, argTypes[index]);
            else
                throw new IllegalArgumentException("Supplied object must be instance of " + argTypes[index].getName());
        }
        constructorArgs[index] = paramValue;
    }

    /**
     * Returns the name of the file opened by this stream.
     * <tt>Null</tt> is returned if this stream was created from the standard input
     * or from a stream.
     * @return the name of the file opened by this stream
     */
    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Stream of '");
        str.append(constructor.getDeclaringClass().getName());
        str.append("' from ");
        if (fileName == null)
            str.append(stream);
        else
            str.append(fileName);
        return str.toString();
    }


    //****************** Iterator methods ******************//

    /**
     * Returns the next object instance from the stream.
     *
     * @return the next object instance from the stream
     * @throws NoSuchElementException if the end-of-file was reached
     * @throws IllegalArgumentException if there was an error creating a new instance of the object
     * @throws IllegalStateException if there was an error reading from the stream
     */
    public E next() throws NoSuchElementException, IllegalArgumentException, IllegalStateException {
        // No next object available
        if (nextObject == null)
            throw new NoSuchElementException("No more objects in the stream");

        // Reading object on the fly from a stream
        currentObject = nextObject;
        nextObject = nextStreamObject();
        return currentObject;
    }

    /**
     * Returns an instance of object returned by the last call to next().
     * @return an instance of object returned by the last call to next()
     * @throws NoSuchElementException if {@link #next} has not been called yet
     */
    public E getCurrentObject() throws NoSuchElementException {
        if (currentObject == null)
            throw new NoSuchElementException("Can't call getCurrentObject() before first call to next()");
        return currentObject;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return nextObject != null;
    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator.
     * This method is unsupported by the stream iterator.
     */
    public void remove() {
        throw new UnsupportedOperationException("This iterator doesn't support remove method - can't remove objects from file");
    }


    //****************** Support for reading from a stream *************//

    /**
     * Returns an instance of object which would be returned by next call to next().
     * @return Returns an instance of object of type E which would be returned by the next call to next(). 
     *         If there is no additional object, null is returned.
     * @throws IllegalArgumentException if there was an error creating a new instance of the object
     * @throws IllegalStateException if there was an error reading from the stream
     */
    protected E nextStreamObject() throws IllegalArgumentException, IllegalStateException {
        try {
            E ret = constructor.newInstance(constructorArgs);
            objectsRead++;
            return ret;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Constructor " + constructor + " is unaccessible (permission denied)");
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Constructor " + constructor + " is unaccessible (abstract class)");
        } catch (InvocationTargetException e) {
            // End of file is normal exit
            if (e.getCause() instanceof EOFException)
                return null;

            // Exception while reading the object from the stream
            throw new IllegalStateException(
                    "Cannot read instance #" + (objectsRead + 1) + " of " +
                    constructor.getDeclaringClass().getName() + " from " +
                    ((fileName == null) ? "STDIN" : fileName) + ": " +
                    e.getCause());
        }
    }

    /**
     * Close the associated stream.
     * The iteration is finished, hasNext() will return <tt>false</tt>.
     * However, getCurrentObject is still valid if there was previous call to next().
     * @throws IOException if there was an I/O error closing the file
     */
    public void close() throws IOException {
        stream.close();
        nextObject = null;
    }

    /**
     * Reset the associated stream and restarts the iteration from beginning.
     * @throws IOException if there was an I/O error re-opening the file
     */
    public void reset() throws IOException {
        // Check if file name was remembered
        if (fileName == null)
            throw new IOException("Cannot reset this stream, file name not provided");
        
        // Try to reopen the file (throws IOException if file was not found)
        BufferedReader newStream = new BufferedReader(new InputStreamReader(DirectoryInputStream.open(fileName)));
        
        // Reset current stream
        stream.close();
        stream = newStream;
        constructorArgs[0] = stream;
        objectsRead = 0;

        nextObject = nextStreamObject();
    }

}
