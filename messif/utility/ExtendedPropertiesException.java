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

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ExtendedPropertiesException extends RuntimeException {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ExtendedPropertiesException with <code>null</code> as its
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public ExtendedPropertiesException() {
	super();
    }

    /**
     * Constructs a new ExtendedPropertiesException with the specified detailed message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detailed message
     */
    public ExtendedPropertiesException(String message) {
	super(message);
    }

    /**
     * Constructs a new ExtendedPropertiesException with the specified detailed message and
     * cause.
     *
     * @param message the detaile message
     * @param cause the cause for this exception
     */
    public ExtendedPropertiesException(String message, Throwable cause) {
        super(message, cause);
    }

}
