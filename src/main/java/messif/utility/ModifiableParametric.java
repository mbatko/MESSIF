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

/**
 * Extension of the {@link Parametric} interface that support modifications.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ModifiableParametric extends Parametric {
    /**
     * Set an additional parameter with the given {@code name} to the given {@code value}.
     * Note that the previous value is <em>replaced</em> with the new one.
     * @param name the name of the additional parameter to set
     * @param value the new value for the parameter
     * @return the previous value of the parameter {@code name} or <tt>null</tt> if it was not set
     */
    public Object setParameter(String name, Object value);

    /**
     * Removes an additional parameter with the given {@code name}.
     * @param name the name of the additional parameter to remove
     * @return the value of the parameter {@code name} that was removed or <tt>null</tt> if it was not set
     */
    public Object removeParameter(String name);

}
