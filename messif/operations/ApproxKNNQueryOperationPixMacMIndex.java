/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.operations;

import java.util.Iterator;
import messif.objects.impl.MetaObjectPixMacShapeAndColor;
import messif.objects.impl.MetaObjectPixMacShapeAndColor.KeywordsJaccardPowerSortedCollection;
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
        super(queryObject, k, localSearchParam, localSearchType, answerType);
        if (from < 0 || from >= k)
            throw new IllegalArgumentException("From parameter cannot be negative and must be smaller than k");
        this.from = from;
        if (queryObject.getKeyWords() != null) {
            setAnswerCollection(new KeywordsJaccardPowerSortedCollection( //new KeywordsJaccardSortedCollection(
                    k, k + kincrease, queryObject.getKeyWords(), keywordsWeight
            ));
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

}
