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

import java.io.IOException;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class PrecompDistPerforatedArrayFilter extends PrecomputedDistancesFixedArrayFilter {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Creates a new instance of ProcmpDistPerforatedArrayFilter */
    public PrecompDistPerforatedArrayFilter() {
    }

    /** Creates a new instance of ProcmpDistPerforatedArrayFilter */
    public PrecompDistPerforatedArrayFilter(int initialSize) {
        super(initialSize);
    }
    
    protected boolean excludeUsingPrecompDistImpl(PrecomputedDistancesFixedArrayFilter targetFilter, float radius) {
        // We have no precomputed distances either in the query or this object
        if (precompDist == null || targetFilter.precompDist == null)
            return false;
        
        // Traverse the precomputed distances by array
        int maxIndex = Math.min(actualSize, targetFilter.actualSize);
        for (int i = 0; i < maxIndex; i++)
            if ((precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (targetFilter.precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (Math.abs(precompDist[i] - targetFilter.precompDist[i]) > radius))
                return true;        
        return false;
    }

    protected boolean includeUsingPrecompDistImpl(PrecomputedDistancesFixedArrayFilter targetFilter, float radius) {
        // We have no precomputed distances either in the query or this object
        if (precompDist == null || targetFilter.precompDist == null)
            return false;
        
        // Traverse the precomputed distances by array
        int maxIndex = Math.min(actualSize, targetFilter.actualSize);
        for (int i = 0; i < maxIndex; i++)
            if ((precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (targetFilter.precompDist[i] != LocalAbstractObject.UNKNOWN_DISTANCE) && (Math.abs(precompDist[i] + targetFilter.precompDist[i]) <= radius))
                return true;        
        return false;
    }

    protected PrecompDistPerforatedArrayFilter(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }
}
