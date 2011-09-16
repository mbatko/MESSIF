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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Factory for creating sequences.
 * The created class must contain at least two constructors. One that
 * accepts a single argument of {@code T} type and another with three
 * arguments {@code T}, {@code Sequence<T>} and {@code int} representing
 * the subsequence data, the original sequence and the offset in the
 * original sequence.
 *
 * @param <T> the type of the sequence data, usually a static array of a primitive type
 *          or {@link java.util.List}
 * @param <O> the class of objects created by this factory
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SequenceFactory<T, O extends Sequence<T>> {
    /** Instantiator for the sequences using only the data */
    private final ConstructorInstantiator<O> toplevelInstantiator;
    /** Instantiator for the subsequences with parent sequence and offset */
    private final ConstructorInstantiator<O> sliceInstantiator;

    /**
     * Creates a new factory for creating sequences.
     * @param objectClass the class of the sequences to create
     * @param sequenceDataClass the class of the sequence data used by the {@code objectClass}
     * @throws NoSuchInstantiatorException if the given class does not have the proper constructors
     */
    public SequenceFactory(Class<? extends O> objectClass, Class<? extends T> sequenceDataClass) throws NoSuchInstantiatorException {
        this.toplevelInstantiator = new ConstructorInstantiator<O>(objectClass, sequenceDataClass);
        this.sliceInstantiator = new ConstructorInstantiator<O>(objectClass, sequenceDataClass, objectClass, int.class);
    }

    /**
     * Returns the class of objects created by this factory.
     * @return the class of objects created by this factory
     */
    public Class<? extends O> getCreatedClass() {
        return toplevelInstantiator.getInstantiatorClass();
    }

    /**
     * Creates a new sequence instance for the provided data.
     * @param sequenceData the sequence data to use
     * @return a new sequence instance
     * @throws InvocationTargetException if there was an exception thrown while creating the instance
     */
    public O create(T sequenceData) throws InvocationTargetException {
        return toplevelInstantiator.instantiate(sequenceData);
    }

    /**
     * Creates a new sequence instance for the provided subsequence data, parent sequence and offset.
     * @param sequenceData the (sub)sequence data to use
     * @param originalSequence the original sequence that the {@code sequenceData} comes from
     * @param originalOffset the offset in the original sequence that the {@code sequenceData} comes from
     * @return a new sequence instance
     * @throws InvocationTargetException if there was an exception thrown while creating the instance
     */
    public O create(T sequenceData, Sequence<? extends T> originalSequence, int originalOffset) throws InvocationTargetException {
        return sliceInstantiator.instantiate(sequenceData, originalSequence, originalOffset);
    }

    /**
     * 
     * @param sequenceData the (sub)sequence data to use
     * @param originalSequence the original sequence that the {@code sequenceData} comes from
     * @param originalOffset the offset in the original sequence that the {@code sequenceData} comes from
     * @param originalLocator the locator of the original sequence
     * @return a new sequence instance
     * @throws InvocationTargetException if there was an exception thrown while creating the instance
     */
    public O create(T sequenceData, Sequence<? extends T> originalSequence, int originalOffset, String originalLocator) throws InvocationTargetException {
        return sliceInstantiator.instantiate(sequenceData, originalSequence, originalOffset, originalLocator);
    }

    /**
     * Creates a new sequence instance for the data from another sequence.
     * @param sequence the sequence the data of which to use
     * @return a new sequence instance
     * @throws InvocationTargetException if there was an exception thrown while creating the instance
     */
    public O create(Sequence<? extends T> sequence) throws InvocationTargetException {
        return create(sequence.getSequenceData());
    }

    /**
     * Creates a new sequence instance for subsequence of the given sequence.
     * @param sequence the sequence the data of which to use
     * @param from the initial index of the subsequence element to be copied, inclusive
     * @param to the final index of the subsequence element to be copied, exclusive
     * @return a new sequence instance
     * @throws InvocationTargetException if there was an exception thrown while creating the instance
     */
    public O create(Sequence<? extends T> sequence, int from, int to) throws InvocationTargetException {
        return create(sequence.getSubsequenceData(from, to), sequence, from);
    }

    /**
     * Creates a new sequence instance for the provided slice.
     * @param slice the sequence data to use
     * @return a new sequence instance
     * @throws InvocationTargetException if there was an exception thrown while creating the instance
     */
    public O create(SequenceSlice<? extends T> slice) throws InvocationTargetException {
        return create(slice.getSequenceData(), slice.getOriginalSequence(), slice.getOffset());
    }

    /**
     * Creates a list of objects that are created from slices provided by
     * slicing the original sequence.
     * @param <T> the type of the sequence data, usually a static array of a primitive type
     *          or {@link java.util.List}
     * @param <O> the class of the created objects
     * @param originalSequence the sequence to slice
     * @param slicer the slicer that is able to slice the given {@code originalSequence}
     * @param createClass the class of the created objects that will hold the slices
     * @return a list of subsequences encapsulated in objects of class {@code O}
     * @throws NoSuchInstantiatorException if the given {@code createClass} does not have the proper constructors
     * @throws InvocationTargetException if there was an exception thrown while creating the {@code createClass} instance
     */
    public static <T, O extends Sequence<T>> List<O> create(Sequence<? extends T> originalSequence, SequenceSlicer<T> slicer, Class<? extends O> createClass) throws NoSuchInstantiatorException, InvocationTargetException {
        List<? extends SequenceSlice<T>> slices = slicer.slice(originalSequence);
        SequenceFactory<T, O> factory = new SequenceFactory<T, O>(createClass, originalSequence.getSequenceDataClass());
        List<O> ret = new ArrayList<O>(slices.size());
        for (SequenceSlice<? extends T> slice : slices)
            ret.add(factory.create(slice));
        return ret;
    }

    /**
     * Creates a list of objects that are created from slices provided by
     * slicing the original sequence. Instances of the same class as the
     * {@code originalSequence} are created.
     *
     * @param <T> the type of the sequence data, usually a static array of a primitive type
     *          or {@link java.util.List}
     * @param <O> the class of objects created by this factory
     * @param originalSequence the sequence to slice
     * @param slicer the slicer that is able to slice the given {@code originalSequence}
     * @return a list of subsequences encapsulated in objects of class {@code O}
     * @throws NoSuchInstantiatorException if the given {@code createClass} does not have the proper constructors
     * @throws InvocationTargetException if there was an exception thrown while creating the {@code createClass} instance
     */
    @SuppressWarnings("unchecked")
    public static <T, O extends Sequence<T>> List<O> create(O originalSequence, SequenceSlicer<T> slicer) throws NoSuchInstantiatorException, InvocationTargetException {
        return create(originalSequence, slicer, originalSequence.getClass());
    }
}
