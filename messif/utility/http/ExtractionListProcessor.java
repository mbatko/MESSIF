/*
 * ExtractObjectProcessor
 *
 */

package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.util.AbstractObjectList;

/**
 * Processor that creates a {@link AbstractObjectList} instance from the HTTP request body.
 * 
 * @param <T> the class of extracted objects the list of which this processor creates
 * @author xbatko
 */
public class ExtractionListProcessor<T extends LocalAbstractObject> implements HttpApplicationProcessor<AbstractObjectList<T>> {
    /** Processor that does the actual extraction */
    private final ExtractionProcessor<? extends T> extractionProcessor;

    /**
     * Create a new instance of extraction processor.
     *
     * @param extractionProcessor the processor that does the actual extraction
     * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
     */
    public ExtractionListProcessor(ExtractionProcessor<? extends T> extractionProcessor) throws IllegalArgumentException {
        this.extractionProcessor = extractionProcessor;
    }

    /**
     * Create a new instace of extraction processor.
     * Extractor is given as a method signature (see {@link ExtractionProcessor#ExtractionProcessor(java.lang.String, java.lang.Class, java.util.Map) ExtractionProcessor}.
     *
     * @param extractorSignature the signature of the extractor (constructor, factory method or named instance)
     * @param extractedClass the class of instances created by the extractor
     * @param namedInstances collection of named instances that are used when converting string parameters
     */
    public ExtractionListProcessor(String extractorSignature, Class<? extends T> extractedClass, Map<String, Object> namedInstances) {
        this.extractionProcessor = new ExtractionProcessor<T>(extractorSignature, extractedClass, namedInstances);
    }

    public AbstractObjectList<T> processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        AbstractObjectList<T> objects = new AbstractObjectList<T>();
        ExtractorDataSource dataSource = extractionProcessor.getExtractorDataSource(httpExchange, httpParams);
        T object = extractionProcessor.extractObject(dataSource);
        while (object != null) {
            objects.add(object);
            object = extractionProcessor.extractObject(dataSource);
        }
        return objects;
    }

    public int getProcessorArgumentCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends AbstractObjectList<T>> getProcessorReturnType() {
        return (Class)AbstractObjectList.class;
    }

}
