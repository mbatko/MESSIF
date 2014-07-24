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
 * Interface for external distance functions.
 * The distance function takes two parameters of type {@code T} and returns
 * their distance as a float number.
 *
 * @param <T> the type of the distance function arguments
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface DistanceFunction<T> {
    /**
     * Returns the distance between object {@code o1} and object {@code o2}.
     *
     * @param o1 the object for which to measure the distance
     * @param o2 the object for which to measure the distance
     * @return the distance between object {@code o1} and object {@code o2}
     */
    public float getDistance(T o1, T o2);

    /**
     * Returns the type of objects that this distance function accepts as arguments.
     * @return the type of objects for this distance function
     */
    public Class<? extends T> getDistanceObjectClass();
}
