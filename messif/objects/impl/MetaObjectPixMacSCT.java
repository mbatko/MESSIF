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
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import messif.buckets.BucketStorageException;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.impl.ObjectIntMultiVector.SortedDataIterator;
import messif.objects.impl.ObjectIntMultiVectorJaccard.WeightProvider;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * Special meta object that stores only the objects required for the PixMac search.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectPixMacSCT extends MetaObject implements BinarySerializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {
        "ColorLayoutType","ColorStructureType","EdgeHistogramType",
        "ScalableColorType","RegionShapeType","KeyWordsType"
    };


    //****************** Attributes ******************//

    /** Object for the ColorLayoutType */
    protected ObjectColorLayout colorLayout;
    /** Object for the ColorStructureType */
    protected ObjectShortVectorL1 colorStructure;
    /** Object for the EdgeHistogramType */
    protected ObjectVectorEdgecomp edgeHistogram;
    /** Object for the ScalableColorType */
    protected ObjectIntVectorL1 scalableColor;
    /** Object for the RegionShapeType */
    protected ObjectXMRegionShape regionShape;
    /** Object for the KeyWordsType */
    protected ObjectIntMultiVectorJaccard keyWords;
    /** Value of the AttractivenessType */
    protected short attractiveness;
    /** Value of the CreditsType */
    protected byte credits;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectPixMacSCT from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param colorLayout color layout descriptor
     * @param colorStructure  color structure descriptor
     * @param edgeHistogram edge histogram descriptor
     * @param scalableColor scalable color descriptor
     * @param regionShape region shape descriptor
     * @param keyWords key words descriptor
     * @param attractiveness value of the attractiveness
     * @param credits number of credits
     */
    public MetaObjectPixMacSCT(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure,
            ObjectVectorEdgecomp edgeHistogram, ObjectIntVectorL1 scalableColor, ObjectXMRegionShape regionShape,
            ObjectIntMultiVectorJaccard keyWords, short attractiveness, byte credits) {
        super(locatorURI);
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.edgeHistogram = edgeHistogram;
        this.scalableColor = scalableColor;
        this.regionShape = regionShape;
        this.keyWords = keyWords;
        this.attractiveness = attractiveness;
        this.credits = credits;
    }

    /**
     * Creates a new instance of MetaObjectPixMacSCT from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects map of objects with the {@link #descriptorNames} as keys
     * @param cloneObjects if <tt>true</tt>, the {@link Object#clone() clonned} objects from the map will be stored in this metaobject
     * @param attractiveness value of the atractiveness
     * @param credits number of credits
     * @throws CloneNotSupportedException if there was a problem clonning an object from the map
     */
    public MetaObjectPixMacSCT(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects, short attractiveness, byte credits) throws CloneNotSupportedException {
        super(locatorURI);
        this.colorLayout = getObjectFromMap(objects, descriptorNames[0], ObjectColorLayout.class, cloneObjects, getObjectKey());
        this.colorStructure = getObjectFromMap(objects, descriptorNames[1], ObjectShortVectorL1.class, cloneObjects, getObjectKey());
        this.edgeHistogram = getObjectFromMap(objects, descriptorNames[2], ObjectVectorEdgecomp.class, cloneObjects, getObjectKey());
        this.scalableColor = getObjectFromMap(objects, descriptorNames[3], ObjectIntVectorL1.class, cloneObjects, getObjectKey());
        this.regionShape = getObjectFromMap(objects, descriptorNames[4], ObjectXMRegionShape.class, cloneObjects, getObjectKey());
        this.keyWords = getObjectFromMap(objects, descriptorNames[5], ObjectIntMultiVectorJaccard.class, cloneObjects, getObjectKey());
        this.attractiveness = attractiveness;
        this.credits = credits;
    }

    /**
     * Creates a new instance of MetaObjectPixMacSCT from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects map of objects with the {@link #descriptorNames} as keys
     * @param attractiveness value of the atractiveness
     * @param credits number of credits
     */
    public MetaObjectPixMacSCT(String locatorURI, Map<String, LocalAbstractObject> objects, short attractiveness, byte credits) {
        super(locatorURI);
        this.colorLayout = (ObjectColorLayout)objects.get(descriptorNames[0]);
        this.colorStructure = (ObjectShortVectorL1)objects.get(descriptorNames[1]);
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get(descriptorNames[2]);
        this.scalableColor = (ObjectIntVectorL1)objects.get(descriptorNames[3]);
        this.regionShape = (ObjectXMRegionShape)objects.get(descriptorNames[4]);
        this.keyWords = (ObjectIntMultiVectorJaccard)objects.get(descriptorNames[5]);
        this.attractiveness = attractiveness;
        this.credits = credits;
    }

    /**
     * Creates a new instance of MetaObjectPixMacSCT from the given {@link MetaObject}.
     * The locator and the encapsulated objects from the source {@code object} are
     * taken.
     *
     * @param object the source metaobject from which to get the data
     */
    public MetaObjectPixMacSCT(MetaObjectPixMacSCT object) {
        this(object.getLocatorURI(), object.getObjectMap(), object.attractiveness, object.credits);
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param keyWordIndex the index for translating keywords to addresses
     * @param titleWords the title words to set for the new object
     * @param keyWords the keywords to set for the new object
     * @param searchWords the searched keywords to set for the new object
     */
    public MetaObjectPixMacSCT(MetaObjectPixMacSCT object, IntStorageIndexed<String> keyWordIndex, String[] titleWords, String[] keyWords, String[] searchWords) {
        this(object);
        if (titleWords != null || keyWords != null || searchWords != null) {
            Set<String> ignoreWords = new HashSet<String>();
            int[][] wordIds;
            if (searchWords != null && searchWords.length > 0) {
                wordIds = new int[3][];
                wordIds[2] = keywordsToIdentifiers(searchWords, ignoreWords, keyWordIndex);
            } else {
                wordIds = new int[2][];
            }
            wordIds[0] = keywordsToIdentifiers(titleWords, ignoreWords, keyWordIndex);
            wordIds[1] = keywordsToIdentifiers(keyWords, ignoreWords, keyWordIndex);
            this.keyWords = new ObjectIntMultiVectorJaccard(wordIds);
        }
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param keyWordIndex the index for translating keywords to addresses
     * @param titleWords the title words to set for the new object
     * @param keyWords the keywords to set for the new object
     */
    public MetaObjectPixMacSCT(MetaObjectPixMacSCT object, IntStorageIndexed<String> keyWordIndex, String[] titleWords, String[] keyWords) {
        this(object, keyWordIndex, titleWords, keyWords, (String[])null);
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param keyWordIndex the index for translating keywords to addresses
     * @param titleWords the title words to set for the new object
     * @param keyWords the keywords to set for the new object
     * @param searchWords the searched keywords to set for the new object
     */
    public MetaObjectPixMacSCT(MetaObjectPixMacSCT object, IntStorageIndexed<String> keyWordIndex, String[] titleWords, String[] keyWords, String searchWords) {
        this(object, keyWordIndex, titleWords, keyWords, (searchWords == null || searchWords.isEmpty()) ? null : searchWords.split("\\s+"));
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given text stream.
     * The stream may contain the '#...' lines with object key and/or precomputed distances
     * and a mandatory line for each descriptor name, from which the respective
     * descriptor {@link LocalAbstractObject} is loaded.
     *
     * @param stream the stream from which the data are read
     * @param haveKeyWords flag whether the data contains keyWords
     * @param readKeyWords flag whether to read the keyWords (<tt>true</tt>) or
     *          skip the keywords line (<tt>false</tt>)
     * @param readValues  flag whether to read the attractiveness and credit lines (<tt>true</tt>) or
     *          skip them (<tt>false</tt>)
     * @throws IOException if there was an error reading the data from the stream
     */
    public MetaObjectPixMacSCT(BufferedReader stream, boolean haveKeyWords, boolean readKeyWords, boolean readValues) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        colorLayout = new ObjectColorLayout(new BufferedReader(new StringReader(line)));
        colorStructure = new ObjectShortVectorL1(stream);
        edgeHistogram = new ObjectVectorEdgecomp(stream);
        scalableColor = new ObjectIntVectorL1(stream);
        regionShape = new ObjectXMRegionShape(stream);
        if (haveKeyWords) {
            if (readKeyWords) {
                keyWords = new ObjectIntMultiVectorJaccard(stream, 2);
            } else {
                stream.readLine(); // Skip the two lines with keywords
                stream.readLine(); // Skip the two lines with keywords
            }
            if (readValues) {
                line = stream.readLine();
                attractiveness = (line == null || line.isEmpty()) ? 0 : Short.parseShort(line);
                line = stream.readLine();
                credits = (line == null || line.isEmpty()) ? 0 : Byte.parseByte(line);
            } else {
                stream.readLine(); // Skip the line with attractiveness
                stream.readLine(); // Skip the line with credits
            }
        }
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor.
     *
     * @param stream stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectPixMacSCT(BufferedReader stream) throws IOException {
        this(stream, true, false, false);
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor.
     * A keyword index is used to translate keywords to addresses.
     *
     * @param stream stream to read the data from
     * @param keyWordIndex the index for translating keywords to addresses
     * @param readAdditionalKeyWords if <tt>true</tt>, the additional keywords are read from the stream and encapsulated in the keyWords object as third array
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectPixMacSCT(BufferedReader stream, IntStorageIndexed<String> keyWordIndex, boolean readAdditionalKeyWords) throws IOException {
        this(stream, false, false, false);
        // Read all data from the stream
        String kwLine1 = stream.readLine();
        String kwLine2 = stream.readLine();
        String line = stream.readLine();
        attractiveness = (line == null || line.isEmpty()) ? 0 : Short.parseShort(line);
        line = stream.readLine();
        credits = (line == null || line.isEmpty()) ? 0 : Byte.parseByte(line);
        // The additional keywords are added AS THE LAST LINE OF THE OBJECT!
        String additionalKeyWords = readAdditionalKeyWords ? stream.readLine() : null;

        // Process the keywords (transformation to identifiers)
        Set<String> ignoreWords = new HashSet<String>();
        try {
            int[][] data = new int[additionalKeyWords != null ? 3 : 2][];
            if (additionalKeyWords != null)
                data[2] = keywordsToIdentifiers(additionalKeyWords.split(";"), ignoreWords, keyWordIndex);
            data[0] = keywordsToIdentifiers(kwLine1.split(";"), ignoreWords, keyWordIndex);
            data[1] = keywordsToIdentifiers(kwLine2.split(";"), ignoreWords, keyWordIndex);
            keyWords = new ObjectIntMultiVectorJaccard(data);
        } catch (Exception e) {
            Logger.getLogger(MetaObjectPixMacSCT.class.getName()).warning("Cannot create keywords for object '" + getLocatorURI() + "': " + e.toString());
            keyWords = new ObjectIntMultiVectorJaccard(new int[][] {{},{}}, false);
        }
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor.
     * A keyword index is used to translate keywords to addresses.
     *
     * @param stream stream to read the data from
     * @param keyWordIndex the index for translating keywords to addresses
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectPixMacSCT(BufferedReader stream, IntStorageIndexed<String> keyWordIndex) throws IOException {
        this(stream, keyWordIndex, false);
    }

    /**
     * Returns list of supported visual descriptor types that this object recognizes in XML.
     * @return list of supported visual descriptor types
     */
    public static String[] getSupportedVisualDescriptorTypes() {
        return descriptorNames;
    }

    /**
     * Return a trimmed, lower-case word with stripped diacritics that is not ignored.
     * Note that the ignore words are updated whenever a non-ignored word is found.
     * @param keyWord the keyword to transform
     * @param ignoreWords set of words to ignore
     * @return a trimmed, lower-case word with stripped diacritics that is not ignored
     */
    protected static String unifyKeyword(String keyWord, Set<String> ignoreWords) {
        if (keyWord == null)
            return null;
        // Trim and check for empty words
        keyWord = keyWord.trim();
        if (keyWord.isEmpty())
            return null;
        // Remove diacritics and make lower case
        keyWord = Normalizer.normalize(keyWord.toLowerCase(), Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Check if not ignored
        if (ignoreWords != null && !ignoreWords.add(keyWord))
            return null;
        return keyWord;
    }

    /**
     * Transforms a list of keywords into array of addresses.
     * Note that unknown keywords are added to the index.
     * All items from the list are removed during the process, so
     * do not pass an unmodifiable list!
     *
     * @param keyWords the list of keywords to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param keyWordIndex the index for translating keywords to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     */
    private static int[] keywordsToIdentifiers(String[] keyWords, Set<String> ignoreWords, IntStorageIndexed<String> keyWordIndex) {
        if (keyWords == null)
            return new int[0];

        // Convert array to a set, ignoring words from ignoreWords (e.g. words added by previous call)
        Set<String> processedKeyWords = new HashSet<String>(keyWords.length);
        for (int i = 0; i < keyWords.length; i++) {
            String keyWord = unifyKeyword(keyWords[i], ignoreWords);
            if (keyWord != null)
                processedKeyWords.add(keyWord);
        }

        // If the keywords list is empty after ignored words, return
        if (processedKeyWords.isEmpty())
            return new int[0];

        // Search the index
        int[] ret = new int[processedKeyWords.size()];
        IntStorageSearch<String> search = keyWordIndex.search(LocalAbstractObjectOrder.trivialObjectComparator, processedKeyWords);
        int retIndex;
        for (retIndex = 0; search.next(); retIndex++) {
            processedKeyWords.remove(search.getCurrentObject());
            ret[retIndex] = search.getCurrentObjectIntAddress();
        }

        // Add all missing keywords
        for (Iterator<String> it = processedKeyWords.iterator(); it.hasNext();) {
            String keyWord = it.next();
            try {
                ret[retIndex] = keyWordIndex.store(keyWord).getAddress();
                retIndex++;
            } catch (BucketStorageException e) {
                Logger.getLogger(MetaObjectPixMacSCT.class.getName()).warning("Cannot insert '" + keyWord + "': " + e.toString());
            }
        }

        // Resize the array if some keywords could not be added to the database
        if (retIndex != ret.length) {
            int[] saved = ret;
            ret = new int[retIndex];
            System.arraycopy(saved, 0, ret, 0, retIndex);
        }

        return ret;
    }

    /**
     * Implementation of {@link WeightProvider} that has a single weight for every data array of the {@link ObjectIntMultiVector}
     *  and it ignores a specified list of integers (created from a given list of keywords).
     */
    public static class MultiWeightIgnoreProviderPixMac extends ObjectIntMultiVectorJaccard.MultiWeightIgnoreProvider {

        /** Class id for serialization. */
        private static final long serialVersionUID = 51201L;

        /**
         * Creates a new instance of MultiWeightProvider with the the given array of weights.
         * @param weights the weights for the data arrays
         * @param ignoreWeight weight used for the {@code ignoredKeywords}
         * @param ignoredKeywords array of keywords to be ignored (before stemming and other corrections)
         * @param keyWordIndex typically database storage to convert keywords to IDs and other parameters
         */
        public MultiWeightIgnoreProviderPixMac(float[] weights, float ignoreWeight, String[] ignoredKeywords, IntStorageIndexed<String> keyWordIndex) {
            super(weights, ignoreWeight, getIgnoredIDs(ignoredKeywords, keyWordIndex));
        }

        /**
         * Internal method to create a set of integer IDs for specified keywords given a PixMac keyword -> ID index.
         * @param ignoredKeywords array of keywords to be ignored (before stemming and other corrections)
         * @param keyWordIndex typically database storage to convert keywords to IDs and other parameters
         * @return
         */
        private static Set<Integer> getIgnoredIDs(String[] ignoredKeywords, IntStorageIndexed<String> keyWordIndex) {
            HashSet<Integer> retVal = new HashSet<Integer>();
            int[] keywordsToIdentifiers = keywordsToIdentifiers(ignoredKeywords, new HashSet<String>(), keyWordIndex);
            for (int id : keywordsToIdentifiers) {
                retVal.add(id);
            }
            return retVal;
        }
    }

    //****************** MetaObject overrides ******************//

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    @Override
    public int getObjectCount() {
        return descriptorNames.length;
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    @Override
    public LocalAbstractObject getObject(String name) {
        if (descriptorNames[0].equals(name))
            return colorLayout;
        else if (descriptorNames[1].equals(name))
            return colorStructure;
        else if (descriptorNames[2].equals(name))
            return edgeHistogram;
        else if (descriptorNames[3].equals(name))
            return scalableColor;
        else if (descriptorNames[4].equals(name))
            return regionShape;
        else if (descriptorNames[5].equals(name))
            return keyWords;
        else
            return null;
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        return Arrays.asList((LocalAbstractObject)colorLayout, colorStructure, edgeHistogram, scalableColor, regionShape, keyWords);
    }

    @Override
    public Collection<String> getObjectNames() {
        return Arrays.asList(descriptorNames);
    }

    /**
     * Returns the object that encapsulates the keywords for this metaobject.
     * @return the object that encapsulates the keywords
     */
    public ObjectIntMultiVectorJaccard getKeyWords() {
        return keyWords;
    }

    @Override
    public int dataHashCode() {
        int rtv = colorLayout.dataHashCode();
        rtv += colorStructure.dataHashCode();
        rtv += edgeHistogram.dataHashCode();
        rtv += scalableColor.dataHashCode();
        rtv += regionShape.dataHashCode();
        //rtv += keyWords.dataHashCode();
        return rtv;
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof MetaObjectPixMacSCT))
            return false;
        MetaObjectPixMacSCT castObj = (MetaObjectPixMacSCT)obj;
        if (!colorLayout.dataEquals(castObj.colorLayout))
            return false;
        if (!colorStructure.dataEquals(castObj.colorStructure))
            return false;
        if (!edgeHistogram.dataEquals(castObj.edgeHistogram))
            return false;
        if (!scalableColor.dataEquals(castObj.scalableColor))
            return false;
        if (!regionShape.dataEquals(castObj.regionShape))
            return false;
        //if (keyWords != null && castObj.keyWords != null && !keyWords.dataEquals(castObj.keyWords))
        //    return false;
        return true;
    }


    //***************************  Distance computation  *******************************//

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectPixMacSCT castObj = (MetaObjectPixMacSCT)obj;

        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null) {
            if (metaDistances != null) {
                metaDistances[0] = colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)/300.0f;
                rtv += metaDistances[0]*2.0f;
            } else {
                rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)*2.0f/300.0f;
            }
        }

        if (colorStructure != null && castObj.colorStructure != null) {
            if (metaDistances != null) {
                metaDistances[1] = colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)/40.0f/255.0f;
                rtv += metaDistances[1]*2.0f;
            } else {
                rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)*2.0f/40.0f/255.0f;
            }
        }

        if (edgeHistogram != null && castObj.edgeHistogram != null) {
            if (metaDistances != null) {
                metaDistances[2] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)/68.0f;
                rtv += metaDistances[2]*5.0f;
            } else {
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*5.0f/68.0f;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[3] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)/3000.0f;
                rtv += metaDistances[3]*2.0f;
            } else {
                rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)*2.0f/3000.0f;
            }
        }

        if (regionShape != null && castObj.regionShape != null) {
            if (metaDistances != null) {
                metaDistances[4] = regionShape.getDistanceImpl(castObj.regionShape, distThreshold)/8.0f;
                rtv += metaDistances[4]*4.0f;
            } else {
                rtv += regionShape.getDistanceImpl(castObj.regionShape, distThreshold)*4.0f/8.0f;
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
        return new float[] { 2.0f, 2.0f, 5.0f, 2.0f, 4.0f};
    }

    @Override
    public float getMaxDistance() {
        return 16f;
    }



    //****************** Clonning ******************//

    /**
     * Creates and returns a copy of this object. The precise meaning
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag wheter the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObjectPixMacSCT rtv = (MetaObjectPixMacSCT)super.clone(cloneFilterChain);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.clone(cloneFilterChain);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.clone(cloneFilterChain);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.clone(cloneFilterChain);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.clone(cloneFilterChain);
        if (regionShape != null)
            rtv.regionShape = (ObjectXMRegionShape)regionShape.clone(cloneFilterChain);
        if (keyWords != null)
            rtv.keyWords = (ObjectIntMultiVectorJaccard)keyWords.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectPixMacSCT rtv = (MetaObjectPixMacSCT)super.clone(true);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.cloneRandomlyModify(args);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.cloneRandomlyModify(args);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.cloneRandomlyModify(args);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.cloneRandomlyModify(args);
        if (regionShape != null)
            rtv.regionShape = (ObjectXMRegionShape)regionShape.cloneRandomlyModify(args);
        if (keyWords != null)
            rtv.keyWords = (ObjectIntMultiVectorJaccard)keyWords.cloneRandomlyModify(args);
        return rtv;
    }

    /**
     * Store this object to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        // Write a line for every object from the list
        if (colorLayout != null)
            colorLayout.writeData(stream);
        else
            stream.write('\n');

        if (colorStructure != null)
            colorStructure.writeData(stream);
        else
            stream.write('\n');

        if (edgeHistogram != null)
            edgeHistogram.writeData(stream);
        else
            stream.write('\n');

        if (scalableColor != null)
            scalableColor.writeData(stream);
        else
            stream.write('\n');

        if (regionShape != null)
            regionShape.writeData(stream);
        else
            stream.write('\n');

        if (keyWords != null)
            keyWords.writeData(stream);
        else
            stream.write('\n');
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor loaded from binary input buffer.
     *
     * @param input the buffer to read the MetaObjectPixMacShapeAndColor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectPixMacSCT(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        regionShape = serializator.readObject(input, ObjectXMRegionShape.class);
        keyWords = serializator.readObject(input, ObjectIntMultiVectorJaccard.class);
        attractiveness = serializator.readShort(input);
        credits = serializator.readByte(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, scalableColor);
        size += serializator.write(output, regionShape);
        size += serializator.write(output, keyWords);
        size += serializator.write(output, attractiveness);
        size += serializator.write(output, credits);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(colorLayout);
        size += serializator.getBinarySize(colorStructure);
        size += serializator.getBinarySize(edgeHistogram);
        size += serializator.getBinarySize(scalableColor);
        size += serializator.getBinarySize(regionShape);
        size += serializator.getBinarySize(keyWords);
        size += Short.SIZE/8;
        size += Byte.SIZE/8;
        return size;
    }

}
