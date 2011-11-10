/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.impl;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.SequenceMatchingCost;

/**
 * 
 * @author xdohnal
 * @deprecated Use features in {@link ObjectFeatureOrderedSet} and {@link SequenceMatchingCost} instead
 */
@Deprecated
public abstract class ObjectFeatureSetStringWindow extends ObjectFeatureSet {

    public enum CoordinateType {
        CartesianAbsolute,
        CartesianRelative
    };

    public enum DistanceWeightType {
        NonWeighted,
        WeightLinear,
        WeightInverse // Inverted value 1/x
    };

    private static final long serialVersionUID = 1L;
 
    protected float gapCostCont = 0.0f;   // non-negative penalty, wil be subtracted
    protected float gapCostOpening = 0.3f; // non-negative penalty, wil be subtracted

    protected float equalityTreshold = 120.0f;
    protected float equalityUpperTreshold = 240.f;

    protected float matchScoreApprox = 2.0f;
    protected float matchScoreExact = 5.0f;
    protected float matchScoreMismatch = -0.5f;

    protected DistanceWeightType weightedScore = DistanceWeightType.NonWeighted;

    public enum Coordinate {
        CoordX, CoordY;
    } ;

    public ObjectFeatureSetStringWindow () {
        super();
    }

    public ObjectFeatureSetStringWindow(BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetStringWindow(BinaryInput input, BinarySerializator serializator) throws IOException {
        super (input, serializator);
    }

    public ObjectFeatureSetStringWindow (ObjectFeatureSetStringWindow superSet, float minX, float maxX, float minY, float maxY) {
        super (superSet, minX, maxX, minY, maxY);
        equalityTreshold = superSet.equalityTreshold;
        equalityUpperTreshold = superSet.equalityUpperTreshold;
        gapCostCont = superSet.gapCostCont;
        gapCostOpening = superSet.gapCostOpening;
        matchScoreApprox = superSet.matchScoreApprox;
        matchScoreExact = superSet.matchScoreExact;
        matchScoreMismatch = superSet.matchScoreMismatch;
        weightedScore = superSet.weightedScore;
    }

    public ObjectFeatureSetStringWindow (ObjectFeatureSetStringWindow superSet) {
        super ((ObjectFeatureSet) superSet);
        equalityTreshold = superSet.equalityTreshold;
        equalityUpperTreshold = superSet.equalityUpperTreshold;
        gapCostCont = superSet.gapCostCont;
        gapCostOpening = superSet.gapCostOpening;
        matchScoreApprox = superSet.matchScoreApprox;
        matchScoreExact = superSet.matchScoreExact;
        matchScoreMismatch = superSet.matchScoreMismatch;
        weightedScore = superSet.weightedScore;
    }

    /**
     * @deprecated Use new class {@link SequenceMatchingCost} and its descendants
     */
    @Deprecated
    public float getCost (final float Distance) {
        if (Distance > equalityUpperTreshold) {
            return matchScoreMismatch;
        } else {
            if (Distance <= equalityTreshold) {
                return matchScoreExact;
            } else {
                switch (weightedScore) {
                    case NonWeighted:
                    default:
                        return matchScoreApprox;
                    case WeightLinear:
                        return matchScoreExact + ((Distance - equalityTreshold)*(matchScoreExact - matchScoreApprox))/(equalityTreshold - equalityUpperTreshold);
                    // return (Distance - equalityTreshold) / equalityUpperTreshold * (matchScoreExact - matchScoreApprox) + matchScoreApprox;
                    case WeightInverse:
                        // norma vzdalenosti mezi 0..1
                        float d1 = (Distance - equalityTreshold)/(equalityUpperTreshold - equalityTreshold);
                        // prevod na krivku 1/x mezi 10 a 0.5 (tedy 1/0.1 a 1/2)
                        float d2 = (1/((2-0.1f)*d1+0.1f));
                        // Prevod z 10..0.5 na matchScoreExact..matchScoreApprox
                        float d3 = (d2 - 10) / (10-0.5f) * (matchScoreExact - matchScoreApprox) + matchScoreExact;
                        return d3;
                }
            }
        }
    }

    /**
     * @deprecated Use new class {@link SequenceMatchingCost} and its descendants
     */
    @Deprecated
    public float getCost (final LocalAbstractObject obj1, final LocalAbstractObject obj2) {
        return getCost (obj1.getDistance(obj2));
    }

    public void SetEqualityThreshold (float equalityThreshold) {
        this.equalityTreshold = equalityThreshold;
    }

    public void SetApproxThreshold (float approximationThreshold)
    {
        this.equalityUpperTreshold = approximationThreshold;
    }

    public void SetParams (float gapCostOpening, float gapCostContinue,
            float matchScoreExact, float matchScoreApproximate, float matchScoreMismatch,
            float equalityTreshold, float approximationTreshold,
            DistanceWeightType weightedScore)  {

        this.gapCostCont = gapCostContinue;
        this.gapCostOpening = gapCostOpening;

        this.equalityTreshold = equalityTreshold;
        this.equalityUpperTreshold = approximationTreshold;

        this.matchScoreApprox = matchScoreApproximate;
        this.matchScoreExact = matchScoreExact;
        this.matchScoreMismatch = matchScoreMismatch;
        this.weightedScore = weightedScore;
    }


    public static float max3 (final float f1, final float f2, final float f3) {
        return Math.max(f1, Math.max(f2, f3));
    }


    public static float max4 (final float f1, final float f2, final float f3, final float f4) {
        return Math.max(Math.max(f1, f2), Math.max(f3, f4));
    }


    public float min3 (float f1, float f2, float f3) {
        return Math.min(f1, Math.min(f2, f3));
    }

    public float min4 (float f1, float f2, float f3, float f4) {
        return Math.min(Math.min(f1, f2), Math.min(f3, f4));
    }

    public void sortObjects (Coordinate coord) {
        synchronized (objects) {
            if (coord == Coordinate.CoordX) {
                Collections.sort(Collections.synchronizedList(objects), new ObjectLocalFeatureComparatorX());
            } else {
                Collections.sort(Collections.synchronizedList(objects), new ObjectLocalFeatureComparatorY());
            }
        }
    }
}
