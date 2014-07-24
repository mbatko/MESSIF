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
package messif.objects.util;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.utility.DirectoryInputStream;

/**
 * This class represents an iterator on {@link LocalAbstractObject}s that are read from a file.
 * The objects are instantiated one by one every time the {@link #next next} method is called.
 * The file should be created using {@link LocalAbstractObject#write} method.
 *
 * @param <E> the class of objects provided by this stream iterator (must be descendant of {@link LocalAbstractObject})
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
    /** Factory for instantiating objects read from a stream */
    protected final LocalAbstractObject.TextStreamFactory<? extends E> factory;


    //****************** Constructors ******************//


    /**
     * Creates a new instance of StreamGenericAbstractObjectIterator.
     * The objects are loaded from the given stream on the fly as this iterator is iterated.
     * The constructor of <code>objClass</code> that accepts {@link BufferedReader}
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
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, BufferedReader stream, Map<String, Object> namedInstances, Object... constructorArgs) throws IllegalArgumentException, IllegalStateException {
        this.stream = stream;
        this.factory = new LocalAbstractObject.TextStreamFactory<E>(objClass, true, namedInstances, constructorArgs);

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
        this(objClass, stream, namedInstances, constructorArgs == null ? null : constructorArgs.toArray());
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
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, String fileName, Map<String, Object> namedInstances, Object... constructorArgs) throws IllegalArgumentException, IOException {
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
     * @param fileName the path to a file from which objects are read;
     *          if it is a directory, all files that match the glob pattern are loaded
     *          (see {@link DirectoryInputStream#open(java.lang.String) DirectoryInputStream} for more informations)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param constructorArgs additional constructor arguments
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamGenericAbstractObjectIterator(Class<? extends E> objClass, String fileName, Map<String, Object> namedInstances, Collection<?> constructorArgs) throws IllegalArgumentException, IOException {
        this(objClass, fileName, namedInstances, constructorArgs == null ? null : constructorArgs.toArray());
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
        this(objClass, fileName, null, (Object[])null);
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
        this(objClass, stream, null, (Object[])null);
    }


    //****************** Attribute access methods ******************//

    @Override
    public void setConstructorParameter(int index, Object paramValue) throws IndexOutOfBoundsException, IllegalArgumentException {
        factory.setConstructorParameter(index, paramValue);
    }

    @Override
    public void setConstructorParameterFromString(int index, String paramValue, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, InstantiationException {
        factory.setConstructorParameterFromString(index, paramValue, namedInstances);
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
        str.append(factory.getCreatedClass().getName());
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
    @Override
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
    @Override
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
    @Override
    public boolean hasNext() {
        return nextObject != null;
    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator.
     * This method is unsupported by the stream iterator.
     */
    @Override
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
    protected final E nextStreamObject() throws IllegalArgumentException, IllegalStateException {
        try {
            E ret = factory.create(stream);
            objectsRead++;
            return ret;
        } catch (InvocationTargetException e) {
            // End of file is normal exit
            if (e.getCause() instanceof EOFException)
                return null;

            // Exception while reading the object from the stream
            throw new IllegalStateException(
                    "Cannot read instance #" + (objectsRead + 1) + " of " +
                    factory.getCreatedClass().getName() + " from " +
                    ((fileName == null) ? "STDIN" : fileName) + ": " +
                    e.getCause(), e.getCause());
        }
    }

    /**
     * Close the associated stream.
     * The iteration is finished, hasNext() will return <tt>false</tt>.
     * However, getCurrentObject is still valid if there was previous call to next().
     * @throws IOException if there was an I/O error closing the file
     */
    @Override
    public void close() throws IOException {
        stream.close();
        nextObject = null;
    }

    /**
     * Reset the associated stream and restarts the iteration from beginning.
     * @throws IOException if there was an I/O error re-opening the file
     */
    @Override
    public void reset() throws IOException {
        // Check if file name was remembered
        if (fileName == null)
            throw new IOException("Cannot reset this stream, file name not provided");
        
        // Try to reopen the file (throws IOException if file was not found)
        BufferedReader newStream = new BufferedReader(new InputStreamReader(DirectoryInputStream.open(fileName)));
        
        // Reset current stream
        stream.close();
        stream = newStream;
        objectsRead = 0;

        nextObject = nextStreamObject();
    }

}
