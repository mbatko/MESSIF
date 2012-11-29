/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureOrderedSet;
import messif.objects.keys.DimensionObjectKey;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * Sorting dimension
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public abstract class SortDimension implements Comparator<ObjectFeature>, BinarySerializable, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Name of implementation of the sort dimension -- it is used for serialization in {@link ObjectFeatureOrderedSet}.
     * @return unique name of implementation.
     */
    public abstract String getImplementationName();
    
    /**
     * Returns value of the primary sorting dimension.
     * @param f object whose primary sort dimension value is returned
     * @return value of dimension
     */
    public abstract float getPrimary(DimensionObjectKey.Point f);

    /**
     * Returns value of the secondary sorting dimension.
     * @param f object whose secondary sort dimension value is returned
     * @return value of dimension
     */
    public abstract float getSecondary(DimensionObjectKey.Point f);

    /**
     * Index of the primary dimension (e.g. x-axis => 0).
     * @return index of dimension
     */
    public abstract int getPrimaryDimension();

    /**
     * Index of the secondary dimension (e.g. x-axis => 0).
     * @return index of dimension
     */
    public abstract int getSecondaryDimension();

    /**
     * Create a {@link ObjectFeatureOrderedSet.Window} object that represents the passed rectangular area.
     * @param minX minimum in x-axis (relative value within the interval [0,1))
     * @param maxX maximum in x-axis (relative value within the interval [0,1))
     * @param minY minimum in y-axis (relative value within the interval [0,1))
     * @param maxY maximum in y-axis (relative value within the interval [0,1))
     * @return the passed rectangular area
     */
    public abstract Window getWindow(float minX, float maxX, float minY, float maxY);

    @Override
    public int compare(ObjectFeature o1, ObjectFeature o2) {
        if (getPrimary(o1) < getPrimary(o2))
            return -1;
        else if (getPrimary(o1) > getPrimary(o2))
            return 1;
        else if (getSecondary(o1) < getSecondary(o2))
            return -1;
        else if (getSecondary(o1) > getSecondary(o2))
            return 1;
        else
            return 0;
    }

    
    
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
     * Factory method for text serialization sake. Implementation name is read from the passed stream, see 
     * implementations of {@link SortDimension#getImplementationName()) for details.
     * @param implName name of implementation
     * @return static instance of a known implementation
     * @throws NoSuchMethodException if unknown implementation is read from the stream
     */
    public static SortDimension getImplementation(String implName) throws NoSuchMethodException {
        if (sortDimensionX.getImplementationName().equals(implName))
            return sortDimensionX;
        else if (sortDimensionY.getImplementationName().equals(implName))
            return sortDimensionY;
        else        // Unknown implementation
            throw new NoSuchMethodException("Unknown implementation name of SortDimension!");
    }

    /**
     * Factory method for text serialization sake. Implementation name is read from the passed stream, see 
     * implementations of {@link SortDimension#getImplementationName()) for details.
     * @param stream stream with serialized sort dimension implementation
     * @return static instance of a known implementation
     * @throws IOException if unknown implementation is read from the stream
     */
    public static SortDimension readImplementation(BufferedReader stream) throws IOException {
        String implName = stream.readLine();
        try {
            return getImplementation(implName);
        } catch (NoSuchMethodException ex) {
            throw new IOException("Unknown implementation of SortDimension has been read from the stream: " + implName);
        }
    }
    
    /**
     * Factory method for text serialization sake. Implementation name is written to the passed stream, see 
     * implementations of {@link SortDimension#getImplementationName()) for details.
     * @param stream stream with serialized sort dimension implementation
     * @throws IOException on error during writing
     */
    public void storeImplementation(OutputStream stream) throws IOException {
        stream.write(getImplementationName().getBytes());
        stream.write('\n');
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize(getImplementationName());
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return serializator.write(output, getImplementationName());
    }

    /** Implementation of SortDimension that sorts the features by x-axis and then by y-axis. */
    public static final SortDimension sortDimensionX = new SortDimension() {
        private static final long serialVersionUID = 1L;

        @Override
        public String getImplementationName() {
            return "SortDimension:XY";
        }

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
        private static final long serialVersionUID = 1L;

        @Override
        public String getImplementationName() {
            return "SortDimension:YX";
        }

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
    
}
