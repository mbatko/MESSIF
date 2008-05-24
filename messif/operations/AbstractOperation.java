/*
 * AbstractOperation.java
 *
 * Created on 19. kveten 2004, 17:36
 */

package messif.operations;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import messif.utility.ErrorCode;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * The base class for all operations.
 * Operations allows to manipulate and query the data held by algorithms.
 * An algorithm is not required to provide implementation for each operation.
 * However, to support an operation, simply create a method with a specific operation
 * class as an argument. See {@link messif.algorithms.Algorithm} for more information.
 *
 * To ease the task of building interfaces, each operation should provide
 * an annotation {@link OperationName} to specify a user-friendly name of
 * the operation. Also the constructors should be annotated by {@link OperationConstructor}
 * to specify descriptors for its arguments. See {@link messif.utility.Convert#stringToType} for
 * the list of types that can be used in operation constructors.
 *
 * @see QueryOperation
 * @see messif.algorithms.Algorithm
 * @author  xbatko
 */
public abstract class AbstractOperation implements Serializable, Cloneable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Supplemental data object associated with this instance ******************/

    /** Supplemental data object */
    public Object suppData = null;

    //****************** Operation ID ******************/

    /** An universaly unique identification of the operation */
    protected final UUID operID = UUID.randomUUID();

    /**
     * Returns the current operation ID.
     * @return the current operation ID
     */
    public UUID getOperationID() {
        return operID;
    }

    /**
     * Returns a hash code value based on this operation ID.
     * @return a hash code value for this operation
     */
    @Override
    public int hashCode() {
        return operID.hashCode();
    }

    /**
     * Indicates whether another operation is equal to this operation.
     * Two operations are equal if their classes and IDs match.
     * Note that the clonned operations are equal as well as the
     * serialized and then deserialized operations.
     *
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!getClass().isInstance(obj))
            return false;
        return operID.equals(((AbstractOperation)obj).operID);
    }


    /****************** Equality driven by operation data ******************/

    /** 
     * Indicates whether some other operation has the same data as this one.
     * Override this method if the operation has its own data.
     *
     * @param   operation   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    public final boolean dataEquals(AbstractOperation operation) {
        if (operation == null)
            return false;
        Class<? extends AbstractOperation> thisClass = getClass();
        Class<? extends AbstractOperation> operationClass = operation.getClass();
        if (thisClass.equals(operationClass))
            return dataEqualsImpl(operation);
        if (thisClass.isAssignableFrom(operationClass))
            return operation.dataEqualsImpl(this);
        return false;
    }

    /** 
     * Indicates whether some other operation has the same data as this one.
     *
     * @param   operation   the reference object with which to compare.
     * @return  <code>true</code> if this object has the same data as the obj
     *          argument; <code>false</code> otherwise.
     */
    protected abstract boolean dataEqualsImpl(AbstractOperation operation);

    /**
     * Returns a hash code value for the data of this operation.
     * Override this method if the operation has its own data.
     * 
     * @return a hash code value for the data of this operation
     */
    public abstract int dataHashCode();

    /**
     * A wrapper class that allows to hash/equal abstract objects
     * using their data and not ID. Especially, standard hashing
     * structures (HashMap, etc.) can be used on wrapped object.
     */
    public static class DataEqualOperation {
        /** Encapsulated operation */
        protected final AbstractOperation operation;

        /**
         * Creates a new instance of DataEqualObject wrapper over the specified LocalAbstractObject.
         * @param operation the encapsulated object
         */
        public DataEqualOperation(AbstractOperation operation) {
            this.operation = operation;
        }

        /**
         * Returns the encapsulated operation.
         * @return the encapsulated operation
         */
        public AbstractOperation get() {
            return operation;
        }

        /**
         * Returns a hash code value for the operation data.
         * @return a hash code value for the data of this operation
         */
        @Override
        public int hashCode() {
            return operation.dataHashCode();
        }

        /** 
         * Indicates whether some other object has the same data as this operation.
         * @param   obj the reference object with which to compare.
         * @return  <code>true</code> if this object has the same data as the obj
         *          argument; <code>false</code> otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AbstractOperation)
                return operation.dataEquals((AbstractOperation)obj);
            else return false;
        }
    }


    //****************** Success flag ******************/

    /** Operation result code */
    protected ErrorCode errValue = ErrorCode.NOT_SET;

    /**
     * Returns <tt>true</tt> if this operation has finished successfuly.
     * Otherwise, <tt>false</tt> is returned - the operation was either unsuccessful or is has not finished yet.
     *
     * @return <tt>true</tt> if this operation has finished successfuly
     */
    public abstract boolean wasSuccessful();

    /**
     * Returns the result code of this operation.
     * @return the result code of this operation
     */
    public ErrorCode getErrorCode() {
        return errValue;
    }

    //****************** Operation finalizer ******************/

    /**
     * End operation with a specific error code.
     * @param errValue the error code to set
     * @throws IllegalArgumentException if the specified error value is <tt>null</tt> or {@link ErrorCode#NOT_SET}
     */
    public void endOperation(ErrorCode errValue) throws IllegalArgumentException {
        if (errValue == null || errValue.equals(ErrorCode.NOT_SET))
            throw new IllegalArgumentException("Can't finish operation - invalid error code specified");
        this.errValue = errValue;
    }

    /**
     * End operation successfully.
     */
    public abstract void endOperation();


    //****************** Clonning ******************/
    
    /**
     * Create a duplicate of this operation.
     * Check (and override) the implementation in subclasses if there are mutable object attributes.
     * Note that supplemental data ({@link #suppData}) is not clonned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    //****************** Data manipulation ******************/

    /**
     * Update all answer data of this operation from another operation.
     * This method is used to merge answers from multiple operations into one.
     * Every subclass that is adding some answer attributes should override this
     * method and define the behavior for that attributes.
     *
     * @param operation the source operation from which to get the update
     */
    public void updateAnswer(AbstractOperation operation) {
        if (!errValue.isSet() || !operation.wasSuccessful())
            errValue = operation.errValue;
    }

    
    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    public void clearSuplusData() {
        suppData = null;
    }


    //****************** String conversion ******************/

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(getClass().getSimpleName());
        if (!errValue.isSet())
            rtv.append(" has not finished yet");
        else if (wasSuccessful())
            rtv.append(" was successful");
        else rtv.append(" failed: ").append(errValue.toString());
        return rtv.toString();
    }


    //****************** Operation markers ******************/

    /**
     * Annotation that specifies operation user-friendly name.
     * It is used in auto-generated clients.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited()
    public @interface OperationName {
        /**
         * The name of the operation.
         * @return the name of the operation
         */
        String value();
    }

    /**
     * Annotation for operation constructors.
     * Each constructor, that should be accessible by auto-generated clients
     * must be annotated. Such constructor can only have parameters that can
     * be converted from a string by {@link messif.utility.Convert#stringToType stringToType}
     * method. Each constructor parameter should be annotated by a description
     * using this annotations values.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    public @interface OperationConstructor {
        /**
         * A list of descriptions for constructor parameters.
         * Each parameter should have a positionally-matching
         * descriptor value.
         * @return list of descriptions for constructor parameters
         */
        String[] value();
    }

    /**
     * Returns the name of operation represented by the provided class.
     * @param operationClass the operation class for which to get the name
     * @return the operation name
     */
    public static String getName(Class<? extends AbstractOperation> operationClass) {
        // Get OperationName annotation of the class
        OperationName annotation = operationClass.getAnnotation(OperationName.class);
        
        return (annotation != null)?annotation.value():null;
    }

    /**
     * Returns the name of this operation.
     * @return the name of this operation
     */
    public String getName() {
        return getName(getClass());
    }

    /**
     * Internal method for searching any annotated constructor in all the subclasses.
     *
     * @param operationClass class to search for the constructor in
     * @throws java.lang.IllegalArgumentException if either the operationClass is null or the class is not annotated using <code>AbstractOperation.OperationName</code>
     * @return appropripate constructor
     */
    private static Constructor getAnnotatedConstructor(Class<?> operationClass) throws IllegalArgumentException {
        return getAnnotatedConstructor(operationClass, -1);
    }

    /**
     * Internal method for searching the annotated constructor in all the subclasses that has appropriate number of arguments.
     *
     * @param operationClass class to search for the constructor in
     * @param nArguments number of arguments that the constructor must have
     * @return appropripate constructor
     * @throws IllegalArgumentException if either the operationClass is null or the class is not annotated by {@link OperationName}
     */
    private static Constructor getAnnotatedConstructor(Class<?> operationClass, int nArguments) throws IllegalArgumentException {
        // Ignore the classes that are not annotated by OperationName
        if (operationClass == null || !operationClass.isAnnotationPresent(OperationName.class))
            throw new IllegalArgumentException("There is no valid annotated constructor in '" + operationClass + "'");

        // Remember the constructor with smallest number of arguments if nArguments == -1
        int minimalConstructorArgs = Integer.MAX_VALUE;
        Constructor minimalConstructor = null;

        // Search all its constructors for proper annotation
        for (Constructor constructor : operationClass.getConstructors()) {
            if (constructor.isAnnotationPresent(OperationConstructor.class)) {
                int thisConstructorArgs = constructor.getParameterTypes().length;
                if (nArguments == -1) {
                    if (minimalConstructorArgs > thisConstructorArgs) {
                        minimalConstructor = constructor;
                        minimalConstructorArgs = thisConstructorArgs;
                    }
                } else {
                    if (thisConstructorArgs == nArguments) {
                        return constructor;
                    }
                }
            }
        }

        // Return the minimal constructor if found
        if (minimalConstructor != null)
            return minimalConstructor;

        // Recurse
        return getAnnotatedConstructor(operationClass.getSuperclass(), nArguments);
    }

    /**
     * Returns constructor argument descriptions for the provided operation class.
     * This is used by auto-generated clients to show descriptiron during operation creation.
     * @param operationClass class to get the descriptions for
     * @return constructor argument descriptions
     * @throws IllegalArgumentException if either the operationClass is null or the class is not annotated by {@link OperationName}
     */
    public static String[] getConstructorArgumentDescriptions(Class<? extends AbstractOperation> operationClass) throws IllegalArgumentException {
        return getAnnotatedConstructor(operationClass).getAnnotation(OperationConstructor.class).value();
    }

    /**
     * Returns constructor argument descriptions for the provided operation class with given number of arguments.
     * This is used by auto-generated clients to show descriptiron during operation creation.
     * @param operationClass class to get the descriptions for
     * @param nArguments the number of arguments of the constructor
     * @return constructor argument descriptions
     * @throws IllegalArgumentException if either the operationClass is null or the class is not annotated by {@link OperationName}
     */
    public static String[] getConstructorArgumentDescriptions(Class<? extends AbstractOperation> operationClass, int nArguments) throws IllegalArgumentException {
        return getAnnotatedConstructor(operationClass, nArguments).getAnnotation(OperationConstructor.class).value();
    }

    /**
     * Returns constructor argument types for the provided operation class.
     * @param operationClass class to get the constructor types for
     * @return constructor argument types
     * @throws IllegalArgumentException if either the operationClass is null or the class is not annotated by {@link OperationName}
     */
    public static Class<?>[] getConstructorArguments(Class<? extends AbstractOperation> operationClass) throws IllegalArgumentException {
        return getAnnotatedConstructor(operationClass).getParameterTypes();
    }

    /**
     * Returns constructor arguments for the provided operation class for an annotated constructor with given number of arguments.
     * @param operationClass class to get the constructor types for
     * @param nArguments the number of arguments of the constructor
     * @return constructor argument types
     * @throws IllegalArgumentException if either the operationClass is null or the class is not annotated by {@link OperationName}
     */
    public static Class<?>[] getConstructorArguments(Class<? extends AbstractOperation> operationClass, int nArguments) throws IllegalArgumentException {
        return getAnnotatedConstructor(operationClass, nArguments).getParameterTypes();
    }
    
    /**
     * Returns full constructor description for the provided operation class.
     * @param operationClass class to get the constructor types for
     * @return full constructor description
     * @throws IllegalArgumentException if either the operationClass is null or the class is not annotated by {@link OperationName}
     */
    public static String getConstructorDescription(Class<? extends AbstractOperation> operationClass) throws IllegalArgumentException {
        StringBuffer rtv = new StringBuffer();
        rtv.append(operationClass.getName());
        for (String argdesc : getConstructorArgumentDescriptions(operationClass))
            rtv.append(" <").append(argdesc).append(">");
        rtv.append("\n\t... ").append(getName(operationClass));
        
        return rtv.toString();
    }
    
    /**
     * Creates a new operation of the specified class.
     * @param operationClass the class of the operation that should be created
     * @param arguments arguments supplied to the constructor; they should match the types of getConstructorArguments(operationClass)
     * @return a new instance of operation
     * @throws IllegalArgumentException if the argument count or their types don't match the specified class
     * @throws InvocationTargetException if there was an exception in the operation's constructor
     */
    public static <E extends AbstractOperation> E createOperation(Class<E> operationClass, Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        // Create a new instance of the class for the specified constructor prototypes and arguments
        try {
            // We must search for the constructor again, because we need the constructor for the lower class not the ancestor's !!!
            return operationClass.getDeclaredConstructor(getConstructorArguments(operationClass, arguments.length)).newInstance(arguments);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns argument that was passed while constructing instance.
     * If the argument is not stored within operation, <tt>null</tt> is returned.
     * @param index index of an argument passed to constructor
     * @return argument that was passed while constructing instance
     * @throws IndexOutOfBoundsException if index parameter is out of range
     * @throws UnsupportedOperationException if this operation doesn't support construction argument retrieval
     */
    public Object getArgument(int index) throws IndexOutOfBoundsException, UnsupportedOperationException {
        throw new UnsupportedOperationException("This operation doesn't support construction argument retrieval");
    }

    /**
     * Returns number of arguments that were passed while constructing this instance.
     * Negative number is returned if this operation doesn't support construction argument retrieval.
     * @return number of arguments that were passed while constructing this instance
     */
    public int getArgumentCount() {
        return -1;
    }

}
