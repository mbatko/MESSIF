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

import com.sun.net.httpserver.HttpHandler;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import messif.algorithms.Algorithm;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.RankingQueryOperation;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class OperationHandler<T extends AbstractOperation> extends SimpleHandler<T> implements HttpHandler {
    private static final String RESULT_XML_TAG = "<result distance=\"%f\" locator=\"%s\"/>";
    private static final String RESULT_JSON_TAG = "[%f,\"%s\"]";

    public OperationHandler(Logger log, Algorithm algorithm, Class<? extends T> operationClass, String args[], int offset, int length, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        super(log, false, new OperationProcessor<T>(algorithm, operationClass, args, offset, length, namedInstances));
    }

    /**
     * Create response text for an interated collection of {@link RankedAbstractObject}.
     * JSON arrays with items containing the distance and the object's locator are returned.
     * @param iterator the iterator containing the data objects
     * @return a response text
     */
    protected String getResponseRankingQuery(Iterator<RankedAbstractObject> iterator) {
        StringBuilder response = new StringBuilder();
        while (iterator.hasNext()) {
            RankedAbstractObject object = iterator.next();
            response.append(String.format(
                    isAsXml() ? RESULT_XML_TAG : RESULT_JSON_TAG,
                    object.getDistance(), object.getObject().getLocatorURI()
            ));
            if (iterator.hasNext() && !isAsXml())
                response.append(',');
        }
        if (!isAsXml()) {
            response.insert(0, '[');
            response.append(']');
        }

        return response.toString();
    }

    /**
     * Create response text for an interated collection of {@link RankedAbstractObject}.
     * JSON arrays with items containing the distance and the object's locator are returned.
     * @param iterator the iterator containing the data objects
     * @return a response text
     */
    protected String getResponseRankingQueryJSON(Iterator<RankedAbstractObject> iterator) {
        StringBuilder response = new StringBuilder();
        response.append('[');
        while (iterator.hasNext()) {
            RankedAbstractObject object = iterator.next();
            response.append('[');
            response.append(object.getDistance());
            response.append(",\"");
            response.append(object.getObject().getLocatorURI());
            response.append("\"]");
            if (iterator.hasNext())
                response.append(',');
        }
        response.append(']');

        return response.toString();
    }

    @Override
    protected HttpApplicationResponse toResponse(T operation) {
        String data;
        if (!operation.wasSuccessful() && operation.isFinished()) {
            data = "Operation failed: " + operation.getErrorCode();
        } else if (operation instanceof RankingQueryOperation) {
            data = getResponseRankingQuery(((RankingQueryOperation)operation).getAnswer());
        } else {
            data = "Operation finished successfully";
        }
        return isAsXml() ? new SimpleXmlResponse(data) : new SimpleTextResponse(data);
    }

}
