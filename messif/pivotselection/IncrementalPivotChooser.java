/*
 * IncrementalPivotChooser.java
 *
 * Created on 19. cervenec 2004, 10:51
 */

package messif.pivotselection;

import java.io.Serializable;
import messif.buckets.BucketFilterAfterAdd;
import messif.buckets.BucketFilterAfterRemove;
import messif.buckets.FilterRejectException;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;

/**
 * Incremental selection of pivots. This pivot chooser is based on the technique called Incremental selection
 * proposed as best in 'Pivot Selection Techniques for Proximity Searching in Metric Spaces` by
 * Benjamin Bustos, Gonzalo Navarro, Edgar Chavez in 2001.
 *
 * @author  Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class IncrementalPivotChooser extends AbstractPivotChooser implements Serializable, BucketFilterAfterAdd, BucketFilterAfterRemove {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /************** PIVOT SELECTION ALGORITHM GLOBAL PARAMETERS ***********/
    
    /** Size of the sample set used to verify the goodness of candidate pivots (used to estimate mu_d)
     */
    public static int SAMPLE_SET_SIZE = 2000; //10000;
    
    /** Size of the candidate set of pivots from which the best pivot is picked.
     */
    public static int SAMPLE_PIVOT_SIZE = 100;  //50;
    
    /** Reselect sample set when the percentage of the bucket changes (with respect to its occupation) exceeds this constant.
     * By default 20% of the bucket occupation is the threshold.
     * The lower number the more frequent sample sets are reselected.
     */
    public static float BUCKETCHANGE_THRESHOLD_TO_RESELECT = 0.2f;
    //public static int sampleSetReselections = 0;

    
    /************* INTERNAL ATTRIBUTES *************************/
    
    /** Current size of sample sets used to estimate the quality of pivots.
     * Equal to zero when not initilized.
     */
    private int sampleSize = 0;
    
    /** Two lists of objects used as samples to estimate the quality of pivots.
     * They are randomly picked among all objects in the bucket.
     */
    private AbstractObjectList<LocalAbstractObject> leftPair  = null;
    private AbstractObjectList<LocalAbstractObject> rightPair = null;
    
    /** Using pivots we transform the metric space into (R^np,L_max) space where np is the number of pivots.
     * This array caches maximum distances between a left and a right sample objects with respect to the 
     * currently selected pivots.
     */
    private float[] distsFormer = null;
    
    /** Number of objects added to or deleted from this bucket since the last selection of sample sets.
     */
    private int changesFromLastSampleSetSelection = 0;
        
    
    /** TEMPORARY ONLY until a correct transaction management of subclass' attributes gets implemented in AbstractPivotChooser
     */
    private int backupSampleSize = 0;
    private AbstractObjectList<LocalAbstractObject> backupLeftPair  = null;
    private AbstractObjectList<LocalAbstractObject> backupRightPair = null;
    private float[] backupDistsFormer = null;
    private int backupChangesFromLastSampleSetSelection = 0;

    public synchronized boolean beginTransaction(boolean blocking) {
        if (! super.beginTransaction(blocking))
            return false;
        
        // Store the current state of internal attributes
        backupSampleSize = sampleSize;
        backupLeftPair = leftPair;      // This is ok, since no changes are done to existing list, but new lists are created upon any change.
        backupRightPair = rightPair;
        backupDistsFormer = (distsFormer != null) ? distsFormer.clone() : null;
        backupChangesFromLastSampleSetSelection = changesFromLastSampleSetSelection;
        
        return true;
    }
    
    public synchronized void commitTransaction() {
        super.commitTransaction();
        // Reset all backup values
        backupSampleSize = 0;
        backupLeftPair  = null;
        backupRightPair = null;
        backupDistsFormer = null;
        backupChangesFromLastSampleSetSelection = 0;
    }
    
    public synchronized void rollbackTransaction() {
        super.rollbackTransaction();
        // Recover the last state of internal attributes
        sampleSize = backupSampleSize;
        leftPair = backupLeftPair;
        rightPair = backupRightPair;
        if (distsFormer == null || backupDistsFormer == null || distsFormer.length != backupDistsFormer.length)
            distsFormer = backupDistsFormer;
        else
            System.arraycopy(backupDistsFormer, 0, distsFormer, 0, backupDistsFormer.length);
        changesFromLastSampleSetSelection = backupChangesFromLastSampleSetSelection;
        // Reset all backup values
        backupSampleSize = 0;
        backupLeftPair  = null;
        backupRightPair = null;
        backupDistsFormer = null;
        backupChangesFromLastSampleSetSelection = 0;
    }
    /** END OF TEMPORARY STUFF
     */
    
    
    /************* METHODS & CONSTRUCTORS **********************/
    
    /** Creates a new instance of IncrementalPivotChooser */
    public IncrementalPivotChooser() {
    }
        
    public void filterAfterAdd(LocalAbstractObject object, LocalBucket bucket) {
        // Increment the number of changes since the last reselection of samples only
        changesFromLastSampleSetSelection++;
    }

    public void filterAfterRemove(LocalAbstractObject object, LocalBucket bucket) {
        // Increment the number of changes since the last reselection of samples only
        changesFromLastSampleSetSelection++;
    }
    
    /** Selects new pivots.
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
        AbstractObjectList<LocalAbstractObject> objectList = new AbstractObjectList<LocalAbstractObject>(objectIter);
        
        // Initialize sample sets
        initializeSampleSets(objectList);
        
        // Select required number of pivots
        for (int i = 0; i < pivots; i++)
            selectOnePivot(objectList);
    }
        
    /** Sets the size of sample sets to be used and initializes lists of sample objects 
     */
    private void initializeSampleSets(AbstractObjectList<LocalAbstractObject> objectList) {
        // Samples have already been initialized & insufficient bucket changes, skip this phase
        if (sampleSize != 0 && 
               (objectList.size() == 0 ? BUCKETCHANGE_THRESHOLD_TO_RESELECT
                                             : (float)changesFromLastSampleSetSelection / (float)objectList.size()
               ) < BUCKETCHANGE_THRESHOLD_TO_RESELECT)
            return;
        //++sampleSetReselections;
        
        // Reset the number of changes since the last reselection of samples
        changesFromLastSampleSetSelection = 0;
        
        // Set a new size of sample sets
        //sampleSize = (Math.sqrt(SAMPLE_SET_SIZE) > objectList.size()) ? objectList.size() * objectList.size() : SAMPLE_SET_SIZE;
        sampleSize = (SAMPLE_SET_SIZE > 2*objectList.size()) ? 2*objectList.size() : SAMPLE_SET_SIZE;
        
        // Select objects randomly, allow repetitions.
        leftPair = new AbstractObjectList<LocalAbstractObject>();
        for (LocalAbstractObject o : objectList.randomList(sampleSize, false, new AbstractObjectList<LocalAbstractObject>()))
            leftPair.add(o);
        rightPair = new AbstractObjectList<LocalAbstractObject>();
        for (LocalAbstractObject o : objectList.randomList(sampleSize, false, new AbstractObjectList<LocalAbstractObject>()))
            rightPair.add(o);
        
        // Initialize the array of distances between left and right sample objects according to current pivots
        distsFormer = new float[sampleSize];    

        AbstractObjectIterator<LocalAbstractObject> leftIter = leftPair.iterator();
        AbstractObjectIterator<LocalAbstractObject> rightIter = rightPair.iterator();
        for (int i = 0; i < sampleSize; i++) {
            LocalAbstractObject leftObj = leftIter.next();
            LocalAbstractObject rightObj = rightIter.next();
            
            distsFormer[i] = 0;
            for (LocalAbstractObject pivot : preselectedPivots ) {
                float distD = Math.abs(leftObj.getDistance(pivot)
                                        - rightObj.getDistance(pivot));
                distsFormer[i] = Math.max(distsFormer[i], distD);
            }
        }
        
    }
    
    /** Selects one new pivot.
     * Implementation of the incremental pivot selection algorithm.
     * This method is not intended to be called directly. It is automatically called from selectPivot(int).
     *
     * This pivot selection technique depends on previously selected pivots. The AbstractObjectList
     * with such the pivots can be passed in getPivot(position,addInfo) in addInfo parameter 
     * (preferable way) or directly set using setAdditionalInfo() method.
     * If the list of pivots is not passed it is assumed that no pivots were selected.
     */
    private void selectOnePivot(AbstractObjectList<LocalAbstractObject> objectList) {
        // Randomly pick a list of candidate pivots, do now allow duplicates.
        AbstractObjectList<LocalAbstractObject> candidatePivots = objectList.randomList(SAMPLE_PIVOT_SIZE, true, new AbstractObjectList<LocalAbstractObject>());

        // Separate arrays for distance were used to remeber distances from objects to pivots and subsequently to save them among precomputed distance of sample objects
        // This is no longer used
        //float[] distsLeftToCand = new float[sampleSize];      // distances between left objects and the new pivot candidate
        //float[] distsRightToCand = new float[sampleSize];     // distances between right objects and the new pivot candidate
        //float[] distsLeftToBest = new float[sampleSize];      // stored distances between left objects and the best pivot candidate
        //float[] distsRightToBest = new float[sampleSize];     // stored distances between right objects and the best pivot candidate

        float[] distsToCand = new float[sampleSize];            // distances between left and right sample objects with respect to a pivot candidate
        float[] distsToBest = new float[sampleSize];            // distances between left and right sample objects with respect to a the best pivot candidate
        
        float bestPivotMu = -1;                                  // mu_D of the best pivot candidate
        LocalAbstractObject bestPivot = null;                     // the best pivot candidate until now

        // go through all candidate pivots and compute their mu
        for (LocalAbstractObject p : candidatePivots) {
            if (preselectedPivots.contains(p))      // skip candidates that have already been selected as pivots.
                continue;
            
            LocalAbstractObject pivot = p;
            
            // compute distance between sample objects and the pivot
            AbstractObjectIterator<LocalAbstractObject> leftIter = leftPair.iterator();
            AbstractObjectIterator<LocalAbstractObject> rightIter = rightPair.iterator();
            for (int i = 0; i < sampleSize; i++) {
                LocalAbstractObject leftObj = leftIter.next();
                LocalAbstractObject rightObj = rightIter.next();
                
                // compute the distance or use the precomputed distance if available but do not STORE!
                distsToCand[i] = Math.abs(leftObj.getDistance(pivot) - rightObj.getDistance(pivot));
                // No longer used, see comments above
                //distsLeftToCand[i] = leftObj.getDistanceFastly(pivot, false);
                //distsRightToCand[i] = rightObj.getDistanceFastly(pivot, false);
            }
            
            // Compute mu
            float mu = 0;
            for (int i = 0; i < sampleSize; i++) {
                mu += Math.max(distsFormer[i], distsToCand[i]);
                // No longer used, see comments above
                //mu += Math.max(distsFormer[i], Math.abs(distsLeftToCand[i] - distsRightToCand[i]));
            }
            mu /= (float)sampleSize;
            
            if (mu > bestPivotMu) {     // the current pivot is the best one until now, store it
                // Store the best mu and pivot
                bestPivotMu = mu;
                bestPivot = pivot;
                // Remember distances from left/right objects to this pivot
                System.arraycopy(distsToCand, 0, distsToBest, 0, sampleSize);
                // No longer used, see comments above
                //System.arraycopy(distsLeftToCand, 0, distsLeftToBest, 0, sampleSize);
                //System.arraycopy(distsRightToCand, 0, distsRightToBest, 0, sampleSize);
            }
        }
        
        if (bestPivot == null) {
            System.out.print("WARNING: Incremental pivot chooser failed! No pivot was selected!");
            if (candidatePivots.isEmpty())
                System.out.print(" Candidate set is empty!");
            else
                System.out.print(" Candidate set contained " + candidatePivots.size() + " elements.");
            System.out.print(" Candidate set was selected from " + objectList.size() + " objects.");
            System.out.print(" Count of already used pivots: " + preselectedPivots.size() + ".");
            
            if (candidatePivots.size() != 0) {
                bestPivot = candidatePivots.get(0);
                System.out.print(" Using the first candidate as the pivot!!!");
            }
            System.out.println("");
        }
        
        // append the selected pivot
        preselectedPivots.add(bestPivot);
        
        // Update distances to generated pivots (distsFormer array)
        for (int i = 0; i < sampleSize; i++) {
            distsFormer[i] = Math.max(distsFormer[i], distsToBest[i]);
            // No longer used, see comments above
            //distsFormer[i] = Math.max(distsFormer[i], Math.abs(distsLeftToBest[i] - distsRightToBest[i]));
        }
        
        // store precomputed distances to this pivot
/*        leftIter = leftPair.iterator();
        rightIter = rightPair.iterator();
        for (int i = 0; i < sampleSize; i++) {
            PrecomputedDistancesObject leftObj = (PrecomputedDistancesObject)leftIter.nextObject();
            PrecomputedDistancesObject rightObj = (PrecomputedDistancesObject)rightIter.nextObject();

            leftObj.setPrecompDist(bestPivot, distsLeftToBest[i]);
            rightObj.setPrecompDist(bestPivot, distsRightToBest[i]);
        }*/
    }

    /** This method appends a new pivot to the currently existing list.
     */
    public void addPivot(LocalAbstractObject newPivot) {
        super.addPivot(newPivot);

        // Update the internal array of distances
        if (leftPair == null || rightPair == null)
            return;
        
        // compute distance between sample objects and the pivot
        AbstractObjectIterator<LocalAbstractObject> leftIter = leftPair.iterator();
        AbstractObjectIterator<LocalAbstractObject> rightIter = rightPair.iterator();
        for (int i = 0; i < sampleSize; i++) {
            LocalAbstractObject leftObj = leftIter.next();
            LocalAbstractObject rightObj = rightIter.next();

            // compute the distance or use the precomputed distance if available but do not STORE!
            float d = Math.abs(leftObj.getDistance(newPivot) - rightObj.getDistance(newPivot));
            distsFormer[i] = Math.max(distsFormer[i], d);
        }
    }

}
