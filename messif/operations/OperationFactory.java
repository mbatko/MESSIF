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
package messif.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import messif.utility.Convert;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Factory for creating operations.
 * The factory can store a predefined values for the operation constructor arguments
 * that are used automatically whenever a <tt>null</tt> is passed in any argument to the
 * {@link #create(java.lang.Object[]) create} method.
 * 
 * @param <T> the type of operation created by this factory
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class OperationFactory<T extends AbstractOperation> extends ConstructorInstantiator<T> {

    //****************** Attributes ******************//

    /** Arguments for the constructor */
    private final Object[] arguments;


    //****************** Constructors ******************//

    /**
     * Creates a new factory for creating operations with the given arguments.
     *
     * @param operationClass the class the operations to create
     * @param arguments the arguments for the operation constructor
     * @throws NoSuchInstantiatorException if the operation class does not have a proper constructor
     */
    public OperationFactory(Class<? extends T> operationClass, Object... arguments) throws NoSuchInstantiatorException {
        super(operationClass, arguments);
        this.arguments = arguments.clone();
    }

    /**
     * Creates a new factory for creating operations with the given arguments.
     *
     * @param operationClass the class the operations to create
     * @param convertStringArguments if <tt>true</tt> the string values from the arguments are converted using {@link Convert#stringToType}
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @param arguments the arguments for the operation constructor
     * @throws NoSuchInstantiatorException if the operation class does not have a proper constructor
     */
    private OperationFactory(Class<? extends T> operationClass, boolean convertStringArguments, Map<String, Object> namedInstances, Object[] arguments) throws NoSuchInstantiatorException {
        super(operationClass, convertStringArguments, namedInstances, arguments);
        this.arguments = arguments;
    }

    /**
     * Creates a new factory for creating operations with the given string arguments.
     * Note that string arguments will be converted to the proper types using {@link Convert#stringToType}.
     *
     * @param operationClass the class the operations to create
     * @param arguments the string arguments for the operation constructor (will be converted to proper types)
     * @param offset the index of the first argument to use from the {@code arguments} array
     * @param length the number of arguments that will be used from the {@code arguments} array (starting from offset)
     * @param namedInstances map of named instances - an instance from this map is returned if the <code>string</code> matches a key in the map
     * @throws NoSuchInstantiatorException if the operation class does not have a proper constructor
     */
    public OperationFactory(Class<? extends T> operationClass, String[] arguments, int offset, int length, Map<String, Object> namedInstances) throws NoSuchInstantiatorException {
        this(operationClass, Convert.copyGenericArray(arguments, offset, length, Object.class), namedInstances);
    }

    /**
     * Creates a new factory for creating operations with the given number of arguments.
     * No stored arguments are prepared.
     * 
     * @param operationClass the class the operations to create
     * @param argumentCount the number of arguments that the operation constructor should have
     * @throws NoSuchInstantiatorException if the operation class does not have a proper constructor
     */
    public OperationFactory(Class<? extends T> operationClass, int argumentCount) throws NoSuchInstantiatorException {
        super(operationClass, argumentCount);
        this.arguments = null;
    }

    /**
     * Creates a new factory for creating operations with the given number of arguments.
     * No stored arguments are prepared.
     * 
     * @param operationClass the class the operations to create
     * @param prototype the types of operation constructor arguments
     * @throws NoSuchInstantiatorException if the operation class does not have a proper constructor
     */
    public OperationFactory(Class<? extends T> operationClass, Class<?>... prototype) throws NoSuchInstantiatorException {
        super(operationClass, prototype);
        this.arguments = null;
    }


    //****************** Attributes access methods ******************//

    /**
     * Returns the value of the {@code index}th argument for the operation constructor.
     * @param index the index of the argument to return
     * @return the value of the {@code index}th argument
     */
    protected Object getArgumentValue(int index) {
        return arguments == null ? null : arguments[index];
    }


    //****************** Creation methods ******************//

    @Override
    public final T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        return create(arguments);
    }

    /**
     * Creates a new instance of the operation created by this factory.
     * @param arguments the arguments to use for the construction; all arguments that
     *          are <tt>null</tt> are replaced by the stored {@link #getArgumentValue(int) argument value}
     * @return a new instance of the operation
     * @throws IllegalArgumentException if the arguments are not compatible with the operation constructor prototype
     * @throws InvocationTargetException if there was an exception thrown when the operation constructor was invoked
     */
    public T create(Object... arguments) throws IllegalArgumentException, InvocationTargetException {
        Object[] argumentsMerged = new Object[getInstantiatorPrototype().length];
        for (int i = 0; i <= argumentsMerged.length; i++) {
            argumentsMerged[i] = arguments != null && i < arguments.length && arguments[i] != null ? arguments[i] : getArgumentValue(i);
        }
        return super.instantiate(argumentsMerged);
    }

    /**
     * Creates a new instance of the operation created by this factory.
     * The stored {@link #getArgumentValue(int) argument values} are used to create the operation.
     * @return a new instance of the operation
     * @throws InvocationTargetException if there was an exception thrown when the operation constructor was invoked
     */
    public T create() throws InvocationTargetException {
        return create((Object[])null);
    }
}
