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
package messif.pivotselection;

import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.statistics.StatisticSimpleWeakrefCounter;

/**
 * This pivot chooser selects a varying number of pivots based on cluster sizes
 * which are limited by the parameter passed to the constructor.
 * The resulting clusters are far from optimal, since the first object that
 * does not join any cluster (i.e. all its distances to the current cluster
 * centroids are greater than the maxClusterRadius) is selected as new cluster
 * centroid.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SimpleClusterPivotChooser extends AbstractPivotChooser implements Serializable {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Statistic reference counter for number of objects that are mapped to a given cluster */
    private static final StatisticSimpleWeakrefCounter refcounterObjectCount = StatisticSimpleWeakrefCounter.getStatistics("ClusterPivotChooser.ObjectCount");

    /** Threshold on the maximum radius of a single cluster, i.e. one half of maximal distance between any two objects from a cluster */
    private final float maxClusterRadius;

    /**
     * Creates a new instance of SimpleClusterPivotChooser for the given maximal cluster radius.
     * @param maxClusterRadius the maximal cluster radius
     */
    public SimpleClusterPivotChooser(float maxClusterRadius) {
        this.maxClusterRadius = maxClusterRadius;
    }

    /**
     * Searches all the currently selected pivots for a pivot, where
     * the object can be accumulated.
     * @param object the object to search the pivots
     * @return the pivot to which the object belongs or <tt>null</tt> if there is no such pivot
     */
    protected LocalAbstractObject assignToPivot(LocalAbstractObject object) {
        for (LocalAbstractObject pivot : preselectedPivots) {
            if (pivot.getDistance(object) < maxClusterRadius)
                return pivot;
        }
        return null;
    }

    @Override
    protected synchronized void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        while (sampleSetIterator.hasNext() && count > 0) {
            LocalAbstractObject object = sampleSetIterator.next();
            LocalAbstractObject assignedPivot = assignToPivot(object);
            if (assignedPivot != null) {
                refcounterObjectCount.add(assignedPivot);
            } else {
                preselectedPivots.add(object);
                count--;
            }
        }
    }

}
