/*
 * StreamGenericAbstractObjectIterator.java
 *
 * Created on 4. listopad 2005, 9:23
 *
 */

package messif.objects;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import messif.utility.Convert;

/**
 * This class represents an iterator on {@link LocalAbstractObject}s that are read from a file.
 * The objects are instantiated one by one every time the {@link #next next} method is called.
 * The file should be created using {@link LocalAbstractObject#write} method.
 *
 * @param <E> the class of objects provided by this stream iterator (must be descendant of {@link LocalAbstractObject})
 * @author xbatko
 */
public class StreamGenericAbstractObjectIterator<E extends LocalAbstractObject> extends GenericObjectIterator<E> {
    
    /** An input stream for reading objects of this iterator from */
    protected BufferedReader stream;
    /** Remembered name of opened file to provide reset capability */
    protected String fileName;
    /** Instance of a next object. This is needed for implementing reading objects from a stream */
    protected E nextObject;
    /** Instance of the current object */
    protected E currentObject;
    /** Class instance of objects of type E needed for instantiating objects read from a stream */
    protected Constructor<? extends E> constructor;
    /** Arguments for the constructor (first will always be the stream) */
    protected Object[] constructorArgs;
    /** Error encountered when accessing next object */
    protected String lastError = null;


    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from a given file on the fly as the contents are iterated.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param stream stream from which objects are read and instantiated
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, BufferedReader stream) throws IllegalArgumentException {
        this.stream = stream;
        this.fileName = null;
        this.constructorArgs = new Object[] { stream };

        try {
            this.constructor = objClass.getDeclaredConstructor(BufferedReader.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Object " + objClass + " lacks proper constructor: " + e.getMessage());
        }
        this.nextObject = nextStreamObject();   // hasNext is set automatically
    }

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from a given file on the fly as the contents are iterated.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @param fileName the path to a file from which objects are read
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, String fileName) throws IllegalArgumentException, IOException {
        this(objClass, new BufferedReader(new InputStreamReader(openFileInputStream(fileName))));

        // Set file name to provided value - it is used in reset functionality
        if (fileName == null || fileName.length() == 0 || fileName.equals("-"))
            this.fileName = null;
        else
            this.fileName = fileName;
    }

    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator from standard input.
     * The objects are loaded from the standard input on the fly as the contents are iterated.
     *
     * @param objClass the class used to create the instances of objects in this stream
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass) throws IllegalArgumentException, IOException {
        this(objClass, (String)null);
    }

    /**
     * Open input stream for the specified file name.
     * If the file name ends with "gz", GZIP stream decompression is used.
     * @param fileName the file path to open
     * @return a new instance of InputStream
     */
    private static InputStream openFileInputStream(String fileName) throws IOException {
        if (fileName == null || fileName.length() == 0 || fileName.equals("-"))
            return System.in;
        else if (fileName.endsWith("gz"))
            return new GZIPInputStream(new FileInputStream(fileName));
        else return new FileInputStream(fileName);
    }


    /****************** Adding parameters ******************/

    /**
     * Adds one parameter to this stream's object constructor.
     * After a successful call to this method, the constructor of the encapsulated
     * object class will be called with additional argument (same for all instances).
     * @param paramClass the class of added parameter that must correspond with constructor
     * @param paramValue the value to pass to the constructor
     * @throws IllegalArgumentException if there is no appropriate constructor in the object's class (the previous constructor remains untouched)
     */
    public void addContructorParameter(Class<?> paramClass, Object paramValue) throws IllegalArgumentException {
        if (!paramClass.isInstance(paramValue))
            throw new IllegalArgumentException("Supplied object is not compatible with supplied class");

        Class<?>[] argTypes = constructor.getParameterTypes();
        Class<?>[] newArgTypes = new Class<?>[argTypes.length + 1];
        System.arraycopy(argTypes, 0, newArgTypes, 0, argTypes.length);
        newArgTypes[argTypes.length] = paramClass;
        try {
            this.constructor = constructor.getDeclaringClass().getDeclaredConstructor(newArgTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Object " + constructor.getDeclaringClass() + " lacks proper constructor: " + e.getMessage());
        }
        Object[] args = constructorArgs;
        constructorArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, constructorArgs, 0, args.length);
        constructorArgs[args.length] = paramValue;
    }

    /**
     * Sets additional parameter of this stream's object constructor.
     * This method can be used to change object passed to {@link #addContructorParameter addContructorParameter}.
     * @param index the parameter index to change
     * @param paramValue the changed value to pass to the constructor
     * @throws IllegalArgumentException when the passed object is incompatible with the constructor's parameter
     * @throws IndexOutOfBoundsException if the index parameter is out of bounds (zero parameter cannot be changed)
     * @throws InstantiationException if the value passed is string that is not convertible to the constructor class
     */
    public void setConstructorParameter(int index, Object paramValue) throws IndexOutOfBoundsException, IllegalArgumentException, InstantiationException {
        Class<?>[] argTypes = constructor.getParameterTypes();
        if (index <= 0 || index >= argTypes.length)
            throw new IndexOutOfBoundsException("Invalid argument passed for " + constructor.toString());
        if (!argTypes[index].isInstance(paramValue)) {
            if (paramValue instanceof String)
                paramValue = Convert.stringToType((String)paramValue, argTypes[index]);
            else throw new IllegalArgumentException("Supplied object must be instance of " + argTypes[index].getName());
        }
        constructorArgs[index] = paramValue;
    }


    /****************** Iterator methods ******************/

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


    /****************** Support for reading from a stream *************/

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
        BufferedReader newStream = new BufferedReader(new InputStreamReader(openFileInputStream(fileName)));
        
        // Reset current stream
        stream.close();
        stream = newStream;
        constructorArgs[0] = stream;
        
        nextObject = nextStreamObject();
    }

}
