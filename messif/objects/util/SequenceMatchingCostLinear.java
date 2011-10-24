/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

/**
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMatchingCostLinear extends SequenceMatchingCost {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    public SequenceMatchingCostLinear() {
    }

    public SequenceMatchingCostLinear(float gapOpen, float gapCont, float match, float semimatch, float mismatch, float matchDist, float mismatchDist) {
        super(gapOpen, gapCont, match, semimatch, mismatch, matchDist, mismatchDist);
    }

    @Override
    protected float getApproxCost(float dist) {
        return matchExact + ((dist - distMatch)*(matchExact - matchApprox))/(distMatch - distMismatch);
    // return (Distance - distMatch) / distMismatch * (matchExact - matchApprox) + matchApprox;
    }
}
