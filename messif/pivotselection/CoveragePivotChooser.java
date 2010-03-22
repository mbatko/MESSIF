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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import messif.buckets.BucketFilterAfterAdd;
import messif.buckets.BucketFilterAfterRemove;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;

/**
 * This pivot chooser selects the pivots in a way that a good coverage of data by ball regions with centers in pivots and radius equal
 * to a fixed value (threshold) is ensured.
 * The fixed valus, so-called threshold on radius, is passed to the constructor of this class.
 *
 * The coverage is done by putting the balls over the data in a way that the balls do not intersect. This implies
 * that some objects might not have been covered by a ball.
 *
 * The procedure starts by defining a ball for each object. The first output ball is the most populated ball (the ball containing the
 * highest number of objects). Next, all balls that intersect are eliminated. The second output ball is the most populated ball again.
 * This procedure is repeated until there are some non-eliminated balls.
 *
 * Because we eliminated all balls that intersect, so the population of a ball (number of objects in the ball) is computed
 * as with the radius equal to double value of the threshold. This implies that the most populated ball leads to elimination of the most
 * other balls.
 *
 * CAVEAT: This pivot chooser ignores the parameter <code>count</code> passed in all {@link #selectPivot} methods.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class CoveragePivotChooser extends AbstractPivotChooser implements Serializable, BucketFilterAfterAdd, BucketFilterAfterRemove {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Size of the data sample used to create clusters. */
    public static int SAMPLE_SET_SIZE = 200;

    /** Ball size -- radius of the cluster. */
    protected final float clusterRadius;

    /** Ball size multiplied by two */
    protected final float clusterDiameter;

    // *************** CONSTRUCTORS **********************

    /**
     * Creates a new instance of CoveragePivotChooser.
     * @param radius radius of clusters (balls) used to create the coverage
     */
    public CoveragePivotChooser(float radius) {
        clusterRadius = radius;
        clusterDiameter = 2f * clusterRadius;
        System.err.println("Initializing " + CoveragePivotChooser.class.getSimpleName() + " with radius " + clusterRadius);
    }

    // *************** PIVOT SELECTION IMPLEMENTATION **********************

    @Override
    protected void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        // Store all passed objects temporarily
        AbstractObjectList<? extends LocalAbstractObject> objectList = AbstractObjectList.randomList(SAMPLE_SET_SIZE, true, sampleSetIterator);

        // Precompute all distances within the objectList
        PrecomputedDistances pd = new PrecomputedDistances(objectList);

        // Initially, create a ball for each object and queue it (sorted by the size of the cluster -- population of the ball)
        List<Ball> list = new LinkedList<Ball>();
        Ball bestBall = null;
        for (int i = 0; i < objectList.size(); i++) {
            Ball b = new Ball(objectList.get(i), i, pd);
            list.add(b);
            if (bestBall == null || bestBall.getObjects() < b.getObjects())
                bestBall = b;
        }

        // Keep selecting the first ball in the queue and removing all ball that are intersecting
        while (!list.isEmpty()) {
            Iterator<Ball> it;

            // Add the best ball's center to the selected pivots
            preselectedPivots.add(bestBall.getPivot());
            System.err.println("Adding pivot " + bestBall.getPivot() + ", cluster size " + bestBall.getObjects());

            // Remove all intersecting balls
            it = list.iterator();
            while (it.hasNext()) {
                Ball b = it.next();
                if (b.intersectsWith(bestBall, pd)) {
                    it.remove();
                    b.decreaseIfContained(list, pd);
                }
            }

            // Obtain new best ball
            bestBall = null;
            for (Ball b : list) {
                if (bestBall == null || bestBall.getObjects() < b.getObjects())
                    bestBall = b;
            }
        }
    }

    // *************** SUPPORT FOR ON-FLY PIVOT SELECTION **********************

    @Override
    public void filterAfterAdd(LocalAbstractObject object, LocalBucket bucket) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void filterAfterRemove(LocalAbstractObject object, LocalBucket bucket) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // *************** INTERNALS **********************

    /**
     * Class encapsulating info about each cluster (ball region).
     */
    protected class Ball implements Comparable<Ball> {
        /** Center of the ball */
        protected final LocalAbstractObject pivot;
        /** Index of the center in the precomputed distances cache */
        protected final int pivotIndex;
        /** Number of objects covered by this ball and its neighborhood of size of clusterRadius */
        protected int objects;

        /**
         * Create a new Ball and compute the number of objects covered by the ball and its neighbohood,
         * see {@link #objects} for more information.
         * @param pivot       object as the cluster (ball) center
         * @param pivotIndex  index of this pivot in precomputed distance cache
         * @param pd          cache of precomputed distance used to compute the number of objects covered by the ball
         */
        public Ball(LocalAbstractObject pivot, int pivotIndex, PrecomputedDistances pd) {
            this.pivot = pivot;
            this.pivotIndex = pivotIndex;

            // Compute the population of this ball
            int cnt = 0;
            for (int i = 0; i < pd.getObjectCount(); i++) {
                if (pd.getDistance(pivotIndex, i) < clusterDiameter)
                    cnt++;
            }
            this.objects = cnt;
        }


        /**
         * Number of objects covered by this ball and its close neighborhood.
         * In particular, the ball centered at {@link #pivot} with the radius set to double value of {@link CoveragePivotChooser#clusterRadius}
         * is used for this computation.
         * @return number of objects
         */
        public int getObjects() {
            return objects;
        }

        /**
         * Center of this ball.
         * @return object that is the center of this ball
         */
        public LocalAbstractObject getPivot() {
            return pivot;
        }

        /**
         * Index of the enter of this ball in the precomputed distances cache
         * @return index of the ball's center
         */
        public int getPivotIndex() {
            return pivotIndex;
        }

        @Override
        public int compareTo(Ball b) {
            return (b.objects - this.objects);
        }

        /**
         * Checks if this ball and the passed ball intersect or not.
         * @param b   ball to check
         * @param pd  cache of precomputed distance used to obtain inter-ball-centers' distance
         * @return <code>true</code> if the balls intersect, oterwise <code>false</code>.
         */
        public boolean intersectsWith(Ball b, PrecomputedDistances pd) {
            return (pd.getDistance(this.pivotIndex, b.pivotIndex) < clusterDiameter);
        }

        private void decreaseIfContained(List<Ball> list, PrecomputedDistances pd) {
            for (Ball b : list) {
                if (pd.getDistance(getPivotIndex(), b.getPivotIndex()) < clusterDiameter)
                    b.objects--;
            }
        }
    }

    /**
     * A cache for distances between a pair of objects
     */
    protected class PrecomputedDistances {

        final int objectCount;

        final Map<UniqueID,Integer> objectToIndex;

        final float[][] distances;

        /**
         * Create and initilize the new instance of PrecomputedDistances. All pairs of distances are computed and cached.
         * @param objectList list of objects
         */
        public PrecomputedDistances(AbstractObjectList<? extends LocalAbstractObject> objectList) {
            objectCount = objectList.size();

            // Initialize the map
            objectToIndex = new HashMap<UniqueID,Integer>();
            for (int i = 0; i < objectCount; i++)
                objectToIndex.put(objectList.get(i).getObjectID(), i);

            // Initalize the distances
            distances = new float[objectCount][objectCount];
            for (int i = 0; i < objectCount; i++) {
                distances[i][i] = 0f;
                for (int j = i + 1; j < objectCount; j++) {
                    distances[i][j] = distances[j][i] = objectList.get(i).getDistance(objectList.get(j));
                }
            }
        }

        float getDistance(LocalAbstractObject obj1, LocalAbstractObject obj2) {
            return getDistance(objectToIndex.get(obj1.getObjectID()),
                               objectToIndex.get(obj2.getObjectID()));
        }

        float getDistance(int index1, int index2) {
            return distances[index1][index2];
        }

        /**
         * Number of objects cached in this class is returned.
         * @return number of objects that all pairs of distances among them are stored.
         */
        public int getObjectCount() {
            return objectCount;
        }
    }

}
