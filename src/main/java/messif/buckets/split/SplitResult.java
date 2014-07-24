/*
 *  This file is part of M-Index library.
 *
 *  M-Index library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  M-Index library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with M-Index library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.buckets.split;

import java.util.List;
import messif.buckets.Bucket;
import messif.buckets.BucketDispatcher;
import messif.buckets.CapacityFullException;

/**
 * Encapsulation of the result of the split operation.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface SplitResult {

    /**
     * Returns the list remote buckets for the newly created buckets (the list can contain nulls). The list 
     *  is of length {@link SplitPolicy#getPartitionsCount() }.
     * @param bucketDisp the bucket dispatcher that created the buckets
     * @return the list remote buckets for the newly created buckets
     * @throws CapacityFullException if not all buckets were created because of capacity constraint
     */
    public List<? extends Bucket> getBuckets(BucketDispatcher bucketDisp) throws CapacityFullException;

    /**
     * Returns the used split policy that can contain output values.
     * @return the used split policy 
     */
    public SplitPolicy getSplitPolicy();

    /**
     * Returns the number of objects moved from the split bucket to the newly created ones.
     * @return the number of objects moved
     */
    public int getObjectsMoved();
    
}
