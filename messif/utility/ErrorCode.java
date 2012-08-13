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
package messif.utility;

import java.io.Serializable;


/**
 * Base class for all error codes.
 * Error codes represent a return value that can be used to indicate various
 * states of execution. Error codes are compared according to their text representation.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ErrorCode implements Serializable {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //***************** Standard error codes *****************//

    /** The initial value of an error code that was not set yet. */
    public static ErrorCode NOT_SET = new ErrorCode("");

    /** Not specific error appeared. You may look at the source code which produced it to get some help. */
    public static ErrorCode UNKNOWN_ERROR = new ErrorCode("unknown error");


    //***************** Attributes *****************//

    /** Holder of the current error code text */
    private final String text;


    //***************** Constructors *****************//

    /**
     * Creates a new instance of ErrorCode.
     * Use this constructor to create static members in classes to define
     * new error codes.
     * @param text the text representation of the new error code
     */
    protected ErrorCode(String text) {
        this.text = text;
    }


    //***************** State query methods *****************//

    /**
     * Returns <tt>true</tt> if the error code is not set yet (i.e. has the value of {@link #NOT_SET}).
     * @return <tt>true</tt> if the error code is not set yet
     */
    public final boolean isSet() {
        return !NOT_SET.equals(this);
    }

    /**
     * Returns <tt>true</tt> if the error code is an unknown error (i.e. has the value of {@link #UNKNOWN_ERROR}).
     * @return <tt>true</tt> if the error code is an unknown error
     */
    public final boolean isUnknownError() {
        return UNKNOWN_ERROR.equals(this);
    }


    //***************** Overridden methods *****************//

    /**
     * Returns the textual representation of this error code.
     * @return the textual representation of this error code
     */
    @Override
    public final String toString() {
        return text;
    }
    
    @Override
    public final int hashCode() {
        return text.hashCode();
    }
    
    @Override
    public final boolean equals(Object object) {
        if (object instanceof ErrorCode)
            return ((ErrorCode)object).text.equals(text) && (object.getClass().equals(this.getClass()));
        else return false;
    }
    
}
