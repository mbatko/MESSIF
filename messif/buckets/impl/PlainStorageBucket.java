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
package messif.buckets.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import messif.buckets.LocalBucket;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.storage.StorageIndexed;
import messif.objects.LocalAbstractObject;
import messif.utility.Convert;
import messif.utility.reflection.MethodInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Encapsulating bucket for a plain storage.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class PlainStorageBucket extends LocalBucket {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Attributes ******************//

    /** Internal index with encapsulated storage */
    private final ModifiableIndex<LocalAbstractObject> index;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of PlainStorageBucket.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param index the index to encapsulate
     */
    public PlainStorageBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, ModifiableIndex<LocalAbstractObject> index) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
        this.index = index;
    }

    @Override
    public void finalize() throws Throwable {
        index.finalize();
        super.finalize();
    }

    @Override
    public void destroy() throws Throwable {
        index.destroy();
        super.destroy();
    }
    
    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        return index;
    }


    //****************** Factory method ******************//
    
    /**
     * Creates a bucket. The additional parameters are specified in the parameters map with
     * the following recognized key names:
     * <ul>
     *   <li><em>storageClass</em> - the class of the storage that this bucket operates on (must implement {@link StorageIndexed})</li>
     * </ul>
     * <p>
     * Note that additional parameters may be required according to the specified <em>storageClass</em>.
     * See the documentation of that storage.
     * </p>
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters (see above)
     * @return a new SimpleDiskBucket instance
     * @throws IOException if something goes wrong when working with the filesystem
     * @throws IllegalArgumentException if the parameters specified are invalid (non existent directory, null values, etc.)
     * @throws ClassNotFoundException if the parameter <em>class</em> could not be resolved or is not a descendant of LocalAbstractObject
     */
    public static PlainStorageBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IOException, IllegalArgumentException, ClassNotFoundException {
        if (parameters == null)
            throw new IllegalArgumentException("No parameters specified");

        try {
            // Create storage - retrieve class from parameter and use "create" factory method
            Class<? extends StorageIndexed> storageClass = null;
            try {
                storageClass = Convert.genericCastToClass(parameters.get("storageClass"), StorageIndexed.class);
            } catch (ClassCastException classCastException) {
                storageClass = Convert.getClassForName((String) parameters.get("storageClass"), StorageIndexed.class);
            }
            @SuppressWarnings("unchecked")
            StorageIndexed<LocalAbstractObject> storage = MethodInstantiator.callFactoryMethod(storageClass, "create", false, true, null, LocalAbstractObject.class, (Object)parameters);

            // Create the comparator from the parameters, encapsulate it with an index and then by the virtual bucket
            return new PlainStorageBucket(capacity, softCapacity, lowOccupation, occupationAsBytes, storage);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (NoSuchInstantiatorException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getCause().toString());
        }
    }

}
