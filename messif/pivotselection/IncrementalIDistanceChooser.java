/*
 * IncrementalIDistanceChooser.java
 *
 * Created on 15. unor 2005, 11:49
 */

package messif.pivotselection;

import java.io.Serializable;
import java.util.Iterator;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;


/**
 *
 * @author xnovak8
 */
public class IncrementalIDistanceChooser extends AbstractPivotChooser implements Serializable {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Size of the sample set used to test the candidate pivot (used to estimate mu_d) */
    public static int sampleSetSize = 1000;
    
    /** Size of the candidate set of pivots from which one pivot will be picked. */
    public static int samplePivotSize = 100;  //50;
    
    /** Creates a new instance of IncrementalPivotChooser */
    public IncrementalIDistanceChooser() {
    }
    
    /** @return the closest pivot for the passed object */
    private LocalAbstractObject getClosestPivot(LocalAbstractObject x, Iterator<? extends AbstractObject> pivotIter) {
        // select pivot closest to "x"
        float minVal = Float.MAX_VALUE;
        float tmpDist;
        LocalAbstractObject preselectedPivot = null;
        LocalAbstractObject pivotX = pivotIter.hasNext() ? ((LocalAbstractObject) pivotIter.next()):null;
        preselectedPivot = pivotX;
        for (; pivotIter.hasNext(); pivotX = (LocalAbstractObject) pivotIter.next()) {
            tmpDist = x.getDistance(pivotX);
            if (minVal > tmpDist) {
                minVal = tmpDist;
                preselectedPivot = pivotX;
            }
        }
        return preselectedPivot;
    }
    
    /**
     * Selects new pivots.
     * Implementation of the incremental pivot selection algorithm.
     * This method is not intended to be called directly. It is automatically called from getPivot(int).
     *
     * This pivot selection technique depends on previously selected pivots. The AbstractObjectList
     * with such the pivots can be passed in getPivot(position,addInfo) in addInfo parameter
     * (preferable way) or directly set using setAdditionalInfo() method.
     * If the list of pivots is not passed it is assumed that no pivots were selected.
     *
     * Statistics are maintained automatically.
     * @param pivots number of pivots to generate
     * @param objectIter Iterator over the sample set of objects to choose new pivots from
     */
    protected void selectPivot(int pivots, AbstractObjectIterator<? extends LocalAbstractObject> objectIter) {
        // Store all passed objects temporarily
        AbstractObjectList<LocalAbstractObject> objectList = new AbstractObjectList<LocalAbstractObject>(objectIter);
        
        int sampleSize = (Math.sqrt(sampleSetSize) > objectList.size()) ? objectList.size() * objectList.size() : sampleSetSize;

        // Select objects randomly
        AbstractObjectList<LocalAbstractObject> leftPair  = objectList.randomList(sampleSize, false, new AbstractObjectList<LocalAbstractObject>());
        AbstractObjectList<LocalAbstractObject> rightPair = objectList.randomList(sampleSize, false, new AbstractObjectList<LocalAbstractObject>());
        
        
        LocalAbstractObject leftObj;
        LocalAbstractObject rightObj;
        
        LocalAbstractObject tmpPivot = null;                    // temporary pivot
        
        float[] distsLeftClosest = new float[sampleSize];      // stored distances between left objects and the best pivot
        float[] distsRightClosest = new float[sampleSize];     // stored distances between right objects and the best pivot
        
        // initialize array of distances to former pivots
        AbstractObjectIterator<LocalAbstractObject> leftIter = leftPair.iterator();
        AbstractObjectIterator<LocalAbstractObject> rightIter= rightPair.iterator();
        
        for (int i = 0; i < sampleSize; i++) {
            leftObj = leftIter.next();
            rightObj = rightIter.next();
            
            tmpPivot = getClosestPivot(leftObj, preselectedPivots.iterator());
            if (tmpPivot != null) {
                distsLeftClosest[i] = leftObj.getDistance(tmpPivot);//Math.abs(leftObj.getDistanceFastly(tmpPivot) - rightObj.getDistanceFastly(tmpPivot));
                distsRightClosest[i] = rightObj.getDistance(tmpPivot);
            } else {
                distsLeftClosest[i] = Float.MAX_VALUE;
                distsRightClosest[i] = Float.MAX_VALUE;
            }
        }
        
        
        // Select required number of pivots
        for (int p = 0; p < pivots; p++) {
            System.out.println("Selecting pivot number "+p);//", DistanceComputations: "+Statistics.printStatistics("DistanceComputations"));
            
            AbstractObjectList<LocalAbstractObject> candidatePivots = objectList.randomList(samplePivotSize, true, new AbstractObjectList<LocalAbstractObject>());
            
            float[] distsLeftToBestCand = new float[sampleSize];      // stored distances between left objects and the best pivot
            float[] distsRightToBestCand = new float[sampleSize];     // stored distances between right objects and the best pivot
            float[] distsLeftToCand = new float[sampleSize];      // stored distances between left objects and the pivot candidate
            float[] distsRightToCand = new float[sampleSize];     // stored distances between right objects and the pivot candidate
            for (int i = 0; i < sampleSize; i++) {
                distsLeftToBestCand[i] = Float.MAX_VALUE;
                distsRightToBestCand[i] = Float.MAX_VALUE;
            }
            
            float bestPivotMu = 0;                                 // mu_D of the best pivot candidate
            LocalAbstractObject bestPivot = null;                   // the best pivot candidate until now
            
            // go through all candidate pivots and compute their mu
            System.out.print("Candidates: "); int iter = 1;
            for (AbstractObjectIterator<LocalAbstractObject> pivotIter = candidatePivots.iterator(); pivotIter.hasNext(); ) {
                System.out.print(iter++ +", "); System.out.flush();
                LocalAbstractObject pivot = pivotIter.next();
                
                // compute distance between sample objects and pivot
                leftIter = leftPair.iterator();
                rightIter = rightPair.iterator();
                float mu = 0;
                for (int i = 0; i < sampleSize; i++) {
                    leftObj = leftIter.next();
                    rightObj = rightIter.next();
                    
                    //for (int i = 0; i < sampleSize; i++) {
                    float distLeftToCand = leftObj.getDistance(pivot);
                    if (distLeftToCand < distsLeftClosest[i]) {
                        distsLeftToCand[i] = distLeftToCand;
                        distsRightToCand[i] = rightObj.getDistance(pivot);
                    } else {
                        distsLeftToCand[i] = distsLeftClosest[i];
                        distsRightToCand[i] = distsRightClosest[i];
                    }
                    mu += Math.abs(distsLeftToCand[i] - distsRightToCand[i]);
                }
                mu /= (float)sampleSize;
                
                if (mu > bestPivotMu) {     // the current pivot is the best one until now, store it
                    // store mu and pivot
                    bestPivotMu = mu;
                    bestPivot = pivot;
                    // store distances from left/right objects to this pivot
                    for (int i = 0; i < sampleSize; i++) {
                        distsLeftToBestCand[i] = distsLeftToCand[i];
                        distsRightToBestCand[i] = distsRightToCand[i];
                    }
                }
            }
            System.out.println();
            // append the selected pivot
            preselectedPivots.add(bestPivot);
            // store distances from left/right objects to this pivot
            for (int i = 0; i < sampleSize; i++) {
                distsLeftClosest[i] = distsLeftToBestCand[i];
                distsRightClosest[i] = distsRightToBestCand[i];
            }
        }
    }
    
}
