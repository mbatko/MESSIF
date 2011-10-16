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
import messif.utility.reflection.InstantiatorSignature;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Processor that creates {@link LocalAbstractObject} instances
 * from {@link InputStream}s taken from the HTTP request body.
 * 
 * @param <T> the class of objects that this processor creates
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractionProcessor<T extends LocalAbstractObject> implements HttpApplicationProcessor<T> {
    /** Extractor for the object created from the input stream */
    protected final Extractor<? extends T> extractor;

    /**
     * Create a new instance of extraction processor.
     *
     * @param extractor the extractor for objects to create
     * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
     */
    public ExtractionProcessor(Extractor<? extends T> extractor) throws IllegalArgumentException {
        this.extractor = extractor;
    }

    /**
     * Create a new instance of extraction processor.
     * Extractor is given as method signature that is parsed using {@link InstantiatorSignature#createInstanceWithStringArgs}.
     * Example: <pre>
     *      messif.objects.extraction.Extractors.createExternalExtractor(messif.objects.impl.MetaObjectMap, some_extractor_binary)
     * </pre>
     *
     * @param extractorSignature the signature of the extractor (constructor, factory method or named instance)
     * @param extractedClass the class of instances created by the extractor
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IllegalArgumentException if there was an error creating the extractor instance
     * @see messif.objects.extraction.Extractors
     */
    public ExtractionProcessor(String extractorSignature, Class<? extends T> extractedClass, Map<String, Object> namedInstances) throws IllegalArgumentException {
        try {
            // Try to get named instance first
            Object instance = namedInstances.get(extractorSignature);
            if (instance != null && instance instanceof Extractor) {
                // Named instance found
                this.extractor = Extractors.cast(instance, extractedClass);
            } else {
                // Create extractor from signature
                this.extractor = Extractors.cast(InstantiatorSignature.createInstanceWithStringArgs(extractorSignature, Extractor.class, namedInstances), extractedClass);
            }
        } catch (NoSuchInstantiatorException e) {
            throw new IllegalArgumentException("Cannot create " + extractorSignature + ": " + e.getMessage());
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
            return new ExtractorDataSource(input, httpParams);
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
     * @throws ExtractorException if there was a problem calling extractor
     */
    protected final T extractObject(ExtractorDataSource dataSource) throws IllegalArgumentException, ExtractorException {
        try {
            return extractor.extract(dataSource);
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            throw new IllegalArgumentException("There was a problem reading the object from the data: " + e.getMessage(), e);
        }
    }

    @Override
    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException, ExtractorException {
        return extractObject(getExtractorDataSource(httpExchange, httpParams));
    }

    @Override
    public int getProcessorArgumentCount() {
        return 1;
    }

    @Override
    public Class<? extends T> getProcessorReturnType() {
        return extractor.getExtractedClass();
    }

}
