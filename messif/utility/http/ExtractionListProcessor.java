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
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.extraction.ExtractorException;
import messif.objects.util.AbstractObjectList;

/**
 * Processor that creates a {@link AbstractObjectList} instance from the HTTP request body.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractionListProcessor implements HttpApplicationProcessor<AbstractObjectList<?>> {
    /** Processor that does the actual extraction */
    private final ExtractionProcessor<?> extractionProcessor;

    /**
     * Create a new instance of extraction processor.
     *
     * @param extractionProcessor the processor that does the actual extraction
     * @throws IllegalArgumentException if the specified {@code objectClass} is not valid
     */
    public ExtractionListProcessor(ExtractionProcessor<?> extractionProcessor) throws IllegalArgumentException {
        this.extractionProcessor = extractionProcessor;
    }

    /**
     * Create a new instace of extraction processor.
     * Extractor is given as a method signature (see {@link ExtractionProcessor#ExtractionProcessor(java.lang.String, java.lang.Class, java.util.Map) ExtractionProcessor}.
     *
     * @param extractorSignature the signature of the extractor (constructor, factory method or named instance)
     * @param namedInstances collection of named instances that are used when converting string parameters
     */
    public ExtractionListProcessor(String extractorSignature, Map<String, Object> namedInstances) {
        this.extractionProcessor = new ExtractionProcessor<LocalAbstractObject>(extractorSignature, LocalAbstractObject.class, namedInstances);
    }

    @Override
    public AbstractObjectList<LocalAbstractObject> processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException, ExtractorException {
        AbstractObjectList<LocalAbstractObject> objects = new AbstractObjectList<LocalAbstractObject>();
        ExtractorDataSource dataSource = extractionProcessor.getExtractorDataSource(httpExchange, httpParams);
        LocalAbstractObject object = extractionProcessor.extractObject(dataSource);
        while (object != null) {
            objects.add(object);
            object = extractionProcessor.extractObject(dataSource);
        }
        return objects;
    }

    @Override
    public int getProcessorArgumentCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends AbstractObjectList<?>> getProcessorReturnType() {
        return (Class)AbstractObjectList.class;
    }

}
