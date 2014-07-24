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
package messif.buckets.index;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparison function, which imposes a <i>total ordering</i> on some
 * collection of keys. Objects stored in the index are compared using
 * keys for ordering; the {@link #extractKey} extracts a key for this
 * comparator from any indexed object.
 * 
 * @param <K> the type of the key arguments of the comparison
 * @param <O> the type of the object arguments of the comparison
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see java.util.Comparator
 */
public interface IndexComparator<K, O> extends Serializable, Comparator<K> {
    /**
     * Compares its two arguments for order. Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     *
     * @param k the key to compare
     * @param o the object to be compared
     * @return a negative integer, zero, or a positive integer as the
     * 	       first argument is less than, equal to, or greater than the
     *	       second.
     * @throws ClassCastException if the arguments' types prevent them from
     * 	       being compared by this comparator.
     */
    int indexCompare(K k, O o);

    /**
     * Returns the key (used for comparison) from an indexed object.
     * @param object the indexed (full) object
     * @return the extracted key
     */
    public K extractKey(O object);

    /**
     *
     * Indicates whether some other object is &quot;equal to&quot; this
     * comparator.  This method must obey the general contract of
     * {@link Object#equals(Object)}.  Additionally, this method can return
     * <tt>true</tt> <i>only</i> if the specified object is also a comparator
     * and it imposes the same ordering as this comparator.  Thus,
     * <code>comp1.equals(comp2)</code> implies that <tt>sgn(comp1.indexCompare(o1,
     * o2))==sgn(comp2.indexCompare(o1, o2))</tt> for every object reference
     * <tt>o1</tt> and <tt>o2</tt>.<p>
     *
     * Note that it is <i>always</i> safe <i>not</i> to override
     * <tt>Object.equals(Object)</tt>.  However, overriding this method may,
     * in some cases, improve performance by allowing programs to determine
     * that two distinct comparators impose the same order.
     *
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> only if the specified object is also
     *		a comparator and it imposes the same ordering as this
     *		comparator.
     * @see Object#equals(Object)
     * @see Object#hashCode()
     */
    @Override
    boolean equals(Object obj);

}
