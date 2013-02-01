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
package messif.pivotselection;

import java.io.IOException;
import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractStreamObjectIterator;
import messif.objects.util.StreamGenericAbstractObjectIterator;


/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class StreamSequencePivotChooser extends AbstractPivotChooser implements Serializable {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Stream to read pivots from */
    protected final AbstractStreamObjectIterator<LocalAbstractObject> stream;


    /****************** Construcotrs ******************/

    /**
     * Creates a new instance of StreamSequencePivotChooser.
     * @param objClass the class of objects to read from the stream
     * @param fileName the file to read objects from
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamSequencePivotChooser(Class<? extends LocalAbstractObject> objClass, String fileName) throws IllegalArgumentException, IOException {
        stream = new StreamGenericAbstractObjectIterator<LocalAbstractObject>(objClass, fileName);
    }
    
    
    /****************** Overrides ******************/

    /**
     * This method carries out the actual pivot selection, that is, read specified
     * number of objects from the stream.
     * @param count number of pivots to generate
     * @param sampleSetIterator ignored by this chooser, since the pivots are provided externally
     */
    @Override
    protected void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        for (;count > 0;count--)
            addPivot(stream.next());
    }

}
