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

import java.util.Comparator;


/**
 * Interface for marking objects that can be ranked according to distance.
 * 
 * @see messif.operations.RankingQueryOperation
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface DistanceRanked {

    /**
     * Default comparator that can be used on DistanceRanked objects.
     * This comparator compares the objects based on their distance rank.
     */
    public static Comparator<DistanceRanked> comparator = new Comparator<DistanceRanked>() {
        public int compare(DistanceRanked o1, DistanceRanked o2) {
            return Float.compare(o1.getDistance(), o2.getDistance());
        }
    };

    /**
     * Returns the ranking distance.
     * @return the ranking distance
     */
    public float getDistance();

}
