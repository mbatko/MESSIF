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
package messif.objects;

/**
 * Enumeration that implements several variants of the {@link DistanceFunctionMultiObject multi-object distance function}.
 * It computes a regular {@link LocalAbstractObject}'s distance to the given object
 * and aggregates the resulting distances into a single distance.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public enum DistanceFunctionMultiObjectAggregation implements DistanceFunctionMultiObject<LocalAbstractObject> {
    /** Computes the overall distance as the sum of the distances to all the query objects */
    SUM,
    /** Computes the overall distance as the maximal distance from all the query objects */
    MAX,
    /** Computes the overall distance as the average distance from all the query objects */
    AVG,
    /** Computes the overall distance as the minimal distance from all the query objects */
    MIN;

    /**
     * Computes the overall distance on the given array of individual distances to the query objects.
     * @param distances the individual distances to the query objects
     * @return the overall distance according to this aggregation definition
     */
    protected float evaluate(float[] distances) {
        float retVal;
        switch (this) {
            case SUM:
                retVal = 0;
                for (int i = 0; i < distances.length; i++)
                    retVal += distances[i];
                return retVal;
            case MAX:
                retVal = 0;
                for (int i = 0; i < distances.length; i++)
                    retVal = Math.max(retVal, distances[i]);
                return retVal;
            case MIN:
                retVal = Float.MAX_VALUE;
                for (int i = 0; i < distances.length; i++)
                    retVal = Math.min(retVal, distances[i]);
                return retVal;
            case AVG:
                retVal = 0;
                for (int i = 0; i < distances.length; i++)
                    retVal += distances[i];
                return retVal / (float) distances.length;
            default:
                throw new InternalError("There is no evaluate method for " + this);
        }
    }

    @Override
    public float getDistanceMultiObject(LocalAbstractObject[] objects, LocalAbstractObject object, float[] individualDistances) throws IndexOutOfBoundsException {
        if (individualDistances == null)
            individualDistances = new float[objects.length];
        for (int i = 0; i < objects.length; i++)
            individualDistances[i] = objects[i].getDistance(object);
        return evaluate(individualDistances);
    }
}
