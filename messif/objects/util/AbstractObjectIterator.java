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
package messif.objects.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.buckets.FilterRejectException;
import messif.buckets.OccupationLowException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.ObjectProvider;
import messif.objects.UniqueID;

/**
 * Implementation of an iterator over a collection of {@link AbstractObject abstract objects}.
 * It provides methods for getting objects by their position, ID, data-equality, locator or
 * {@link ObjectMatcher}.
 * 
 * <p>
 * All methods are implemented in a "stream" fashion, i.e.
 * only the {@link #next()} method is used to accomplish all the getting methods.
 * Note that iterator cannot go back, so for example the {@link #getObjectByID(messif.objects.UniqueID) get-by-id}
 * method will only find the object if it has not been skipped in the iterator before.
 * </p>
 * 
 * @param <E> the class of the iterated objects
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class AbstractObjectIterator<E extends AbstractObject> implements Iterator<E>, ObjectProvider<E> {

    //****************** Methods for current object access ******************//

    /**
     * Returns an object returned by the last call to {@link #next()}.
     * @return an object returned by the last call to {@link #next()}
     * @throws NoSuchElementException if {@link #next()} has not been called yet
     */
    public abstract E getCurrentObject() throws NoSuchElementException;

    /**
     * Returns an ID of the object returned by the last call to {@link #next()}.
     * @return an ID of the object returned by the last call to {@link #next()}.
     * @throws NoSuchElementException if next() has not been called yet.
     */
    public UniqueID getCurrentObjectID() throws NoSuchElementException {
        return getCurrentObject().getObjectID();
    }

    /**
     * Returns an ID of the object returned by a call to {@link #next()}.
     * That is, the next object is obtained from the iterator via {@link #next()}
     * and its {@link AbstractObject#getObjectID() ID} is returned.
     * @return an ID of the object returned by a call to {@link #next()}
     */
    public UniqueID nextObjectID() { 
        return next().getObjectID();
    }


    //****************** GetBySomething methods ******************//

    /**
     * Returns an instance of object on the position of <code>position</code> from the current object.
     * Specifically, next() is called <code>position</code> times and then the current object is returned.
     * That is, position 0 means current object, 1 means the next object, etc.
     *
     * @param position the position from the current object
     * @return an instance of object on the position of <code>position</code> from the current object
     * @throws NoSuchElementException if such an object cannot be found.
     */
    public E getObjectByPosition(int position) throws NoSuchElementException {
        skip(position);
        
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
     * Returns the first instance of object with a locator that matches the
     * given regular expression. Note that this method can be called repeatedly
     * to obtain all objects the locator of which match.
     *
     * @param locatorRegexp the regular expression for matching the locators of objects that we are searching for
     * @return the first instance of object with a matching locator
     * @throws NoSuchElementException if there is no object with the specified locator
     */
    public final E getObjectByLocatorRegexp(String locatorRegexp) throws NoSuchElementException {
        while (true) {
            String locator = next().getLocatorURI(); // NoSuchElement is thrown automatically here when trying to access an object after the last one.
            if (locator != null && locator.matches(locatorRegexp))
                return getCurrentObject();
        }
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

    /**
     * Returns a randomly choosen object from the objects remaining in this iterator.
     * Note that all the remaining objects in this iterator are read.
     * @return a randomly choosen object
     * @throws NoSuchElementException if this iterator has no objects left
     */
    public E getRandomObject() throws NoSuchElementException {
        return getRandomObjects(1, false).get(0);
    }

    /**
     * Returns a list containing randomly choosen objects from the objects remaining in this iterator.
     * Note that all the remaining objects in this iterator are read.
     *
     * @param count the number of objects to return
     * @param unique flag if the returned list contains each object only once
     * @return a new list instance which contains randomly selected objects
     * @see AbstractObjectList#randomList(int, boolean, java.util.List, java.util.Iterator)
     */
    public AbstractObjectList<E> getRandomObjects(int count, boolean unique) {
        return AbstractObjectList.randomList(count, unique, this);
    }


    //****************** ObjectProvider implementation ******************//

    /**
     * Returns an iterator over the {@link ObjectProvider provided} objects.
     * This implementation of the {@link ObjectProvider} interface returns itself as an iterator.
     * @return an iterator over the {@link ObjectProvider provided} objects
     */
    @Override
    public AbstractObjectIterator<E> provideObjects() {
        return this;
    }


    //****************** Object matching ******************//

    /**
     * Returns matching objects.
     * Method returns all objects that satisfy the matching contraints specified by matcher,
     * i.e. {@link ObjectMatcher#match} method in the matcher returns non-zero when applied on them.
     * 
     * @param matcher The matching condition implemented in the ObjectMatcher interface.
     * @return a list of objects which satisfy the matching condition
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher<? super E> matcher) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        return getMatchingObjects(matcher, false);
    }

    /**
     * Returns matching objects.
     * Method returns all objects that satisfy the matching contraints specified by matcher
     * (i.e. {@link ObjectMatcher#match} method in the matcher returns non-zero when applied on them)
     * and deletes matching objects from the bucket when required.
     * 
     * @param matcher The matching condition implemented in the ObjectMatcher interface.
     * @param removeMatching Matching objects are also deleted from the bucket.
     *
     * @return a list of objects which satisfy the matching condition
     *
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher<? super E> matcher, boolean removeMatching) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        return getMatchingObjects(matcher, removeMatching, 0);
    }

    /**
     * Returns matching objects.
     * Method returns all objects that satisfy the matching contraints specified by matcher and deletes matching
     * objects from the bucket when required. An object is considered as matching if and only if
     * {@link ObjectMatcher#match} returns value different from whoStays.
     * 
     * @param matcher        The matching condition implemented in the ObjectMatcher interface.
     * @param removeMatching Matching objects are also deleted from the bucket.
     * @param whoStays       Identification of a partition whose objects stay in this bucket.
     *
     * @return a list of objects which satisfy the matching condition (i.e. {@link ObjectMatcher#match} is not equal 
     * to <code>whoStays</code> when applied on them)
     *
     * @throws NoSuchElementException if deletion reported it or if this method is called after next was called.
     * @throws FilterRejectException if delettion of a matching object was rejected by a filter (in case this is an iterator of LocalFilteredBucket).
     * @throws OccupationLowException if deletion of matching objects caused too low an occupation of bucket than allowed.
     */
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher<? super E> matcher, boolean removeMatching, int whoStays) throws NoSuchElementException, OccupationLowException, FilterRejectException {
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
    public GenericMatchingObjectList<E> getMatchingObjects(ObjectMatcher<? super E> matcher, boolean removeMatching, int[] whoStays) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        UnsupportedOperationException caughtException = null;
        
        // Prepare return holder
        GenericMatchingObjectList<E> rtv = new GenericMatchingObjectList<E>();
        
        // Sort whoStays array to be more efficient in checking
        Arrays.sort(whoStays);
        
        // Enumerate all objects
        while (hasNext()) {
            E obj = next();
            
            // Get matcher result for next object
            int matchingPart = matcher.match(obj);
            
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

    //****************** Singleton implementation ******************//

    /**
     * Returns an iterator on a single object.
     * @param <T> the class of the object returned by the iterator
     * @param object the object returned by the iterator
     * @return an iterator on a single object
     */
    public static <T extends LocalAbstractObject> AbstractObjectIterator<T> singleton(final T object) {
        return new AbstractObjectIterator<T>() {
            private boolean returned = false;
            @Override
            public T getCurrentObject() throws NoSuchElementException {
                if (!returned)
                    throw new NoSuchElementException();
                return object;
            }

            @Override
            public boolean hasNext() {
                return !returned;
            }

            @Override
            public T next() {
                if (returned)
                    throw new NoSuchElementException();
                returned = true;
                return object;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported on singleton iterator");
            }
        };
    }

    //****************** Other iterator methods ******************//

    /**
     * Skip the passed number of objects in the iterator.
     * @param cnt number of objects to skip
     * @throws NoSuchElementException if there are fewer objects than <code>cnt</code> remaining.
     * @return <code>this</code>
     */
    public AbstractObjectIterator<E> skip(int cnt) throws NoSuchElementException {
        for (; cnt > 0; cnt--)
            next(); // NoSuchElement is thrown automatically
        return this;
    }

}
