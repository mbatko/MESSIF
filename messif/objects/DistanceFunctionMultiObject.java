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
 * Interface for distance functions that compute the distance from a set of objects
 * to the given object. The distance function takes two parameters - the set of objects
 * and the object the distance of which is measured - and returns their distance
 * as a float number.
 * 
 * <p>
 * Note that the actual distance function used to compute the distance is given
 * by the implementation.
 * </p>
 *
 * @param <T> the type of the distance function arguments
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz */
public interface DistanceFunctionMultiObject<T> {
    /**
     * Measures the distance between all the objects in set (the first argument) and
     * the specified object (the second argument).
     * The passed array {@code individualDistances} will be filled with the
     * distances to the individual objects in the set.
     * 
     * @param objects the set of objects for which to measure the distance to the second parameter
     * @param object the object for which to measure the distance
     * @param individualDistances the array to fill with the distances to the respective objects from the set;
     *          if not <tt>null</tt>, it must have the same number of allocated elements as the number of the set of objects
     * @return the distance between the given set of {@code objects} and the {@code object}
     * @throws IndexOutOfBoundsException if the passed {@code individualDistances} array is not big enough
     */
    public float getDistanceMultiObject(T[] objects, T object, float[] individualDistances) throws IndexOutOfBoundsException;
}
