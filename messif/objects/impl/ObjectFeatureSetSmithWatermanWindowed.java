/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.SequenceMatchingCost;
import messif.objects.util.SequenceMatchingCostEquality;
import messif.objects.util.SortDimension;

/**
 * Smith-Waterman local sequence alignment computed by sliding windows.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author Tomáš Homola, Masaryk University, Brno, Czech Republic, xhomola@fi.muni.cz
 */
public class ObjectFeatureSetSmithWatermanWindowed extends ObjectFeatureOrderedSet {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;
    
    /** UkBench settings for UkBench visual words: images are 640x480 pxs */
    public static SlidingWindow window = new SlidingWindow(80, 60, 0.5f, 0.5f);
    public static SequenceMatchingCost defaultCost = new SequenceMatchingCostEquality(0.3f, 0.05f, 5f, 0f, 0f, 0f, 0f);
//    public static SlidingWindow window = new SlidingWindow(160, 120, 0.5f, 0.5f);
//    public static SequenceMatchingCost defaultCost = new SequenceMatchingCostEquality(0.3f, 0.05f, 5f, 0f, 0f, 0f, 0f);

//    public static SlidingWindow window = new SlidingWindow(320, 240, 1f, 1f);
//    public static SequenceMatchingCost defaultCost = new SequenceMatchingCostEquality(0.3f, 0.05f, 5f, 0f, 0f, 0f, 0f);
    
//    public static SlidingWindow window = new SlidingWindow(128, 128, 0.5f, 0.5f);
//    public static SequenceMatchingCost defaultCost = new SequenceMatchingCostLinear(0.3f, 0.05f, 5.0f, 2.0f, -0.5f, 120.0f, 240.0f);
////    public static SequenceMatchingCost defaultCost = new SequenceMatchingCostLinear(0.0f, 0.3f, 5.0f, 2.0f, -0.5f, 120.0f, 240.0f);

    public ObjectFeatureSetSmithWatermanWindowed(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

    public ObjectFeatureSetSmithWatermanWindowed(BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetSmithWatermanWindowed(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters) throws IOException {
        super(stream, additionalParameters);
    }

    public ObjectFeatureSetSmithWatermanWindowed(String locatorURI, int width, int height, Collection<? extends ObjectFeature> objects) {
        super(locatorURI, width, height, objects);
    }

    
    @Override
    protected float getDistanceImpl(LocalAbstractObject o, float distThreshold) {
        ObjectFeatureSetSmithWatermanWindowed obj = (ObjectFeatureSetSmithWatermanWindowed)o;
        
        this.orderFeatures(SortDimension.sortDimensionX);
        obj.orderFeatures(SortDimension.sortDimensionX);

        return ObjectFeatureSetSmithWaterman.getDistanceByWindowing(defaultCost, window, this, obj);
    }

    @Override
    public float getMaxDistance() {
        return 1f;
    }

    public static void setDefaultCostAndSlidingWindow(SequenceMatchingCost cost, SlidingWindow wnd) {
        defaultCost = cost;
        window = wnd;
    }
}
