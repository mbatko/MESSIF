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
package messif.buckets;

import java.io.IOException;

/**
 * Interface for objects that can be temporarily closed to save resources.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface TemporaryCloseable {

    /**
     * Release the resources associated with this instance.
     * After this method is called, the instance can still read/write data
     * but the first access will be more expensive.
     * @param resetAccessCounter flag whether to reset the access counter
     * @return <tt>true</tt> if the close was successful and resources were released or
     *      <tt>false</tt> if the current instance was not idle (i.e. there was an access since last access counter reset)
     * @throws IOException if there was a problem closing the instance resources
     */
    public boolean closeTemporarilyIfIdle(boolean resetAccessCounter) throws IOException;

}
