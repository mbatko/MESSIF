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
package messif.objects.util;

import java.io.Closeable;
import java.io.IOException;
import messif.objects.LocalAbstractObject;

/**
 * This class represents an iterator on {@link LocalAbstractObject}s that are read from a file.
 * The objects are instantiated one by one every time the {@link #next next} method is called.
 * The file should be created using {@link LocalAbstractObject#write} method.
 *
 * @param <E> the class of objects provided by this stream iterator (must be descendant of {@link LocalAbstractObject})
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class AbstractStreamObjectIterator<E extends LocalAbstractObject> extends AbstractObjectIterator<E> implements Closeable {
    

    //****************** Attribute access methods ******************//

    /**
     * Sets the value of this stream's object constructor argument.
     * This method can be used to change object passed to <code>constructorArgs</code>.
     * 
     * @param index the parameter index to change (zero-based)
     * @param paramValue the changed value to pass to the constructor
     * @throws IllegalArgumentException when the passed object is incompatible with the constructor's parameter
     * @throws IndexOutOfBoundsException if the index parameter is out of bounds (zero parameter cannot be changed)
     * @throws InstantiationException if the value passed is string that is not convertible to the constructor class
     */
    public abstract void setConstructorParameter(int index, Object paramValue) throws IndexOutOfBoundsException, IllegalArgumentException, InstantiationException;


    /**
     * Reset the associated stream and restarts the iteration from beginning.
     * @throws IOException if there was an I/O error re-opening the file
     */
    public abstract void reset() throws IOException;

}
