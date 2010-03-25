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
 * Processor that returns a value from the HTTP parameter.
 *
 * @param <T> the type of the parameter value that this processor returns
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ParameterProcessor<T> implements HttpApplicationProcessor<T> {
    /** Type to convert the http request parameter to */
    private final Class<? extends T> parameterClass;
    /** Collection of named instances that are used when converting string parameters */
    private final Map<String, Object> namedInstances;
    /** Name of the HTTP parameter to process */
    private final String parameterName;

    /**
     * Creates a new instance of parameter value processor.
     * @param parameterName the name of the HTTP parameter to process
     * @param parameterClass the class of parameter value
     * @param namedInstances collection of named instances that are used when converting string parameters
     */
    public ParameterProcessor(String parameterName, Class<? extends T> parameterClass, Map<String, Object> namedInstances) {
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.namedInstances = namedInstances;
    }

    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws IllegalArgumentException {
        String parameter = httpParams.get(parameterName);
        if (parameter == null)
            throw new IllegalArgumentException("Parameter '" + parameterName + "' was not specified");
        try {
            return Convert.stringToType(parameter, parameterClass, namedInstances);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage(), e.getCause());
        }
    }

    public int getProcessorArgumentCount() {
        return 1;
    }

    public Class<? extends T> getProcessorReturnType() {
        return parameterClass;
    }

}
