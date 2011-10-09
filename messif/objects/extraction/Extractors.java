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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.AbstractObjectKey;
import messif.utility.ExtendedProperties;
import messif.utility.ExtendedPropertiesException;
import messif.utility.ExternalProcessInputStream;

/**
 * Collection of utility methods for {@link Extractor}s.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Extractors {

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
     * constructor that takes {@link java.io.BufferedReader} as argument.
     * Note that data are read either from a data source parameter {@code dataParameter}
     * or the data source stream (if {@code dataParameter} is <tt>null</tt>).
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param dataParameter parameter of the data source that contains the object data
     * @param locatorParameter parameter of the data source used to set the extracted object's locator;
     *          if <tt>null</tt>, the object uses the default locator set by the factory
     * @param additionalArguments additional arguments for the constructor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, final String dataParameter, final String locatorParameter, Object... additionalArguments) throws IllegalArgumentException {
        final LocalAbstractObject.TextStreamFactory<? extends T> factory = new LocalAbstractObject.TextStreamFactory<T>(objectClass, additionalArguments);

        return new Extractor<T>() {
            @Override
            public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                try {
                    T object;
                    if (dataParameter == null)
                        object = factory.create(dataSource.getBufferedReader());
                    else
                        object = factory.create(dataSource.getRequiredParameter(dataParameter, String.class));
                    if (locatorParameter != null)
                        object.setObjectKey(new AbstractObjectKey(dataSource.getRequiredParameter(locatorParameter).toString()));
                    return object;
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof IOException)
                        throw (IOException)e.getCause();
                    else
                        throw new ExtractorException("Cannot create instance using " + factory + ": " + e.getCause(), e.getCause());
                }
            }
            @Override
            public Class<? extends T> getExtractedClass() {
                return factory.getCreatedClass();
            }
        };
    }

    /**
     * Creates an extractor that creates objects from text InputStream using the
     * constructor that takes {@link java.io.BufferedReader} as argument.
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param locatorParameter parameter of the data source used to set the extracted object's locator;
     *          if <tt>null</tt>, the object uses the default locator set by the factory
     * @param additionalArguments additional arguments for the constructor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, final String locatorParameter, Object... additionalArguments) throws IllegalArgumentException {
        return createTextExtractor(objectClass, null, locatorParameter, additionalArguments);
    }

    /**
     * Creates an extractor that creates objects from text InputStream using the
     * constructor that takes {@link java.io.BufferedReader} as argument.
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param additionalArguments additional arguments for the constructor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, Object... additionalArguments) throws IllegalArgumentException {
        return createTextExtractor(objectClass, null, additionalArguments);
    }

    /**
     * Calls an external extractor command and returns its output.
     * If {@code fileAsArgument} is <tt>true</tt>, the {@code dataSource} must
     * represent a valid file, which is passed in place of %s parameter in the {@code command}.
     * Otherwise, the external extractor receives the data from the {@code dataSource}
     * on its standard input.
     * 
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @param dataSource the source of binary data for the extraction
     * @return the extracted data read from the standard output of the {@code command}
     * @throws IOException if there was a problem calling the external command or passing arguments to it
     */
    public static InputStream callExternalExtractor(String command, boolean fileAsArgument, ExtractorDataSource dataSource) throws IOException {
        Process extractorProcess;
        if (fileAsArgument) {
            Object dataFile = dataSource.getDataSource();
            if (!(dataFile instanceof File))
                throw new IOException("External extractor requires file");
            extractorProcess = Runtime.getRuntime().exec(String.format(command, ((File)dataFile).getAbsoluteFile()));
        } else {
            extractorProcess = Runtime.getRuntime().exec(command);
            OutputStream os = extractorProcess.getOutputStream();
            dataSource.pipe(os);
            os.close();
        }

        return new BufferedInputStream(new ExternalProcessInputStream(extractorProcess));
    }

    /**
     * Creates an extractor that creates objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input and return the text parsable by
     * the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, String command) throws IllegalArgumentException {
        return createExternalExtractor(objectClass, command, false);
    }

    /**
     * Creates an extractor that creates objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input and return the text parsable by
     * the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @param locatorParameter parameter of the data source used to set the extracted object's locator;
     *          if <tt>null</tt>, the object uses the default locator set by the factory
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, String command, String locatorParameter) throws IllegalArgumentException {
        return createExternalExtractor(objectClass, command, false, locatorParameter);
    }

    /**
     * Creates an extractor that creates objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input if {@code fileAsArgument} is <tt>true</tt>
     * or the data are read from file that is passed as "%s" argument to the external command if
     * {@code fileAsArgument} is <tt>false</tt>.
     * The extractor must return the text parsable by the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @param additionalArguments additional arguments for the constructor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, Object... additionalArguments) throws IllegalArgumentException {
        return createExternalExtractor(objectClass, command, false, (String)null);
    }

    /**
     * Creates an extractor that creates objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input if {@code fileAsArgument} is <tt>true</tt>
     * or the data are read from file that is passed as "%s" argument to the external command if
     * {@code fileAsArgument} is <tt>false</tt>.
     * The extractor must return the text parsable by the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @param locatorParameter parameter of the data source used to set the extracted object's locator;
     *          if <tt>null</tt>, the object uses the default locator set by the factory
     * @param additionalArguments additional arguments for the constructor
     * @return extractor for creating objects from binary data by external command
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, String locatorParameter, Object... additionalArguments) throws IllegalArgumentException {
        final Extractor<T> textExtractor = createTextExtractor(objectClass, locatorParameter, additionalArguments);
        return new Extractor<T>() {
            @Override
            public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                return textExtractor.extract(new ExtractorDataSource(
                        callExternalExtractor(command, fileAsArgument, dataSource),
                        dataSource.getParameterMap()
                ));
            }
            @Override
            public Class<? extends T> getExtractedClass() {
                return textExtractor.getExtractedClass();
            }
        };
    }

    /**
     * Creates an extractor that creates multiple objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input if {@code fileAsArgument} is <tt>true</tt>
     * or the data are read from file that is passed as "%s" argument to the external command if
     * {@code fileAsArgument} is <tt>false</tt>.
     * The extractor must return the text parsable by the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @param locatorParameter parameter of the data source used to set the extracted object's locator;
     *          if <tt>null</tt>, the object uses the default locator set by the factory
     * @param additionalArguments additional arguments for the constructor
     * @return extractor for creating multiple objects from single binary data by external command
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> MultiExtractor<T> createExternalMultiExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, String locatorParameter, Object... additionalArguments) throws IllegalArgumentException {
        final Extractor<T> textExtractor = createTextExtractor(objectClass, locatorParameter, additionalArguments);
        return new MultiExtractor<T>() {
            @Override
            public Iterator<T> extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                return new ExtractorIterator<T>(textExtractor, new ExtractorDataSource(
                        callExternalExtractor(command, fileAsArgument, dataSource),
                        dataSource.getParameterMap()
                ));
            }
            @Override
            public Class<? extends T> getExtractedClass() {
                return textExtractor.getExtractedClass();
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

    /**
     * Creates an instance of {@link Extractor} from property values.
     * A property with the given {@code key} must be a type of extractor to create.
     * <p>
     * If type is "external", additional property {@code <key>.command} is required
     * to contain the external command to execute. Optionally, {@code <key>.fileAsArgument},
     * {@code <key>.locatorParameter}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createExternalExtractor(java.lang.Class, java.lang.String, boolean, java.lang.String, java.lang.Object[]) createExternalExtractor}.
     * </p>
     * <p>
     * If type is "text", no additional parameters are required. Optionally,
     * {@code <key>.locatorParameter}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createTextExtractor(java.lang.Class, java.lang.String, java.lang.Object[]) createTextExtractor}.
     * </p>
     *
     * @param <T> the class of objects that will be created by the extractor
     * @param objectClass the class of objects that will be created by the extractor
     * @param properties the properties with values
     * @param key the key property name
     * @return a new instance of extractor
     * @throws ExtendedPropertiesException if there some of the required properties were missing or invalid
     * @throws IllegalArgumentException if the extractor cannot be created with the specified parameters
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExtractorFromProperties(Class<? extends T> objectClass, ExtendedProperties properties, String key) throws ExtendedPropertiesException, IllegalArgumentException {
        String extractorType = properties.getRequiredProperty(key).trim().toLowerCase();
        if (extractorType.equals("external")) {
            return createExternalExtractor(
                    objectClass,
                    properties.getRequiredProperty(key + ".command"),
                    properties.getBoolProperty(key + ".fileAsArgument", false),
                    properties.getProperty(key + ".locatorParameter"),
                    (Object[])properties.getMultiProperty(key + ".additionalArg")
            );
        } else if (extractorType.equals("text")) {
            return createTextExtractor(objectClass,
                    properties.getProperty(key + ".locatorParameter"),
                    (Object[])properties.getMultiProperty(key + ".additionalArg")
            );
        } else if (extractorType.equals("constructor")) {
            try {
                Class<? extends Extractor> extractorClass = properties.getClassProperty(key + ".constructorClass", true, Extractor.class);
                return cast(extractorClass.getConstructor(ExtendedProperties.class).newInstance(ExtendedProperties.restrictProperties(properties, key)), objectClass);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Error creating extractor " + objectClass + " by properties constructor: " + e, e);
            } catch (Exception e) {
                throw new ExtendedPropertiesException("Cannot create extractor " + objectClass + ": " + e, e);
            }
        } else if (extractorType.equals("method")) {
            try {
                Class<? extends Extractor> extractorClass = properties.getClassProperty(key + ".methodClass", true, Extractor.class);
                Method method = extractorClass.getMethod(properties.getRequiredProperty(key + ".methodName"), ExtendedProperties.class);
                return cast(method.invoke(null, ExtendedProperties.restrictProperties(properties, key)), objectClass);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Error creating extractor " + objectClass + " by properties constructor: " + e, e);
            } catch (Exception e) {
                throw new ExtendedPropertiesException("Cannot create extractor " + objectClass + ": " + e, e);
            }
        }
        throw new ExtendedPropertiesException("Unknown extractor type: " + extractorType);
    }

    /**
     * Creates an instance of {@link Extractor} from property values.
     * A property with the given {@code key} must be a type of extractor to create.
     * A property {@code <key>.class} must contain the name of the object class
     * (descendant of {@link LocalAbstractObject}) that will be created by the extractor.
     * <p>
     * If type is "external", additional property {@code <key>.command} is required
     * to contain the external command to execute. Optionally, {@code <key>.fileAsArgument},
     * {@code <key>.locatorParameter}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createExternalExtractor(java.lang.Class, java.lang.String, boolean, java.lang.String, java.lang.Object[]) createExternalExtractor}.
     * </p>
     * <p>
     * If type is "text", no additional parameters are required. Optionally,
     * {@code <key>.locatorParameter}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createTextExtractor(java.lang.Class, java.lang.String, java.lang.Object[]) createTextExtractor}.
     * </p>
     *
     * @param properties the properties with values
     * @param key the key property name
     * @return a new instance of extractor
     * @throws ExtendedPropertiesException if there some of the required properties were missing or invalid
     * @throws IllegalArgumentException if the extractor cannot be created with the specified parameters
     */
    public static Extractor<?> createExtractorFromProperties(ExtendedProperties properties, String key) throws ExtendedPropertiesException, IllegalArgumentException {
        return createExtractorFromProperties(properties.getClassProperty(key + ".class", true, LocalAbstractObject.class), properties, key);
    }

}
