/*
 * OperationErrorCode.java
 *
 * Created on 22. zari 2005, 15:43
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package messif.operations;

import messif.utility.ErrorCode;

/**
 * Error codes related to operations.
 *
 * @author Michal Batko, <xbatko@fi.muni.cz>, Masaryk University, Czech Republic
 */
public class OperationErrorCode extends ErrorCode {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Response to a query has been returned, i.e. the query finished successfully. */
    public static OperationErrorCode RESPONSE_RETURNED = new OperationErrorCode("response returned");

    /** Empty answer returned. */
    public static OperationErrorCode EMPTY_ANSWER = new OperationErrorCode("empty answer returned");  
    
    /** Response to a query has not been returned completely, i.e. the same operation should be called again. */
    public static OperationErrorCode HAS_NEXT = new OperationErrorCode("having more objects to return");
    
    /** Creates a new instance of OperationErrorCode
     * 
     * @param text description of the error code
     */
    public OperationErrorCode(String text) {
        super(text);
    }
    
}
