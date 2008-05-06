/*
 * RandomPivotChooser.java
 *
 * Created on 8. cervenec 2004, 13:21
 */

package messif.pivotselection;

import java.io.Serializable;
import messif.objects.GenericAbstractObjectList;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;

/**
 * RandomPivotChooser provides the capability of selecting a random object from the whole bucket
 * 
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class RandomPivotChooser extends AbstractPivotChooser implements Serializable {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Creates a new instance of RandomPivotChooser */
    public RandomPivotChooser() {
    }
    
    /** Method for selecting pivots and appending to the list of pivots.
     * It is used in getPivot() function for automatic selection of missing pivots.
     *
     * Statistics are maintained automatically.
     */
    protected void selectPivot(int count, GenericObjectIterator<? extends LocalAbstractObject> samplesIter) {
        for (LocalAbstractObject o : GenericAbstractObjectList.randomList(count, false, samplesIter))
            preselectedPivots.add(o);
    }
    
}
