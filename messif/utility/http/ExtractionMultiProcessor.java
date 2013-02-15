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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.ExtractorException;
import messif.objects.extraction.MultiExtractor;
import messif.utility.reflection.InstantiatorSignature;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Processor that creates {@link Iterator} of {@link LocalAbstractObject} instances
 * from {@link InputStream}s taken from the HTTP request body.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractionMultiProcessor implements HttpApplicationProcessor<Iterator<? extends LocalAbstractObject>> {
    /** Extractor for the object created from the input stream */
    protected final MultiExtractor<?> extractor;

    /**
     * Create a new instance of extraction processor.
     *
     * @param extractor the extractor for objects to create
     * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
     */
    public ExtractionMultiProcessor(MultiExtractor<?> extractor) throws IllegalArgumentException {
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
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IllegalArgumentException if there was an error creating the extractor instance
     * @see messif.objects.extraction.Extractors
     */
    public ExtractionMultiProcessor(String extractorSignature, Map<String, Object> namedInstances) throws IllegalArgumentException {
        try {
            // Try to get named instance first
            Object instance = namedInstances.get(extractorSignature);
            if (instance != null && instance instanceof MultiExtractor) {
                // Named instance found
                this.extractor = (MultiExtractor<?>)instance;
            } else {
                // Create extractor from signature
                this.extractor = InstantiatorSignature.createInstanceWithStringArgs(extractorSignature, MultiExtractor.class, namedInstances);
            }
        } catch (NoSuchInstantiatorException e) {
            throw new IllegalArgumentException("Cannot create " + extractorSignature + ": " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot create " + extractorSignature + ": " + e.getCause(), e.getCause());
        }
    }

    @Override
    public Iterator<? extends LocalAbstractObject> processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException, ExtractorException {
        try {
            return extractor.extract(ExtractionProcessor.getExtractorDataSource(httpExchange, httpParams));
        } catch (IOException e) {
            throw new IllegalArgumentException("There was a problem reading the object from the data: " + e.getMessage(), e);
        }
    }

    @Override
    public int getProcessorArgumentCount() {
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Iterator<? extends LocalAbstractObject>> getProcessorReturnType() {
        return (Class)Iterator.class; // This cannot be cast safely
    }

}
