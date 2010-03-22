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

/**
 * Processes a HTTP exchange and provides a value for the processors down in the chain.
 *
 * @param <T> the type of object that this processor returns
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface HttpApplicationProcessor<T> {
    /**
     * Processes a HTTP exchange and provides a value.
     *
     * @param httpExchange the HTTP exchange (request and response) to use
     * @param httpParams parsed parameters from the HTTP request
     * @return a processed value
     * @throws Exception if there was a problem processing the exchange
     */
    public T processHttpExchange(HttpExchange httpExchange, Map<String, String> httpParams) throws Exception;

    /**
     * Returns the number of arguments that this processor requires.
     * Always returns a value greater than 0.
     * @return the number of arguments that this processor requires
     */
    public int getProcessorArgumentCount();

    /**
     * Returns the class the instances of which are produced by this processor.
     * @return the class the returned instances
     */
    public Class<? extends T> getProcessorReturnType();
}
