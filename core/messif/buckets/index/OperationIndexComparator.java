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
package messif.buckets.index;

import java.util.Collection;
import messif.objects.LocalAbstractObject;
import messif.operations.QueryOperation;

/**
 * A comparison function, which imposes a <i>total ordering</i> on some
 * collection of keys. Objects stored in the index are compared using
 * keys for ordering; the {@link #extractKey} extracts a key for this
 * comparator from any indexed object. The imposed order also corresponds
 * to a certain {@link QueryOperation}, which can be obtained by
 * {@link #createIndexOperation}.
 * 
 * <p>
 * This IndexComparator is restricted to {@link LocalAbstractObject} only,
 * so that the query operation can work for them.
 * </p>
 * @param <K> the type of the key arguments of the comparison
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 * @see java.util.Comparator
 */
public interface OperationIndexComparator<K> extends IndexComparator<K, LocalAbstractObject> {

    /**
     * Creates a query operation for the given keys.
     * The returned operation represents the same ordering as this index comparator.
     * @param keys the list of keys the operation searches for
     * @return a new instance of query operation for the given keys
     */
    public QueryOperation<?> createIndexOperation(Collection<? extends K> keys);
}
