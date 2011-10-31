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
package messif.objects;

import java.io.Serializable;

/**
 * Wrapper for {@link DistanceFunction} that computes the distances on
 * encapsulated objects of the {@link MetaObject}.
 * 
 * @param <T> the class of the encapsulated objects
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DistanceFunctionMetaSubobject<T> implements DistanceFunction<MetaObject>, Serializable {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Name of the encapsulated object on which the wrapped distance is computed */
    private final String subobjectName;
    /** Class of the encapsulated objects */
    private final Class<? extends T> subobjectClass;
    /** Wrapped distance function that computes the actual distance */
    private final DistanceFunction<? super T> subobjectDistanceFunction;

    /**
     * Creates a new instance of DistanceFunctionMetaSubobject.
     * @param subobjectName the name of the encapsulated objects on which the wrapped distance is computed
     * @param subobjectClass the class of the encapsulated objects on which the wrapped distance is computed
     * @param subobjectDistanceFunction the wrapped distance function that computes the actual distance
     */
    public DistanceFunctionMetaSubobject(String subobjectName, Class<? extends T> subobjectClass, DistanceFunction<? super T> subobjectDistanceFunction) {
        this.subobjectName = subobjectName;
        this.subobjectClass = subobjectClass;
        this.subobjectDistanceFunction = subobjectDistanceFunction;
    }

    /**
     * Creates a new instance of DistanceFunctionMetaSubobject.
     * The class of the encapsulated objects is derived from the distance function.
     * @param subobjectName the name of the encapsulated object on which the wrapped distance is computed
     * @param subobjectDistanceFunction the wrapped distance function that computes the actual distance
     */
    public DistanceFunctionMetaSubobject(String subobjectName, DistanceFunction<T> subobjectDistanceFunction) {
        this(subobjectName, subobjectDistanceFunction.getDistanceObjectClass(), subobjectDistanceFunction);
    }

    /**
     * Returns the encapsulated object for the distance function.
     * @param metaObject the meta object from which to get the encapsulated object
     * @return the encapsulated object for the distance function
     * @throws ClassCastException if the encapsulated object is not compatible with the {@link #subobjectDistanceFunction}
     */
    protected T getSubobject(MetaObject metaObject) throws ClassCastException {
        return subobjectClass.cast(metaObject.getObject(subobjectName));
    }

    /**
     * {@inheritDoc }
     * @throws ClassCastException if the encapsulated object is not compatible with the {@link #subobjectDistanceFunction}
     */
    @Override
    public float getDistance(MetaObject o1, MetaObject o2) throws ClassCastException {
        return subobjectDistanceFunction.getDistance(getSubobject(o1), getSubobject(o2));
    }

    @Override
    public Class<? extends MetaObject> getDistanceObjectClass() {
        return MetaObject.class;
    }
}
