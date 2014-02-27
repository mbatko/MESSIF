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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.impl.MetaObjectParametricMap;
import messif.objects.keys.AbstractObjectKey;
import messif.utility.Convert;
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
    /** Suffix for the external extractor temp file. If <tt>null</tt>, no temp file is created */
    private static String externalExtractorTempFileSuffix;

    /**
     * Set the global external extractor temp file suffix.
     * If <tt>null</tt>, the external extractor temp file creation is disabled
     * @param suffix the new suffix
     */
    public static void setExternalExtractorTempFileSuffix(String suffix) {
        externalExtractorTempFileSuffix = suffix;
    }

    /**
     * Converts a plain {@link Extractor} to {@link MultiExtractor}.
     * The returned multi-extractor iterator will return the single object 
     * returned by the encapsulated extractor.
     * @param <T> the class of objects the extractor creates
     * @param extractor the plain extractor to wrap
     * @return converted multi-extractor instance
     */
    public static <T extends LocalAbstractObject> MultiExtractor<T> extractorToMultiExtractor(final Extractor<? extends T> extractor) {
        return new MultiExtractor<T>() {
            @Override
            public Iterator<T> extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                return Collections.singleton(extractor.extract(dataSource)).iterator();
            }
            @Override
            public Class<? extends T> getExtractedClass() {
                return extractor.getExtractedClass();
            }
            @Override
            public String toString() {
                return "MultiExtractor wrapping " + extractor.toString();
            }
        };
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
     * Returns a type-safe cast of a given multi-extractor instance.
     * @param <T> the class of objects the multi-extractor creates
     * @param object the instance to cast
     * @param extractedClass the class of objects the multi-extractor creates
     * @param allowPlainExtractor flag whether to require the object to be {@link MultiExtractor} (<tt>false</tt>), or
     *          also {@link Extractor} is allowed and converted via {@link #extractorToMultiExtractor}
     * @return a cast multi-extractor
     * @throws ClassCastException if the specified {@code multiExtractorInstance} is not an {@link MultiExtractor} or it extracts an incompatible class
     */
    @SuppressWarnings("unchecked")
    public static <T extends LocalAbstractObject> MultiExtractor<T> castToMultiExtractor(Object object, Class<? extends T> extractedClass, boolean allowPlainExtractor) throws ClassCastException {
        if (object == null)
            return null;

        MultiExtractor<T> extractor;
        if (allowPlainExtractor && object instanceof Extractor) {
            extractor = extractorToMultiExtractor((Extractor<T>)object); // This cast IS checked in the following
        } else {
            extractor = (MultiExtractor<T>)object; // This cast IS checked in the following
        }
        if (!extractedClass.isAssignableFrom(extractor.getExtractedClass()))
            throw new ClassCastException("MultiExtractor " + extractor + " does not provide " + extractedClass);
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
     * @param parameterMapArgument index of a additional parameter that will get the
     *          {@link ExtractorDataSource#getParameterMap() data source parameter map}
     * @param additionalArguments additional arguments for the constructor
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, final String dataParameter, final String locatorParameter, final int parameterMapArgument, Object[] additionalArguments) throws IllegalArgumentException {
        if (parameterMapArgument >= 0) {
            if (parameterMapArgument == 0 && (additionalArguments == null || additionalArguments.length == 0)) // If the parameter map argument is the only additional argument
                additionalArguments = new Object[] { new HashMap<String, Object>(0) };
            if (additionalArguments == null || parameterMapArgument >= additionalArguments.length)
                throw new IllegalArgumentException("Parameter map argument " + parameterMapArgument + " was specified, but not enough additional arguments were specified");
            if (!(additionalArguments[parameterMapArgument] instanceof Map))
                throw new IllegalArgumentException("Parameter map argument " + parameterMapArgument + " was specified, but additional arguments does not have Map in that argument");
        }

        final LocalAbstractObject.TextStreamFactory<? extends T> factory = new LocalAbstractObject.TextStreamFactory<T>(objectClass, additionalArguments);

        return new Extractor<T>() {
            @Override
            public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                try {
                    T object;
                    LocalAbstractObject.TextStreamFactory<? extends T> localFactory = factory;
                    if (parameterMapArgument >= 0) {
                        localFactory = new LocalAbstractObject.TextStreamFactory<T>(factory);
                        localFactory.setConstructorParameter(parameterMapArgument, dataSource.getParameterMap());
                    }

                    if (dataParameter == null)
                        object = localFactory.create(dataSource.getBufferedReader());
                    else
                        object = localFactory.create(dataSource.getRequiredParameter(dataParameter, String.class));
                    if (locatorParameter != null) {
                        String locator = dataSource.getRequiredParameter(locatorParameter).toString();
                        AbstractObjectKey key = object.getObjectKey();
                        try {
                            object.setObjectKey(key == null ? new AbstractObjectKey(locator) : key.clone(locator));
                        } catch (CloneNotSupportedException e) {
                            throw new ExtractorException("Cannot set object locator", e);
                        }
                    }
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
            @Override
            public String toString() {
                return "Extracts " + getExtractedClass() + " as text stream";
            }
        };
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
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, final String dataParameter, String locatorParameter, Object[] additionalArguments) throws IllegalArgumentException {
        return createTextExtractor(objectClass, dataParameter, locatorParameter, -1, additionalArguments);
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
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, final String locatorParameter, Object[] additionalArguments) throws IllegalArgumentException {
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
    public static <T extends LocalAbstractObject> Extractor<T> createTextExtractor(Class<? extends T> objectClass, Object[] additionalArguments) throws IllegalArgumentException {
        return createTextExtractor(objectClass, null, additionalArguments);
    }

    /** Regular expression for parsing question-mark enclosed variables */
    private static final Pattern externalCmdVariableSubstitution = Pattern.compile("\\?([^?]+?)([:!][^?]*)?\\?", Pattern.MULTILINE);

    /**
     * Calls an external extractor command and returns its output.
     * If {@code fileAsArgument} is <tt>true</tt>, the {@code dataSource} must
     * represent a valid file, which is passed in place of %s parameter in the {@code command}.
     * Otherwise, the external extractor receives the data from the {@code dataSource}
     * on its standard input. Note that variable substitution using question-mark enclosure
     * is applied using {@link ExtractorDataSource#getParameterMap() data source parameters}, i.e.
     * every ?variable-name:default-value? is replaced with the value of {@link ExtractorDataSource#getParameter}("variable-name").
     *
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @param dataSource the source of binary data for the extraction
     * @return the extracted data read from the standard output of the {@code command}
     * @throws IOException if there was a problem calling the external command or passing arguments to it
     */
    public static InputStream callExternalExtractor(String command, boolean fileAsArgument, ExtractorDataSource dataSource) throws IOException {
        Process extractorProcess;
        command = Convert.substituteVariables(command, externalCmdVariableSubstitution, 1, 2, dataSource.getParameterMap());
        if (fileAsArgument) {
            Object dataFile = dataSource.getDataSource();
            if (!(dataFile instanceof File)) {
                if (externalExtractorTempFileSuffix == null)
                    throw new IOException("External extractor requires file - use messif.objects.extraction.Extractors.setExternalExtractorTempFileSuffix to enable automatic temp file creation");
                // External extractor requires file, but the data sources is not a file - store the data into a temporary file
                final File tempFile = dataSource.pipeToTemporaryFile("externalExtractorTempFile_", externalExtractorTempFileSuffix, null);
                return new BufferedInputStream(new ExternalProcessInputStream(Runtime.getRuntime().exec(Convert.splitBySpaceWithQuotes(String.format(command, tempFile.getAbsoluteFile())))) {
                    @Override
                    protected void onExit(int processExitCode) {
                        tempFile.delete();
                    }
                });
            } else {
                extractorProcess = Runtime.getRuntime().exec(Convert.splitBySpaceWithQuotes(String.format(command, ((File)dataFile).getAbsoluteFile())));
            }
        } else {
            extractorProcess = Runtime.getRuntime().exec(Convert.splitBySpaceWithQuotes(command));
            OutputStream os = extractorProcess.getOutputStream();
            try {
                dataSource.pipe(os);
            } catch (IOException e) {
                StringBuilder error = new StringBuilder().append("Error '").append(e.getMessage()).append("' occurred when writing the data to external extractor: ");
                try {
                    Convert.readStringData(extractorProcess.getErrorStream(), error);
                } catch (IOException ignore) {
                    throw e; // Cannot read error message from process, throw the original error instead
                }
                throw new IOException(error.toString());
            } finally {
                os.close();
            }
        }

        return new BufferedInputStream(new ExternalProcessInputStream(extractorProcess));
    }

    /**
     * Creates an extractor encapsulated in application resources that creates objects from binary data.
     * The encapsulated extractor must be a binary that receives the binary data
     * on its standard input and return the text parsable by the constructor
     * of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param resourcePath the absolute path of the extractor resource
     * @return object created by the extractor
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     * @throws IOException if there was an error preparing the extractor from resources
     */
    public static <T extends LocalAbstractObject> ExtractorCloseable<T> createResourcesExtractor(Class<? extends T> objectClass, String resourcePath) throws IllegalArgumentException, IOException {
        final File extractorFile = Convert.resourceToTemporaryFile(resourcePath);
        extractorFile.setExecutable(true, true);
        final Extractor<T> extractor = createExternalExtractor(objectClass, '"' + extractorFile.getAbsolutePath() + "\" -");
        return new ExtractorCloseable<T>() {
            @Override
            public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                return extractor.extract(dataSource);
            }
            @Override
            public Class<? extends T> getExtractedClass() {
                return extractor.getExtractedClass();
            }
            @Override
            public void close() throws IOException {
                if (extractor instanceof Closeable)
                    ((Closeable)extractor).close();
                if (!extractorFile.delete())
                    Logger.getLogger(getClass().getName()).warning("Cannot delete resources extractor file: " + extractorFile.getAbsolutePath());
            }
        };
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
        return createExternalExtractor(objectClass, command, null);
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
        return createExternalExtractor(objectClass, command, false, locatorParameter, null);
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
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, Object[] additionalArguments) throws IllegalArgumentException {
        return createExternalExtractor(objectClass, command, fileAsArgument, null, additionalArguments);
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
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, String locatorParameter, Object[] additionalArguments) throws IllegalArgumentException {
        return createExternalExtractor(objectClass, command, fileAsArgument, locatorParameter, -1, additionalArguments);
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
     * @param parameterMapArgument index of a additional parameter that will get the
     *          {@link ExtractorDataSource#getParameterMap() data source parameter map}
     * @param additionalArguments additional arguments for the constructor
     * @return extractor for creating objects from binary data by external command
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> Extractor<T> createExternalExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, String locatorParameter, int parameterMapArgument, Object[] additionalArguments) throws IllegalArgumentException {
        final Extractor<T> textExtractor = createTextExtractor(objectClass, null, locatorParameter, parameterMapArgument, additionalArguments);
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
            @Override
            public String toString() {
                return textExtractor + " from " + command;
            }
        };
    }

    /**
     * Creates an extractor that creates multiple objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input if {@code fileAsArgument} is <tt>false</tt>
     * or the data are read from file that is passed as "%s" argument to the external command if
     * {@code fileAsArgument} is <tt>true</tt>.
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
    public static <T extends LocalAbstractObject> MultiExtractor<T> createExternalMultiExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, String locatorParameter, Object[] additionalArguments) throws IllegalArgumentException {
        return createExternalMultiExtractor(objectClass, command, fileAsArgument, locatorParameter, -1, additionalArguments);
    }

    /**
     * Creates an extractor that creates multiple objects from binary data by external command.
     * The command is executed using the specified {@code command} and is expected to
     * receive the binary data on its standard input if {@code fileAsArgument} is <tt>false</tt>
     * or the data are read from file that is passed as "%s" argument to the external command if
     * {@code fileAsArgument} is <tt>true</tt>.
     * The extractor must return the text parsable by the constructor of {@code objectClass} on its standard output.
     *
     * @param <T> the class of object that is created by the extractor
     * @param objectClass the class of object that is created by the extractor
     * @param command the external command (including all necessary arguments)
     * @param fileAsArgument if <tt>true</tt>, the "%s" argument of external command is replaced with the filename
     * @param locatorParameter parameter of the data source used to set the extracted object's locator;
     *          if <tt>null</tt>, the object uses the default locator set by the factory
     * @param parameterMapArgument index of a additional parameter that will get the
     *          {@link ExtractorDataSource#getParameterMap() data source parameter map}
     * @param additionalArguments additional arguments for the constructor
     * @return extractor for creating multiple objects from single binary data by external command
     * @throws IllegalArgumentException if the {@code objectClass} has no valid constructor
     */
    public static <T extends LocalAbstractObject> MultiExtractor<T> createExternalMultiExtractor(Class<? extends T> objectClass, final String command, final boolean fileAsArgument, String locatorParameter, int parameterMapArgument, Object[] additionalArguments) throws IllegalArgumentException {
        final Extractor<T> textExtractor = createTextExtractor(objectClass, null, locatorParameter, parameterMapArgument, additionalArguments);
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
            @Override
            public String toString() {
                return textExtractor + " (multiple) from " + command;
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
            return createTextExtractor((Class<? extends LocalAbstractObject>)usingClass, null);
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
     * Creates an instance of {@link Extractor} or {@link MultiExtractor} from property values.
     * A property with the given {@code key} must be a type of extractor to create.
     * <p>
     * If type is "external", additional property {@code <key>.command} is required
     * to contain the external command to execute. Optionally, {@code <key>.fileAsArgument},
     * {@code <key>.locatorParameter}, {@code <key>.parameterArg}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createExternalExtractor(java.lang.Class, java.lang.String, boolean, java.lang.String, int, java.lang.Object[]) createExternalExtractor}.
     * </p>
     * <p>
     * If type is "text", no additional parameters are required. Optionally,
     * {@code <key>.dataParameter}, <key>.locatorParameter}, {@code <key>.parameterArg},
     * and {@code <key>.additionalArg1...n} can be specified - see
     * {@link #createTextExtractor(java.lang.Class, java.lang.String, java.lang.String, int, java.lang.Object[]) createTextExtractor}.
     * </p>
     * <p>
     * If type is "constructor", additional property {@code <key>.constructorClass} is required.
     * The parameter specify a class with public constructor having a single {@link ExtendedProperties} argument.
     * </p>
     * <p>
     * If type is "method", additional properties {@code <key>.methodClass} and {@code <key>.methodName} are required.
     * The parameters specify the class and the static method name in that class with a single {@link ExtendedProperties} argument.
     * </p>
     *
     * @param multiExtractor flag whether to create a {@link MultiExtractor} or {@link Extractor}
     * @param objectClass the class of objects that will be created by the extractor
     * @param properties the properties with values
     * @param key the key property name
     * @return a new instance of extractor
     * @throws ExtendedPropertiesException if there some of the required properties were missing or invalid
     * @throws IllegalArgumentException if the extractor cannot be created with the specified parameters
     */
    private static Object createExtractorFromPropertiesImpl(boolean multiExtractor, Class<? extends LocalAbstractObject> objectClass, ExtendedProperties properties, String key) throws ExtendedPropertiesException, IllegalArgumentException {
        String extractorType = properties.getRequiredProperty(key).trim().toLowerCase();
        if (extractorType.equals("external")) {
            return multiExtractor ?
                    createExternalMultiExtractor(
                        objectClass,
                        properties.getRequiredProperty(key + ".command"),
                        properties.getBoolProperty(key + ".fileAsArgument", false),
                        properties.getProperty(key + ".locatorParameter"),
                        properties.getIntProperty(key + ".parameterArg", -1),
                        (Object[])properties.getMultiProperty(key + ".additionalArg")
                    ) : createExternalExtractor(
                        objectClass,
                        properties.getRequiredProperty(key + ".command"),
                        properties.getBoolProperty(key + ".fileAsArgument", false),
                        properties.getProperty(key + ".locatorParameter"),
                        properties.getIntProperty(key + ".parameterArg", -1),
                        (Object[])properties.getMultiProperty(key + ".additionalArg")
                    );
        } else if (extractorType.equals("text")) {
            return createTextExtractor(objectClass,
                    properties.getProperty(key + ".dataParameter"),
                    properties.getProperty(key + ".locatorParameter"),
                    properties.getIntProperty(key + ".parameterArg", -1),
                    (Object[])properties.getMultiProperty(key + ".additionalArg")
            );
        } else if (extractorType.equals("constructor")) {
            try {
                Class<?> extractorClass = properties.getClassProperty(key + ".constructorClass", true, multiExtractor ? MultiExtractor.class : Extractor.class);
                return extractorClass.getConstructor(ExtendedProperties.class).newInstance(ExtendedProperties.restrictProperties(properties, key + "."));
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Error creating extractor " + objectClass + " by properties constructor: " + e.getCause(), e.getCause());
            } catch (Exception e) {
                throw new ExtendedPropertiesException("Cannot create extractor " + objectClass + ": " + e, e);
            }
        } else if (extractorType.equals("method")) {
            try {
                Class<?> extractorClass = properties.getClassProperty(key + ".methodClass", true, multiExtractor ? MultiExtractor.class : Extractor.class);
                Method method = extractorClass.getMethod(properties.getRequiredProperty(key + ".methodName"), ExtendedProperties.class);
                return method.invoke(null, ExtendedProperties.restrictProperties(properties, key + "."));
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Error creating extractor " + objectClass + " by properties constructor: " + e.getCause(), e.getCause());
            } catch (Exception e) {
                throw new ExtendedPropertiesException("Cannot create extractor " + objectClass + ": " + e, e);
            }
        }
        throw new ExtendedPropertiesException("Unknown extractor type: " + extractorType);
    }

    /**
     * Creates an instance of {@link Extractor} from property values.
     * A property with the given {@code key} must be a type of extractor to create.
     * <p>
     * If type is "external", additional property {@code <key>.command} is required
     * to contain the external command to execute. Optionally, {@code <key>.fileAsArgument},
     * {@code <key>.locatorParameter}, {@code <key>.parameterArg}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createExternalExtractor(java.lang.Class, java.lang.String, boolean, java.lang.String, int, java.lang.Object[]) createExternalExtractor}.
     * </p>
     * <p>
     * If type is "text", no additional parameters are required. Optionally,
     * {@code <key>.dataParameter}, <key>.locatorParameter}, {@code <key>.parameterArg},
     * and {@code <key>.additionalArg1...n} can be specified - see
     * {@link #createTextExtractor(java.lang.Class, java.lang.String, java.lang.String, int, java.lang.Object[]) createTextExtractor}.
     * </p>
     * <p>
     * If type is "constructor", additional property {@code <key>.constructorClass} is required.
     * The parameter specify a class with public constructor having a single {@link ExtendedProperties} argument.
     * </p>
     * <p>
     * If type is "method", additional properties {@code <key>.methodClass} and {@code <key>.methodName} are required.
     * The parameters specify the class and the static method name in that class with a single {@link ExtendedProperties} argument.
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
        return cast(createExtractorFromPropertiesImpl(false, objectClass, properties, key), objectClass);
    }

    /**
     * Creates an instance of {@link MultiExtractor} from property values.
     * A property with the given {@code key} must be a type of extractor to create.
     * <p>
     * If type is "external", additional property {@code <key>.command} is required
     * to contain the external command to execute. Optionally, {@code <key>.fileAsArgument},
     * {@code <key>.locatorParameter}, {@code <key>.parameterArg}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createExternalExtractor(java.lang.Class, java.lang.String, boolean, java.lang.String, int, java.lang.Object[]) createExternalExtractor}.
     * </p>
     * <p>
     * If type is "text", no additional parameters are required. Optionally,
     * {@code <key>.dataParameter}, <key>.locatorParameter}, {@code <key>.parameterArg},
     * and {@code <key>.additionalArg1...n} can be specified - see
     * {@link #createTextExtractor(java.lang.Class, java.lang.String, java.lang.String, int, java.lang.Object[]) createTextExtractor}.
     * </p>
     * <p>
     * If type is "constructor", additional property {@code <key>.constructorClass} is required.
     * The parameter specify a class with public constructor having a single {@link ExtendedProperties} argument.
     * </p>
     * <p>
     * If type is "method", additional properties {@code <key>.methodClass} and {@code <key>.methodName} are required.
     * The parameters specify the class and the static method name in that class with a single {@link ExtendedProperties} argument.
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
    public static <T extends LocalAbstractObject> MultiExtractor<T> createMultiExtractorFromProperties(Class<? extends T> objectClass, ExtendedProperties properties, String key) throws ExtendedPropertiesException, IllegalArgumentException {
        return castToMultiExtractor(createExtractorFromPropertiesImpl(true, objectClass, properties, key), objectClass, false);
    }

    /**
     * Creates an instance of {@link Extractor} from property values.
     * A property with the given {@code key} must be a type of extractor to create.
     * A property {@code <key>.class} must contain the name of the object class
     * (descendant of {@link LocalAbstractObject}) that will be created by the extractor.
     * <p>
     * If type is "external", additional property {@code <key>.command} is required
     * to contain the external command to execute. Optionally, {@code <key>.fileAsArgument},
     * {@code <key>.locatorParameter}, {@code <key>.parameterArg}, and {@code <key>.additionalArg1...n} can be specified
     * - see {@link #createExternalExtractor(java.lang.Class, java.lang.String, boolean, java.lang.String, int, java.lang.Object[]) createExternalExtractor}.
     * </p>
     * <p>
     * If type is "text", no additional parameters are required. Optionally,
     * {@code <key>.dataParameter}, <key>.locatorParameter}, {@code <key>.parameterArg},
     * and {@code <key>.additionalArg1...n} can be specified - see 
     * {@link #createTextExtractor(java.lang.Class, java.lang.String, java.lang.String, int, java.lang.Object[]) createTextExtractor}.
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

    /**
     * Creates extractor that combines multiple extractors defined in properties
     * into one {@link MetaObjectParametricMap} object.
     * 
     * @param properties the properties where the extractors are defined
     * @param extractorPropertyKeys the properties key (prefix) that defines the respective extractors
     *          (see {@link #createExtractorFromProperties(messif.utility.ExtendedProperties, java.lang.String)})
     * @param contentParameter the name of the parameter where the original
     *          binary data source (e.g. the image) is stored; nothing is stored if <tt>null</tt>
     * @param expandMetaObjects flag whether the {@link MetaObject} instances created by the respective
     *          extractors are put into the created object as multiple separated {@link LocalAbstractObject}s (<tt>true</tt>),
     *          or without expansion directly as the extracted {@link MetaObject}s (<tt>false</tt>)
     * @return the new extractor instance that combines multiple extractors
     * @throws ExtendedPropertiesException if there some of the required properties were missing or invalid
     * @throws IllegalArgumentException if the extractor cannot be created with the specified parameters
     */
    public static Extractor<MetaObjectParametricMap> createCombinedExtractorFromProperties(ExtendedProperties properties, final String[] extractorPropertyKeys, final String contentParameter, final boolean expandMetaObjects) throws ExtendedPropertiesException, IllegalArgumentException {
        final Extractor<?>[] extractors = new Extractor<?>[extractorPropertyKeys.length];
        for (int i = 0; i < extractorPropertyKeys.length; i++)
            extractors[i] = Extractors.createExtractorFromProperties(properties, extractorPropertyKeys[i]);
        return new Extractor<MetaObjectParametricMap>() {
            @Override
            public MetaObjectParametricMap extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                // Read the binary data from the data source
                byte[] data = dataSource.getBinaryData();
                dataSource.close();

                // Extract the descriptors by their respective extractors
                Map<String, LocalAbstractObject> objects = new LinkedHashMap<String, LocalAbstractObject>(extractorPropertyKeys.length);
                for (int i = 0; i < extractorPropertyKeys.length; i++) {
                    LocalAbstractObject extractedObject = extractors[i].extract(new ExtractorDataSource(data, dataSource.getParameterMap())); // Data source from the binary data
                    if (expandMetaObjects && extractedObject instanceof MetaObject) {
                        MetaObject castExtractedObject = (MetaObject)extractedObject;
                        for (String name : castExtractedObject.getObjectNames()) {
                            objects.put(extractorPropertyKeys[i] + "." + name, castExtractedObject.getObject(name));
                        }
                    } else {
                        objects.put(extractorPropertyKeys[i], extractedObject);
                    }
                }

                // Copy the requested keys from the parameters and add object binary content
                Map<String, Serializable> parameters = Convert.copyAllMapValues(dataSource.getParameterMap(), null, Serializable.class);
                if (contentParameter != null)
                    parameters.put(contentParameter, data);

                // Create the final object
                return new MetaObjectParametricMap(dataSource.getLocator(), parameters, objects);
            }
            @Override
            public Class<? extends MetaObjectParametricMap> getExtractedClass() {
                return MetaObjectParametricMap.class;
            }
        };
    }
}
