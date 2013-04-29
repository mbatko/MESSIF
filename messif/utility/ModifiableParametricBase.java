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
 * Basic implementation of the {@link ModifiableParametric} interface on encapsulated {@link Map}.
 * Note that this class can be used as wrapper for {@link Map}.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ModifiableParametricBase extends ParametricBase implements ModifiableParametric {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Encapsulated {@link Map} that provides the parameter values */
    private final Map<String, Object> map;

    /**
     * Creates a new instance of ModifiableParametricBase backed-up by the given map with parameters.
     * @param map the map that provides the parameter values
     */
    public ModifiableParametricBase(Map<String, Object> map) {
        super(map);
        this.map = map;
    }

    @Override
    public Object setParameter(String name, Object value) {
        return map.put(name, value);
    }

    @Override
    public Object removeParameter(String name) {
        return map.remove(name);
    }

}
