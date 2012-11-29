/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

import java.io.Serializable;
import messif.objects.LocalAbstractObject;

/**
 * Class encapsulating costs for matching two {@link LocalAbstractObject objects}.
 * It is used in sequence alignment algorithms.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMatchingCost implements Serializable {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    protected float gapContinue;   // non-negative penalty, will be subtracted
    protected float gapOpening; // non-negative penalty, will be subtracted

    protected float distMatch;
    protected float distMismatch;

    protected float matchExact;
    protected float matchApprox;
    protected float matchMismatch;

    /** Default costs good for comparing SIFT descriptors */
    public static SequenceMatchingCost SIFT_DEFAULT = new SequenceMatchingCost();
    
    
    public SequenceMatchingCost() {
        this(0.3f, 0.0f, 5.0f, 2.0f, -0.5f, 120.0f, 240.0f);
    }

    public SequenceMatchingCost(float gapOpen, float gapCont, float match, float semimatch, float mismatch, float matchDist, float mismatchDist) {
        this.gapOpening = gapOpen;
        this.gapContinue = gapCont;
        this.matchExact = match;
        this.matchApprox = semimatch;
        this.matchMismatch = mismatch;
        this.distMatch = matchDist;
        this.distMismatch = mismatchDist;
        
        if (!(matchExact >= matchApprox && matchApprox >= matchMismatch))
            throw new IllegalArgumentException("Incorrect match scores passed!");
    }

    public float getCost(float dist) {
        if (dist > distMismatch) {
            return matchMismatch;
        } else {
            if (dist <= distMatch) {
                return matchExact;
            } else {
                return getApproxCost(dist);
            }
        }
    }

    /**
     * Computes cost corresponding to the distance computed between the passed objects.
     * Calls {@link #getCost(float) } internally.
     * @param obj1 first object
     * @param obj2 second object
     * @return cost corresponding to the passed objects
     */
    public float getCost(LocalAbstractObject obj1, LocalAbstractObject obj2) {
        return getCost(obj1.getDistance(obj2));
    }
    
    protected float getApproxCost(float dist) {
        return matchApprox;
    }
    
    /**
     * Maximum cost that can be assigned to a pair of objects by {@link #getCost(float) }.
     * @return value of maximum cost
     */
    public float getMaxCost() {
        return Math.max(matchExact, Math.max(matchApprox, matchMismatch));
    }
    
    public float getGapOpening() {
        return gapOpening;
    }
    
    public float getGapContinue() {
        return gapContinue;
    }

    public float getMatchExact() {
        return matchExact;
    }

    public float getMatchApprox() {
        return matchApprox;
    }

    public float getMatchMismatch() {
        return matchMismatch;
    }
    
}
