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
import messif.objects.AbstractObject;
import messif.objects.impl.MetaObjectProfiSCT;
import messif.objects.impl.MetaObjectProfiSCT.Territory;
import messif.objects.impl.ObjectIntMultiVectorJaccard;
import messif.objects.impl.ObjectIntMultiVectorJaccard.WeightProvider;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.query.ApproxKNNQueryOperation.LocalSearchType;

/**
 * Special query for {@link MetaObjectProfiSCT} objects with keywords.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
@AbstractOperation.OperationName("Approximate kNN Query parametrized for Metric Index using Profi text data")
public class ApproxKNNQueryOperationWeightedProfiMIndex extends ApproxKNNQueryOperationMIndex {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 2L;

    /** Index of the first objects returned from the answer (to simulate an incremental query) */
    private final int from;

    /** Weight for combining the keywords distance with the visual descriptors distance */
    private final float keywordsWeight;
    /** Weights for query object's keywords */
    private final WeightProvider queryWeights;
    /** Weights for database objects' keywords */
    private final WeightProvider dbWeights;
    /** The territory used to filter the answer */
    private final Territory filterTerritory;

    /**
     * Creates a new instance of ApproxKNNQueryOperationWeightedProfiMIndex
     * with specified parameters for centralized approximation.
     *
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param from index of the first objects returned from the answer (to simulate an incremental query)
     * @param keywordsWeight weight for the keywords jaccard coefficient
     * @param queryWeights the weights provider for query object's keywords
     * @param dbWeights the weights provider for database objects' keywords
     * @param filterTerritory the territory used to filter the answer
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Starting index of object to return",
            "Text similarity weight", "Query keywords weight provider", "Database keywords weight provider", "Filter territory",
            "Local search param", "Type of local search param", "Answer type"})
    public ApproxKNNQueryOperationWeightedProfiMIndex(MetaObjectProfiSCT queryObject, int k, int from,
            float keywordsWeight, WeightProvider queryWeights, WeightProvider dbWeights, Territory filterTerritory,
            int localSearchParam, LocalSearchType localSearchType, AnswerType answerType
    ) {
        super(queryObject, k + from, localSearchParam, localSearchType, answerType);
        if (from < 0)
            throw new IllegalArgumentException("From parameter cannot be negative and must be smaller than k");
        this.from = from;
        this.keywordsWeight = keywordsWeight;
        this.queryWeights = queryWeights;
        this.dbWeights = dbWeights;
        this.filterTerritory = filterTerritory;
    }

    /**
     * Creates a new instance of ApproxKNNQueryOperationWeightedProfiMIndex
     * with specified parameters for centralized approximation.
     * No territory is filtered.
     *
     * @param queryObject query object
     * @param k number of objects to be returned
     * @param from index of the first objects returned from the answer (to simulate an incremental query)
     * @param keywordsWeight weight for the keywords jaccard coefficient
     * @param queryWeights the weights provider for query object's keywords
     * @param dbWeights the weights provider for database objects' keywords
     * @param localSearchParam local search parameter - typically approximation parameter
     * @param localSearchType type of the local search parameter
     * @param answerType the type of objects this operation stores in its answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "# of nearest objects", "Starting index of object to return",
            "Text similarity weight", "Query keywords weight provider", "Database keywords weight provider",
            "Local search param", "Type of local search param", "Answer type"})
    public ApproxKNNQueryOperationWeightedProfiMIndex(MetaObjectProfiSCT queryObject, int k, int from,
            float keywordsWeight, WeightProvider queryWeights, WeightProvider dbWeights,
            int localSearchParam, LocalSearchType localSearchType, AnswerType answerType
    ) {
        this(queryObject, k, from, keywordsWeight, queryWeights, dbWeights, null, localSearchParam, localSearchType, answerType);
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
    public MetaObjectProfiSCT getQueryObject() {
        return (MetaObjectProfiSCT)super.getQueryObject();
    }

    @Override
    public RankedAbstractObject addToAnswer(AbstractObject object, float distance, float[] objectDistances) throws IllegalArgumentException {
        MetaObjectProfiSCT castObj = (MetaObjectProfiSCT)object;
        if (filterTerritory != null && !castObj.containsTerritory(filterTerritory))
            return null;

        if (keywordsWeight != 0 && castObj != null) {
            // Compute new distance
            distance += keywordsWeight * getKeywordsDistance(castObj);
        }

        if (distance > getAnswerThreshold())
            return null;

        return super.addToAnswer(castObj, distance, objectDistances);
    }

    /**
     * Returns the distance between the keywords from the query object and the given database object.
     * @param dbObject the database object for which to get the distance
     * @return the query and database objects' keywords distance
     */
    protected float getKeywordsDistance(MetaObjectProfiSCT dbObject) {
        ObjectIntMultiVectorJaccard queryKeyWords = getQueryObject().getKeyWords();
        ObjectIntMultiVectorJaccard dbObjectKeyWords = dbObject.getKeyWords();
        if (queryKeyWords == null || dbObjectKeyWords == null)
            return 0;
        else
            return queryKeyWords.getWeightedDistance(dbObjectKeyWords, queryWeights, dbWeights);
    }

    @Override
    public String toString() {
        return new StringBuilder(" Profi search <from=").append(from).append(", k=").append(getK()).append("> ").
                append(" approx precision: ").append(localSearchParam).append(" ").append(localSearchType.toString()).toString();
    }

}
