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
package messif.objects.keys;

import java.io.Serializable;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DoubleKeyOperator extends KeyOperator<DoubleKey> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    public int compare(DoubleKey o1, DoubleKey o2) {
        return o1.compareTo(o2);
    }

    @Override
    public DoubleKey getMaxKey() {
        return new DoubleKey(null, Double.MAX_VALUE);
    }

    @Override
    public DoubleKey getMinKey() {
        return new DoubleKey(null, 0d);
    }

    @Override
    public DoubleKey getNextKey(DoubleKey key) {
        return new DoubleKey(null, key.key + Double.MIN_VALUE);
    }

    @Override
    public DoubleKey getPreviousKey(DoubleKey key) {
        return new DoubleKey(null, key.key - Double.MIN_VALUE);
    }

    @Override
    public KeyInterval<DoubleKey> createInteral(DoubleKey from, DoubleKey to) {
        return new DoubleKeyInterval(from, to);
    }

    @Override
    public KeyInterval<DoubleKey> makeLeftOpen(KeyInterval<DoubleKey> interval) {
        return new DoubleKeyInterval(getNextKey(interval.getFrom()), interval.getTo());
    }

    /**
     * Given two keys, return their distance (difference)
     * @param first the first key
     * @param second the second
     * @return distance of the keys
     */
    @Override
    public DoubleKey getDifference(DoubleKey first, DoubleKey second) {
        return new DoubleKey(null, first.key - second.key);
    }

    @Override
    public DoubleKey getMiddleKey(DoubleKey first, DoubleKey second) {
        return new DoubleKey(null, (first.key + second.key) / 2);
    }
}
