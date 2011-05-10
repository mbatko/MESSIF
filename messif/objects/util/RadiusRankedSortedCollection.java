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
package messif.objects.util;

/**
 * Specialization of {@link RankedSortedCollection} that maintains an internal radius and
 *  it does not store objects with a larger distance than this radius.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class RadiusRankedSortedCollection extends RankedSortedCollection {

    /** The maximal distance of the {@link RankedAbstrawctObjects} to be stored in this collection */
    private final float radius;

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * The order is defined using the natural order of items.
     * @param radius maximal distance of the objects to be stored in this location
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capacity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RadiusRankedSortedCollection(float radius, int initialCapacity, int maximalCapacity) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity);
        this.radius = radius;
    }

    /**
     * Constructs an empty collection, the order is defined using the natural order of items.
     * @param radius maximal distance of the objects to be stored in this location
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RadiusRankedSortedCollection(float radius) throws IllegalArgumentException {
        super();
        this.radius = radius;
    }

    @Override
    public boolean add(RankedAbstractObject e) {
        if (e.getDistance() >= radius) {
            return super.add(e);
        }
        return false;
    }
}
