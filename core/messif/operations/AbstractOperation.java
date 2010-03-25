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
import messif.utility.Clearable;
import messif.utility.Convert;


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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class AbstractOperation implements Serializable, Cloneable, Clearable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Supplemental data object associated with this instance ******************//

    /** Supplemental data object */
    public Object suppData = null;

    //****************** Operation ID ******************//

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
    public final int hashCode() {
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
    public final boolean equals(Object obj) {
        if (!getClass().isInstance(obj))
            return false;
        return operID.equals(((AbstractOperation)obj).operID);
    }


    //****************** Equality driven by operation data ******************//

    /** 
     * Indicates whether some other operation has the same data as this one.
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


    //****************** Success flag ******************//

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

    /**
     * Returns <tt>true</tt> if this operation has finished its processing - either successfully or unsuccessfully.
     * In other words, <tt>true</tt> is returned if the error code of this operation is set.
     * If the operation has not finished yet (i.e. the error code is {@link ErrorCode#NOT_SET not set},
     * <tt>false</tt> is returned.
     * @return <tt>true</tt> if this operation has finished its processing
     */
    public boolean isFinished() {
        return errValue.isSet();
    }


    //****************** Operation finalizer ******************//

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


    //****************** Clonning ******************//
    
    /**
     * Create a duplicate of this operation.
     * Check (and override) the implementation in subclasses if there are mutable object attributes.
     * Note that supplemental data ({@link #suppData}) is not clonned.
     *
     * @return a clone of this operation
     * @throws CloneNotSupportedException if the operation instance cannot be cloned
     */
    @Override
    public AbstractOperation clone() throws CloneNotSupportedException {
        return (AbstractOperation)super.clone();
    }


    //****************** Data manipulation ******************//

    /**
     * Update the error code of this operation from another operation.
     * @param operation the source operation from which to get the update
     * @throws ClassCastException if the specified operation is incompatible with this operation
     */
    public void updateFrom(AbstractOperation operation) throws ClassCastException {
        if (!errValue.isSet() || !operation.wasSuccessful())
            errValue = operation.errValue;
    }

    
    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    public void clearSurplusData() {
        suppData = null;
    }


    //****************** String conversion ******************//

    /**
     * Appends the constructor arguments of this query to the specified string.
     * Arguments are enclosed in &lt; and &gt; and separated by commas.
     * They are added in the same order as in the constructor.
     * Leading space is added.
     * @param str the string to add the arguments to
     */
    protected void appendArguments(StringBuilder str) {
        if (getArgumentCount() <= 0)
            return;
        str.append(" <");
        str.append(getArgument(0));
        for (int i = 1; i < getArgumentCount(); i++)
            str.append(", ").append(getArgument(i));
        str.append('>');
    }

    /**
     * Appends the error code of this query to the specified string.
     * If the error code is not set yet, the "has not finished yet" string is added.
     * If the operation {@link #wasSuccessful() was successful}, the "was successful" string is added.
     * Otherwise, a string "failed: " with the error code name is added.
     * Leading space is added.
     * @param str the string to add the error code to
     */
    protected void appendErrorCode(StringBuilder str) {
        if (!errValue.isSet())
            str.append(" has not finished yet");
        else if (wasSuccessful())
            str.append(" was successful");
        else str.append(" failed: ").append(errValue.toString());
    }

    /**
     * Returns a string representation of this operation.
     * @return a string representation of this operation.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getClass().getSimpleName());
        appendArguments(str);
        appendErrorCode(str);
        return str.toString();
    }


    //****************** Operation markers ******************//

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
     * Searches the given {@code operationClass} for an annotated constructor.
     * The constructor with the smallest number of arguments is returned.
     *
     * @param <T> the operation class
     * @param operationClass the operation class to search the constructor for
     * @return the constructor for the operation
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static <T extends AbstractOperation> Constructor<T> getAnnotatedConstructor(Class<? extends T> operationClass) throws NoSuchMethodException {
        return getAnnotatedConstructor(operationClass, -1);
    }

    /**
     * Searches the given {@code operationClass} for an annotated constructor
     * that has the given {@code argumentsCount}.
     *
     * @param <T> the operation class 
     * @param operationClass the operation class to search the constructor for
     * @param argumentsCount number of arguments that the constructor must have
     * @return the constructor for the operation
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static <T extends AbstractOperation> Constructor<T> getAnnotatedConstructor(Class<? extends T> operationClass, int argumentsCount) throws NoSuchMethodException {
        // Ignore the classes that are not annotated by OperationName
        if (operationClass == null || !operationClass.isAnnotationPresent(OperationName.class))
            throw new NoSuchMethodException("Class " + operationClass.getName() + " cannot be created automatically, because the annotation is missing");

        // Remember the constructor with smallest number of arguments if nArguments == -1
        int minimalConstructorArgs = Integer.MAX_VALUE;
        Constructor<T> minimalConstructor = null;

        // Search all its constructors for proper annotation
        for (Constructor<T> constructor : Convert.getConstructors(operationClass)) {
            if (constructor.isAnnotationPresent(OperationConstructor.class)) {
                int thisConstructorArgs = constructor.getParameterTypes().length;
                if (argumentsCount == -1) {
                    if (minimalConstructorArgs > thisConstructorArgs) {
                        minimalConstructor = constructor;
                        minimalConstructorArgs = thisConstructorArgs;
                    }
                } else {
                    if (thisConstructorArgs == argumentsCount) {
                        return constructor;
                    }
                }
            }
        }

        // Return the minimal constructor if found
        if (minimalConstructor != null)
            return minimalConstructor;

        // Not found
        throw new NoSuchMethodException("There is no valid annotated constructor in " + operationClass + " for " + argumentsCount + " parameters");
    }

    /**
     * Returns constructor argument descriptions for the provided operation class.
     * This is used by auto-generated clients to show descriptiron during operation creation.
     * @param operationClass class to get the descriptions for
     * @return constructor argument descriptions
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static String[] getConstructorArgumentDescriptions(Class<? extends AbstractOperation> operationClass) throws NoSuchMethodException {
        return getAnnotatedConstructor(operationClass).getAnnotation(OperationConstructor.class).value();
    }

    /**
     * Returns constructor argument descriptions for the provided operation class with given number of arguments.
     * This is used by auto-generated clients to show descriptiron during operation creation.
     * @param operationClass class to get the descriptions for
     * @param nArguments the number of arguments of the constructor
     * @return constructor argument descriptions
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static String[] getConstructorArgumentDescriptions(Class<? extends AbstractOperation> operationClass, int nArguments) throws NoSuchMethodException {
        return getAnnotatedConstructor(operationClass, nArguments).getAnnotation(OperationConstructor.class).value();
    }

    /**
     * Returns constructor argument types for the provided operation class.
     * @param operationClass class to get the constructor types for
     * @return constructor argument types
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static Class<?>[] getConstructorArguments(Class<? extends AbstractOperation> operationClass) throws NoSuchMethodException {
        return getAnnotatedConstructor(operationClass).getParameterTypes();
    }

    /**
     * Returns constructor arguments for the provided operation class for an annotated constructor with given number of arguments.
     * @param operationClass class to get the constructor types for
     * @param nArguments the number of arguments of the constructor
     * @return constructor argument types
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static Class<?>[] getConstructorArguments(Class<? extends AbstractOperation> operationClass, int nArguments) throws NoSuchMethodException {
        return getAnnotatedConstructor(operationClass, nArguments).getParameterTypes();
    }
    
    /**
     * Returns full constructor description for the provided operation class.
     * @param operationClass class to get the constructor types for
     * @return full constructor description
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     */
    public static String getConstructorDescription(Class<? extends AbstractOperation> operationClass) throws NoSuchMethodException {
        StringBuffer rtv = new StringBuffer();
        rtv.append(operationClass.getName());
        for (String argdesc : getConstructorArgumentDescriptions(operationClass))
            rtv.append(" <").append(argdesc).append(">");
        rtv.append("\n\t... ").append(getName(operationClass));
        
        return rtv.toString();
    }
    
    /**
     * Creates a new operation of the specified class.
     * @param <E> the class of the operation that should be created
     * @param operationClass the class of the operation that should be created
     * @param arguments arguments supplied to the constructor; they should match the types of getConstructorArguments(operationClass)
     * @return a new instance of operation
     * @throws NoSuchMethodException if either the {@code operationClass} is <tt>null</tt> or the class is not annotated using {@link AbstractOperation.OperationName}
     * @throws IllegalArgumentException if the argument count or their types don't match the specified operation class constructor
     * @throws InvocationTargetException if there was an exception in the operation's constructor
     */
    public static <E extends AbstractOperation> E createOperation(Class<E> operationClass, Object... arguments) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        // Create a new instance of the class for the specified constructor prototypes and arguments
        try {
            return getAnnotatedConstructor(operationClass, arguments.length).newInstance(arguments);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Error creating a new instance of " + operationClass + ": cannot instantiate abstract class");
        } catch (IllegalAccessException e) {
            throw new InternalError("This should never happen, since getAnnotatedConstructor returns a public constructor");
        }
    }

    /**
     * Returns argument that was passed while constructing instance.
     * If the argument is not stored within operation, <tt>null</tt> is returned.
     * @param index zero-based index of an argument passed to constructor
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
