/*
 * GenericObjectIterator.java
 *
 * Created on 23. kveten 2006, 11:37
 */

package messif.objects;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.buckets.FilterRejectException;
import messif.buckets.OccupationLowException;

/**
 *
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public abstract class GenericObjectIterator<E extends AbstractObject> implements Iterator<E>, ObjectProvider<E> {
    
    /****************** Just for convenience *************/
    
    /** Returns an ID of the object returned by next().
     * That is the result of 'next().getObjectID()' is returned;
     */
    public UniqueID nextObjectID() { 
        return next().getObjectID();
    }
    
    /** Returns an instance of object returned by the last call to next().
     * @throws NoSuchElementException if next() has not been called yet.
     */
    public abstract E getCurrentObject() throws NoSuchElementException;
    
    /** Returns an ID of object returned by the last call to next().
     * @throws NoSuchElementException if next() has not been called yet.
     */
    public UniqueID getCurrentObjectID() throws NoSuchElementException {
        return getCurrentObject().getObjectID();
    }
    
    /**
     * Returns an instance of object on the position of <code>position</code> from the current object.
     * Specifically, next() is called <code>position</code> times and then the current object is returned.
     * That is, position 0 means current object, 1 means the next object, etc.
     *
     * @throws NoSuchElementException if such an object cannot be found.
     */
    public E getObjectByPosition(int position) throws NoSuchElementException {
        for (; position > 0; position--)
            next(); // NoSuchElement is thrown automatically
        
        return getCurrentObject();
    }

    /**
     * Returns the first instance of object, that has the specified ID.
     *
     * @param objectID ID of the object that we are searching for
     * @return the first instance of object, that has the specified objectID
     * @throws NoSuchElementException if such an object cannot be found.
     */
    public E getObjectByID(UniqueID objectID) throws NoSuchElementException {
        if (objectID == null)
            throw new NoSuchElementException("Cannot search for null object ID");
        while (true)
            if (next().equals(objectID)) // NoSuchElement is thrown automatically here when trying to access an object after the last one.
                return getCurrentObject();
    }

    /**
     * Returns the first instance of object, that has data equal to the provided object.
     *
     * @param object the object to match the data against
     * @return the first instance of object, that has the data equal to the specified object
     * @throws NoSuchElementException if such an object cannot be found.
     */
    public E getObjectByData(LocalAbstractObject object) throws NoSuchElementException {
        if (object == null)
            throw new NoSuchElementException("Cannot search for null object");
        while (true)
            if (object.dataEquals(next())) // NoSuchElement is thrown automatically here when trying to access an object after the last one.
                return getCurrentObject();
    }

    /**
     * Returns the first instance of object, that has the specified locator.
     *
     * @param locatorURI the locator of the object that we are searching for
     * @return the first instance of object, that has specified locatorURI
     * @throws NoSuchElementException if there is no object with the specified locator
     */
    public final E getObjectByLocator(String locatorURI) throws NoSuchElementException {
        return getObjectByAnyLocator((locatorURI == null)?null:Collections.singleton(locatorURI), false);
    }

    /**
     * Returns the first instance of object, that has one of the specified locators.
     *
     * @param locatorURIs the set of locators that we are searching for
     * @param removeFound if <tt>true</tt> the locators which were found are removed from the <tt>locatorURIs</tt> set, otherwise, <tt>locatorURIs</tt> is not touched
     * @return the first instance of object, that has one of the specified locators
     * @throws NoSuchElementException if there is no object with any of the specified locators
     */
    public E getObjectByAnyLocator(Set<String> locatorURIs, boolean removeFound) throws NoSuchElementException {
        if (locatorURIs == null)
            throw new NoSuchElementException("Cannot search for null locator set");
        while (true)
            if (locatorURIs.contains(next().getLocatorURI())) {// NoSuchElement is thrown automatically here when trying to access an object after the last one.
                if (removeFound)
                    locatorURIs.remove(getCurrentObject().getLocatorURI());
                return getCurrentObject();
            }
    }

    /****************** Object matching ******************/
    
    /** Get matching objects
     * Method returns all objects that satisfy the matching contraints specified by matcher,
     * i.e. <code>match</code> method in the matcher returns non-zero when applied on them.
     * 
     * @param matcher The matching condition implemented in the ObjectMatcher interface.
     *
     * @return
     * Returns list of objects which satisfy the matching condition.
     *
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher matcher) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        return getMatchingObjects(matcher, false);
    }

    /** Get matching objects
     * Method returns all objects that satisfy the matching contraints specified by matcher
     * (i.e. <code>match</code> method in the matcher returns non-zero when applied on them)
     * and deletes matching objects from the bucket when required.
     * 
     * @param matcher The matching condition implemented in the ObjectMatcher interface.
     * @param removeMatching Matching objects are also deleted from the bucket.
     *
     * @return
     * Returns list of objects which satisfy the matching condition.
     *
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher matcher, boolean removeMatching) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        return getMatchingObjects(matcher, removeMatching, 0);
    }

        
    /** Get matching objects
     * Method returns all objects that satisfy the matching contraints specified by matcher and deletes matching
     * objects from the bucket when required. An object is considered as matching if and only if
     * ObjectMatcher.match() returns value different from whoStays.
     * 
     * @param matcher        The matching condition implemented in the ObjectMatcher interface.
     * @param removeMatching Matching objects are also deleted from the bucket.
     * @param whoStays       Identification of a partition whose objects stay in this bucket.
     *
     * @return
     * Returns list of objects which satisfy the matching condition (i.e. ObjectMatcher.match() is not equal 
     * to whoStays when applied on them).
     *
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher matcher, boolean removeMatching, int whoStays) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        int[] whoStaysArray = {whoStays};
        return getMatchingObjects(matcher, removeMatching, whoStaysArray);
    }
        
    /** Get matching objects
     * Method returns all objects that satisfy the matching contraints specified by matcher and deletes matching
     * objects from the bucket when required. An object is considered as matching if and only if
     * ObjectMatcher.match() returns value different from all elements of whoStays.
     * 
     * @param matcher        The matching condition implemented in the ObjectMatcher interface.
     * @param removeMatching Matching objects are also deleted from the bucket.
     * @param whoStays       An array of identifications of partitions whose objects stay in this bucket.
     *
     * @return
     * Returns list of objects which satisfy the matching condition (i.e. ObjectMatcher.match() is not equal 
     * to whoStays when applied on them).
     *
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher matcher, boolean removeMatching, int[] whoStays) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        UnsupportedOperationException caughtException = null;
        
        // Prepare return holder
        GenericMatchingObjectList<E> rtv = new GenericMatchingObjectList<E>();
        
        // Sort whoStays array to be more efficient in checking
        Arrays.sort(whoStays);
        
        // Enumerate all objects
        while (hasNext()) {
            E obj = next();
            
            // Get matcher result for next object
            int matchingPart = matcher.match(obj.getLocalAbstractObject());
            
            // If matchingPart is not found in whoStays
            if (Arrays.binarySearch(whoStays, matchingPart) < 0) {
                // Remove object if necessary
                if (removeMatching) {
                    try {
                        remove();
                    } catch (UnsupportedOperationException e) {
                        if (caughtException == null) {
                            // No exception intercepted yet, store this to throw it at the end of the method.
                            caughtException = e;
                        }
                    }
                }
                
                // Add to return array
                rtv.add(obj, matchingPart);
            }
        }
        
        // Throw the first intercepted exception.
        if (caughtException != null) {
            Throwable c = caughtException.getCause();
            if (c != null) {
                if (c instanceof OccupationLowException)
                    throw (OccupationLowException)c;
                else if (c instanceof NoSuchElementException)
                    throw (NoSuchElementException)c;
                else if (c instanceof FilterRejectException)
                    throw (FilterRejectException)c;
            }
            throw caughtException;
        }
        
        return rtv;
    }



    /**
     * The iterator returning provided objects must be returned.
     *
     * @return iterator for provided objects
     */
    public GenericObjectIterator<E> provideObjects() {
        return this;
    }
}
