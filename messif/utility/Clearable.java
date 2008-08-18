/*
 * Clearable
 * 
 */

package messif.utility;

/**
 * Interface for marking objects that have clearable data.
 * Its method {@link #clearSurplusData} is intended to be called whenever the object is
 * sent back to client in order to minimize problems with unknown
 * classes after deserialization.
 *
 * @author xbatko
 */
public interface Clearable {

    /**
     * Clears the data stored in this object.
     */
    public void clearSurplusData();
}
