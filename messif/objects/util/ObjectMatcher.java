/*
 * ObjectMatcher.java
 *
 * Created on 4. kveten 2003, 21:10
 */

package messif.objects.util;

import messif.objects.LocalAbstractObject;


/**
 * Interface which provides matching capabilities. 
 * Matching functionality is used when you need to filter out some objects of the whole bucket, for example.
 *
 *
 * @author  Michal Batko, Faculty of Informatics, Masaryk University, Brno, Czech Republic, xbatko@fi.muni.cz
 */
public interface ObjectMatcher {
    
    /**
     * Matching method.
     * This method provides matching functionality and is used for categorization of objects into groups 
     * (partitions).
     *
     * @param object An object that is tested for the matching condition.
     *
     * @return Returns an identification of partition to which the object falls.
     *         When applied on a bucket (through the method GetMatchingObjects()) it is convenient to return 0 for all objects
     *         which stay in the bucket. Zero value returned means that object doesn't match.
     */
    public int match(LocalAbstractObject object);
}
