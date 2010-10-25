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
package messif.utility.reflection;

/**
 * This exception indicates that the {@link Instantiator} cannot be created.
 * For example, the exception is thrown when a constructor (method or field)
 * with a given name was not found.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class NoSuchInstantiatorException extends Exception {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>NoSuchInstantiatorException</code> without detail message.
     */
    public NoSuchInstantiatorException() {
    }


    /**
     * Constructs an instance of <code>NoSuchInstantiatorException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoSuchInstantiatorException(String msg) {
        super(msg);
    }

    protected NoSuchInstantiatorException(Class<?> clazz, String name) {
        super(appendType(new StringBuilder("There is no "), clazz, name, false).toString());
    }

    protected NoSuchInstantiatorException(Class<?> clazz, String name, Class<?>[] argumentClasses) {
        super(appendArguments(
                appendType(new StringBuilder("There is no "), clazz, name, true),
                false, true, argumentClasses
            ).toString());
    }

    protected NoSuchInstantiatorException(Class<?> clazz, String name, boolean convertStringArguments, Object[] arguments) {
        super(appendArguments(
                appendType(new StringBuilder("There is no "), clazz, name, true),
                convertStringArguments, false, arguments
            ).toString());
    }

    protected NoSuchInstantiatorException(Class<?> clazz, String name, int argumentCount) {
        super(appendArguments(
                appendType(new StringBuilder("There is no "), clazz, name, true),
                argumentCount
            ).toString());
    }

    private static StringBuilder appendType(StringBuilder str, Class<?> clazz, String name, boolean haveArguments) {
        str.append(!haveArguments ? "field " : (name == null ? "constructor " : "method "));
        str.append(clazz.getName());
        if (name != null)
            str.append('.').append(name);
        return str;
    }

    private static StringBuilder appendArguments(StringBuilder str, int argumentCount) {
        if (argumentCount >= 0)
            str.append("(...) with ").append(argumentCount).append(" arguments");
        return str;
    }

    private static StringBuilder appendArguments(StringBuilder str, boolean convertStringArguments, boolean classOnly, Object[] arguments) {
        if (arguments != null) {
            str.append('(');
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0)
                    str.append(", ");
                if (arguments[i] == null)
                    str.append("null");
                else if (convertStringArguments)
                    str.append(arguments[i]);
                else if (classOnly)
                    str.append(((Class<?>)arguments[i]).getName());
                else
                    str.append(arguments[i].getClass().getName());
            }
            str.append(')');
        }
        return str;
    }
}
