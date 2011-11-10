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
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.keys.DimensionObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.utility.ArrayResetableIterator;
import messif.utility.ResetableIterator;

/**
 * This class adds ordering of the features in the set.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectFeatureOrderedSet extends ObjectFeatureSet {
    /** Class id for serialization. */
    private static final long serialVersionUID = 6L;
    
    //****************** Attributes ******************//

    /** Current ordering of object features (initially undefined) */
    protected SortDimension sortDim = null;
    
    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFeatureSet for the given locatorURI and encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param width x-axis dimension
     * @param height y-axis dimension
     * @param objects the list of objects to encapsulate in this object
     */
    public ObjectFeatureOrderedSet(String locatorURI, int width, int height, Collection<? extends ObjectFeature> objects) {
        super(locatorURI, objects);
        setObjectKey(new DimensionObjectKey(locatorURI, width, height));
    }

    /**
     * Creates a new instance of ObjectFeatureSet from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureOrderedSet(BufferedReader stream) throws IOException {
        super(stream);
        AbstractObjectKey key = getObjectKey();
        if (key != null && !(key instanceof DimensionObjectKey))
            throw new IllegalArgumentException("Incorrect object key class: " + key.getClass().getCanonicalName() + " Required: " + DimensionObjectKey.class.getCanonicalName());
    }

//    /**
//     * Creates a new instance of ObjectFeatureSet as a subset of an existing ObjectFeatureSet.
//     * Subset is determined as a sub-window.
//     * @param supSet original set of features (super-set)
//     * @param minX minimal X-coordinate to be included in the resulting subset
//     * @param maxX maximal X-coordinate to be included in the resulting subset
//     * @param minY minimal Y-coordinate to be included in the resulting subset
//     * @param maxY maximal Y-coordinate to be included in the resulting subset
//     */
//    public ObjectFeatureOrderedSet(ObjectFeatureSet supSet, float minX, float maxX, float minY, float maxY) {
//        super (supSet.getLocatorURI());
//        this.objects =  new ArrayList<LocalAbstractObject>();
//        for (int i = 0; i < supSet.getObjectCount(); i++) {
//            ObjectFeature of = (ObjectFeature) supSet.getObject(i);
//            if (of.getX() >= minX && of.getX() <= maxX && of.getY() >= minY && of.getY() <= maxY) {
//               addObject(of);
//            }
//        }
//    }

//    public ObjectFeatureOrderedSet(ObjectFeatureSet superSet) {
//        super (superSet.getLocatorURI());
//        this.objects = new ArrayList<LocalAbstractObject>();
//        for (LocalAbstractObject o : superSet.objects) {
//            addObject(o);
//        }
//    }

    //****************** Attribute access ******************//

    /** 
     * Width of this object in absolute value (width in pixels).
     * @return this object's width
     */
    public int getWidth() {
        return getDimension(0);
    }
    
    /** 
     * Height of this object in absolute value (height in pixels).
     * @return this object's height
     */
    public int getHeight() {
        return getDimension(1);
    }
    
    /** 
     * Size of this object in the passed dimension as an absolute value (in pixels).
     * @param dim index of dimension
     * @return this object's size
     */
    public int getDimension(int dim) {
        return getObjectKey(DimensionObjectKey.class).getDimension(dim);
    }
    
    public void orderFeatures(SortDimension sortDim) {
        synchronized (objects) {
            this.sortDim = sortDim;
            Collections.sort(objects, new SortComparatorByDimension(sortDim));
        }
    }
    
    public SortDimension getOrderOfFeatures() {
        return sortDim;
    }
    
    public boolean isFeaturesOrdered() {
        return (sortDim != null);
    }
    
    /**
     * Sorting dimension
     */
    public static interface SortDimension {
        /**
         * Returns value of the primary sorting dimension.
         * @param f object whose primary sort dimension value is returned
         * @return value of dimension
         */
        public float getPrimary(DimensionObjectKey.Point f);
        /**
         * Returns value of the secondary sorting dimension.
         * @param f object whose secondary sort dimension value is returned
         * @return value of dimension
         */
        public float getSecondary(DimensionObjectKey.Point f);
        
        /**
         * Index of the primary dimension (e.g. x-axis => 0).
         * @return index of dimension
         */
        public int getPrimaryDimension();
        /**
         * Index of the secondary dimension (e.g. x-axis => 0).
         * @return index of dimension
         */
        public int getSecondaryDimension();
        
        /**
         * Create a {@link ObjectFeatureOrderedSet.Window} object that represents the passed rectangular area.
         * @param minX minimum in x-axis (relative value within the interval [0,1))
         * @param maxX maximum in x-axis (relative value within the interval [0,1))
         * @param minY minimum in y-axis (relative value within the interval [0,1))
         * @param maxY maximum in y-axis (relative value within the interval [0,1))
         * @return the passed rectangular area
         */
        public Window getWindow(float minX, float maxX, float minY, float maxY);
    }
    
    /** Implementation of SortDimension that sorts the features by x-axis and then by y-axis. */
    public static final SortDimension sortDimensionX = new SortDimension() {
        @Override
        public float getPrimary(DimensionObjectKey.Point f) {
            return f.getX();
        }
        @Override
        public float getSecondary(DimensionObjectKey.Point f) {
            return f.getY();
        }

        @Override
        public int getPrimaryDimension() {
            return 0;
        }

        @Override
        public int getSecondaryDimension() {
            return 1;
        }

        @Override
        public Window getWindow(final float minX, final float maxX, final float minY, final float maxY) {
            return new Window() {
                @Override
                public float getPrimaryMin() {
                    return minX;
                }
                @Override
                public float getPrimaryMax() {
                    return maxX;
                }
                @Override
                public float getSecondaryMin() {
                    return minY;
                }
                @Override
                public float getSecondaryMax() {
                    return maxY;
                }
                @Override
                public String toString() {
                    return String.format("Window: [%f;%f-%f;%f]", minX, minY, maxX, maxY);
                }
            };
        }
    };
    
    /** Implementation of SortDimension that sorts the features by y-axis and then by x-axis. */
    public static final SortDimension sortDimensionY = new SortDimension() {
        @Override
        public float getPrimary(DimensionObjectKey.Point f) {
            return f.getY();
        }
        @Override
        public float getSecondary(DimensionObjectKey.Point f) {
            return f.getX();
        }

        @Override
        public int getPrimaryDimension() {
            return 1;
        }

        @Override
        public int getSecondaryDimension() {
            return 0;
        }

        @Override
        public Window getWindow(final float minX, final float maxX, final float minY, final float maxY) {
            return new Window() {
                @Override
                public float getPrimaryMin() {
                    return minY;
                }
                @Override
                public float getPrimaryMax() {
                    return maxY;
                }
                @Override
                public float getSecondaryMin() {
                    return minX;
                }
                @Override
                public float getSecondaryMax() {
                    return maxX;
                }
                @Override
                public String toString() {
                    return String.format("Window: [%f;%f-%f;%f]", minX, minY, maxX, maxY);
                }
            };
        }
    };
    
    public static class SortComparatorByDimension implements Comparator<ObjectFeature> {
        private final SortDimension sortDim;
        
        public SortComparatorByDimension(SortDimension dim) {
            sortDim = dim;
        }
        
        @Override
        public int compare(ObjectFeature o1, ObjectFeature o2) {
            if (sortDim.getPrimary(o1) < sortDim.getPrimary(o2))
                return -1;
            else if (sortDim.getPrimary(o1) > sortDim.getPrimary(o2))
                return 1;
            else if (sortDim.getSecondary(o1) < sortDim.getSecondary(o2))
                return -1;
            else if (sortDim.getSecondary(o1) > sortDim.getSecondary(o2))
                return 1;
            else
                return 0;
        }
    }
    
    //****************** Windowing access ******************//
    
    /** 
     * Interface for encapsulating a rectangular 2-D window in a way that it recognizes primary and secondary ordering
     * instead of rigid references to x and y axes.
     */
    public static interface Window {
        public float getPrimaryMin();
        public float getPrimaryMax();
        public float getSecondaryMin();
        public float getSecondaryMax();
    }

    /**
     * Iterate over all features within the passed window. 
     * All features having their positions in [minX,maxX) x [minY,maxY) are returned.
     * @param minX minimum in x-axis of the window (absolute value)
     * @param maxX maximum in x-axis of the window (absolute value)
     * @param minY minimum in y-axis of the window (absolute value)
     * @param maxY maximum in y-axis of the window (absolute value)
     * @return an iterator over all features in the window
     * @throws IllegalStateException is thrown if any sorting dimension has not been set yet.
     */
    public Iterator<ObjectFeature> iterator(final int minX, final int maxX, final int minY, final int maxY) throws IllegalStateException {
        final DimensionObjectKey objKey = getObjectKey(DimensionObjectKey.class);
        return iterator(sortDim.getWindow(objKey.convertToRelative(minX, 0), objKey.convertToRelative(maxX, 0), 
                                          objKey.convertToRelative(minY, 1), objKey.convertToRelative(maxY, 1)));
    }
    
    /**
     * Iterate over all features within the passed window.
     * @param wnd window that restricts the features returned
     * @return an iterator over all features in the window
     * @throws IllegalStateException is thrown if any sorting dimension has not been set yet.
     */
    public ResetableIterator<ObjectFeature> iterator(final Window wnd) throws IllegalStateException {
        if (sortDim == null)
            throw new IllegalStateException("This feature set has not had any SortDimension assigned yet.");

        ObjectFeature[] cache = new ObjectFeature[getObjectCount()];
        // Fill all matching objects into the cache
        int cnt = 0;
        for (ObjectFeature o : objects) {
            if (sortDim.getPrimary(o) < wnd.getPrimaryMin()) {
                continue;       // Point is in front of the window
            } else if (sortDim.getPrimary(o) >= wnd.getPrimaryMax()) {
                break;          // Point is behind the window, so stop
            } else if (sortDim.getSecondary(o) < wnd.getSecondaryMin() || sortDim.getSecondary(o) >= wnd.getSecondaryMax()) {
                continue;       // In the primary range but out of the secondary range, so skip it
            } else {
                cache[cnt++] = o;   // Point is in window
            }
        }
        // Resize the cache, if needed
        return new ArrayResetableIterator<ObjectFeature>( (cache.length > cnt) ? Arrays.copyOf(cache, cnt) : cache );
    }
    
    /**
     * Iterate over all possible positions of the window.
     * @param wndWidth window's width (in pixels -- i.e. absolute value)
     * @param wndHeight window's height (in pixels -- i.e. absolute value)
     * @param shiftX window's shift in x-axis (in pixels -- i.e. absolute value)
     * @param shiftY window's shift in Y-axis (in pixels -- i.e. absolute value)
     * @return iterator over windows, where the windows are rectangles with relative boundaries
     * @throws IllegalStateException is thrown if any sorting dimension has not been set yet.
     */
    public Iterator<Window> windowIterator(final int wndWidth, final int wndHeight, final int shiftX, final int shiftY) throws IllegalStateException {
        if (sortDim == null)
            throw new IllegalStateException("This feature set has not had any SortDimension assigned yet.");
        
        final DimensionObjectKey objKey = getObjectKey(DimensionObjectKey.class);        
        
        return new Iterator<Window>() {
            private final float winWidth = objKey.convertToRelative(wndWidth, 0);
            private final float winHeight = objKey.convertToRelative(wndHeight, 1);
            private final float deltaX = objKey.convertToRelative(shiftX, 0);
            private final float deltaY = objKey.convertToRelative(shiftY, 1);
            private float curX = 0;
            private float curY = 0;
            private boolean isNext = true;

            @Override
            public boolean hasNext() {
                return isNext;
            }

            @Override
            public Window next() {
                Window w;
                
                if (winWidth >= 1f && winHeight >= 1f) {
                    // Only one window
                    w = sortDim.getWindow(0, 1f, 0, 1f);
                    // Stop condition
                    isNext = false;
                } else {
                    float maxX = curX + winWidth;
                    if (maxX > 1f) {
                        maxX = 1f;
                        curX = Math.max(1f - winWidth, 0);
                    }
                    float maxY = curY + winHeight;
                    if (maxY > 1f) {
                        maxY = 1f;
                        curY = Math.max(1f - winHeight, 0);
                    }
                    w = sortDim.getWindow(curX, maxX, curY, maxY);
                    curX += deltaX;
                    if (maxX >= 1f) {
                        if (maxY >= 1f) {
                            // Stop condition
                            isNext = false;
                        } else {
                            // Move in Y -- next row
                            curY += deltaY;
                            curX = 0;
                        }
                    }
                }
                
                return w;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported.");
            }
        };
    }

    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFeatureSet loaded from binary input.
     *
     * @param input the input to read the ObjectFeatureSet from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureOrderedSet(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
//        serializator.readEnum(input, xx);
    }

//    @Override
//    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
//        return super.binarySerialize(output, serializator) + serializator.write(output, xx);
//    }
//
//    @Override
//    public int getBinarySize(BinarySerializator serializator) {
//        return super.getBinarySize(serializator) + serializator.getBinarySize(xx);
//    }

}
