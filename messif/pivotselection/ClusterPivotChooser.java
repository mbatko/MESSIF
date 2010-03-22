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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import messif.buckets.BucketFilterAfterAdd;
import messif.buckets.BucketFilterAfterRemove;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;
import messif.utility.SortedCollection;

/**
 * This pivot chooser selects a varying number of pivots based on cluster sizes which are limited by the parameter passed to the constructor.
 *
 * Clustering of the sample set is done by creating as compact clusters as possible. The cluster radius (maximum distance within the cluster for the cluster's centroid)
 * is less than the threshold passed in the constructor. The clusteriods (pivots, centers of clusters) are selected as the objects having lowest sum of distances to all
 * other objects in the cluster.
 *
 * CAVEAT: This pivot chooser ignores the parameter <code>count</code> passed in all {@link #selectPivot} methods.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ClusterPivotChooser extends AbstractPivotChooser implements Serializable, BucketFilterAfterAdd, BucketFilterAfterRemove {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Size of the data sample used to create clusters. */
    public static int SAMPLE_SET_SIZE = 100;

    /**
     * Threshold on the maximum distance within a single cluster.
     */
    protected float maxClusterRadius;

    // *************** CONSTRUCTORS **********************

    /**
     * Creates a new instance of ClusterPivotChooser.
     * @param radius maximum radius of cluster created
     */
    public ClusterPivotChooser(float radius) {
        maxClusterRadius = radius;
    }

    // *************** PIVOT SELECTION IMPLEMENTATION **********************

    @Override
    protected void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        // Store all passed objects temporarily
        AbstractObjectList<? extends LocalAbstractObject> objectList = AbstractObjectList.randomList(SAMPLE_SET_SIZE, true, sampleSetIterator);

        // Precompute all distances within the objectList
        PrecomputedDistances pd = new PrecomputedDistances(objectList);

        // Initially, create a cluster for each object
        Map<Integer,Cluster> clusters = new HashMap<Integer,Cluster>();
        for (int i = 0; i < objectList.size(); i++)
            clusters.put(i, new Cluster(i, objectList.get(i), pd));

        // Initialize the queue of clusters to merge
        SortedCollection<Pair> queue = new SortedCollection<Pair>();
        int clusterCnt = clusters.size();
        for (int i = 0; i < clusterCnt - 1; i++) {
            for (int j = i + 1; j < clusterCnt; j++)
                queue.add(new Pair(clusters.get(i), clusters.get(j), pd));
        }

        // Keep merging two closest clusters if their radius is smaller than the threshold
        while (queue.size() > 0 && queue.first().getRadius() <= maxClusterRadius) {
            // Get first pair and merge the clusters
            Pair pMerge = queue.removeFirst();
            pMerge.getFirstCluster().mergeWithCluster(pMerge.getSecondCluster(), pd);

            // Remove the second cluster from the map of all clusters
            clusters.remove(pMerge.getSecondCluster().getId());

            // Remove all pairs having the second cluster of "p" and update radius of all pairs having the first cluster of "p"
            Iterator<Pair> it = queue.iterator();
            List<Pair> resort = new ArrayList<Pair>();
            while (it.hasNext()) {
                Pair p = it.next();
                if (p.getFirstCluster().equals(pMerge.getSecondCluster()) || p.getSecondCluster().equals(pMerge.getSecondCluster())) {
                    it.remove();
                } else if (p.getFirstCluster().equals(pMerge.getFirstCluster()) || p.getSecondCluster().equals(pMerge.getFirstCluster())) {
                    it.remove();
                    p.update(pd);
                    resort.add(p);
                }
            }
            queue.addAll(resort);
        }

        // Select clusteroid of the clusters produced
        for (Cluster c : clusters.values())
            preselectedPivots.add(c.getClusteroid());
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
     * A cache for distances between a pair of objects
     */
    protected class PrecomputedDistances {

        Map<UniqueID,Integer> objectToIndex;

        float[][] distances;

        /**
         * Create and initilize the new instance of PrecomputedDistances. All pairs of distances are computed and cached.
         * @param objectList list of objects
         */
        public PrecomputedDistances(AbstractObjectList<? extends LocalAbstractObject> objectList) {
            int objCount = objectList.size();

            // Initialize the map
            objectToIndex = new HashMap<UniqueID,Integer>();
            for (int i = 0; i < objCount; i++)
                objectToIndex.put(objectList.get(i).getObjectID(), i);

            // Initalize the distances
            distances = new float[objCount][objCount];
            for (int i = 0; i < objCount; i++) {
                distances[i][i] = 0f;
                for (int j = i + 1; j < objCount; j++) {
                    distances[i][j] = distances[j][i] = objectList.get(i).getDistance(objectList.get(j));
                }
            }
        }

        float getDistance(LocalAbstractObject obj1, LocalAbstractObject obj2) {
            int idx1 = objectToIndex.get(obj1.getObjectID());
            int idx2 = objectToIndex.get(obj2.getObjectID());
            return distances[idx1][idx2];
        }
    }

    /**
     * Class encapsulating objects of one cluster and storing the cluster's radius.
     */
    protected class Cluster {

        /** Current clusteroid of this cluster */
        private LocalAbstractObject clusteroid;

        /** Diameter of a cluster if cluster1 and cluster2 were merged. */
        protected float radius;

        /** Id of the cluster */
        protected int id;

        /** List of objects of this cluster */
        protected AbstractObjectList<LocalAbstractObject> objects;

//        /**
//         * Create a new Cluster containing the passed list of objects.
//         * This list is not cloned!
//         * @param id identification of the cluster
//         * @param objects list of objects which form the cluster
//         * @param pd cache of precomputed distances between all pairs of objects
//         */
//        public Cluster(int id, AbstractObjectList<LocalAbstractObject> objects, PrecomputedDistances pd) {
//            this.id = id;
//            this.objects = objects;
//            computeDiameter(pd);
//        }

        /**
         * Create a new Cluster containing just the passed object.
         * @param id identification of the cluster
         * @param object single object that forms the cluster
         * @param pd cache of precomputed distances between all pairs of objects
         */
        public Cluster(int id, LocalAbstractObject object, PrecomputedDistances pd) {
            this.id = id;
            this.objects = new AbstractObjectList<LocalAbstractObject>();
            this.objects.add(object);
            this.clusteroid = object;
            this.radius = 0f;
            //computeDiameter(pd);
        }

        /**
         * Copy constructor
         * @param model a cluster to create a copy of
         */
        public Cluster(Cluster model) {
            this.id = model.id;
            this.objects = new AbstractObjectList<LocalAbstractObject>(model.objects);
            this.clusteroid = model.clusteroid;
            this.radius = model.radius;
        }

        /**
         * Returns an object (clusteriod) that is in the center of this cluster.
         * This object has the minimum sum of distance to the other objects of this cluster than the others.
         *
         * @return clusteroid of this cluster
         */
        public LocalAbstractObject getClusteroid() {
            return clusteroid;
        }

        /**
         * Covering radius of this cluster (maximum distance between a pair of objects of this cluster).
         * @return radius of this cluster
         */
        public float getRadius() {
            return radius;
        }

        /**
         * Size of the cluster
         * @return nubmer of objects of this cluster
         */
        public int size() {
            return objects.size();
        }

        /**
         * Returns identification of this cluster
         * @return cluster's id
         */
        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Cluster)
                return id == ((Cluster)obj).id;
            else
                return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + this.id;
            return hash;
        }

        /**
         * Retrieves an object at the passed index.
         * @param index index of objects to retrive
         * @return object that is at the passed index in the list of objects forming this cluster
         */
        public LocalAbstractObject getObject(int index) {
            return objects.get(index);
        }

        /**
         * Returns all objects of this cluster as an iterator.
         * @return iterator over all objects in this cluster
         */
        public AbstractObjectIterator<LocalAbstractObject> getAllObjects() {
            return objects.iterator();
        }

        /**
         * Merge this cluster with the passed cluster and set the new cluster's radius.
         * @param cluster a cluster to add to this
         * @param pd cache of precomputed distances between all pairs of objects
         */
        public void mergeWithCluster(Cluster cluster, PrecomputedDistances pd) {
            objects.addAll(cluster.getAllObjects());
            updateClusteroid(pd);
            updateRadius(pd);
        }

        /**
         * Returns an object (clusteroid), i.e., the center of this cluster.
         * This object has the minimum sum of distance to the other objects of this cluster than the others.
         *
         * @param pd cache of precomputed distances between all pairs of objects
         */
        private void updateClusteroid(PrecomputedDistances pd) {
            clusteroid = null;
            double minRowSum = Double.MAX_VALUE;
            for (LocalAbstractObject pivot : objects) {
                double rowSum = 0;
                for (LocalAbstractObject object : objects)
                    rowSum += Math.pow(pd.getDistance(pivot, object), 2);
                
                if (minRowSum > rowSum) {
                    clusteroid = pivot;
                    minRowSum = rowSum;
                }
            }
        }

        /**
         * Initializes the member {@link #radius} by computing the maximum distance between two objects within this cluster.
         * @param pd cache of precomputed distances between all pairs of objects
         */
        private void updateRadius(PrecomputedDistances pd) {
            radius = 0f;
            int cnt = objects.size();
            for (int i = 0; i < cnt - 1; i++) {
                for (int j = i + 1; j < cnt; j++) {
                    float d = pd.getDistance(objects.get(i), objects.get(j));
                    if (d > radius)
                        radius = d;
                }
            }
        }
    }

    /**
     * Class encapsulating two clusters and the diameters of a cluster that would be produced be merging these clusters.
     */
    protected class Pair implements Comparable<Pair> {
        /** Cluster id */
        protected Cluster cluster1;
        /** Cluster id */
        protected Cluster cluster2;

        /** Clusteroid of the merged cluster */
        private LocalAbstractObject clusteroid;

        /** Covering radius of a cluster if cluster1 and cluster2 were merged. */
        protected float radius;

        /**
         * Creates a new pair of two cluster. It also computes the radius of the cluster that would result from merging
         * the passed clusters.
         *
         * @param cluster1 the first cluster
         * @param cluster2 the second cluster
         * @param pd cache of precomputed distances between all pairs of objects
         */
        public Pair(Cluster cluster1, Cluster cluster2, PrecomputedDistances pd) {
            this.cluster1 = cluster1;
            this.cluster2 = cluster2;

            // Compute the clusteroid and the radius if the passed clusters were joined.
            update(pd);
        }

        @Override
        public int compareTo(Pair o) {
            if (radius < o.radius)
                return -1;
            else if (radius > o.radius)
                return 1;
            else
                return clusteroid.compareTo(clusteroid);
        }

        /**
         * Clusteroid of the cluster that would be formed by merging the two clusters passed to the constructor.
         * @return clusteroid of the merged cluster
         */
        public LocalAbstractObject getClusteroid() {
            return clusteroid;
        }

        /**
         * Radius of the cluster that would result from merging the clusters of this pair.
         * @return radius of the merged cluster
         */
        public float getRadius() {
            return radius;
        }

        /**
         * Returns the first cluster in this pair
         * @return the first cluster
         */
        public Cluster getFirstCluster() {
            return cluster1;
        }

        /**
         * Returns the second cluster in this pair
         * @return the second cluster
         */
        public Cluster getSecondCluster() {
            return cluster2;
        }

        /**
         * Re-initializes the members {@link #clusteroid} and {@link #radius} by computing the covering ball of the cluster formed
         * by the first and the second cluster of this pair.
         * @param pd cache of precomputed distances between all pairs of objects
         */
        public void update(PrecomputedDistances pd) {
            Cluster merged = new Cluster(cluster1);

            merged.mergeWithCluster(cluster2, pd);

            clusteroid = merged.getClusteroid();
            radius = merged.getRadius();
        }

    }
}
