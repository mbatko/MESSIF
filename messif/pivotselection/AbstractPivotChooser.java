/*
 * AbstractPivotChooser.java
 *
 * Created on 19. unor 2004, 1:44
 */

package messif.pivotselection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.ObjectProvider;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.ObjectProvidersIterator;


/**
 * Abstract class for pivot selection algorithms hierarchy
 *
 * This class provides basic methods for selecting and accessing pivots and automatically registers 
 * statistic <I>DistanceComputations.PivotChooser</I>, i.e. number of distance computations
 * spent in choosing pivots. Pivots are automatically selected if additional pivots are required
 * by pivot getter methods.
 *
 * Pivot selection also supports transactions - if a transaction is initialized, the actual state
 * of the list of selected objects is remember. The transaction ends by either commiting all changes
 * made to the list or by rolling back all the changes.
 * 
 *
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 *
 */
public abstract class AbstractPivotChooser {
    
    /** List of selected pivots */
    protected final List<LocalAbstractObject> preselectedPivots = new AbstractObjectList<LocalAbstractObject>();
    

    /****************** Pivot Access Methods ******************/
    
    /**
     * Clears the list of preselected pivots.
     */
    public void clear() {
        preselectedPivots.clear();
    }
    
    /**
     * The number of currently selected pivots.
     * If a transaction is running, it returns the current state
     * not the state before the transaction has been initiated.
     *
     * @return Returns the number of currently selected pivots
     */
    public int size() {
        return preselectedPivots.size();
    }
    
    /**
     * Provides a read-only iterator over the collection of currently selected pivots
     *
     * @return Returns iterator over a collection of currently selected pivots
     */
    public Iterator<LocalAbstractObject> iterator() {
        return preselectedPivots.iterator();
    }
    
    
    /**
     * Access to the first selected pivot (even if more pivots are available).
     * The pivot selection is automatically called if necessary.
     *
     * @return Returns the first pivot
     */
    public LocalAbstractObject getPivot() {
        return getPivot(0);
    }
    
    /**
     * Access to the last selected pivot.
     * Null is returned if there are no selected pivots yet,
     * i.e. no automatic selection is issued.
     * 
     * @return Returns the last pivot from the list of selected pivots.
     */
    public LocalAbstractObject getLastPivot() {
        if (preselectedPivots.size() == 0)
            return null;
        return getPivot(preselectedPivots.size() - 1);
    }
    
    /**
     * Returns a new pivot that is freshly selected. The pivot selection
     * is always called to choose additional pivot.
     * 
     * @return Returns a new pivot
     */
    public LocalAbstractObject getNextPivot() {
        return getPivot(preselectedPivots.size());
    }
    
    /**
     * Returns a reference to a preselected pivot at the desired position.
     * If the list does not contain enough pivots, the remaining ones are newly selected.
     * Moreover, if the position is still out of range, method throws IndexOutOfBoundsException.
     * 
     * @param position The possition (number) of the requested pivot
     * @return Returns the pivot at requested position
     * @throws IndexOutOfBoundsException Thrown if the pivot selection cannot select enough additional pivots
     */
    public LocalAbstractObject getPivot(int position) throws IndexOutOfBoundsException {
        synchronized (preselectedPivots) {
            // Select all missing pivots at once
            if (preselectedPivots.size() < position + 1)
                selectPivot(position + 1 - preselectedPivots.size());
            
            return preselectedPivots.get(position);
        }
    }


    /****************** Pivot list change methods ******************/
    
    /**
     * Deletes the last pivot from the list of current pivots and returns it.
     *
     * @return Returns an instance of AbstractObject which is the pivot that
     *         got deleted.
     *         Returns null if there are no more pivots to delete.
     */
    public LocalAbstractObject removeLastPivot() {
        synchronized (preselectedPivots) {
            if (preselectedPivots.size() == 0)
                return null;
            
            return preselectedPivots.remove(preselectedPivots.size() - 1);
        }
    }

    /**
     * This method appends a new pivot to the currently existing list.
     * @param pivot The pivot added to the preselected list
     */
    public void addPivot(LocalAbstractObject pivot) {
        preselectedPivots.add(pivot);
    }

    /**
     * Selects one pivot and appends it to the list of pivots.
     * This method internally calls selectPivot(1).
     */
    public final void selectPivot() {
        selectPivot(1);
    }

    /**
     * Method for selecting pivots and appending them to the list of pivots.
     * It is called from getPivot() automatically.
     *
     * @param count  Number of pivots to generate
     */
    public final void selectPivot(int count) {
        selectPivot(count, new ObjectProvidersIterator<LocalAbstractObject>(sampleProviders));
    }

    /**
     * This method carries out the actual pivot selection and must be implemented by
     * subclasses. The implementation must select at least <i>count</i> pitvots and
     * add them by <code>addPivot</code> method.
     * @param count Number of pivots to generate
     * @param sampleSetIterator Iterator over the sample set of objects to choose new pivots from
     */
    protected abstract void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator);


    /****************** Sample set management methods ******************/
    
    /** Registered sample providers */
    protected final Set<ObjectProvider<? extends LocalAbstractObject>> sampleProviders = Collections.synchronizedSet(new HashSet<ObjectProvider<? extends LocalAbstractObject>>());

    /**
     * Registers a new sample set provider used by this pivot chooser to select pivots.
     * @param provider the sample set provider
     */
    public void registerSampleProvider(ObjectProvider<? extends LocalAbstractObject> provider) {
        sampleProviders.add(provider);
    }

    /**
     * Removes registration of a sample set provider that was previously registered
     * (others are silently ignored).
     * @param provider the previously registered sample set provider
     */
    public void deregisterSampleProvider(ObjectProvider<? extends AbstractObject> provider) {
        sampleProviders.remove(provider);
    }

}
