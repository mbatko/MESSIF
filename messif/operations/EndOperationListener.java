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
package messif.operations;

import messif.utility.ErrorCode;

/**
 * Implements a listener that is called when an operation
 * {@link AbstractOperation#endOperation() ends}.
 * Note that operations call this interface on every
 * {@link messif.utility.Parametric encapsulated parameter}
 * as well as the answer collection of the {@link RankingQueryOperation} whenever
 * they implement this interface.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface EndOperationListener {
    /**
     * Called whenever the operation's {@link AbstractOperation#endOperation()} or
     * {@link AbstractOperation#endOperation(messif.utility.ErrorCode)} is called.
     * @param operation the operation that has ended
     * @param errorCode the error code of the operation
     */
    public void onEndOperation(AbstractOperation operation, ErrorCode errorCode);
}
