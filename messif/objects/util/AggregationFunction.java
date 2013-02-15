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
package messif.objects.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.utility.Convert;

/**
 * The predecessor class of the similarity aggregate functions.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class AggregationFunction implements DistanceFunction<MetaObject>, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Aggregation function evaluation ******************//

    /**
     * Returns the number of distance parameters (i.e. the descriptor names) for the {@link #compute} function.
     * @return the number of parameter (descriptor) names of the {@link #compute} function
     */
    public abstract int getParameterCount();

    /**
     * Returns the name of the {@code index}th distance parameter (i.e. the descriptor name) for the {@link #compute} function.
     * @param index the index of the parameter for which to get the name
     * @return the name of the {@code index}th distance parameter (i.e. the descriptor name) for the {@link #compute} function
     * @throws IndexOutOfBoundsException if the index is smaller than zero or greater than or equal to {@link #getParameterCount()}
     */
    public abstract String getParameterName(int index) throws IndexOutOfBoundsException;

    /**
     * Returns the maximal distance for the specified parameter of the {@link #compute} function.
     * This method returns the {@link LocalAbstractObject#MAX_DISTANCE} by default.
     * @param parameterIndex the index of a parameter (corresponds to the index of the
     *          parameter name as given by {@link #getParameterName(int)})
     * @return the maximal distances for the parameters of the {@link #compute} function
     * @throws IndexOutOfBoundsException if the specified parameter index is not valid
     */
    public float getParameterMaximalDistance(int parameterIndex) throws IndexOutOfBoundsException {
        return LocalAbstractObject.MAX_DISTANCE;
    }

    /**
     * Computes the value of the aggregate distance from the provided sub-distances.
     * The <code>distances</code> array items must correspond with the parameter
     * names as returned by {@link #getParameterName(int)}.
     * @param distances the distances in respective descriptors
     * @return the aggregate distance
     */
    public abstract float compute(float... distances);


    //****************** Distance evaluation ******************//

    /**
     * Computes distance of two meta objects using this combination function.
     * @param descriptorDistances array that will be filled with distances of the respective sub-distances;
     *        i.e. the distance between object1.getObject(parameterName[0]) and object2.getObject(parameterName[0])
     *        will be stored in the first array component, etc.
     * @param object1 the one meta object to compute distance for
     * @param object2 the other meta object to compute distance for
     * @return the distance between object1 and object1 using this combination function
     */
    public float getDistance(MetaObject object1, MetaObject object2, float[] descriptorDistances) {
        // Allocate descriptor distance if not provided
        if (descriptorDistances == null)
            descriptorDistances = new float[getParameterCount()];

        // Compute the array of distances in respective descriptors
        for (int paramIndex = 0; paramIndex < getParameterCount(); paramIndex++) {
            String descriptorName = getParameterName(paramIndex);
            LocalAbstractObject descriptorObject1 = object1.getObject(descriptorName);
            LocalAbstractObject descriptorObject2 = object2.getObject(descriptorName);

            if (descriptorObject1 == null || descriptorObject2 == null)
                descriptorDistances[paramIndex] = getParameterMaximalDistance(paramIndex);
            else descriptorDistances[paramIndex] = descriptorObject1.getDistance(descriptorObject2);
        }

        // Compute overall distance
        return compute(descriptorDistances);
    }

    /**
     * Computes distance of two meta objects using this combination function.
     * @param object1 the one meta object to compute distance for
     * @param object2 the other meta object to compute distance for
     * @return the distance between object1 and object1 using this combination function
     */
    @Override
    public float getDistance(MetaObject object1, MetaObject object2) {
        return getDistance(object1, object2, null);
    }

    @Override
    public Class<? extends MetaObject> getDistanceObjectClass() {
        return MetaObject.class;
    }


    //****************** Factory method ******************//

    /** The constructor of the threshold function for the factory method */
    private static Constructor<? extends AggregationFunction> thresholdFunctionFactoryConstructor = null;
    static { // Initializer for the thresholdFunctionFactoryConstructor
        try {
            Class<AggregationFunction> factoryClass = Convert.getClassForName(
                    System.getProperty("messif.objects.util.aggregationFunction.factoryClass", "messif.objects.util.impl.AggregationFunctionEvaluator"),
                    AggregationFunction.class
            );
            setFactoryClass(factoryClass);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Factory method for creating threshold functions from strings.
     * 
     * @param string the threshold function (using standard expression syntax)
     * @throws NoSuchMethodException if there is no threshold function class set by {@link #setFactoryClass} yet
     * @throws InvocationTargetException if the parsing of the threshold function has thrown an exception
     * @return a new instance of <code>AggregationFunction</code>
     */
    public static AggregationFunction valueOf(String string) throws NoSuchMethodException, InvocationTargetException {
        if (thresholdFunctionFactoryConstructor == null)
            throw new NoSuchMethodException("The factory class for the threshold function was not specified");
        try {
            return thresholdFunctionFactoryConstructor.newInstance(string);
        } catch (IllegalArgumentException e) {
            // This should never happen
            throw new NoSuchMethodException("The factory class for the threshold function was not specified: " + e);
        } catch (InstantiationException e) {
            // This should never happen
            throw new NoSuchMethodException("The factory class for the threshold function was not specified: " + e);
        } catch (IllegalAccessException e) {
            // This should never happen
            throw new NoSuchMethodException("The factory class for the threshold function was not specified: " + e);
        }
    }

    /**
     * Sets the class created by factory method {@link #valueOf}.
     * 
     * @param factoryClass the new class for factory method
     * @throws NoSuchMethodException if the specified class is abstract or lacks a public constructor with one <code>String</code> argument
     */
    public static void setFactoryClass(Class<? extends AggregationFunction> factoryClass) throws NoSuchMethodException {
        if (Modifier.isAbstract(factoryClass.getModifiers()))
            throw new NoSuchMethodException("Class " + factoryClass.getName() + " is abstract");
        thresholdFunctionFactoryConstructor = factoryClass.getConstructor(String.class);
    }

    /**
     * Returns the class currently set for the factory method {@link #valueOf}.
     * @return the class currently set for the factory method {@link #valueOf}
     */
    public static Class<? extends AggregationFunction> getFactoryClass() {
        return (thresholdFunctionFactoryConstructor == null)?null:thresholdFunctionFactoryConstructor.getDeclaringClass();
    }
}
