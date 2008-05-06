
package messif.objects;

/**
 * The object key that contains an Integer and an locator URI.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class IntegerKey extends AbstractObjectKey {

    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** The integer key */
    public final int key;
    
    /** Creates a new instance of IntegerKey 
     * @param locatorURI the URI locator
     * @param key the integer key of the object - it musn't be null
     * @throws java.lang.IllegalArgumentException if the key is null
     */
    public IntegerKey(String locatorURI, Integer key) throws IllegalArgumentException {
        super(locatorURI);
        if (key == null)
            throw new IllegalArgumentException("The integer key of the object must not be null");
        this.key = key;
    }
    
    /**
     * Creates a new instance of AbstractObjectKey given a buffered reader with the first line of the
     * following format: "integerKey locatorUri"
     * 
     * @param keyString the text stream to read an object from
     * @throws IllegalArgumentException if the string is not of format "integerKey locatorUri"
     */
    public IntegerKey(String keyString) throws IllegalArgumentException {
        super(getLocatorURI(keyString));
        try {
            this.key = Integer.valueOf(keyString.substring(0, keyString.indexOf(" ")));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'integerKey locatorUri': "+keyString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("string must be of format 'integerKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Auxiliary method for parsing the string 'integerKey locatorURI'
     * @return the locatorURI string
     */
    private static String getLocatorURI(String keyString) throws IllegalArgumentException {
        try {
            return keyString.substring(keyString.indexOf(" ") + 1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("string must be of format 'integerKey locatorUri': "+keyString);
        }
    }
    
    /**
     * Returns the string representation of this key (the key and the locator).
     * @return the string representation of this key 
     */
    @Override
    public String getText() {
        StringBuffer buf = new StringBuffer(Integer.toString(key));
        buf.append(' ').append(locatorURI);
        return buf.toString();
    }
        
    /**
     * Compare the keys according to the integer key
     * @param o the key to compare this key with
     */
    @Override
    public int compareTo(AbstractObjectKey o) {
        if (o == null)
            return 3;
        if (! (o.getClass().equals(IntegerKey.class)))
            return 3;
        if (key < ((IntegerKey) o).key)
            return -1;
        if (key > ((IntegerKey) o).key)
            return 1;
        return 0;
    }

    /**
     * Return the integer key itself.
     * @return the integer key itself
     */
    @Override
    public int hashCode() {
        return key;
    }

    /**
     * Equals according to the integer key. If the parameter is not of the IntegerKey class then 
     * return false.
     * @param obj object to compare this object to
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (! (obj.getClass().equals(IntegerKey.class)))
            return false;
        return (key == ((IntegerKey) obj).key);
    }
    
    /** Return the URI string. */
    @Override
    public String toString() {
        return (new StringBuffer(key)).append(": ").append(locatorURI).toString();
    }
    
}
