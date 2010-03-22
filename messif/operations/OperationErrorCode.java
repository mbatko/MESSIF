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
 * Error codes related to operations.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class OperationErrorCode extends ErrorCode {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Response to a query has been returned, i.e. the query finished successfully. */
    public static OperationErrorCode RESPONSE_RETURNED = new OperationErrorCode("response returned");

    /** Empty answer returned. */
    public static OperationErrorCode EMPTY_ANSWER = new OperationErrorCode("empty answer returned");  
    
    /** Response to a query has not been returned completely, i.e. the same operation should be called again. */
    public static OperationErrorCode HAS_NEXT = new OperationErrorCode("having more objects to return");
    
    /** Creates a new instance of OperationErrorCode
     * 
     * @param text description of the error code
     */
    public OperationErrorCode(String text) {
        super(text);
    }
    
}
