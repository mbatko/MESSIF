/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * Provides various utility methods for {@link CollectionProvider} and
 * {@link CollectionMapProvider} interfaces.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class CollectionProviders {
    /**
     * Cast the given object to {@link CollectionProvider} with a generic-safe
     * type of objects.
     * @param <T> the class of objects stored in the collections
     * @param object the object to cast
     * @param objectsType the class of objects stored in the collections
     * @param throwException flag determining whether the ClassCastException is thrown
     *          (<tt>true</tt>) or <tt>null</tt> is returned (<tt>false</tt>), if the
     *          given {@code object} cannot be cast
     * @return the type-safe cast of {@code object}
     * @throws ClassCastException if the given object is not {@link CollectionProvider}
     *          or the class of objects stored in the collections is not {@code objectsType}
     */
    public static <T> CollectionProvider<T> castCollectionProvider(Object object, Class<? extends T> objectsType, boolean throwException) throws ClassCastException {
        if (object == null || (!throwException && !(object instanceof CollectionProvider)))
            return null;
        @SuppressWarnings("unchecked")
        CollectionProvider<T> rtv = (CollectionProvider<T>)object; // This cast IS checked on the next line
        if (!objectsType.isAssignableFrom(rtv.getCollectionValueClass())) {
            if (throwException)
                throw new ClassCastException("Collection provider " + object + " does not provide collections with " + objectsType.getName());
            else
                return null;
        }                
        return rtv;
    }

    /**
     * Returns the number of collections in the given object.
     * If the object implements the {@link CollectionProvider}, the actual
     * collection count is returned. Otherwise, a zero is returned.
     * @param object the object the number of collections to get
     * @return the number of collections
     */
    public static int getCollectionCount(Object object) {
        if (object instanceof CollectionProvider) {
            return ((CollectionProvider<?>)object).getCollectionCount();
        } else {
            return 0;
        }        
    }

    /**
     * Returns the collection with the given index from the given object.
     * The object must implement {@link CollectionProvider} interface with
     * the compatible {@code objectsType}.
     * Note that the returned collection (typically) cannot be modified.
     * @param <T> the class of objects stored in the collections
     * @param object the object the collection of which to get
     * @param index the index of the collection to return
     * @param objectsType the class of objects stored in the collections
     * @param throwException flag determining whether the ClassCastException is thrown
     *          (<tt>true</tt>) or <tt>null</tt> is returned (<tt>false</tt>), if the
     *          given {@code object} cannot be cast to compatible {@link CollectionProvider}
     * @return the collection with the given index
     * @throws IndexOutOfBoundsException if the given index is negative or
     *          greater or equal to {@link #getCollectionCount(java.lang.Object)}
     * @throws ClassCastException if the given object is not {@link CollectionProvider}
     *          or the class of objects stored in the collections is not {@code objectsType}
     */
    public static <T> Collection<T> getCollection(Object object, int index, Class<? extends T> objectsType, boolean throwException) throws IndexOutOfBoundsException, ClassCastException {
        CollectionProvider<T> ret = castCollectionProvider(object, objectsType, throwException);
        if (ret == null)
            return null;
        return ret.getCollection(index);
    }

    /**
     * Returns the iterator of the collection with the given index from the given object.
     * The object must implement {@link CollectionProvider} interface with
     * the compatible {@code objectsType}.
     * @param <T> the class of objects returned by the iterator
     * @param object the object the collection iterator of which to get
     * @param index the index of the collection iterator to return
     * @param objectsType the class of objects returned by the iterator
     * @param throwException flag determining whether the ClassCastException is thrown
     *          (<tt>true</tt>) or <tt>null</tt> is returned (<tt>false</tt>), if the
     *          given {@code object} cannot be cast to compatible {@link CollectionProvider}
     * @return the iterator of the collection with the given index
     * @throws IndexOutOfBoundsException if the given index is negative or
     *          greater or equal to {@link #getCollectionCount(java.lang.Object)}
     * @throws ClassCastException if the given object is not {@link CollectionProvider}
     *          or the class of objects stored in the collections is not {@code objectsType}
     */
    public static <T> Iterator<T> getCollectionIterator(Object object, int index, Class<? extends T> objectsType, boolean throwException) throws IndexOutOfBoundsException, ClassCastException {
        Collection<T> collection = getCollection(object, index, objectsType, throwException);
        return collection == null ? null : collection.iterator();
    }

    /**
     * Returns the iterator of the collection with the given index from the given object.
     * The object must implement {@link CollectionProvider} interface with
     * the compatible {@code objectsType}.
     * @param <T> the class of objects returned by the iterator
     * @param object the object the collection iterator of which to get
     * @param index the index of the collection iterator to return
     * @param objectsType the class of objects returned by the iterator
     * @return the iterator of the collection with the given index
     * @throws IndexOutOfBoundsException if the given object is not {@link CollectionProvider},
     *          the class of objects stored in the collections is not {@code objectsType},
     *          the given index is negative, or the index is greater or equal
     *          to {@link #getCollectionCount(java.lang.Object)}
     */
    public static <T> Iterator<T> getCollectionIterator(Object object, int index, Class<? extends T> objectsType) throws IndexOutOfBoundsException {
        Collection<T> collection = getCollection(object, index, objectsType, false);
        if (collection == null)
            throw new IndexOutOfBoundsException("Collection provider for " + objectsType + " is not implemented by " + object.getClass());
        return collection.iterator();
    }

    /**
     * Returns the collection with the given key from the given object.
     * The object must implement {@link CollectionMapProvider} interface with
     * the compatible {@code objectsType}.
     * Note that the returned collection (typically) cannot be modified.
     * @param <K> the type of keys used for accessing the collections
     * @param <T> the class of objects stored in the collections
     * @param object the object the collection of which to get
     * @param key the key of the collection to return
     * @param objectsType the class of objects stored in the collections
     * @param throwException flag determining whether the ClassCastException is thrown
     *          (<tt>true</tt>) or <tt>null</tt> is returned (<tt>false</tt>), if the
     *          given {@code object} cannot be cast to compatible {@link CollectionMapProvider}
     * @return the collection with the given key
     * @throws ClassCastException if the given object is not {@link CollectionMapProvider}
     *          or the class of objects stored in the collections is not {@code objectsType}
     */
    public static <K, T> Collection<T> getCollectionByKey(Object object, K key, Class<? extends T> objectsType, boolean throwException) throws ClassCastException {
        @SuppressWarnings("unchecked")
        CollectionMapProvider<K, T> collectionMapProvider = (CollectionMapProvider<K, T>)castCollectionProvider(object, objectsType, throwException); // This cast IS checked on the next line
        if (collectionMapProvider == null)
            return null;
        if (!collectionMapProvider.getCollectionKeyClass().isInstance(key))
            throw new ClassCastException("Collection provider requires " + collectionMapProvider.getCollectionKeyClass() + " as key but " + key.getClass() + " was provided");
        return collectionMapProvider.getCollectionByKey(key);
    }

    /**
     * Returns the iterator of the collection with the given key from the given object.
     * The object must implement {@link CollectionMapProvider} interface with
     * the compatible {@code objectsType}.
     * @param <K> the type of keys used for accessing the collections
     * @param <T> the class of objects returned by the iterator
     * @param object the object the collection iterator of which to get
     * @param key the key of the collection iterator to return
     * @param objectsType the class of objects returned by the iterator
     * @param throwException flag determining whether the ClassCastException is thrown
     *          (<tt>true</tt>) or <tt>null</tt> is returned (<tt>false</tt>), if the
     *          given {@code object} cannot be cast to compatible {@link CollectionMapProvider}
     * @return the iterator of the collection with the given index
     * @throws ClassCastException if the given object is not {@link CollectionMapProvider}
     *          or the class of objects stored in the collections is not {@code objectsType}
     */
    public static <K, T> Iterator<T> getCollectionByKeyIterator(Object object, K key, Class<? extends T> objectsType, boolean throwException) throws ClassCastException {
        Collection<T> collection = getCollectionByKey(object, key, objectsType, throwException);
        return collection == null ? null : collection.iterator();
    }

}
