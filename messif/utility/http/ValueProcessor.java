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
package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import messif.utility.Convert;

/**
 * Processor that returns a fixed value.
 *
 * @param <T> the type of the fixed value that this processor returns
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ValueProcessor<T> implements HttpApplicationProcessor<T> {
    /** Fixed value returned by this processor */
    private final T value;

    /**
     * Creates a new instance of fixed value processor.
     * @param value the fixed value returned by this processor
     */
    public ValueProcessor(T value) {
        this.value = value;
    }

    /**
     * Creates a new instance of fixed value processor.
     * The value is converted from the given string value to the class using
     * {@link Convert#stringToType(java.lang.String, java.lang.Class) stringToType} method.
     *
     * @param value the string of the fixed value
     * @param valueClass the class of the fixed value
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @throws IllegalArgumentException if the specified {@code value} cannot be converted to {@code valueClass}
     */
    public ValueProcessor(String value, Class<? extends T> valueClass, Map<String, Object> namedInstances) throws IllegalArgumentException {
        try {
            this.value = Convert.stringToType(value, valueClass, namedInstances);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        return value;
    }

    @Override
    public int getProcessorArgumentCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends T> getProcessorReturnType() {
        return (Class<T>)value.getClass();
    }

}
