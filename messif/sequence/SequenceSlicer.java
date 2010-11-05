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
package messif.sequence;

import java.util.List;

/**
 * Provides a functionality of slicing a {@link Sequence} into several {@link SequenceSlice}s.
 * The particular implementation of this interface decides whether the slices
 * are disjoint, overlaping, etc.
 *
 * @param <T> the type of the sequence data, usually a static array of a primitive type
 *          or {@link java.util.List}
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface SequenceSlicer<T> {
    /**
     * Slices a given {@code sequence} into several {@link SequenceSlice}s.
     *
     * @param sequence the sequence to slice
     * @return a list of resulting {@link SequenceSlice slices}
     */
    public abstract List<? extends SequenceSlice<T>> slice(Sequence<? extends T> sequence);
}
