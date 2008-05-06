/*
 * ObjectStringEditDistSleep.java
 *
 * Created on October 3, 2006, 10:55
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;


/**
 * An object that whose getDistance() method takes 10 miliseconds more than std Edit distance
 *
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class ObjectStringEditDistSleep extends ObjectStringEditDist {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectStringEditDistSleep(String text) {
        super(text);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectStringEditDistSleep() {
        super();
    }
    
    /** Creates a new instance of Object random generated 
     * with minimal length equal to minLength and maximal 
     * length equal to maxLength */
    public ObjectStringEditDistSleep(int minLength, int maxLength) {
        super(minLength, maxLength);
    }
    
    /** Creates a new instance of Object from stream */
    public ObjectStringEditDistSleep(BufferedReader stream) throws IOException {
        super(stream);
    }
    
    
    /** Metric function
     *      Implements euclidean distance measure (so-called L2 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        synchronized (this) {
            try {
                this.wait(10);
            } catch (InterruptedException ignore) { }
        }
        return super.getDistanceImpl(obj, distThreshold);
    }
}
