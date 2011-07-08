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
package messif.operations.query;

import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectPixMacShapeAndColor;
import messif.objects.impl.MetaObjectPixMacShapeAndColor.KeywordsJaccardPowerSortedCollection;
import messif.objects.impl.MetaObjectPixMacShapeAndColor.KeywordsJaccardSortedCollection;
import messif.objects.util.DoubleSortedCollection;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.query.ApproxKNNQueryOperation.LocalSearchType;

/**
 * Special query for {@link MetaObjectPixMacShapeAndColor} objects with keywords.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Approximate kNN Query parametrized for Metric Index using PixMac text data")
public class ApproxKNNQueryOperationPixMacMIndex extends ApproxKNNQueryOperationMIndex {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** Index of the first objects returned from the answer (to simulate an incremental query) */
    private final int from;
    /** Internal collection with keyword reranking */
    private DoubleSortedCollection collection;

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
    public RankedAbstractObject addToAnswer(LocalAbstractObject object, float distThreshold) {
        if (collection == null)
            return super.addToAnswer(object, distThreshold);
        if (object == null)
            return null;
        float distance = getQueryObject().getDistance(object, distThreshold);

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

    //****************** Clonning ******************//

    /**
     * Create a duplicate of this operation.
     * The answer of the query is not clonned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public ApproxKNNQueryOperationPixMacMIndex clone() throws CloneNotSupportedException {
        ApproxKNNQueryOperationPixMacMIndex operation = (ApproxKNNQueryOperationPixMacMIndex)super.clone();

        // Create a new double sorted collection for the answer set
        if (collection != null) {
            operation.collection = new KeywordsJaccardPowerSortedCollection(
                    k + from, collection.getMaximalCapacity(), ((MetaObjectPixMacShapeAndColor) queryObject).getKeyWords(),
                    ((KeywordsJaccardSortedCollection) collection).getKeywordsWeight()
            );
            operation.setAnswerCollection(operation.collection);
        }
        return operation;
    }

    @Override
    public String toString() {
        return new StringBuilder(" Pixmac search <from=").append(from).append(", k=").append(getK()).append("> ").
                append(" approx precision: ").append(localSearchParam).append(" ").append(localSearchType.toString()).toString();
    }

}
