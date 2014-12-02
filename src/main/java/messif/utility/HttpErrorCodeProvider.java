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
 * This interface is to be implemented mainly by custom MESSIF exceptions thrown
 *  during operation processing or any other action; the implementing class provides
 *  a HTTP error code to be returned in case the exception was thrown.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface HttpErrorCodeProvider {

    /** Constant holding information that the HTTP error code was not set. */
    public static final int ERROR_CODE_NOT_SET = -1;
    
    /** Constant holding a HTTP error code of internal error (default error code). */
    public static final int ERROR_CODE_INTERNAL_ERROR = 500;
    
    /** Constant holding a HTTP error code of a 'conflict'; it is used when a duplicate insert appears. */
    public static final int ERROR_CODE_CONFLICT = 409;

    /** Constant holding a HTTP error code of 'Not found'. */
    public static final int ERROR_CODE_NOT_FOUND = 404;
    
    /**
     * Returns true, if the HTTP code was set, false otherwise.
     * @return true, if the HTTP code was set, false otherwise.
     */
    public boolean isHttpErrorCodeSet();
    
    /** 
     * Returns a HTTP error code to be returned to the HTTP client.
     * @return the HTTP error code to be returned to the HTTP client.
     */
    public int getHttpErrorCode();
    
    /**
     * Sets a new HTTP code to be later returned by this provider.
     * @param httpErrorCode a new HTTP code to be later returned by this provider.
     */
    public void setHttpErrorCode(int httpErrorCode);
    
}
