/*
 * SplitPolicy.java
 *
 * Created on 6. listopad 2007, 11:07
 *
 */

package messif.buckets.split;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.buckets.LocalBucket;
import messif.objects.BallRegion;
import messif.objects.util.ObjectMatcher;

/**
 * This class defines an abstract policy for bucket splitting.
 *
 * The policy is fully defined by the internal parameters, e.g. a policy for ball partitioning
 * requires a pivot and a radius. Each policy implements the {@link messif.objects.util.ObjectMatcher object matcher}
 * that decides the target partition of a given object according to this policy.
 *
 * Some (or all) parameters can be unspecified when the policy is instantiated and can be provided through
 * {@link #setParameter} method later. However, the <code>match</code> method returns
 * {@link UnsupportedOperationException} until all the parameters are fully specified, i.e.
 * {@link #isComplete} returns <tt>true</tt>).
 *
 * If desired, some parameters can be locked, so they can't be changed anymore.
 *
 * @author xbatko
 */
public abstract class SplitPolicy implements ObjectMatcher {

    /** The table of annotated parameters of this split policy */
    private Map<String, Parameter> parameters;

    /**
     * Creates a new instance of SplitPolicy.
     * All policy parameters are neither locked nor filled.
     */
    protected SplitPolicy() {
        parameters = new HashMap<String, Parameter>();
        for (Field field : getClass().getDeclaredFields()) {
            ParameterField fieldAnnotation = field.getAnnotation(ParameterField.class);
            if (fieldAnnotation != null)
                parameters.put(fieldAnnotation.value(), new Parameter(field));
        }
    }


    /**
     * Returns the group (partition) to which the whole ball region belongs.
     * Returns -1 if not all objects from the specified ball region fall into just one partition
     * or if this policy cannot decide. In that case, the ball region must be searched one object by one
     * using the {@link #match} method.
     *
     * @param region a ball region that is tested for the matching condition
     * @return the group (partition) to which the whole ball region belongs or -1 if it is uncertain
     */
    public abstract int match(BallRegion region);

    /**
     * Returns the {@link BucketBallRegion} associated with the specified bucket.
     * It must be registered in the bucket as filter, otherwise a NoSuchElementException is thrown.
     *
     * @param bucket the bucket to get the region for
     * @return the {@link BucketBallRegion} associated with bucket
     * @throws NoSuchElementException if there was no ball region associated
     */
    protected BucketBallRegion getBucketBallRegion(LocalBucket bucket) throws NoSuchElementException {
        try {
            return bucket.getFilter(BucketBallRegion.class);
        } catch (ClassCastException e) {
            throw new NoSuchElementException("Provided bucket is not an instance of LocalFilteredBucket");
        }
    }


    /**
     * Returns the group (partition) to which the whole bucket belongs.
     * Returns -1 if not all objects from the specified bucket fall into just one partition
     * or if this policy cannot decide. In that case, the bucket must be searched one object by one
     * using the {@link #match} method.
     * If there is no {@link BucketBallRegion} registered in the bucket, -1 is returned.
     *
     * @param bucket a bucket that is tested for the matching condition
     * @return the group (partition) to which the whole bucket belongs or -1 if it is uncertain
     */
    public int match(LocalBucket bucket) {
        try {
            return match(bucket.getFilter(BucketBallRegion.class));
        } catch (ClassCastException e) {
            return -1;
        } catch (NoSuchElementException e) {
            return -1;
        }
    }


    /****************** Parameter handling ******************/

    /**
     * Returns <tt>true</tt> if this policy has all the arguments necessary for a split defined.
     * Otherwise, some arguments are missing (depends on the specific policy type).
     * 
     * @return <tt>true</tt> if this policy has all the arguments necessary for a split defined
     */
    public boolean isComplete() {
        for (Parameter parameter : parameters.values())
            if (!parameter.filled)
                return false;
        return true;
    }

    /**
     * Returns all parameter names for this policy.
     * @return all parameter names for this policy
     */
    public Set<String> getParameterNames() {
        return Collections.unmodifiableSet(parameters.keySet());
    }

    /**
     * Use this method to set the policy parameter.
     * @param parameter the name of the policy parameter
     * @param value new value for the parameter
     * @throws IllegalStateException if the specified parameter is locked
     * @throws NoSuchElementException if there is no parameter for the specified name
     * @throws NullPointerException if the specified value is <tt>null</tt>
     */
    public void setParameter(String parameter, Object value) throws IllegalStateException, NoSuchElementException, NullPointerException {
        Parameter paramInfo = parameters.get(parameter);
        if (paramInfo == null)
            throw new NoSuchElementException("There is no parameter '" + parameter + "' in split policy " + getClass().getSimpleName());
        if (paramInfo.locked)
            throw new IllegalStateException("The parameter '" + parameter + "' is locked and cannot be changed");
        if (value == null)
            throw new NullPointerException("The value for parameter '" + parameter + "' is null");
        try {
            paramInfo.field.set(this, value);
            paramInfo.filled = true;
        } catch (IllegalAccessException e) {
            // this should never happen
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the value of the specified parameter.
     * @param parameter the name of the policy parameter
     * @return the value of the specified parameter
     * @throws IllegalStateException if the parameter is not filled yet
     * @throws NoSuchElementException if there is no parameter for the specified name
     */
    public Object getParameter(String parameter) throws IllegalStateException, NoSuchElementException {
        Parameter paramInfo = parameters.get(parameter);
        if (paramInfo == null)
            throw new NoSuchElementException("There is no parameter '" + parameter + "' in split policy " + getClass().getSimpleName());
        if (!paramInfo.filled)
            throw new IllegalStateException("The parameter '" + parameter + "' has no value set yet");
        try {
            return paramInfo.field.get(this);
        } catch (IllegalArgumentException e) {
            // this should never happen
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            // this should never happen
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns <tt>true</tt> if the specified parameter is locked.
     * @param parameter the name of the policy parameter
     * @return <tt>true</tt> if the specified parameter is locked.
     * @throws NoSuchElementException if there is no parameter for the specified name
     */
    public boolean isParameterLocked(String parameter) throws NoSuchElementException {
        Parameter paramInfo = parameters.get(parameter);
        if (paramInfo == null)
            throw new NoSuchElementException("There is no parameter '" + parameter + "' in split policy " + getClass().getSimpleName());
        return paramInfo.locked;
    }

    /**
     * Locks the specified policy parameter.
     * Once locked, a parameter can't be changed by {@link #setParameter} method anymore.
     *
     * @param parameter the name of the policy parameter
     * @throws NoSuchElementException if there is no parameter for the specified name
     */
    public void lockParameter(String parameter) throws NoSuchElementException {
        Parameter paramInfo = parameters.get(parameter);
        if (paramInfo == null)
            throw new NoSuchElementException("There is no parameter '" + parameter + "' in split policy " + getClass().getSimpleName());
        paramInfo.locked = true;
    }

    /**
     * Returns <tt>true</tt> if the specified parameter has a value set.
     * That is, if the {@link #setParameter} was called for the specified parameter.
     * @param parameter the name of the policy parameter
     * @return <tt>true</tt> if the specified parameter is locked.
     * @throws NoSuchElementException if there is no parameter for the specified name
     */
    public boolean isParameterFilled(String parameter) throws NoSuchElementException {
        Parameter paramInfo = parameters.get(parameter);
        if (paramInfo == null)
            throw new NoSuchElementException("There is no parameter '" + parameter + "' in split policy " + getClass().getSimpleName());
        return paramInfo.filled;
    }

    /**
     * Returns the type of the specified policy parameter.
     * @param parameter the name of the policy parameter
     * @return the type of the specified policy parameter
     * @throws NoSuchElementException if there is no parameter for the specified name
     */
    public Class<?> getParameterType(String parameter) throws NoSuchElementException {
        Parameter paramInfo = parameters.get(parameter);
        if (paramInfo == null)
            throw new NoSuchElementException("There is no parameter '" + parameter + "' in split policy " + getClass().getSimpleName());
        return paramInfo.field.getType();
    }

    /**
     * Returns the number of partitions of this policy.
     * @return the number of partitions of this policy
     */
    public abstract int getPartitionsCount();

    /**
     * Annotation of split policy parameter field.
     * Each policy parameter attribute of a {@link SplitPolicy} descendant
     * class must be marked with this annotation. Otherwise, the parameter
     * setter/getter methods will not work.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface ParameterField {
        /**
         * The name of the policy parameter stored in this field.
         * @return name of the policy parameter stored in this field
         */
        String value();
    }

    /** This class defines a policy parameter */
    private static class Parameter {
        /** The field of this policy that holds the parameter */
        private final Field field;
        /** Locked flag */
        private boolean locked = false;
        /** Filled flag */
        private boolean filled = false;

        /**
         * Creates a new instance of Parameter.
         * @param field the field upon which this parameter operates
         */
        private Parameter(Field field) {
            this.field = field;
            field.setAccessible(true);
        }
    }

}
