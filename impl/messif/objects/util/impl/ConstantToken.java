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
package messif.objects.util.impl;

/**
 * Simple float constant token for the aggragation function evaluator.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ConstantToken implements PatternToken {

    /** Class id for object serialization. */
    private static final long serialVersionUID = 1L;

    /** The float constant. */
    private final float constant;

    /**
     * Constructs this object given a string which must be interpreted as a flost number.
     * @param constantString the float string
     * @throws java.lang.IllegalArgumentException
     */
    public ConstantToken(String constantString) throws IllegalArgumentException {
        try {
            this.constant = Float.parseFloat(constantString);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(numberFormatException);
        }
    }

    /**
     * Return the constant
     * @param subdistances distnaces that are ignored by the constant.
     * @return the constant value
     */
    public final float evaluate(float[] subdistances) {
        return constant;
    }

    @Override
    public String toString() {
        return String.valueOf(constant);
    }
}
