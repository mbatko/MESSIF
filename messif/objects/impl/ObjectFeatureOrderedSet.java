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

import messif.objects.util.SortDimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.keys.DimensionObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.SortDimension.Window;
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
    private static final long serialVersionUID = 7L;
    
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
    
    /**
     * Adds the object to the internal list of objects.
     * If objects are sorted, the new object is inserted into its correct place.
     * Otherwise it is appended to the end of the list.
     * @param obj object to be added
     */
    @Override
    public void addObject(ObjectFeature obj) {
        int idx = objects.size() - 1;
        
        if (isFeaturesOrdered()) {
            // Find the correct place
            while (idx >= 0) {
                int res = sortDim.compare(objects.get(idx), obj);
                if (res <= 0)       // The element at idx is smaller or equal, so insert the new object following it.
                    break;
                idx--;
            }
        }
        objects.add(idx + 1, obj);
    }
    
    public void orderFeatures(SortDimension sortDim) {
        if (this.sortDim == sortDim)
            return;
        synchronized (objects) {
            this.sortDim = sortDim;
            Collections.sort(objects, sortDim);
        }
    }
    
    public SortDimension getOrderOfFeatures() {
        return sortDim;
    }
    
    public boolean isFeaturesOrdered() {
        return (sortDim != null);
    }
    
    //****************** Windowing access ******************//
    
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
        DimensionObjectKey key = getObjectKey(DimensionObjectKey.class);
        // Fill all matching objects into the cache
        int cnt = 0;
        for (ObjectFeature o : objects) {
            float pri = key.convertToRelative(sortDim.getPrimary((DimensionObjectKey.Point)o), sortDim.getPrimaryDimension());
            float sec = key.convertToRelative(sortDim.getSecondary((DimensionObjectKey.Point)o), sortDim.getSecondaryDimension());
            if (pri < wnd.getPrimaryMin()) {
                continue;       // Point is in front of the window
            } else if (pri >= wnd.getPrimaryMax()) {
                break;          // Point is behind the window, so stop
            } else if (sec < wnd.getSecondaryMin() || sec >= wnd.getSecondaryMax()) {
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
     * @param slidingWnd specification window and its shifts
     * @return iterator over windows, where the windows are rectangles with relative boundaries
     * @throws IllegalStateException is thrown if any sorting dimension has not been set yet.
     */
    public Iterator<Window> windowIterator(SlidingWindow slidingWnd) throws IllegalStateException {
        return windowIterator(slidingWnd.getWidth(), slidingWnd.getHeight(), slidingWnd.getShiftX(), slidingWnd.getShiftY());
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
    protected Iterator<Window> windowIterator(final int wndWidth, final int wndHeight, final int shiftX, final int shiftY) throws IllegalStateException {
        if (sortDim == null)
            throw new IllegalStateException("This feature set has not had any SortDimension assigned yet.");

        final DimensionObjectKey objKey = getObjectKey(DimensionObjectKey.class);        
        
        if ((shiftX == 0 && objKey.getWidth() > wndWidth) || (shiftY == 0 && objKey.getHeight() > wndHeight))
            throw new IllegalArgumentException("Shift values must be non-zero!!!");
        
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

    //****************** Text stream I/O ******************//

    /**
     * Creates a new instance of ObjectFeatureSet from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureOrderedSet(BufferedReader stream) throws IOException {
        this(stream, null);
    }

    /**
     * Creates a new instance of ObjectFeatureSet from a text stream.
     * @param stream the text stream to read an object from
     * @param additionalParameters additional parameters for this meta object
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureOrderedSet(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters) throws IOException {
        super(stream, additionalParameters);
        
//        this.sortDim = SortDimension.readImplementation(stream);
        
        AbstractObjectKey key = getObjectKey();
        if (key != null && !(key instanceof DimensionObjectKey))
            throw new IllegalArgumentException("Incorrect object key class: " + key.getClass().getCanonicalName() + " Required: " + DimensionObjectKey.class.getCanonicalName());
    }

    @Override
    protected boolean parseObjectComment(String line) throws IllegalArgumentException {
        if (super.parseObjectComment(line))
            return true;

        // Process my private comments
        if (line.startsWith("#sortDim ")) {
            try {
                sortDim = SortDimension.getImplementation(line.substring(9));
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(ObjectFeatureOrderedSet.class.getName()).log(Level.SEVERE, null, ex);
                sortDim = null;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void writeObjectComment(OutputStream stream) throws IOException {
        super.writeObjectComment(stream);
        
        if (sortDim != null) {
            stream.write("#sortDim ".getBytes());
            stream.write(sortDim.getImplementationName().getBytes());
            stream.write('\n');
        }
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
        try {
            sortDim = SortDimension.getImplementation(serializator.readString(input));
        } catch (NoSuchMethodException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) + serializator.write(output, sortDim);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) 
                + serializator.getBinarySize((sortDim == null) ? null : sortDim.getBinarySize(serializator));
    }

    /**
     * Sliding window -- used in sequence matching of {@link ObjectFeatureSet feature sets}.
     *
     * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
     */
    public static class SlidingWindow {
        protected int width;
        protected int height;
        protected int shiftX;
        protected int shiftY;

        public SlidingWindow() {
            this(80, 60, 40, 30);
        }

        public SlidingWindow(int width, int height, int shiftX, int shiftY) {
            this.width = width;
            this.height = height;
            this.shiftX = shiftX;
            this.shiftY = shiftY;
        }

        public SlidingWindow(int width, int height, float percShiftX, float percShiftY) {
            this(width, height, (int)(width * percShiftX), (int)(height * percShiftY));
        }    

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getShiftX() {
            return shiftX;
        }

        public int getShiftY() {
            return shiftY;
        }    
    }
}
