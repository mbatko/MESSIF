/*
 * ErrorCode.java
 *
 * Created on 20. zari 2005, 18:40
 *
 */

package messif.utility;

import java.io.Serializable;


/**
 *
 * @author xbatko
 */
public class ErrorCode implements Serializable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /***************** Standard error codes *****************/

    /** The initial value of an error code that was not set yet. */
    public static ErrorCode NOT_SET = new ErrorCode("");

    /** Not specific error appeared. You may look at the source code which produced it to get some help. */
    public static ErrorCode UNKNOWN_ERROR = new ErrorCode("unknown error");
    
    
    /***************** Internal data *****************/

    /** Holder of the current error code text */
    protected final String text;
    
    
    /***************** Constructors *****************/

    /** Creates a new instance of ErrorCode.
     *  Use this constructor to create static members in classes to define
     *  new error codes.
     */
    protected ErrorCode(String text) {
        this.text = text;
    }
    
    /** Creates a new instance of ErrorCode.
     *  Use this constructor to create static members as a copy
     *  of another constant. That is, the constants will be
     *  equal, but accessible by different names.
     */
    protected ErrorCode(ErrorCode source) {
        this(source.text);
    }
    
    
    /***************** State query methods *****************/
    
    /** Returns true if the error code is not set yet (i.e. has the value of NOT_SET) */
    public boolean isSet() { return !NOT_SET.equals(this); }
    
    /** Returns true if the error code is unknown error (i.e. has the value of UNKNOWN_ERROR) */
    public boolean isUnknownError() { return UNKNOWN_ERROR.equals(this); }


    /***************** Interface and global overrides *****************/
    
    public String toString() {
        return text;
    }
    
    public int hashCode() {
        return text.hashCode();
    }
    
    public boolean equals(Object object) {
        if (object instanceof ErrorCode)
            return ((ErrorCode)object).text.equals(text) && (object.getClass().equals(this.getClass()));
        else return false;
    }
    
}
