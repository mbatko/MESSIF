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
package messif.objects.text;

import java.util.Collection;

/**
 * Interface for objects that provide textual data in multiple fields.
 * The inherited {@link #getStringData()} method provides data from all fields
 * returned by {@link #getStringDataFields()}.
 * 
 * <p>
 * Note that at least one field should always be returned by the {@link #getStringDataFields()}
 * method, i.e. the returned array should have one non-null non-empty string value.
 * </p>
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface StringFieldDataProvider extends StringDataProvider {
    /**
     * Returns the names of the textual data fields of this object.
     * @return the names of the textual data fields of this object
     */
    public Collection<String> getStringDataFields();

    /**
     * Returns the textual data for field {@code fieldName} provided by this object.
     * @param fieldName the name of the field the data of which to return
     * @return the textual data for field {@code fieldName}
     * @throws IllegalArgumentException if the given field name is unknown for this data provider 
     */
    public String getStringData(String fieldName) throws IllegalArgumentException;
}
