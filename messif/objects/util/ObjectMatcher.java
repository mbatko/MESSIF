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

import messif.objects.LocalAbstractObject;


/**
 * Interface which provides matching capabilities. 
 * Matching functionality is used when you need to filter out some objects of the whole bucket, for example.
 *
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface ObjectMatcher {
    
    /**
     * Matching method.
     * This method provides matching functionality and is used for categorization of objects into groups 
     * (partitions).
     *
     * @param object An object that is tested for the matching condition.
     *
     * @return Returns an identification of partition to which the object falls.
     *         When applied on a bucket (through the method GetMatchingObjects()) it is convenient to return 0 for all objects
     *         which stay in the bucket. Zero value returned means that object doesn't match.
     */
    public int match(LocalAbstractObject object);
}
