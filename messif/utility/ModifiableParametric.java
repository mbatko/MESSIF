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

import java.util.Map;

/**
 * Extension of the {@link Parametric} interface that support modifications.
 *
 * @param <T> the super type of all parameters held by this parametric object
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ModifiableParametric<T> extends Parametric {
    @Override
    public T getParameter(String name);

    @Override
    public <T> T getRequiredParameter(String name, Class<? extends T> parameterClass) throws IllegalArgumentException, ClassCastException;

    @Override
    public Map<String, ? extends T> getParameterMap();

    @Override
    public T getRequiredParameter(String name) throws IllegalArgumentException;

    /**
     * Sets an additional parameter with the given {@code name}.
     * @param name the name of the additional parameter to set
     * @param value the new value of the parameter {@code name}
     */
    public void setParameter(String name, T value);

    /**
     * Removes an additional parameter with the given {@code name}.
     * @param name the name of the additional parameter to remove
     * @return the value of the parameter {@code name} that was removed or <tt>null</tt> if it was not set
     */
    public T removeParameter(String name);

}
