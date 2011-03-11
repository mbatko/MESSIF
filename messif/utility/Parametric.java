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
package messif.utility;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for objects that support definition of additional parameters.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Parametric {
    /**
     * Returns the number of additional parameters.
     * @return the number of additional parameters
     */
    public int getParameterCount();

    /**
     * Returns a set of additional parameter names present in this object.
     * @return a set of additional parameter names present in this object
     */
    public abstract Collection<String> getParameterNames();

    /**
     * Returns an additional parameter with the given {@code name}.
     * @param name the name of the additional parameter to get
     * @return the value of the parameter {@code name} or <tt>null</tt> if it is not set
     */
    public Object getParameter(String name);

    /**
     * Returns an additional parameter with the given {@code name}.
     * If the parameter with the given {@code name} is not set, an exception is thrown.
     * @param name the name of the additional parameter to get
     * @return the value of the parameter {@code name}
     * @throws IllegalArgumentException if the parameter with the given {@code name} is not set
     */
    public Object getRequiredParameter(String name) throws IllegalArgumentException;

    /**
     * Returns an additional parameter with the given {@code name}.
     * If the parameter is not set or is not an instance of {@code parameterClass},
     * the {@code defaultValue} is returned instead.
     *
     * @param <T> the class of the parameter
     * @param name the name of the additional parameter to get
     * @param parameterClass the class of the parameter to get
     * @param defaultValue the default value to use if the parameter is <tt>null</tt>
     * @return the parameter value
     */
    public <T> T getParameter(String name, Class<? extends T> parameterClass, T defaultValue);

    /**
     * Returns an additional parameter with the given {@code name}.
     * If the parameter {@code name} exists but it is not an instance of
     * {@code parameterClass}, <tt>null</tt> is returned.
     *
     * @param <T> the class of the parameter
     * @param name the name of the additional parameter to get
     * @param parameterClass the class of the parameter to get
     * @return the value of the parameter {@code name} or <tt>null</tt> if it is not set
     */
    public <T> T getParameter(String name, Class<? extends T> parameterClass);

    /**
     * Returns the map of all additional parameters.
     * Note that the map is not modifiable.
     * @return the map of additional parameters
     */
    public Map<String, ? extends Object> getParameterMap();
}
