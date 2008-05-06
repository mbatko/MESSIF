
package messif.pivotselection;

import java.util.ArrayList;
import java.util.List;
import messif.objects.GenericAbstractObjectList;
import messif.objects.GenericAbstractObjectList;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.PrecompDistPerforatedArrayFilter;
import messif.objects.PrecomputedDistancesFixedArrayFilter;

/**
 * This class uses the k-means algorithm adapted for metric spaces to cluster the objects
 *
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class KMeansPivotChooser extends AbstractPivotChooser {
    
    /** Size of the sample set to select a pivot from in each iteration of the k-means */
    public static final int PIVOTS_SAMPLE_SIZE = 1000;
    
    /** Threshhold to consider 2 pivots the same */
    public static final float PIVOTS_DISTINCTION_THRESHOLD = 0.1f;
    
    /** Maximal number of iterations to let run */
    public static final int MAX_ITERATIONS = 100;
    
    protected GenericAbstractObjectList<LocalAbstractObject> initialPivots;
    
    public KMeansPivotChooser() {
        this(null);
    }
    
    /** Creates a new instance of KMeansPivotChooser */
    public KMeansPivotChooser(GenericAbstractObjectList<LocalAbstractObject> initialPivots) {
        this.initialPivots = initialPivots;
    }
    
    /**
     *  This method only uses the preselected pivots as initial pivots for k-means and rewrites the pivots completely
     */
    protected void selectPivot(int count, GenericObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        
        // Store all passed objects temporarily
        GenericAbstractObjectList<LocalAbstractObject> objectList = new GenericAbstractObjectList<LocalAbstractObject>(sampleSetIterator);
        
        List<LocalAbstractObject> pivots = new ArrayList<LocalAbstractObject>(count);
        // initially select "count" pivots at random - or use (partly) preselected pivots
        if (initialPivots != null) {
            for (LocalAbstractObject preselPivot : initialPivots) {
                if (count > pivots.size()) {
                    pivots.add(preselPivot);
                    System.out.println("adding preselected pivot: "+preselPivot.getLocatorURI());
                }
            }
        }
        if (count > pivots.size()) {
            System.out.println("Selecting: "+(count - pivots.size()) +" pivots at random");
            pivots.addAll(objectList.randomList(count - pivots.size(), true, new GenericAbstractObjectList<LocalAbstractObject>()));
        }
        
        boolean continueKMeans = true;
        
        // one step of the k-means algorithm
        List<GenericAbstractObjectList<LocalAbstractObject>> actualClusters;
        int nIterations = 0;
        while (continueKMeans && (nIterations++ < MAX_ITERATIONS)) {
            System.out.println("Running "+nIterations+"th iteration");
            System.out.print("    Voronoi partitioning... ");
            actualClusters = voronoiLikePartitioning(objectList, pivots);
            System.out.println("done");
            StringBuffer buf = new StringBuffer("       cluster sizes: ");
            for (GenericAbstractObjectList<LocalAbstractObject> cluster : actualClusters) {
                buf.append(cluster.size()).append(", ");
            }
            System.out.println(buf.toString());
            
            System.out.println("    Selecting clustroids...");
            // now calculate the new pivots for the new clusters
            int i = 0;
            List<SelectClustroidThread> selectingThreads = new ArrayList<SelectClustroidThread>();
            for (GenericAbstractObjectList<LocalAbstractObject> cluster : actualClusters) {
                SelectClustroidThread thread = new SelectClustroidThread(cluster, pivots.get(i++));
                thread.start();
                selectingThreads.add(thread);
            }
            
            i = 0;
            continueKMeans = false;
            for (SelectClustroidThread thread : selectingThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (thread.clustroid == null) {
                    System.out.println("        WARNING: no clustroid selected - empty cluster?: "+ actualClusters.get(i).size());
                    //System.out.println("          selecting the pivot at random");
                    continueKMeans = true;
                } else {
                    float pivotShiftDist = pivots.get(i).getDistance(thread.clustroid);
                    if (pivotShiftDist > PIVOTS_DISTINCTION_THRESHOLD) {
                        System.out.println("        pivot "+ i +" shifted by "+pivotShiftDist);
                        pivots.set(i, thread.clustroid);
                        continueKMeans = true;
                    }
                }
                i++;
            }
        }
        
        //this.preselectedPivots.clear();
        for (LocalAbstractObject pivot : pivots)
            preselectedPivots.add(pivot);
        
    }
    
    /** Given a set of objects, a set of pivots, */
    private List<GenericAbstractObjectList<LocalAbstractObject>> voronoiLikePartitioning(GenericAbstractObjectList<LocalAbstractObject> objects, List<LocalAbstractObject> pivots) {
        List<GenericAbstractObjectList<LocalAbstractObject>> clusters =  new ArrayList<GenericAbstractObjectList<LocalAbstractObject>>(pivots.size());
        
        // precompute the mutual distances between the pivots
        for (LocalAbstractObject pivot : pivots) {
            PrecompDistPerforatedArrayFilter filter = pivot.getDistanceFilter(PrecompDistPerforatedArrayFilter.class);
            if (filter == null) {
                filter = new PrecompDistPerforatedArrayFilter(pivots.size());
                pivot.chainFilter(filter, true);
            } else filter.resetAllPrecompDist();
            
            for (LocalAbstractObject pivot2 : pivots) {
                filter.addPrecompDist(pivot.getDistance(pivot2));
            }
            //  init the cluster for this pivot
            clusters.add(new GenericAbstractObjectList<LocalAbstractObject>());
        }
        
        for (LocalAbstractObject object : objects) {
            PrecompDistPerforatedArrayFilter filter = object.getDistanceFilter(PrecompDistPerforatedArrayFilter.class);
            if (filter == null) {
                filter = new PrecompDistPerforatedArrayFilter(pivots.size());
                object.chainFilter(filter, true);
            } else filter.resetAllPrecompDist();
            
            float minDistance = Float.MAX_VALUE;
            int i = 0;
            int closestPivot = -1;
            float objPivotDist;
            for (LocalAbstractObject pivot : pivots) {
                if (object.excludeUsingPrecompDist(pivot, minDistance))
                    filter.addPrecompDist(LocalAbstractObject.UNKNOWN_DISTANCE);
                else {
                    objPivotDist = object.getDistance(pivot);
                    filter.addPrecompDist(objPivotDist);
                    if (minDistance > objPivotDist) {
                        closestPivot = i;
                        minDistance = objPivotDist;
                    }
                }
                i++;
            }
            clusters.get(closestPivot).add(object);
        }
        
        return clusters;
    }
    
    protected class SelectClustroidThread extends Thread {
        
        // parameter
        GenericAbstractObjectList<LocalAbstractObject> cluster;
        
        // always try the original pivot - put it to the sample pivots list
        LocalAbstractObject originalPivot;
        
        // result
        LocalAbstractObject clustroid;
        
        protected SelectClustroidThread(GenericAbstractObjectList<LocalAbstractObject> cluster, LocalAbstractObject originalPivot) {
            this.cluster = cluster;
            this.originalPivot = originalPivot;
        }
        
        public void run() {
            GenericAbstractObjectList<LocalAbstractObject> samplePivots = cluster.randomList(PIVOTS_SAMPLE_SIZE, true, new GenericAbstractObjectList<LocalAbstractObject>(PIVOTS_SAMPLE_SIZE));
            samplePivots.add(this.originalPivot);
            
            double minRowSum = Double.MAX_VALUE;
            for (LocalAbstractObject pivot : samplePivots) {
                double rowSum = 0;
                for (LocalAbstractObject object : cluster) {
                    rowSum += Math.pow(pivot.getDistance(object), 2);
                }
                if (minRowSum > rowSum) {
                    clustroid = pivot;
                    minRowSum = rowSum;
                }
            }
        }
    }
}
