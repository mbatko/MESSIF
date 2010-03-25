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
package messif.objects.extraction;

import java.io.EOFException;
import java.io.IOException;
import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;

/**
 * Iterator that provides objects by {@link Extractor}.
 * The iterator is initialized by given extractor and {@link ExtractorDataSource} and
 * returns objects until the extraction fails or an end of the data source is reached.
 *
 * @param <T> the class of objects returned by this iterator
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtractorIterator<T extends LocalAbstractObject> extends AbstractObjectIterator<T> {
    /** Extractor to use for creating objects */
    private final Extractor<? extends T> extractor;
    /** Data source for the extractor */
    private final ExtractorDataSource dataSource;
    /** Last object returned by {@link #next()} */
    private T currentObject;
    /** Next extracted object that will be given by call to {@link #next()} */
    private T nextObject;

    /**
     * Creates a new instance of ExtractorIterator.
     * @param extractor the extractor to use for creating objects
     * @param dataSource the data source for the extractor
     */
    public ExtractorIterator(Extractor<? extends T> extractor, ExtractorDataSource dataSource) {
        this.extractor = extractor;
        this.dataSource = dataSource;
    }

    @Override
    public T getCurrentObject() throws NoSuchElementException {
        if (currentObject == null)
            throw new NoSuchElementException("There is no current object");
        return currentObject;
    }

    public boolean hasNext() {
        // If there is another next object
        if (nextObject != null && currentObject != nextObject)
            return true;
        // If there was an object but there is none now
        if (currentObject != null && nextObject == null)
            return false;
        // We do not have a next object, try to extract one
        try {
            nextObject = extractor.extract(dataSource);
        } catch (EOFException e) {
            nextObject = null;
        } catch (IOException e) {
            nextObject = null;
            throw new NoSuchElementException("Cannot read next object: " + e);
        } catch (ExtractorException e) {
            nextObject = null;
            throw new NoSuchElementException("Cannot extract next object: " + e);
        }
        return nextObject != null;
    }

    public T next() throws NoSuchElementException {
        // Check next (extracts an object if necessary)
        if (!hasNext())
            throw new NoSuchElementException("There is no next object");
        currentObject = nextObject;
        return currentObject;
    }

    public void remove() {
        throw new UnsupportedOperationException("ExtractorIterator does not support removal");
    }

    /**
     * Reset the associated data source and restarts the iteration from beginning.
     * @throws IOException if there was an I/O error reseting the data source
     */
    public void reset() throws IOException {
        dataSource.reset();
        currentObject = null;
        nextObject = null;
    }
}
