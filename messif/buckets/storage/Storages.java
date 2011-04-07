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
package messif.buckets.storage;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import messif.utility.Convert;
import messif.utility.reflection.MethodInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;


/**
 * Collection of utility methods for {@link Storage}s.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class Storages {

    //****************** Factory method ******************//
    
    /**
     * Creates a storage using factory method.
     * A factory method of the following prototype (which every storage class should implement) is used:
     * <pre>
     *      public static AStorageType create(Class storedObjectsClass, Map&lt;String, Object&gt; parameters)
     * </pre>
     * The parameters map is filled with storage-specific instantiation values.
     * 
     * @param <T> the class of objects that the new storage will work with
     * @param <S> the class of the storage to create
     * @param storageClass the class of the storage to create
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param parameters list of named parameters for the storage to create
     * @return a new storage instance
     * @throws IllegalArgumentException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    public static <T, S extends Storage<T>> S createStorage(Class<? extends S> storageClass, Class<T> storedObjectsClass, Map<String, Object> parameters) throws IllegalArgumentException, ClassNotFoundException {
        try {
            return MethodInstantiator.callFactoryMethod(storageClass, "create", false, true, null, storedObjectsClass, (Object)parameters);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (NoSuchInstantiatorException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getCause().toString());
        }        
    }

    /**
     * Creates a storage using factory method.
     * A factory method of the following prototype (which every storage class should implement) is used:
     * <pre>
     *      public static AStorageType create(Class storedObjectsClass, Map&lt;String, Object&gt; parameters)
     * </pre>
     * The parameters map is filled with storage-specific instantiation values.
     * 
     * @param <T> the class of objects that the new storage will work with
     * @param <S> the class of the storage to create
     * @param storageClassToCheck the super-class of the storage to create (i.e. {@link Storage})
     * @param storedObjectsClass the class of objects that the new storage will work with
     * @param storageClassParamName the name of the parameter where the storage class is stored
     * @param parameters list of named parameters for the storage to create, it must contain at least
     *      the {@code storageClass} parameter that specifies the storage class to create
     * @return a new storage instance
     * @throws IllegalArgumentException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    public static <T, S extends Storage<T>> S createStorageClassParameter(Class<T> storedObjectsClass, Map<String, Object> parameters, String storageClassParamName, Class<? extends S> storageClassToCheck) throws IllegalArgumentException, ClassNotFoundException {
        if (parameters == null)
            throw new IllegalArgumentException("No parameters specified");

        // Retrieve class from parameters
        Class<? extends S> storageClass = null;
        try {
            storageClass = Convert.genericCastToClass(parameters.get(storageClassParamName), storageClassToCheck);
        } catch (ClassCastException classCastException) {
            storageClass = Convert.getClassForName((String) parameters.get(storageClassParamName), storageClassToCheck);
        }

        return createStorage(storageClass, storedObjectsClass, parameters);
    }

    
}
