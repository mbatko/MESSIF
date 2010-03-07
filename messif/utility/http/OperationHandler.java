/*
 * OperationHandler
 *
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
 * @author xbatko
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
