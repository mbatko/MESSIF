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
package messif.objects.classification;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Basic implementation of the {@link Classification} interface.
 * @param <C> the class of instances that represent the classification categories
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ClassificationBase<C> implements Classification<C>, Serializable {
    /** Serial version for {@link java.io.Serializable} */
    private static final long serialVersionUID = 1L;

    //****************** Constants *************//

    /** Default initial capacity used in constructor */
    protected static final int DEFAULT_INITIAL_CAPACITY = 10;


    //****************** Attributes *************//

    /** Class of instances that represent the classification categories */
    private final Class<? extends C> storedClass;
    /** Internal list that keeps the classification */
    private final Collection<C> classification;


    //****************** Storage constructor *************//

    /**
     * Creates a default storage instance for holding classifications.
     * @param <C> the class of instances that represent the classification categories
     * @param storedClass the class of instances that represent the classification categories
     * @param initialCapacity the initial capacity of the classification
     * @return a new storage instance for holding classifications
     */
    protected static <C> Collection<C> createDefaultStorage(Class<? extends C> storedClass, int initialCapacity) {
        return new ArrayList<C>(initialCapacity == 0 ? DEFAULT_INITIAL_CAPACITY : initialCapacity);
    }


    //****************** Constructors *************//

    /**
     * Creates an empty classification.
     * @param storedClass the class of instances that represent the classification categories
     * @throws NullPointerException if the {@code categoriesClass} is <tt>null</tt>
     */
    public ClassificationBase(Class<? extends C> storedClass) throws NullPointerException {
        this(storedClass, 0);
    }

    /**
     * Creates an empty classification with the specified initial capacity.
     * @param storedClass the class of instances that represent the classification categories
     * @param initialCapacity the initial capacity of the classification
     * @throws NullPointerException if the {@code categoriesClass} is <tt>null</tt>
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public ClassificationBase(Class<? extends C> storedClass, int initialCapacity) throws NullPointerException, IllegalArgumentException {
        this(storedClass, createDefaultStorage(storedClass, initialCapacity));
    }

    /**
     * Creates an empty classification with the given storage instance.
     * @param storedClass the class of instances that represent the classification categories
     * @param storage the instance responsible for holding the objects in this classification
     * @throws NullPointerException if the {@code categoriesClass} or {@code storage} is <tt>null</tt>
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    protected ClassificationBase(Class<? extends C> storedClass, Collection<C> storage) throws NullPointerException, IllegalArgumentException {
        if (storedClass == null || storage == null)
            throw new NullPointerException();
        this.storedClass = storedClass;
        this.classification = storage;
    }


    //****************** Update methods *************//

    /**
     * Adds the given category to this classification.
     * Note that <tt>null</tt> category is silently ignored.
     * @param category the category to add
     * @return this instance to allow chaining
     */
    public ClassificationBase<C> add(C category) {
        if (category != null)
            this.classification.add(category);
        return this;
    }

    /**
     * Cast the given object to stored category class safely.
     * If the object cannot be cast and {@code ignoreIncompatibleCategory} is <tt>true</tt>,
     * a <tt>null</tt> is returned.
     * @param object the object to cast
     * @param ignoreIncompatible flag whether to silently ignore incompatible object (<tt>true</tt>) or
     *          throw a {@link ClassCastException} (<tt>false</tt>)
     * @return the type-safe category cast from the given object or
     *          <tt>null</tt> if it is not compatible and {@code ignoreIncompatibleCategory} is <tt>true</tt>
     * @throws ClassCastException if the object is not compatible with this classification's categories
     */
    @SuppressWarnings("unchecked")
    protected C castToStored(Object object, boolean ignoreIncompatible) throws ClassCastException {
        if (object == null || storedClass.isInstance(object))
            return (C)object;
        if (ignoreIncompatible)
            return null;
        throw new ClassCastException("Cannot cast " + object.getClass() + " to " + storedClass.getName());
    }

    /**
     * Adds the categories provided by the iterator to this classification.
     * Note that the objects from the iterator are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param categories an iterator over the categories to add
     * @param ignoreIncompatible flag whether to silently ignore incompatible objects
     *          from the iterator (<tt>true</tt>) or throw a {@link ClassCastException} (<tt>false</tt>)
     * @return this instance to allow chaining
     * @throws ClassCastException if there was an object incompatible with this classification's categories
     */
    public ClassificationBase<C> addAll(Iterator<?> categories, boolean ignoreIncompatible) throws ClassCastException {
        while (categories.hasNext())
            add(castToStored(categories.next(), ignoreIncompatible));
        return this;
    }

    /**
     * Adds the categories provided by the {@link Iterable} to this classification.
     * Note that the objects from the {@link Iterable} are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param categories an {@link Iterable} over the categories to add
     * @param ignoreIncompatible flag whether to silently ignore incompatible objects
     *          from the iterator (<tt>true</tt>) or throw a {@link ClassCastException} (<tt>false</tt>)
     * @return this instance to allow chaining
     * @throws ClassCastException if there was an object incompatible with this classification's categories
     */
    public ClassificationBase<C> addAll(Iterable<?> categories, boolean ignoreIncompatible) throws ClassCastException {
        return addAll(categories.iterator(), ignoreIncompatible);
    }

    /**
     * Adds the categories from a static array to this classification.
     * Note that the objects from the array are type-checked to be compatible with {@code C}
     * and <tt>null</tt> items are silently ignored.
     *
     * @param array a static array with the categories to add
     * @param ignoreIncompatibleCategory flag whether to silently ignore incompatible categories
     *          from the array (<tt>true</tt>) or throw a {@link ClassCastException} (<tt>false</tt>)
     * @return this instance to allow chaining
     * @throws ClassCastException if there was an object incompatible with this classification's categories
     * @throws IllegalArgumentException if the object {@code array} is not a static array
     */
    public ClassificationBase<C> addArray(Object array, boolean ignoreIncompatibleCategory) throws ClassCastException, IllegalArgumentException {
        int size = Array.getLength(array);
        for (int i = 0; i < size; i++)
            add(castToStored(Array.get(array, i), ignoreIncompatibleCategory));
        return this;
    }

    /**
     * Removes the given category from this classification.
     * @param category the category to remove
     * @return <tt>true</tt> if this classification changed as a result of the call
     * @throws NullPointerException if the given category is <tt>null</tt>
     */
    public boolean remove(C category) throws NullPointerException {
        return classification.remove(category);
    }


    //****************** Classification interface methods *************//

    @Override
    public Class<? extends C> getStoredClass() {
        return storedClass;
    }

    @Override
    public int size() {
        return classification.size();
    }

    @Override
    public Iterator<C> iterator() throws UnsupportedOperationException {
        return classification.iterator();
    }

    @Override
    public boolean contains(C category) throws NullPointerException {
        if (category == null)
            throw new NullPointerException();
        return classification.contains(category);
    }


    //****************** String conversion *************//

    @Override
    public String toString() {
        return classification.toString();
    }

}
