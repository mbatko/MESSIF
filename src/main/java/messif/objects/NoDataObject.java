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
package messif.objects;

/**
 * Object of this class represents an AbstractObject only by its URI locator.
 * It does not contain any data.
 *
 * @see messif.objects.AbstractObject
 * @see messif.objects.LocalAbstractObject
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public final class NoDataObject extends AbstractObject {
    /** Class version id for serialization. */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of NoDataObject using the specified locator.
     * A new unique object ID is generated and a
     * new {@link messif.objects.keys.AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     */
    public NoDataObject(String locatorURI) {
        super(locatorURI);
    }

    /**
     * Creates a new instance of NoDataObject from the specified LocalAbstractObject.
     * @param object the local object from which to create the new one
     */
    public NoDataObject(AbstractObject object) {
        super(object); // Copy object ID and key
    }

    @Override
    public NoDataObject getNoDataObject() {
        return this;
    }

}
