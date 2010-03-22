/*
 * ExtractionProcessor
 *
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.Extractor;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.extraction.ExtractorException;
import messif.objects.extraction.Extractors;
import messif.utility.reflection.Instantiators;

/**
 * Processor that creates {@link LocalAbstractObject} instances
 * from {@link InputStream}s taken from the HTTP request body.
 * 
 * @param <T> the class of objects that this processor creates
 * @author xbatko
 */
public class ExtractionProcessor<T extends LocalAbstractObject> implements HttpApplicationProcessor<T> {
    /** Extractor for the object created from the input stream */
    protected final Extractor<? extends T> extractor;
    /** Name of the HTTP parameter that contains the name for the extracted object */
    protected final String nameParameter;

    /**
     * Create a new instance of extraction processor.
     *
     * @param nameParameter the name of the HTTP parameter that contains the name for the extracted object
     * @param extractor the extractor for objects to create
     * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
     */
    public ExtractionProcessor(String nameParameter, Extractor<? extends T> extractor) throws IllegalArgumentException {
        this.extractor = extractor;
        this.nameParameter = nameParameter;
    }

    /**
     * Create a new instace of extraction processor.
     * Extractor is given as method signature that is parsed using {@link Instantiators#createInstanceWithStringArgs}.
     * If the signature starts with a quoted name, it is used as HTTP parameter passed to the data source.
     * Example: <pre>
     *      "id" messif.objects.extraction.Extractors.createExternalExtractor(messif.objects.impl.MetaObjectMap, some_extractor_binary)
     * </pre>
     *
     * @param extractorSignature the signature of the extractor (constructor, factory method or named instance)
     * @param extractedClass the class of instances created by the extractor
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IllegalArgumentException if there was an error creating the extractor instance
     */
    public ExtractionProcessor(String extractorSignature, Class<? extends T> extractedClass, Map<String, Object> namedInstances) throws IllegalArgumentException {
        try {
            // Parse quoted start
            String[] parsedSignature = extractorSignature.startsWith("\"") ?
                extractorSignature.substring(1).split("\\s*\"\\s*", 2) :
                null;
            if (parsedSignature != null && parsedSignature.length == 2) {
                this.nameParameter = parsedSignature[0];
                extractorSignature = parsedSignature[1];
            } else {
                this.nameParameter = null;
            }

            // Try to get named instance first
            Object instance = namedInstances.get(extractorSignature);
            if (instance != null && instance instanceof Extractor) {
                // Named instance found
                this.extractor = Extractors.cast(instance, extractedClass);
            } else {
                // Create extractor from signature
                this.extractor = Extractors.cast(Instantiators.createInstanceWithStringArgs(extractorSignature, Extractor.class, namedInstances), extractedClass);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot create " + extractorSignature + ": class not found");
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot create " + extractorSignature + ": " + e.getCause(), e.getCause());
        }
    }

    /**
     * Returns an {@link ExtractorDataSource} derived from the HTTP request.
     * The stream is automatically decompressed, if "content-encoding" header was given.
     *
     * @param httpExchange the HTTP exchange with the request
     * @param httpParams parsed parameters from the HTTP request
     * @return an input stream from the HTTP request
     * @throws IllegalArgumentException if there was a problem reading the HTTP body
     */
    protected final ExtractorDataSource getExtractorDataSource(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        try {
            InputStream input = httpExchange.getRequestBody();
            String contentEncoding = httpExchange.getRequestHeaders().getFirst("content-encoding");
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip"))
                input = new GZIPInputStream(input);
            return new ExtractorDataSource(input, httpParams.get(nameParameter));
        } catch (IOException e) {
            throw new IllegalArgumentException("There was a problem reading the HTTP body: " + e.getMessage(), e);
        }
    }

    /**
     * Performs an extraction on the given data source and returns the created instance.
     * If the end-of-file in the data source is encountered, <tt>null</tt> value is returned.
     * 
     * @param dataSource the data source to use for extraction
     * @return a new extracted instance
     * @throws IllegalArgumentException if there was a problem reading data from the data source or extracting the object
     */
    protected final T extractObject(ExtractorDataSource dataSource) throws IllegalArgumentException {
        try {
            return extractor.extract(dataSource);
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            throw new IllegalArgumentException("There was a problem reading the object from the data: " + e.getMessage(), e);
        } catch (ExtractorException e) {
            throw new IllegalArgumentException("There was a problem extracting descriptors from the object: " + e.getMessage(), e);
        }
    }

    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        return extractObject(getExtractorDataSource(httpExchange, httpParams));
    }

    public int getProcessorArgumentCount() {
        return 1;
    }

    public Class<? extends T> getProcessorReturnType() {
        return extractor.getExtractedClass();
    }

}
