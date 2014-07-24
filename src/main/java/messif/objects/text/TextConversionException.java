/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.text;

/**
 * Exceptions that indicates an error when evaluating some of the text conversions.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class TextConversionException extends Exception {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new instance of <code>TextConversionException</code> without detail message.
     */
    public TextConversionException() {
    }

    /**
     * Constructs an instance of <code>TextConversionException</code> with the specified detail message.
     * @param msg the detail message
     */
    public TextConversionException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of <code>TextConversionException</code> with
     * the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt>
     * @param cause the cause of this exception
     */
    public TextConversionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an instance of <code>TextConversionException</code> with
     * the specified detail message and cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i>
     * automatically incorporated in this exception's detail message.
     * </p>
     * @param msg the detail message
     * @param cause the cause of this exception
     */
    public TextConversionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
