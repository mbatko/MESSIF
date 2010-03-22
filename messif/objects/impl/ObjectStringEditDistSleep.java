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
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;


/**
 * An object that whose getDistance() method takes 10 miliseconds more than std Edit distance
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectStringEditDistSleep extends ObjectStringEditDist {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectStringEditDistSleep(String text) {
        super(text);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectStringEditDistSleep() {
        super();
    }
    
    /** Creates a new instance of Object random generated 
     * with minimal length equal to minLength and maximal 
     * length equal to maxLength */
    public ObjectStringEditDistSleep(int minLength, int maxLength) {
        super(minLength, maxLength);
    }
    
    /** Creates a new instance of Object from stream */
    public ObjectStringEditDistSleep(BufferedReader stream) throws IOException {
        super(stream);
    }
    
    
    /** Metric function
     *      Implements euclidean distance measure (so-called L2 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        synchronized (this) {
            try {
                this.wait(10);
            } catch (InterruptedException ignore) { }
        }
        return super.getDistanceImpl(obj, distThreshold);
    }
}
