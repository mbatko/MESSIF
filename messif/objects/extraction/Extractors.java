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
package messif.objects.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import messif.objects.LocalAbstractObject;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.Instantiator;

/**
 * Collection of utility methods for {@link Extractor}s.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
     * Returns a type-safe cast of a given extractor instance.
     * @param <T> the class of objects the extractor creates
     * @param extractorInstance the instance to cast
     * @param extractedClass the class of objects the extractor creates
     * @return a cast extractor
     * @throws ClassCastException if the specified {@code extractorInstance} is not an {@link Extractor} or it extracts an incompatible class
     */
    public static <T extends LocalAbstractObject> Extractor<T> cast(Object extractorInstance, Class<? extends T> extractedClass) throws ClassCastException {
        if (extractorInstance == null)
            return null;

        @SuppressWarnings("unchecked")
        Extractor<T> extractor = (Extractor<T>)extractorInstance; // This cast IS checked on the next line
        if (!extractedClass.isAssignableFrom(extractor.getExtractedClass()))
            throw new ClassCastException("Extractor " + extractor + " does not provide " + extractedClass);
        return extractor;
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
     * @param additionalArguments additional arguments for the constructor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, Object... additionalArguments) throws IllegalArgumentException {
        final LocalAbstractObject.TextStreamFactory<? extends T> factory = new LocalAbstractObject.TextStreamFactory<T>(objectClass, additionalArguments);

        return new Extractor<T>() {
            public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                try {
                    return factory.create(dataSource.getBufferedReader());
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof IOException)
                        throw (IOException)e.getCause();
                    else
                        throw new ExtractorException("Cannot create instance using " + factory + ": " + e.getCause(), e.getCause());
                }
            }
            public Class<? extends T> getExtractedClass() {
                return factory.getCreatedClass();
            }
        };
    }

    /**
     * Creates an extractor that creates objects from binary data by external command.
     * The command is executed using the specified {@code cmdarray} and is expected to
     * receive the binary data on its standard input and return the text parsable by
     * the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(final Class<? extends T> objectClass, final String command) throws IllegalArgumentException {
        return createExternalExtractor(objectClass, command, false);
    }

    /**
     * Creates an extractor that creates objects from binary data by external command.
     * The command is executed using the specified {@code cmdarray} and is expected to
     * receive the binary data on its standard input if {@code fileAsArgument} is <tt>true</tt>
     * or the data are read from file that is passed as "%s" argument to the external command if
     * {@code fileAsArgument} is <tt>false</tt>.
     * The extractor must return the text parsable by the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(final Class<? extends T> objectClass, final String command, final boolean fileAsArgument) throws IllegalArgumentException {
        final Instantiator<? extends T> instantiator = new ConstructorInstantiator<T>(objectClass, BufferedReader.class);
        return new Extractor<T>() {
            public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                Process extractorProcess;
                if (fileAsArgument) {
                    Object dataFile = dataSource.getDataSource();
                    if (!(dataFile instanceof File))
                        throw new ExtractorException("External extractor requires file");
                    extractorProcess = Runtime.getRuntime().exec(String.format(command, ((File)dataFile).getAbsoluteFile()));
                } else {
                    extractorProcess = Runtime.getRuntime().exec(command);
                    OutputStream os = extractorProcess.getOutputStream();
                    dataSource.pipe(os);
                    os.close();
                }
                try {
                    return instantiator.instantiate(new BufferedReader(new InputStreamReader(extractorProcess.getInputStream())));
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof IOException)
                        throw (IOException)e.getCause();
                    else
                        throw new ExtractorException("Cannot create instance using " + instantiator + ": " + e.getCause(), e.getCause());
                }
            }
            public Class<? extends T> getExtractedClass() {
                return instantiator.getInstantiatorClass();
            }
        };
    }

    /**
     * Creates extractor for the provided class.
     * If the class is a descendant of {@link LocalAbstractObject}, a
     * {@link #createTextExtractor text extractor} is returned.
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
