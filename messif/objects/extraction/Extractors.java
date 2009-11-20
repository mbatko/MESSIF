/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import messif.objects.LocalAbstractObject;

/**
 * Collection of utility methods for {@link Extractor}s.
 *
 * @author xbatko
 */
public abstract class Extractors {

    /** Number of bytes that the {@link #readStreamData(java.io.InputStream, int) readStreamData} method allocates */
    private static final int readStreamDataAllocation = 4096;

    /**
     * Read data from input stream into a byte buffer.
     * If the {@code maxBytes} parameter is greater than zero, then no more than
     * {@code maxBytes} will be read from the input stream. Otherwise, the buffer
     * will contain all the data from the input stream until the end-of-stream.
     * <p>Note that the stream is not closed</p>.
     *
     * @param inputStream the stream from which to read the data
     * @param maxBytes maximal number of bytes to read from the stream (unlimited if less or equal to zero)
     * @return a buffer containing the data
     * @throws IOException if there was a problem reading from the input stream
     */
    public static byte[] readStreamData(InputStream inputStream, int maxBytes) throws IOException {
        // Create buffer (has always at least bufferSize bytes available)
        byte[] buffer = new byte[maxBytes > 0 ? maxBytes : readStreamDataAllocation];
        int offset = 0;
        int bytes;
        while ((bytes = inputStream.read(buffer, offset, buffer.length - offset)) > 0) {
            offset += bytes;
            // Check if the buffer is not full
            if (offset == buffer.length && maxBytes <= 0) {
                // Add some space
                byte[] copy = new byte[offset + readStreamDataAllocation];
                System.arraycopy(buffer, 0, copy, 0, offset);
                buffer = copy;
            }
        }

        // Shrink the array
        if (offset != buffer.length) {
            byte[] copy = new byte[offset];
            System.arraycopy(buffer, 0, copy, 0, offset);
            buffer = copy;
        }

        return buffer;
    }

    /**
     * Extracts object downloaded from the {@code url} using the given {@code extractor}.
     * @param <T> the type of object returned by the extractor
     * @param extractor the extractor to use on the data
     * @param url the URL from which to download the data
     * @param mimeTypeRegexp regular expression for the mimetype of the data on the given {@code url}
     * @return the object extracted from the data downloaded from the given {@code url}
     * @throws ExtractorException if there was an error reading or extracting the data
     */
    public static <T extends LocalAbstractObject> T extract(Extractor<? extends T> extractor, URL url, String mimeTypeRegexp) throws ExtractorException {
        try {
            return extractor.extract(new ExtractorDataSource(url, mimeTypeRegexp));
        } catch (IOException e) {
            throw new ExtractorException("Cannot load data from " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Extracts object from the {@code file} using the given {@code extractor}.
     * @param <T> the type of object returned by the extractor
     * @param extractor the extractor to use on the data
     * @param file the file from which to load the data
     * @return the object extracted from the file
     * @throws ExtractorException if there was an error reading or extracting the data
     */
    public static <T extends LocalAbstractObject> T extract(Extractor<? extends T> extractor, File file) throws ExtractorException {
        try {
            return extractor.extract(new ExtractorDataSource(file));
        } catch (IOException e) {
            throw new ExtractorException("Cannot load data from " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Creates an extractor that creates objects from text InputStream using the
     * constructor that takes {@link BufferedReader} as argument.
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass) throws IllegalArgumentException {
        // Prepare constructor
        final Constructor<? extends T> constructor;
        try {
            constructor = objectClass.getConstructor(BufferedReader.class);
            if (!Modifier.isPublic(constructor.getModifiers()))
                throw new IllegalArgumentException("Buffered reader constructor in " + objectClass + " is not public");
            if (Modifier.isAbstract(objectClass.getModifiers()))
                throw new IllegalArgumentException("Cannot create abstract " + objectClass);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no buffered reader constructor in " + objectClass, e);
        }

        // Create extractor
        return new Extractor<T>() {
            public T extract(ExtractorDataSource dataSource) throws ExtractorException {
                try {
                    return constructor.newInstance(dataSource.getBufferedReader());
                } catch (InvocationTargetException e) {
                    throw new ExtractorException("Cannot create instance of " + constructor.getDeclaringClass() + ": " + e.getCause(), e.getCause());
                } catch (InstantiationException e) {
                    throw new InternalError(); // This should never happen - the class is checked
                } catch (IllegalAccessException e) {
                    throw new InternalError(); // This should never happen - the constructor is checked
                }
            }
        };
    }

    /**
     * Creates extractor for the provided class.
     * If the class is a descendant of {@link LocalAbstractObject}, a
     * {@link #createTextExtractor(java.lang.Class) text extractor} is returned.
     * If the class implements {@link Extractor} interface, a new instance
     * of this class is returned (using nullary constructor).
     *
     * @param usingClass the class used to create the extractor
     * @return extractor for the {@code usingClass}
     */
    @SuppressWarnings("unchecked")
    public static Extractor<?> createExtractor(Class<?> usingClass) {
        if (LocalAbstractObject.class.isAssignableFrom(usingClass)) {
            return createTextExtractor((Class<? extends LocalAbstractObject>)usingClass);
        } else if (Extractor.class.isAssignableFrom(usingClass)) {
            try {
                return (Extractor<?>)usingClass.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Class " + usingClass.getName() + " is not valid parameter for creating extractor: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Class " + usingClass.getName() + " is not valid parameter for creating extractor");
        }
    }
}
