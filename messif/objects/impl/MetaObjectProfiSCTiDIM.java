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
import java.util.EnumSet;
import java.util.Map;
import messif.buckets.storage.IntStorageIndexed;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.objects.text.Stemmer;
import messif.objects.text.TextConversionException;
import messif.objects.text.WordExpander;

/**
 * Special meta object that stores only the objects required for the Profi search.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectProfiSCTiDIM extends MetaObjectProfiSCT {
    /** Class id for serialization. */
    private static final long serialVersionUID = 10103201L;

    //****************** Constants ******************//

    /** Weights for the visual descriptors */
    private static final float[] visualWeights = { 4.0f, 6.0f, 7.5f, 7.5f, 3.0f };


    //****************** Constructors ******************//

    public MetaObjectProfiSCTiDIM(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectIntVectorL1 scalableColor, ObjectXMRegionShape regionShape, ObjectIntMultiVectorJaccard keyWords, Rights rights, EnumSet<Territory> territories, int added, int archiveID, int[] attractiveness) {
        super(locatorURI, colorLayout, colorStructure, edgeHistogram, scalableColor, regionShape, keyWords, rights, territories, added, archiveID, attractiveness);
    }

    public MetaObjectProfiSCTiDIM(String locatorURI, Map<String, LocalAbstractObject> objects, Rights rights, EnumSet<Territory> territories, int added, int archiveID, int[] attractiveness) {
        super(locatorURI, objects, rights, territories, added, archiveID, attractiveness);
    }

    public MetaObjectProfiSCTiDIM(MetaObjectProfiSCT object, String titleString, String keywordString, ObjectIntMultiVectorJaccard keyWords) {
        super(object, titleString, keywordString, keyWords);
    }

    public MetaObjectProfiSCTiDIM(MetaObjectProfiSCT object) {
        super(object);
    }

    public MetaObjectProfiSCTiDIM(MetaObjectProfiSCT object, String titleString, String keywordString, String searchString, WordExpander expander, Stemmer stemmer, IntStorageIndexed<String> wordIndex) {
        super(object, titleString, keywordString, searchString, expander, stemmer, wordIndex);
    }

    public MetaObjectProfiSCTiDIM(MetaObjectProfiSCT object, String titleString, String keywordString, WordExpander expander, Stemmer stemmer, IntStorageIndexed<String> wordIndex) {
        super(object, titleString, keywordString, expander, stemmer, wordIndex);
    }

    public MetaObjectProfiSCTiDIM(MetaObjectProfiSCT object, int[] searchWordIds) {
        super(object, searchWordIds);
    }

    public MetaObjectProfiSCTiDIM(MetaObjectProfiSCT object, String searchString, WordExpander expander, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws TextConversionException {
        super(object, searchString, expander, stemmer, wordIndex);
    }

    public MetaObjectProfiSCTiDIM(BufferedReader stream, boolean haveStringWords, boolean haveConvertedWords) throws IOException {
        super(stream, haveStringWords, haveConvertedWords);
    }

    public MetaObjectProfiSCTiDIM(BufferedReader stream) throws IOException {
        super(stream);
    }

    public MetaObjectProfiSCTiDIM(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> wordIndex, String searchString) throws IOException {
        super(stream, stemmer, wordIndex, searchString);
    }

    public MetaObjectProfiSCTiDIM(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IOException {
        super(stream, stemmer, wordIndex);
    }

    public MetaObjectProfiSCTiDIM(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //***************************  Distance computation  *******************************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectProfiSCT castObj = (MetaObjectProfiSCTiDIM)obj;

        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null) {
            if (metaDistances != null) {
                metaDistances[0] = colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)/300.0f;
                rtv += metaDistances[0]*visualWeights[0];
            } else {
                rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)*visualWeights[0]/300.0f;
            }
        }

        if (colorStructure != null && castObj.colorStructure != null) {
            if (metaDistances != null) {
                metaDistances[1] = colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)/40.0f/255.0f;
                rtv += metaDistances[1]*visualWeights[1];
            } else {
                rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)*visualWeights[1]/40.0f/255.0f;
            }
        }

        if (edgeHistogram != null && castObj.edgeHistogram != null) {
            if (metaDistances != null) {
                metaDistances[2] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)/68.0f;
                rtv += metaDistances[2]*visualWeights[2];
            } else {
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*visualWeights[2]/68.0f;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[3] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)/3000.0f;
                rtv += metaDistances[3]*visualWeights[3];
            } else {
                rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)*visualWeights[3]/3000.0f;
            }
        }

        if (regionShape != null && castObj.regionShape != null) {
            if (metaDistances != null) {
                metaDistances[4] = regionShape.getDistanceImpl(castObj.regionShape, distThreshold)/8.0f;
                rtv += metaDistances[4]*visualWeights[4];
            } else {
                rtv += regionShape.getDistanceImpl(castObj.regionShape, distThreshold)*visualWeights[4]/8.0f;
            }
        }

        return rtv;
    }

    /**
     * Returns the weights for the respective {@link #getSupportedVisualDescriptorTypes() descriptors}
     * that are used in the distance function.
     *
     * @return the weights used in the distance function
     */
    public static float[] getWeights() {
        return visualWeights.clone();
    }

    @Override
    public float getMaxDistance() {
        float ret = 0;
        for (int i = 0; i < visualWeights.length; i++)
            ret += visualWeights[i];
        return ret;
    }



    //****************** Cloning ******************//

    /**
     * Creates and returns a copy of this object. The precise meaning
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag whether the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support cloning or there was an error
     */
    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObjectProfiSCTiDIM rtv = (MetaObjectProfiSCTiDIM)super.clone(cloneFilterChain);
        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectProfiSCTiDIM rtv = (MetaObjectProfiSCTiDIM)super.clone(true);
        return rtv;
    }


    //****************** Distance wrapper objects ******************//

    /**
     * Object that holds only keywords and measures the distance as the
     * weighted Cosine distance with weights based on tf-idf algorithm. Note
     * that the other object for the distance must be {@link MetaObjectProfiSCT}.
     */
    public static class MetaObjectProfiSCTiDIMKwDistCosine extends MetaObjectProfiSCTiDIM {
        /** Class id for serialization. */
        private static final long serialVersionUID = 2L;

        //****************** Attributes ******************//

        /** Weight for combining the keywords distance with the visual descriptors distance */
        private final Float keywordsWeight;
        /** Internal keyword weight provider based on tf-idf */
        private final ObjectIntMultiVector.WeightProvider kwWeightProvider;


        //****************** Constructor ******************//

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMKwDistCosine from the given {@link MetaObjectProfiSCT}.
         * The locator and the encapsulated objects from the source {@code object} are
         * taken.
         *
         * @param object the source metaobject from which to get the data
         * @param keywordsWeight the weight for combining the keywords distance with the visual descriptors distance,
         *          if <tt>null</tt>, only the text distance is used
         * @param keywordLayerWeights the weights for different layers of keywords (title, etc.)
         */
        public MetaObjectProfiSCTiDIMKwDistCosine(MetaObjectProfiSCT object, Float keywordsWeight, float[] keywordLayerWeights) {
            super(object);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = new DatabaseSupport.KeywordWeightProvider(null, keywordLayerWeights);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMKwDistCosine.
         * A keyword index is used to translate keywords to addresses.
         *
         * @param stream stream to read the data from
         * @param stemmer instances that provides a {@link Stemmer} for word transformation
         * @param wordIndex the index for translating words to addresses
         * @param keywordsWeight the weight for combining the keywords distance with the visual descriptors distance,
         *          if <tt>null</tt>, only the text distance is used
         * @param keywordLayerWeights the weights for different layers of keywords (title, etc.)
         * @throws IOException if reading from the stream fails
         */
        public MetaObjectProfiSCTiDIMKwDistCosine(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> wordIndex, Float keywordsWeight, float[] keywordLayerWeights) throws IOException {
            super(stream, stemmer, wordIndex);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = new DatabaseSupport.KeywordWeightProvider(null, keywordLayerWeights);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMKwDistCosine from the given text stream.
         * Note that the keywords are expected to be present and already converted to IDs.
         *
         * @param stream the stream from which the data are read
         * @param keywordsWeight the weight for combining the keywords distance with the visual descriptors distance,
         *          if <tt>null</tt>, only the text distance is used
         * @param keywordLayerWeights the weights for different layers of keywords (title, etc.)
         * @throws IOException if there was an error reading the data from the stream
         */
        public MetaObjectProfiSCTiDIMKwDistCosine(BufferedReader stream, Float keywordsWeight, float[] keywordLayerWeights) throws IOException {
            super(stream, true, true);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = new DatabaseSupport.KeywordWeightProvider(null, keywordLayerWeights);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMKwDistCosine from the given text stream.
         * Note that the keywords are expected to be present and already converted to IDs.
         * Null keyword weight is applied, i.e. the distance is computed by text only.
         *
         * @param stream the stream from which the data are read
         * @throws IOException if there was an error reading the data from the stream
         */
        public MetaObjectProfiSCTiDIMKwDistCosine(BufferedReader stream) throws IOException {
            this(stream, null, null);
        }


        //****************** Distance function ******************//

        @Override
        protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
            try {
                float distance = ObjectIntMultiVectorCosine.getWeightedCosineDistance(keyWords, kwWeightProvider, ((MetaObjectProfiSCT)obj).keyWords, kwWeightProvider);
                if (keywordsWeight != null)
                    distance = super.getDistanceImpl(obj, metaDistances, distThreshold) + keywordsWeight * distance;
                return distance;
            } catch (RuntimeException e) {
                throw new IllegalStateException("Error computing distance between " + this + " and " + obj + ": " + e, e);
            }
        }
    }

    /**
     * Extension of the MetaObjectProfiSCT that preserves also the title and keywords
     * strings in both binary and Java serialization.
     */
    public static class MetaObjectProfiSCTiDIMWithTKStrings extends MetaObjectProfiSCTiDIM {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMWithTKStrings from the given {@link MetaObjectProfiSCT}.
         * The locator, the attributes and the encapsulated objects from the source {@code object} are
         * taken.
         *
         * @param object the source metaobject from which to get the data
         */
        public MetaObjectProfiSCTiDIMWithTKStrings(MetaObjectProfiSCT object) {
            super(object);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMWithTKStrings from the given {@link MetaObjectProfiSCT}.
         * The locator, the attributes and the encapsulated objects from the source {@code object} are
         * copied. The key word identifiers object as well as the title and keyword strings are replaced by the given ones.
         *
         * @param object the source metaobject from which to get the data
         * @param titleString the title of this object as string
         * @param keywordString the keywords of this object as string
         * @param keyWords new value for the {@link #keyWords} object
         */
        public MetaObjectProfiSCTiDIMWithTKStrings(MetaObjectProfiSCT object, String titleString, String keywordString, ObjectIntMultiVectorJaccard keyWords) {
            super(object, titleString, keywordString, keyWords);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTiDIMWithTKStrings loaded from binary input buffer.
         *
         * @param input the buffer to read the MetaObjectProfiSCTWithTKStrings from
         * @param serializator the serializator used to write objects
         * @throws IOException if there was an I/O error reading from the buffer
         */
        protected MetaObjectProfiSCTiDIMWithTKStrings(BinaryInput input, BinarySerializator serializator) throws IOException {
            super(input, serializator);
            titleString = serializator.readString(input);
            keywordString = serializator.readString(input);
        }

        @Override
        public int getBinarySize(BinarySerializator serializator) {
            int size = super.getBinarySize(serializator);
            size += serializator.getBinarySize(titleString);
            size += serializator.getBinarySize(keywordString);
            return size;
        }

        @Override
        public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
            int size = super.binarySerialize(output, serializator);
            size += serializator.write(output, titleString);
            size += serializator.write(output, keywordString);
            return size;
        }

        /**
         * Java native serialization method.
         * @param out the stream to serialize this object to
         * @throws IOException if there was an error writing to the stream {@code out}
         */
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeObject(titleString);
            out.writeObject(keywordString);
        }

        /**
         * Java native serialization method.
         * @param in the stream to deserialize this object from
         * @throws IOException if there was an error reading from the stream {@code in}
         * @throws ClassNotFoundException if an unknown class was encountered in the stream 
         */
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            titleString = (String)in.readObject();
            keywordString = (String)in.readObject();
        }
    }

}
