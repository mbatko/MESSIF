/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.operations;

import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectPixMacShapeAndColor;
import messif.objects.impl.MetaObjectPixMacShapeAndColor.KeywordsJaccardPowerSortedCollection;
import messif.objects.util.DoubleSortedCollection;
import messif.objects.util.RankedAbstractObject;
import messif.operations.ApproxKNNQueryOperation.LocalSearchType;

/**
 * Special query for {@link MetaObjectPixMacShapeAndColor} objects with keywords.
 * @author xbatko
 */
@AbstractOperation.OperationName("Approximate kNN Query parametrized for Metric Index using PixMac text data")
public class ApproxKNNQueryOperationPixMacMIndex extends ApproxKNNQueryOperationMIndex {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** Index of the first objects returned from the answer (to simulate an incremental query) */
    private final int from;
    /** Internal collection with keyword reranking */
    private final DoubleSortedCollection collection;

    /**
     * Creates a new instance of ApproxKNNQueryOperationPixMacMIndex
     * with specified parameters for centralized approximation.
     *
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param from index of the first objects returned from the answer (to simulate an incremental query)
     * @param kincrease number of objects added to k that are actually stored in the answer
     *          (applicable only if the query object contains keywords)
     * @param keywordsWeight weight for the keywords jaccard coefficient
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Starting index of object to return", "Addition to k", "Text similarity weight", "Local search param", "Type of <br/>local search param", "Answer type"})
    public ApproxKNNQueryOperationPixMacMIndex(MetaObjectPixMacShapeAndColor queryObject, int k, int from, int kincrease, float keywordsWeight, int localSearchParam, LocalSearchType localSearchType, AnswerType answerType) {
        super(queryObject, k + from, localSearchParam, localSearchType, answerType);
        if (from < 0)
            throw new IllegalArgumentException("From parameter cannot be negative and must be smaller than k");
        this.from = from;
        if (queryObject.getKeyWords() != null) {
            collection = new KeywordsJaccardPowerSortedCollection( //new KeywordsJaccardSortedCollection(
                    k + from, k + from + kincrease, queryObject.getKeyWords(), keywordsWeight
            );
            setAnswerCollection(collection);
        } else {
            collection = null;
        }
    }

    @Override
    public int getAnswerCount() {
        return super.getAnswerCount() - from;
    }

    @Override
    public Iterator<RankedAbstractObject> getAnswer() {
        return getAnswer(from, getK() - from);
    }

    @Override
    public RankedAbstractObject addToAnswer(LocalAbstractObject queryObject, LocalAbstractObject object, float distThreshold) {
        if (collection == null)
            return super.addToAnswer(queryObject, object, distThreshold);
        if (object == null)
            return null;
        float distance = queryObject.getDistance(object, null, distThreshold);

        // Create ranked object with new distance
        try {
            RankedAbstractObject ret = new RankedAbstractObject(
                answerType.update(object),
                collection.getNewDistance(object, distance)
            );
            if (collection.add(ret, distance))
                return ret;
            else
                return null;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
