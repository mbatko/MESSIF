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
package messif.utility.reflection;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface for creating instances of a given class.
 *
 * <p>
 * This class provides a convenient way of repeatable creation of instances
 * without the need of repetable inspection of the target class.
 * </p>
 *
 * @param <T> the class the instances of which will be created by this Instantiator
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Instantiator<T> {
    /**
     * Creates an instance for the given arguments.
     * @param arguments the arguments for the intstance
     * @return a new instance
     * @throws IllegalArgumentException if the arguments are not compatible
     * @throws InvocationTargetException if there was an exception thrown when the instance was created
     */
    public T instantiate(Object... arguments) throws IllegalArgumentException, InvocationTargetException;

    /**
     * Returns the class instantiated by this Instantiator.
     * @return the instantiated class
     */
    public Class<? extends T> getInstantiatorClass();

    /**
     * Returns the classes of arguments for the {@link #instantiate(java.lang.Object[])} method.
     * @return the prototype of instantiatior arguments
     */
    public Class<?>[] getInstantiatorPrototype();
}
