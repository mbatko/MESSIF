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

import messif.objects.AbstractObject;
import messif.objects.NoDataObject;

/**
 * Enumeration of types a query operation can return.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see QueryOperation
 */
public enum AnswerType {
    //****************** Constants ******************//

    /** Answer contains the original objects as is */
    ORIGINAL_OBJECTS,
    /** Answer contains clones of the original objects */
    CLONED_OBJECTS,
    /** Answer contains clones of the original objects with {@link messif.objects.AbstractObject#clearSurplusData() cleared surplus data} */
    CLEARED_OBJECTS,
    /** Answer contains only {@link messif.objects.NoDataObject objects} */
    NODATA_OBJECTS,
    /**
     * Answer contains only {@link messif.objects.NoDataObject objects}
     * @deprecated Use {@link #NODATA_OBJECTS} instead.
     */
    @Deprecated
    REMOTE_OBJECTS;


    //****************** Methods for updating objects according to answer type ******************//

    /**
     * Updates a {@link AbstractObject} so that it conforms to this answer type.
     * That means, the object is cloned, cleared or transformed into a {@link NoDataObject}.
     * @param object the object to update
     * @return an updated object
     * @throws CloneNotSupportedException if a clone was requested but the specified object cannot be cloned
     */
    public AbstractObject update(AbstractObject object) throws CloneNotSupportedException {
        switch (this) {
            case ORIGINAL_OBJECTS:
                return object;
            case CLONED_OBJECTS:
                return object.clone();
            case CLEARED_OBJECTS:
                object = object.clone();
                object.clearSurplusData();
                return object;
            case NODATA_OBJECTS:
            case REMOTE_OBJECTS:
            default:
                return object.getNoDataObject();
        }
    }

}
