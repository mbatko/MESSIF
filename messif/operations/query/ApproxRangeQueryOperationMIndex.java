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

import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedSortedRadiusRestrictCollection;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;

/**
 * Operation for approximate range on M-Index.
 * Note that this operation overrides its collection to implement the radius
 * restriction, so the collection <em>cannot be changed</em>.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ApproxRangeQueryOperationMIndex extends ApproxKNNQueryOperationMIndex {
    /** Serialization id. */
    private static final long serialVersionUID = 61010L;

    /**
     * Operation constructor; the "k" is set to max, query radius is used via the internal answer collection.
     * 
     * @param queryObject range query object
     * @param queryRadius radius
     * @param filteringRadius radius used for filtering; not used if LocalAbstractObject.UNKNOWN_DISTANCE
     * @param accessedObjects approximation precision parameter (# of accessed objects)
     * @param answerType type of answer
     */
    @AbstractOperation.OperationConstructor({"Query object", "query radius", "radius used for filtering", "max # of accessed objs", "answer type"})
    public ApproxRangeQueryOperationMIndex(LocalAbstractObject queryObject, float queryRadius, float filteringRadius, int accessedObjects, AnswerType answerType) {
        super(queryObject, Integer.MAX_VALUE, accessedObjects, ApproxKNNQueryOperation.LocalSearchType.ABS_OBJ_COUNT, answerType, filteringRadius);
        this.setAnswerCollection(new RankedSortedRadiusRestrictCollection(queryRadius));
    }
}
