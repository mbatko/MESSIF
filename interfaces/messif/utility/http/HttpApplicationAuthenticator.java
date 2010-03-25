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

import com.sun.net.httpserver.BasicAuthenticator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Authenticator for the {@link com.sun.net.httpserver.HttpServer HTTP server}
 * that provides a basic HTTP authentication based on the stored user/password
 * values.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class HttpApplicationAuthenticator extends BasicAuthenticator {

    //****************** Constants ******************//

    /** Default realm for the constructor */
    public static final String DEFAULT_REALM = "MESSIF HTTP Application";


    //****************** Attributes ******************//

    /** List of users and their passwords for the authentication */
    private final Map<String, String> credentials;


    //****************** Constructors ******************//

    /**
     * Creates a HttpApplicationAuthenticator for the given HTTP realm.
     * @param realm the HTTP Basic authentication realm
     */
    public HttpApplicationAuthenticator(String realm) {
        super(realm);
        this.credentials = new HashMap<String, String>();
    }

    /**
     * Creates a HttpApplicationAuthenticator for the {@link #DEFAULT_REALM default realm}.
     */
    public HttpApplicationAuthenticator() {
        this(DEFAULT_REALM);
    }

    /**
     * Updates the stored credentials of this authenticator.
     * If the password is <tt>null</tt>, the user is removed.
     * Otherwise, the user identified by the password is added to the
     * list of known users (replacing the previous password if the user
     * was already known).
     *
     * @param username the user name to add to the list
     * @param password the password for the username to add to the list
     */
    public void updateCredentials(String username, String password) {
        if (password == null)
            credentials.remove(username);
        else
            credentials.put(username, password);
    }

    /**
     * Returns whether this authenticator has at least one user set.
     * @return <tt>true</tt> if there are credentials for at least one user
     */
    public boolean hasCredentials() {
        return !credentials.isEmpty();
    }

    /**
     * Called for each incoming request to verify the given name and password
     * in the context of this Authenticator's realm.
     * @param username the username from the request
     * @param password the password from the request
     * @return <tt>true</tt> if the credentials are valid, <tt>false</tt> otherwise
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        String checkPasswd = credentials.get(username);
        return checkPasswd != null && checkPasswd.equals(password);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        Iterator<String> iterator = credentials.keySet().iterator();
        while (iterator.hasNext()) {
            str.append(iterator.next());
            if (iterator.hasNext())
                str.append(", ");
        }
        return str.toString();
    }

}
