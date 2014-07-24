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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectMap;

/**
 * This class is to contruct MetaObjects from several simultaneously opened files (subObjectIterators). The iterator expects
 *  the files to store corresponding objects always in the same order alowing gaps - locators are always checked for
 *  actually "first" objects in the opened subObjectIterators. If more than one locator is at the top of the subObjectIterators, the majority
 *  of them is expected to be the right locator to be created at the moment and the other are not created.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
//public class StreamsMetaObjectMapIterator extends AbstractObjectIterator<MetaObjectMap> implements Closeable {
public class StreamsMetaObjectMapIterator extends AbstractStreamObjectIterator<MetaObjectMap> {

    /** Particular iterators */
    protected Map<String, StreamGenericAbstractObjectIterator<?>> subObjectIterators = new HashMap<String, StreamGenericAbstractObjectIterator<?>>();

    /** Instance of a next object. This is needed for implementing reading objects from a stream */
    protected MetaObjectMap nextObject;

    /** Instance of the current object */
    protected MetaObjectMap currentObject;

    /** Flag saying whether we have already started reading from the files or not yet. */
    protected boolean readingStarted;

    /** A flag that is set to <b>true</b> if a gap appeared in some of the files when constructing the last created meta object. */
    private boolean gapAppeared = false;


    /****************************************   Constructors and stream adders    *******************************/

    /** 
     * The empty constructor.
     */
    public StreamsMetaObjectMapIterator() {
    }

    /**
     * Add new object iterator given a name to be generated for this object in the MetaObjectMap.
     * @param name name to be generated for this object in the MetaObjectMap
     * @param iterator object iterator
     * @throws IllegalStateException when trying to add a stream and reading from the other subObjectIterators already started
     */
    public void addObjectStream(String name, StreamGenericAbstractObjectIterator<? extends LocalAbstractObject> iterator) throws IllegalStateException {
        if (readingStarted) {
            throw new IllegalStateException("adding new stream after the other streams but reading from other streams already started!");
        }
        subObjectIterators.put(name, iterator);
    }

    /**
     * Add new object iterator given a name to be generated for this object in the MetaObjectMap.
     * @param <T> the type of the objects that are created from the file
     * @param name name to be generated for this object in the MetaObjectMap
     * @param objClass the type of the objects that are created from the file
     * @param fileName file where the objects are stored
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public <T extends LocalAbstractObject> void addObjectStream(String name, Class<? extends T> objClass, String fileName) throws IllegalArgumentException, IOException {
        addObjectStream(name, new StreamGenericAbstractObjectIterator<T>(objClass, fileName));
    }


    // ************************     Interface methods   ************************************** //

    @Override
    public MetaObjectMap getCurrentObject() throws NoSuchElementException {
        if (currentObject == null)
            throw new NoSuchElementException("Can't call getCurrentObject() before first call to next()");
        return currentObject;
    }

    @Override
    public boolean hasNext() {
        if (! readingStarted) {
            try {
                startReadingObjects();
            } catch (IOException iOException) {
                throw new IllegalStateException("error closing file", iOException);
            }
        }
        return nextObject != null;
    }

    @Override
    public MetaObjectMap next() {
        try {
            // No next object available
            if (nextObject == null) {
                if (!readingStarted) {
                    startReadingObjects();
                }
                if (nextObject == null) {
                    throw new NoSuchElementException("No more objects in the stream");
                }
            }

            // Reading object on the fly from a stream
            currentObject = nextObject;
            setNextObject();
            return currentObject;
        } catch (IOException iOException) {
            throw new IllegalStateException("error closing file", iOException);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator doesn't support remove method - can't remove objects from file");
    }

    @Override
    public void close() throws IOException {
        for (StreamGenericAbstractObjectIterator<?> stream : subObjectIterators.values()) {
            stream.close();
        }
        this.nextObject = null;
    }


    // ***************************************    Internal methods   ************************************* //

    /** 
     * This method is internaly used to mark that the reading from the subObjectIterators started.
     * @throws IllegalStateException if there is a gap in the files and there are two candidates for the next locator
     * @throws IOException error while closing stream iterator that reached its end
     */
    public void startReadingObjects() throws IllegalStateException, IOException {
        this.readingStarted = true;
        setNextObject();
    }

    /**
     * Internal method to read objects from the strems and construct new metaobject and set the new object
     *  to {@link #nextObject}.
     * @throws IllegalStateException if there is a gap in the files and there are two candidates for the next locator
     * @throws IOException error while closing stream iterator that reached its end
     */
    protected void setNextObject() throws IllegalStateException, IOException {
        // call "next" on all subObjectIterators that were used to construct meta object last time
        if (gapAppeared) {
            for (String name : nextObject.getObjectNames()) {
                StreamGenericAbstractObjectIterator<?> iterator = subObjectIterators.get(name);
                if (iterator.hasNext())
                    iterator.next();
                else subObjectIterators.remove(name).close();
            }
        } else {
            for (Iterator<Map.Entry<String, StreamGenericAbstractObjectIterator<?>>> itEntry = subObjectIterators.entrySet().iterator(); itEntry.hasNext(); ) {
                Map.Entry<String, StreamGenericAbstractObjectIterator<?>> entry = itEntry.next();
                if (entry.getValue().hasNext()) {
                    entry.getValue().next();
                } else {
                    entry.getValue().close();
                    itEntry.remove();
                }
            }
        }

        // go over all objects and check their locators
        String currentLocator = null;
        Map<String, LocalAbstractObject> objects = new HashMap<String, LocalAbstractObject>();
        gapAppeared = false;
        for (Map.Entry<String, StreamGenericAbstractObjectIterator<?>> entry : subObjectIterators.entrySet()) {
            LocalAbstractObject currentSubObject = entry.getValue().getCurrentObject();
            if (currentSubObject.getLocatorURI() == null) {
                throw new IllegalStateException("empty locator not relevant for StreamMetaObjectMapIterator; " +
                        "(after creating object: "+((currentObject==null)?"null":currentObject.getLocatorURI())+ ")");
            }
            if ((currentLocator != null) && (! currentLocator.equals(currentSubObject.getLocatorURI()))) {
                gapAppeared = true;
                Logger.getLogger(getClass().getName()).info("GAP in streams: " + entry.getKey() + " provides " + currentSubObject.getLocatorURI() + " instead of " + currentLocator);
                break;
            }
            objects.put(entry.getKey(), currentSubObject);
            currentLocator = currentSubObject.getLocatorURI();
        }

        // if a gap appeared in the
        if (gapAppeared) {
            objects.clear();
            currentLocator = getMajorityObjects(objects);
        }

        if (objects.isEmpty())
            this.nextObject = null;
        else this.nextObject = new MetaObjectMap(currentLocator, objects);
    }

    /** 
     * If a gap appears in some of the subObjectIterators, find objects with prevailing locator.
     * @param objects OUTPUT parameter - put the prevailing top-stream objects there
     * @return locator of the prevailing top-stream objects
     * @throws IllegalStateException if there is a gap in the files and there are two candidates for the next locator
     */
    private String getMajorityObjects(Map<String, LocalAbstractObject> objects) throws IllegalStateException {

        // partition the top objects according to their locator
        Map<String, Map<String, LocalAbstractObject>> locatorObjectsMap = new HashMap<String, Map<String, LocalAbstractObject>>();
        for (Map.Entry<String, StreamGenericAbstractObjectIterator<?>> entry : subObjectIterators.entrySet()) {
            LocalAbstractObject currentSubObject = entry.getValue().getCurrentObject();
            Map<String, LocalAbstractObject> currentLocatorMap = locatorObjectsMap.get(currentSubObject.getLocatorURI());
            if (currentLocatorMap == null) {
                currentLocatorMap = new HashMap<String, LocalAbstractObject>();
                locatorObjectsMap.put(currentSubObject.getLocatorURI(), currentLocatorMap);
            }
            currentLocatorMap.put(entry.getKey(), currentSubObject);
        }

        // identify objects with the greatest number of locators
        Map.Entry<String, Map<String, LocalAbstractObject>> retVal = null;
        Map.Entry<String, Map<String, LocalAbstractObject>> conflictCandidate = null;
        int maxNumber = -1;
        for (Map.Entry<String, Map<String, LocalAbstractObject>> locatorObjects : locatorObjectsMap.entrySet()) {
            if (maxNumber == locatorObjects.getValue().size()) {
                conflictCandidate = locatorObjects;
                continue;
            }
            if (maxNumber < locatorObjects.getValue().size()) {
                retVal = locatorObjects;
                maxNumber = locatorObjects.getValue().size();
            }
        }

        // check the conflict
        if ((conflictCandidate != null) && (conflictCandidate.getValue().size() == retVal.getValue().size())) {
            throw new IllegalStateException("there is a gap in the files and there are two candidates for the next locator: "
                    + conflictCandidate.getKey() + "(" + conflictCandidate.getValue().keySet().iterator().next() + ")  and " + retVal.getKey() + "("+ retVal.getValue().keySet().iterator().next() + ")");
        }
        
        // return the prevailing objects and their locator
        objects.putAll(retVal.getValue());
        return retVal.getKey();
    }

    @Override
    public void setConstructorParameter(int index, Object paramValue) throws IndexOutOfBoundsException, IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setConstructorParameterFromString(int index, String paramValue, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, InstantiationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
