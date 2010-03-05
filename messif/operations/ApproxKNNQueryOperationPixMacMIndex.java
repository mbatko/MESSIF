/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.operations;

import java.util.Iterator;
import messif.objects.impl.MetaObjectPixMacShapeAndColor;
import messif.objects.impl.MetaObjectPixMacShapeAndColor.KeywordsJaccardSortedCollection;
import messif.objects.util.RankedAbstractObject;
import messif.operations.ApproxKNNQueryOperation.LocalSearchType;

/**
 *
 * @author xbatko
 */
@AbstractOperation.OperationName("Approximate kNN Query parametrized for Metric Index using PixMac text data")
public class ApproxKNNQueryOperationPixMacMIndex extends ApproxKNNQueryOperationMIndex {
    private static final long serialVersionUID = 1L;

    private final KeywordsJaccardSortedCollection answerCollection;
    private final int from;

    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Starting index of object to return", "Addition to k", "Text similarity weight", "Local search param", "Type of <br/>local search param"})
    public ApproxKNNQueryOperationPixMacMIndex(MetaObjectPixMacShapeAndColor queryObject, int k, int from, int kincrease, float keywordsWeight, int localSearchParam, LocalSearchType localSearchType) {
        super(queryObject, k, localSearchParam, localSearchType);
        if (from < 0 || from >= k)
            throw new IllegalArgumentException("From parameter cannot be negative and must be smaller than k");
        this.answerCollection = new KeywordsJaccardSortedCollection(
                k, k + kincrease, queryObject.getKeyWords(), keywordsWeight
        );
        this.from = from;
        setAnswerCollection(this.answerCollection);
    }

    @Override
    public int getAnswerCount() {
        return super.getAnswerCount() - from;
    }

    @Override
    public Iterator<RankedAbstractObject> getAnswer() {
        return answerCollection.iterator(from, getK() - from);
    }

}
