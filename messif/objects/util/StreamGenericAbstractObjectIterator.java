/*
 * StreamGenericAbstractObjectIterator.java
 *
 * Created on 4. listopad 2005, 9:23
 *
 */

package messif.objects.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.utility.Convert;
import messif.utility.DirectoryInputStream;

/**
 * This class represents an iterator on {@link LocalAbstractObject}s that are read from a file.
 * The objects are instantiated one by one every time the {@link #next next} method is called.
 * The file should be created using {@link LocalAbstractObject#write} method.
 *
 * @param <E> the class of objects provided by this stream iterator (must be descendant of {@link LocalAbstractObject})
 * @author xbatko
 */
public class StreamGenericAbstractObjectIterator<E extends LocalAbstractObject> extends AbstractObjectIterator<E> implements Closeable {
    
    //****************** Attributes ******************//

    /** An input stream for reading objects of this iterator from */
    protected BufferedReader stream;
    /** Remembered name of opened file to provide reset capability */
    protected String fileName;
    /** Instance of a next object. This is needed for implementing reading objects from a stream */
    protected E nextObject;
    /** Instance of the current object */
    protected E currentObject;
    /** Class instance of objects of type E needed for instantiating objects read from a stream */
    protected final Constructor<? extends E> constructor;
    /** Arguments for the constructor (first will always be the stream) */
    protected final Object[] constructorArgs;
    /** Error encountered when accessing next object */
    protected String lastError;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from the given stream on the fly as this iterator is iterated.
     * The constructor of <code>objClass</code> that acceps {@link BufferedReader}
     * as the first argument and all the arguments from the <code>constructorArgs</code>
     * is used to read objects from the stream.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param stream stream from which objects are read and instantiated
     * @param constructorArgs additional constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, BufferedReader stream, Collection<?> constructorArgs) throws IllegalArgumentException {
        this.stream = stream;

        // Read constructor arguments
        this.constructorArgs = new Object[(constructorArgs == null)?1:(1+constructorArgs.size())];

        // First argument of the prototype is always BufferedReader
        this.constructorArgs[0] = stream;

        // Next arguments
        if (constructorArgs != null) {
            int i = 1;
            for (Object arg : constructorArgs)
                this.constructorArgs[i++] = arg;
        }

        // Get constructor for arguments
        try {
            this.constructor = Convert.getConstructor(objClass, true, this.constructorArgs);
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
     * @param constructorArgs additional constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, String fileName, Collection<?> constructorArgs) throws IllegalArgumentException, IOException {
        this(objClass, new BufferedReader(new InputStreamReader(DirectoryInputStream.open(fileName))), constructorArgs);

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
        this(objClass, fileName, null);
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
        this(objClass, stream, null);
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
     * @throws NoSuchElementException if the end-of-file was reached or there was an error reading the stream's file
     */
    public E next() throws NoSuchElementException {
        // No next object available
        if (nextObject == null) {
            String msg = "No more objects in the stream";
            if (lastError != null)
                msg += ": " + lastError;
            throw new NoSuchElementException(msg);
        }

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
     * @throws IllegalArgumentException if there was an error reading from the stream
     */
    protected E nextStreamObject() throws IllegalArgumentException {
        try {
            return constructor.newInstance(constructorArgs);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Object " + constructor.getDeclaringClass() + " constructor is unaccesible (permission denied): " + e.getMessage());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Object " + constructor.getDeclaringClass() + " constructor instantiation failed: " + e.getMessage());
        } catch (InvocationTargetException e) {
            // The constructor threw an exception
            if (e.getCause() instanceof IOException) {
                lastError = e.getCause().getMessage();
                return null;
            } else
                throw new IllegalArgumentException("Object " + constructor.getDeclaringClass() + " constructor invocation ended up with an exception: " + e.getMessage());
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
        
        nextObject = nextStreamObject();
    }

}
