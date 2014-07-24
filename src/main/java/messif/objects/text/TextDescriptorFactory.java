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

import messif.objects.LocalAbstractObject;

/**
 * Provides factory method for creating textual descriptor objects.
 *
 * @param <T> the type of objects returned by this factory
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface TextDescriptorFactory<T extends LocalAbstractObject> {
    /**
     * Creates a {@link LocalAbstractObject object} that represents a descriptor
     * for the given text strings. Various fields can be represented by the items
     * of the string array (e.g. title and description strings).
     * @param strings the text to convert to a descriptor
     * @return a new instance of textual descriptor for the given strings
     * @throws TextConversionException if there was an error converting the text to descriptor
     */
    public T createTextDescriptor(String... strings) throws TextConversionException;
}
