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

/**
 * Part of a {@link Sequence} returned from the {@link SequenceSlicer#slice(messif.sequence.Sequence) slicing}.
 * The slice holds a subsequence data and the offset in the original sequence.
 *
 * @param <T> the type of the sequence data, usually a static array of a primitive type
 *          or {@link java.util.List}
 *
 * @see SequenceSlicer
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SequenceSlice<T> {
    /** Sequence data held by this slice */
    private final T sequenceData;
    /** The original sequence that the {@link #sequenceData} comes from */
    private final Sequence<? extends T> originalSequence;
    /** The original sequence locator */
    private String originalSequenceLocator;
    /** Offset in the {@link #originalSequence} that the {@link #sequenceData} comes from */
    private final int originalOffset;

    /**
     * Creates a new sequence slice.
     * @param sequenceData the sequence data for this slice
     * @param originalSequence the original sequence that the {@code sequenceData} comes from
     * @param originalOffset the offset in the original sequence that the {@code sequenceData} comes from
     */
    public SequenceSlice(T sequenceData, Sequence<? extends T> originalSequence, int originalOffset) {
        this.sequenceData = sequenceData;
        this.originalSequence = originalSequence;
        this.originalOffset = originalOffset;
    }

    /**
     * Returns the sequence data in this slice.
     * @return the sequence data in this slice
     */
    public T getSequenceData() {
        return sequenceData;
    }

    /**
     * Returns the original sequence to which the {@link #getSequenceData() sequence data} originally belonged.
     * @return the original sequence of this slice
     */
    public Sequence<? extends T> getOriginalSequence() {
        return originalSequence;
    }

    /**
     * Returns the offset in the original sequence that the {@link #getSequenceData() sequence data} comes from.
     * @return the offset in the original sequence of this slice
     */
    public int getOffset() {
        return originalOffset;
    }

    /**
     * Returns the locator string for the original sequence.
     * @return the locator of the original sequence
     */
    public String getOriginalSequenceLocator() {
        return originalSequenceLocator;
    }
}
