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
import messif.objects.impl.MetaObjectPixMacSCT;
import messif.objects.impl.MetaObjectPixMacShapeAndColor;
import messif.objects.impl.ObjectIntMultiVectorJaccard.WeightProvider;
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
public class ApproxKNNQueryOperationWeightedPixMacMIndex extends ApproxKNNQueryOperationMIndex {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    /** Index of the first objects returned from the answer (to simulate an incremental query) */
    private final int from;

    /** Weight for combining the keywords distance with the visual descriptors distance */
    private final float keywordsWeight;
    /** Weights for query object's keywords */
    private final WeightProvider queryWeights;
    /** Weights for database objects' keywords */
    private final WeightProvider dbWeights;

    /**
     * Creates a new instance of ApproxKNNQueryOperationPixMacMIndex
     * with specified parameters for centralized approximation.
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
            "Local search param", "Type of <br/>local search param", "Answer type"})
    public ApproxKNNQueryOperationWeightedPixMacMIndex(MetaObjectPixMacSCT queryObject, int k, int from,
            float keywordsWeight, WeightProvider queryWeights, WeightProvider dbWeights,
            int localSearchParam, LocalSearchType localSearchType, AnswerType answerType
    ) {
        super(queryObject, k + from, localSearchParam, localSearchType, answerType);
        if (from < 0)
            throw new IllegalArgumentException("From parameter cannot be negative and must be smaller than k");
        this.from = from;
        this.keywordsWeight = keywordsWeight;
        this.queryWeights = queryWeights;
        this.dbWeights = dbWeights;
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
    public MetaObjectPixMacSCT getQueryObject() {
        return (MetaObjectPixMacSCT)super.getQueryObject();
    }

    @Override
    public RankedAbstractObject addToAnswer(AbstractObject object, float distance, float[] objectDistances) throws IllegalArgumentException {
        if (keywordsWeight != 0 && object != null && object instanceof MetaObjectPixMacSCT) {
            // Compute new distance
            distance += keywordsWeight * getKeywordsDistance((MetaObjectPixMacSCT)object);
        }

        return super.addToAnswer(object, distance, objectDistances);
    }

    /**
     * Returns the distance between the keywords from the query object and the given database object.
     * @param dbObject the database object for which to get the distance
     * @return the query and database objects' keywords distance
     */
    protected float getKeywordsDistance(MetaObjectPixMacSCT dbObject) {
        return getQueryObject().getKeyWords().getWeightedDistance(dbObject.getKeyWords(), queryWeights, dbWeights);
    }

    @Override
    public String toString() {
        return new StringBuilder(" Pixmac search <from=").append(from).append(", k=").append(getK()).append("> ").
                append(" approx precision: ").append(localSearchParam).append(" ").append(localSearchType.toString()).toString();
    }

}
