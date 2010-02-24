/*
 * HttpApplicationAuthenticator
 *
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
 * @author xbatko
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
