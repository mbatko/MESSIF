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

import java.io.IOException;
import java.util.Iterator;
import messif.objects.LocalAbstractObject;

/**
 * Interface for extractors that can create multiple {@link LocalAbstractObject objects}
 * from a single binary data source.
 *
 * @param <T> the type of {@link LocalAbstractObject object} that is extracted by this extractor
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface MultiExtractor<T extends LocalAbstractObject> {
    /**
     * Extracts multiple {@link LocalAbstractObject}s from the specified binary data source.
     * @param dataSource the source of binary data for the extraction
     * @return an iterator over new instances of objects extracted from the binary data source
     * @throws ExtractorException if the extractor encountered problem
     *          extracting the object from the binary data
     * @throws IOException if there was a problem reading data from the {@code dataSource}
     */
    public Iterator<T> extract(ExtractorDataSource dataSource) throws ExtractorException, IOException;

    /**
     * Returns the object class extracted by this extractor.
     * @return the object class extracted by this extractor
     */
    public Class<? extends T> getExtractedClass();
}
