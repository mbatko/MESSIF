/*
 * ThresholdFunction.java
 *
 * Created on 21. cerven 2007, 16:44
 *
 */

package messif.objects;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import messif.utility.Convert;

/**
 * The predecessor class of the similarity aggregate functions.
 *
 * @author xbatko
 */
public abstract class ThresholdFunction implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /**
     * Computes the value of the aggregate distance from the provided sub-distances.
     * @param distances the distances in respective descriptors
     * @return the aggregate distance
     */
    public abstract float compute(float... distances);

    /**
     * Returns the names of distance parameters (i.e. the descriptor names) for the {@link #compute} function.
     *
     * @return the list of parameter (descriptor) names of the {@link #compute} function.
     */
    public abstract String[] getParameterNames();


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
            descriptorDistances = new float[getParameterNames().length];

        // Compute the array of distances in respective descriptors
        int paramIndex = 0;
        for (String descriptorName : getParameterNames()) {
            LocalAbstractObject descriptorObject1 = object1.getObject(descriptorName);
            LocalAbstractObject descriptorObject2 = object2.getObject(descriptorName);

            if (descriptorObject1 == null || descriptorObject2 == null)
                descriptorDistances[paramIndex] = LocalAbstractObject.MAX_DISTANCE;
            else descriptorDistances[paramIndex] = descriptorObject1.getDistance(descriptorObject2);
            paramIndex++;
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
    public float getDistance(MetaObject object1, MetaObject object2) {
        return getDistance(object1, object2, null);
    }


    /****************** Factory method ******************/

    /** The constructor of the threshold function for the factory method */
    private static Constructor<? extends ThresholdFunction> thresholdFunctionFactoryConstructor = null;
    static { // Initializer for the thresholdFunctionFactoryConstructor
        try {
            Class<ThresholdFunction> factoryClass = Convert.getClassForName(
                    System.getProperty("messif.objects.thresholdFunction.factoryClass", "messif.objects.impl.ThresholdFunctionSimpleEvaluator"),
                    ThresholdFunction.class
            );
            setFactoryClass(factoryClass);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Factory method for creating threshold functions from strings.
     * 
     * @param string the theshold function (using standard expression syntax)
     * @throws NoSuchMethodException if there is no threshold function class set by {@link #setFactoryClass} yet
     * @throws InvocationTargetException if the parsing of the threshold function has thrown an exception
     * @return a new instance of <code>ThresholdFunction</code>
     */
    public static ThresholdFunction valueOf(String string) throws NoSuchMethodException, InvocationTargetException {
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
    public static void setFactoryClass(Class<? extends ThresholdFunction> factoryClass) throws NoSuchMethodException {
        if (Modifier.isAbstract(factoryClass.getModifiers()))
            throw new NoSuchMethodException("Class " + factoryClass.getName() + " is abstract");
        thresholdFunctionFactoryConstructor = factoryClass.getConstructor(String.class);
    }

    /**
     * Returns the class currently set for the factory method {@link #valueOf}.
     * @return the class currently set for the factory method {@link #valueOf}
     */
    public static Class<? extends ThresholdFunction> getFactoryClass() {
        return (thresholdFunctionFactoryConstructor == null)?null:thresholdFunctionFactoryConstructor.getDeclaringClass();
    }
}
