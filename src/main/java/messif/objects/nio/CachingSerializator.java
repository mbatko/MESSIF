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
package messif.objects.nio;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import messif.utility.Convert;

/**
 * This class extends the deserializing support of {@link MultiClassSerializator} with
 * caching. The constructors/factory methods of all the <code>cachedClasses</code>
 * are cached and thus this serializator is quite fast when deserializing from binary stream.
 * If the class is not predefined, but supports {@link BinarySerializable} interface,
 * there will be an additional cost of the class name and constructor/factory method lookup.
 * 
 * This serializator checks the serialUIDs of the cached objects for changes.
 * 
 * @param <T> default class used when reading serialized object
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class CachingSerializator<T> extends MultiClassSerializator<T> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** The hash table of cached classes with references to the constructor/factory method lists */
    protected final Map<Class<? extends BinarySerializable>, Integer> cachedClasses;

    /** The list of constructors for the cached classes with the exactly the same order as specified by cachedClasses */
    protected transient List<Constructor<?>> cachedConstructors;

    /** The list of factory methods for the cached classes with the exactly the same order as specified by cachedClasses */
    protected transient List<Method> cachedFactoryMethods;

    /** List of classes that are not cached but were serialized by this serializator. */
    protected transient Set<Class<? extends BinarySerializable>> notCachedClasses;
    
    /**
     * Creates a new instance of CachingSerializator.
     * The constructors/factory methods of the <code>cachedClasses</code>
     * are cached.
     * 
     * <p>
     * If any of the items of this the list of predefined classes is replaced or deleted,
     * the deserialization of the older binary stream <b>will fail</b>. Appending the
     * list is safe.
     * </p>
     * 
     * @param defaultClass the default class that is used for deserialization when a class is not specified
     * @param cachedClasses the classes that are used frequently and should be cached
     * @throws IllegalArgumentException if there is an invalid value in <code>cachedClasses</code>
     */
    public CachingSerializator(Class<? extends T> defaultClass, Class<?>[] cachedClasses) throws IllegalArgumentException {
        super(defaultClass);

        if (cachedClasses == null || cachedClasses.length == 0)
            throw new IllegalArgumentException("At least one class must be specified for BinarySerializator");

        this.cachedClasses = new HashMap<Class<? extends BinarySerializable>, Integer>(cachedClasses.length);
        this.cachedConstructors = new ArrayList<Constructor<?>>(cachedClasses.length);
        this.cachedFactoryMethods = new ArrayList<Method>(cachedClasses.length);
        this.notCachedClasses = new HashSet<Class<? extends BinarySerializable>>();

        // Fill the predefined data
        for (Class<?> selClass : cachedClasses) {
            try {
                if (Modifier.isAbstract(selClass.getModifiers()))
                    throw new IllegalArgumentException("Cannot cache class '" + selClass.getName() + "' because it is abstract");
                Class<? extends BinarySerializable> castClass = Convert.genericCastToClass(selClass, BinarySerializable.class);
                if (this.cachedClasses.put(castClass, addToCache(castClass)) != null)
                    throw new IllegalArgumentException("Class '" + selClass.getName() + "' was specified twice");
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Class '" + selClass.getName() + "' does not implement BinarySerializable");
            }
        }
    }

    /**
     * Add the specified class to cache.
     * @param classToAdd the class to add
     * @return the position in the cache
     * @throws IllegalArgumentException if the class has invalid constructor/factory method or there is already another class on this position
     */
    private int addToCache(Class<? extends BinarySerializable> classToAdd) throws IllegalArgumentException {
        // Get the constructor/factory method for the predefined class
        Constructor<?> selConstructor = getNativeSerializableConstructor(classToAdd);
        Method selFactoryMethod = getNativeSerializableFactoryMethod(classToAdd);
        if (selConstructor == null && selFactoryMethod == null)
            throw new IllegalArgumentException("Class '" + classToAdd.getName() + "' lacks proper constructor/factory method for BinarySerializable");

        // Add to the cache
        cachedConstructors.add(selConstructor);
        cachedFactoryMethods.add(selFactoryMethod);
        return cachedConstructors.size() - 1;
    }

    /**
     * Returns a constructor for the specified flag and object class.
     * If the object was serialized as one of the predefined classes, the cached
     * constructor is used.
     * For the {@link #DEFAULTCLASS_SERIALIZATION DEFAULTCLASS} serialization,
     * current constructor is returned. If the stored object implements the
     * {@link BinarySerializable} interface, its constructor is looked up.
     * Otherwise, <tt>null</tt> is returned.
     * 
     * @param flag the type of deserialization (see constants)
     * @param objectClass the class that is expected to be in the stream
     * @return a constructor for the specified flag and object class
     * @throws IllegalArgumentException if there was a problem getting the constructor
     */
    @Override
    protected Constructor<?> getConstructor(byte flag, Class<?> objectClass) throws IllegalArgumentException {
        try {
            return cachedConstructors.get(flag);
        } catch (IndexOutOfBoundsException ignore) {
            throw new IllegalArgumentException("Unknown predefined class index: " + flag);
        }
    }

    /**
     * Returns a factory method for the specified flag and object class.
     * If the object was serialized as one of the predefined classes, the cached
     * factory method is used.
     * For the {@link #DEFAULTCLASS_SERIALIZATION DEFAULTCLASS} serialization,
     * current factory method is returned. If the stored object implements the
     * {@link BinarySerializable} interface, its factory method is looked up.
     * Otherwise, <tt>null</tt> is returned.
     * 
     * @param flag the type of deserialization (see constants)
     * @param objectClass the class that is expected to be in the stream
     * @return a constructor for the specified flag and object class
     * @throws IllegalArgumentException if there was a problem getting the factory method
     */
    @Override
    protected Method getFactoryMethod(byte flag, Class<?> objectClass) throws IllegalArgumentException {
        try {
            return cachedFactoryMethods.get(flag);
        } catch (IndexOutOfBoundsException ignore) {
            throw new IllegalArgumentException("Unknown predefined class index: " + flag);
        }
    }

    /**
     * Returns the index of the predefined class for this object.
     * @param object the object whose class is looked up
     * @return the index of the predefined class for this object
     */
    @Override
    protected int getClassIndex(BinarySerializable object) {
        // Try the super check first
        int index = super.getClassIndex(object);
        if (index != CLASSNAME_SERIALIZATION)
            return index;

        // Cached class
        Integer position = cachedClasses.get(object.getClass());
        if (position != null)
            return position;

        // Other class
        if (! notCachedClasses.contains(object.getClass())) {
            log.log(Level.INFO, "Consider using cache for class {0}", object.getClass());
            notCachedClasses.add(object.getClass());
        }
        return CLASSNAME_SERIALIZATION;
    }

    /**
     * Returns the unmodifiable list of classes that are not cached but were serialized 
     *  by this serializator.
     */
    public Collection<Class<? extends BinarySerializable>> getNotCachedClasses() {
        return Collections.unmodifiableCollection(notCachedClasses);
    }

    //************************ Overridden methods ************************//

    /**
     * Returns a hash code value for this serializator. 
     * It is based on the serialVersionUIDs of the cached classes.
     * @return a hash code value for this serializator
     */
    @Override
    public int hashCode() {
        int value = super.hashCode();
        for (Class<?> cachedClass : cachedClasses.keySet())
            value ^= getSerialVersionUIDHash(cachedClass);
        return value;
    }

    /**
     * Indicates whether some other object is "equal to" this serializator.
     * In particular, the other object must be CachingSerializator and have
     * the same cached classes.
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this serializator is the same as the {@code obj}
     *          argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CachingSerializator))
            return false;
        if (!super.equals(obj))
            return false;
        CachingSerializator<?> castObj = (CachingSerializator<?>)obj;
        for (Class<? extends BinarySerializable> cachedClass : cachedClasses.keySet())
            if (!castObj.cachedClasses.containsKey(cachedClass))
                return false;
        return true;
    }


    //****************** Serialization ******************//

    /**
     * Read this serializator from the object stream
     * @param in the stream to read this serialized object data from
     * @throws IOException if there was a problem reading data from the stream
     * @throws ClassNotFoundException if there was an instance of an unknown class in the stream
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {        
        // Proceed with standard deserialization first
        in.defaultReadObject();

        // Restore cached constructors and factory methods
        cachedConstructors = new ArrayList<Constructor<?>>(cachedClasses.size());
        cachedFactoryMethods = new ArrayList<Method>(cachedClasses.size());
        notCachedClasses = new HashSet<Class<? extends BinarySerializable>>();

        // Get the list of classes sorted by position
        List<Class<? extends BinarySerializable>> classes = new ArrayList<Class<? extends BinarySerializable>>(cachedClasses.keySet());
        Collections.sort(classes, new Comparator<Class<? extends BinarySerializable>>() {
            @Override
            public int compare(Class<? extends BinarySerializable> o1, Class<? extends BinarySerializable> o2) {
                return cachedClasses.get(o1) - cachedClasses.get(o2);
            }
        });

        // Restore cache
        for (Class<? extends BinarySerializable> cls : classes)
            addToCache(cls);
    }
}
