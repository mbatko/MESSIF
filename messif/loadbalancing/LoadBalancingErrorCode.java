/*
 * LoadBalancingErrorCode.java
 *
 * Created on October 6, 2006, 12:42
 */

package messif.loadbalancing;

import messif.utility.ErrorCode;

/**
 * Error codes for load balancing operations.
 *
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class LoadBalancingErrorCode extends ErrorCode {
    /** Class id for serialization */
    private static final long serialVersionUID = 2L;

    /** the node was successfully created on given host */
    public static LoadBalancingErrorCode NODE_CREATED = new LoadBalancingErrorCode("Node successfully created");

    /** the node cannot be created on given host */
    public static LoadBalancingErrorCode ERROR_NODE_CREATION = new LoadBalancingErrorCode("Node cannot be created");
    
    /** the node cannot be created on given host */
    public static LoadBalancingErrorCode ERROR_NODE_MIGRATION = new LoadBalancingErrorCode("Node cannot be created at new host");

    /** The new host was registered by an existing host */
    public static LoadBalancingErrorCode HOST_REGISTERED = new LoadBalancingErrorCode("New host was successfully registered");

    /** The host answers: yes, you can use me for balancing */
    public static LoadBalancingErrorCode SUITABLE_HOST = new LoadBalancingErrorCode("Yes, I can be used for load balancing");
    
    /** The host answers: no, I don't want to be used for balancing now */
    public static LoadBalancingErrorCode NOT_SUITABLE = new LoadBalancingErrorCode("No, do not use me for balancing now");

    /** The host answers: yes, I will use you for load balancing */
    public static LoadBalancingErrorCode WILL_BALANCE = new LoadBalancingErrorCode("Yes, I will use you for load balancing");
    
    /** The host answers: No, I won't use you for load balancing */
    public static LoadBalancingErrorCode WONT_BALANCE = new LoadBalancingErrorCode("No, I won't use you for load balancing");

    /** The host answers: yes, you can use me for balancing */
    public static LoadBalancingErrorCode ERROR_BUCKET_NOT_EXISTS = new LoadBalancingErrorCode("Specified bucket does not exist at given replica");
    
    /** The host answers: yes, you can use me for balancing */
    public static LoadBalancingErrorCode REPLICA_REMOVED = new LoadBalancingErrorCode("Replica removed");
    
    /** The host answers: yes, you can use me for balancing */
    public static LoadBalancingErrorCode ERROR_REPLICA_REMOVAL = new LoadBalancingErrorCode("Error removing specified replica");
    
    /** The host answers: yes, you can use me for balancing */
    public static LoadBalancingErrorCode MIGRATION_REGISTERED = new LoadBalancingErrorCode("Migration (of a replica) successfully registered");
    
    /** A host tried to do a balancing action on a host that wasn't ask for */
    public static LoadBalancingErrorCode ERROR_NOT_ASKED = new LoadBalancingErrorCode("I wasn't asked about being used for load balancing");
    
    /** Creates a new instance of LoadBalancingErrorCode */
    public LoadBalancingErrorCode(String str) {
        super(str);
    }
}
