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
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import messif.buckets.BucketStorageException;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.buckets.storage.impl.DatabaseStorage;
import messif.buckets.storage.impl.DatabaseStorage.BinarySerializableColumnConvertor;
import messif.buckets.storage.impl.DatabaseStorage.ColumnConvertor;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.CachingSerializator;

/**
 * Special meta object that stores only the objects required for the Profi search.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectProfiSCT extends MetaObject implements BinarySerializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {
        "ColorLayoutType","ColorStructureType","EdgeHistogramType",
        "ScalableColorType","RegionShapeType","KeyWordsType"
    };

    /** Weights for the visual descriptors */
    protected static float[] visualWeights = { 2.0f, 2.0f, 5.0f, 2.0f, 4.0f };


    //****************** Stemmer interface ******************//

    /**
     * Interface for stemmer instances.
     */
    public static interface Stemmer {
        /**
         * Provides a stem for the given word.
         * @param word the word for which to provide the stem
         * @return the stem for the given word
         */
        public abstract String stem(String word);
    }


    //****************** Enumeration classes ******************//

    /** List of rights */
    public static enum Rights {
        /** No right set */
        EMPTY,
        /** RM right */
        RM,
        /** RF right */
        RF;
    }

    /** List of territories */
    public static enum Territory {
        /** Czech Republic */
        CZ,
        /** Slovak Republic */
        SK,
        /** Hungary */
        HU,
        /** Finland */
        FI,
        /** Morocco */
        MA,
        /** English */
        EN,
        /** Suriname */
        SR,
        /** Sierra Leone */
        SL,
        /** Croatia */
        HR
    }


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
    /** Rights for this object */
    protected Rights rights;
    /** List of territories associated with this object */
    protected EnumSet<Territory> territories;
    /** Date this object was added to the collection */
    protected int added;
    /** ID of the archive from which this object was added */
    protected int archiveID;
    /** List of attractiveness values for all existing territories */
    protected int[] attractiveness;
    /** Title of this object as string (this is not serialized!) */
    protected transient String titleString;
    /** Keywords of this object as string (this is not serialized!) */
    protected transient String keywordString;
    /** Object for the KeyWordsType */
    protected ObjectIntMultiVectorJaccard keyWords;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param colorLayout color layout descriptor
     * @param colorStructure  color structure descriptor
     * @param edgeHistogram edge histogram descriptor
     * @param scalableColor scalable color descriptor
     * @param regionShape region shape descriptor
     * @param keyWords key words descriptor
     * @param rights the rights for this object
     * @param territories the list of territories associated with this object
     * @param added the date this object was added to the collection
     * @param archiveID the ID of the archive from which this object was added
     * @param attractiveness value of the attractiveness
     */
    public MetaObjectProfiSCT(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure,
            ObjectVectorEdgecomp edgeHistogram, ObjectIntVectorL1 scalableColor, ObjectXMRegionShape regionShape,
            ObjectIntMultiVectorJaccard keyWords, Rights rights, EnumSet<Territory> territories,
            int added, int archiveID, int[] attractiveness) {
        super(locatorURI);
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.edgeHistogram = edgeHistogram;
        this.scalableColor = scalableColor;
        this.regionShape = regionShape;
        this.rights = rights;
        this.territories = EnumSet.copyOf(territories);
        this.added = added;
        this.archiveID = archiveID;
        this.attractiveness = attractiveness;
        this.titleString = null;
        this.keywordString = null;
        this.keyWords = keyWords;
    }


    /**
     * Creates a new instance of MetaObjectProfiSCT from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects map of objects with the {@link #descriptorNames} as keys
     * @param rights the rights for this object
     * @param territories the list of territories associated with this object
     * @param added the date this object was added to the collection
     * @param archiveID the ID of the archive from which this object was added
     * @param attractiveness value of the atractiveness
     */
    public MetaObjectProfiSCT(String locatorURI, Map<String, LocalAbstractObject> objects,
            Rights rights, EnumSet<Territory> territories, int added, int archiveID, int[] attractiveness) {
        this(locatorURI,
                (ObjectColorLayout)objects.get(descriptorNames[0]),
                (ObjectShortVectorL1)objects.get(descriptorNames[1]),
                (ObjectVectorEdgecomp)objects.get(descriptorNames[2]),
                (ObjectIntVectorL1)objects.get(descriptorNames[3]),
                (ObjectXMRegionShape)objects.get(descriptorNames[4]),
                (ObjectIntMultiVectorJaccard)objects.get(descriptorNames[5]),
                rights, territories, added, archiveID, attractiveness);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObject}.
     * The locator and the encapsulated objects from the source {@code object} are
     * taken.
     *
     * @param object the source metaobject from which to get the data
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object) {
        this(object.getLocatorURI(), object.getObjectMap(),
                object.rights, object.territories, object.added, object.archiveID, object.attractiveness);
        this.titleString = object.titleString;
        this.keywordString = object.keywordString;
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param stemmer instances that provides a {@link Stemmer} for word transformation
     * @param keyWordIndex the index for translating keywords to addresses
     * @param titleWords the title words to set for the new object
     * @param keyWords the keywords to set for the new object
     * @param searchWords the searched keywords to set for the new object
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex, String[] titleWords, String[] keyWords, String[] searchWords) {
        this(object);
        if (titleWords != null || keyWords != null || searchWords != null) {
            Set<String> ignoreWords = new HashSet<String>();
            int[][] wordIds;
            if (searchWords != null && searchWords.length > 0) {
                wordIds = new int[3][];
                wordIds[2] = keywordsToIdentifiers(searchWords, ignoreWords, stemmer, keyWordIndex);
            } else {
                wordIds = new int[2][];
            }
            wordIds[0] = keywordsToIdentifiers(titleWords, ignoreWords, stemmer, keyWordIndex);
            wordIds[1] = keywordsToIdentifiers(keyWords, ignoreWords, stemmer, keyWordIndex);
            this.titleString = joinString(titleWords, " ");
            this.keywordString = joinString(keyWords, ",");
            this.keyWords = new ObjectIntMultiVectorJaccard(wordIds);
        }
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param stemmer instances that provides a {@link Stemmer} for word transformation
     * @param keyWordIndex the index for translating keywords to addresses
     * @param titleWords the title words to set for the new object
     * @param keyWords the keywords to set for the new object
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex, String[] titleWords, String[] keyWords) {
        this(object, stemmer, keyWordIndex, titleWords, keyWords, (String[])null);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param stemmer instances that provides a {@link Stemmer} for word transformation
     * @param keyWordIndex the index for translating keywords to addresses
     * @param titleWords the title words to set for the new object
     * @param keyWords the keywords to set for the new object
     * @param searchWords the searched keywords to set for the new object
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex, String[] titleWords, String[] keyWords, String searchWords) {
        this(object, stemmer, keyWordIndex, titleWords, keyWords, (searchWords == null || searchWords.isEmpty()) ? null : searchWords.split("\\s+"));
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given text stream.
     * The stream may contain the '#...' lines with object key and/or precomputed distances
     * and a mandatory line for each descriptor name, from which the respective
     * descriptor {@link LocalAbstractObject} is loaded.
     *
     * @param stream the stream from which the data are read
     * @param haveWords flag whether the data contains titlewords and keywords lines
     * @param readWords flag whether to read the titlewords and keywords lines(<tt>true</tt>) or
     *          skip them (<tt>false</tt>)
     * @throws IOException if there was an error reading the data from the stream
     */
    public MetaObjectProfiSCT(BufferedReader stream, boolean haveWords, boolean readWords) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        colorLayout = new ObjectColorLayout(new BufferedReader(new StringReader(line)));
        colorStructure = new ObjectShortVectorL1(stream);
        edgeHistogram = new ObjectVectorEdgecomp(stream);
        scalableColor = new ObjectIntVectorL1(stream);
        regionShape = new ObjectXMRegionShape(stream);
        String rightsStr = stream.readLine();
        rights = rightsStr.isEmpty() ? Rights.EMPTY : Rights.valueOf(rightsStr);
        territories = stringToTerritories(stream.readLine());
        added = Integer.valueOf(stream.readLine());
        archiveID = Integer.valueOf(stream.readLine());
        attractiveness = ObjectIntVector.parseIntVector(stream.readLine());
        titleString = null;
        keywordString = null;
        if (haveWords) {
            if (readWords) {
                keyWords = new ObjectIntMultiVectorJaccard(stream, 2);
            } else {
                stream.readLine(); // Skip the line with titlewords
                stream.readLine(); // Skip the line with keywords
            }
        }
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT.
     *
     * @param stream stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectProfiSCT(BufferedReader stream) throws IOException {
        this(stream, true, false);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT.
     * A keyword index is used to translate keywords to addresses.
     *
     * @param stream stream to read the data from
     * @param stemmer instances that provides a {@link Stemmer} for word transformation
     * @param keyWordIndex the index for translating keywords to addresses
     * @param readAdditionalKeyWords if <tt>true</tt>, the additional keywords are read from the stream and encapsulated in the keyWords object as third array
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectProfiSCT(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex, boolean readAdditionalKeyWords) throws IOException {
        this(stream, false, false);
        // Read all data from the stream
        titleString = stream.readLine();
        keywordString = stream.readLine();
        // The additional keywords are added AS THE LAST LINE OF THE OBJECT!
        String additionalKeyWords = readAdditionalKeyWords ? stream.readLine() : null;

        // Process the keywords (transformation to identifiers)
        int[][] data = new int[additionalKeyWords != null ? 3 : 2][];
        Set<String> ignoreWords = new HashSet<String>();
        try {
            if (additionalKeyWords != null)
                data[2] = keywordsToIdentifiers(additionalKeyWords.split("\\W+"), ignoreWords, stemmer, keyWordIndex);
            data[0] = keywordsToIdentifiers(titleString.split("\\W+"), ignoreWords, stemmer, keyWordIndex);
            data[1] = keywordsToIdentifiers(keywordString.split("\\W+"), ignoreWords, stemmer, keyWordIndex);
        } catch (Exception e) {
            Logger.getLogger(MetaObjectProfiSCT.class.getName()).warning("Cannot create keywords for object '" + getLocatorURI() + "': " + e.toString());
            keyWords = new ObjectIntMultiVectorJaccard(new int[][] {{},{}}, false);
        }
        keyWords = new ObjectIntMultiVectorJaccard(data);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT.
     * A keyword index is used to translate keywords to addresses.
     *
     * @param stream stream to read the data from
     * @param stemmer instances that provides a {@link Stemmer} for word transformation
     * @param keyWordIndex the index for translating keywords to addresses
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectProfiSCT(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex) throws IOException {
        this(stream, stemmer, keyWordIndex, false);
    }


    //****************** Conversion methods ******************//

    /**
     * Transforms a list of keywords into array of addresses.
     * Note that unknown keywords are added to the index.
     * All items from the list are removed during the process, so
     * do not pass an unmodifiable list!
     *
     * @param keyWords the list of keywords to transform
     * @param ignoreWords set of words to ignore (e.g. the previously added keywords);
     *          if <tt>null</tt>, all keywords are added
     * @param stemmer instances that provides a {@link Stemmer} for word transformation
     * @param keyWordIndex the index for translating keywords to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     */
    private int[] keywordsToIdentifiers(String[] keyWords, Set<String> ignoreWords, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex) {
        if (keyWords == null)
            return new int[0];

        // Convert array to a set, ignoring words from ignoreWords (e.g. words added by previous call)
        Set<String> processedKeyWords = new HashSet<String>(keyWords.length);
        for (int i = 0; i < keyWords.length; i++) {
            String keyWord = keyWords[i].trim().toLowerCase();
            if (keyWord.isEmpty())
                continue;
            if (stemmer != null)
                keyWord = stemmer.stem(keyWord);
            if (ignoreWords == null || ignoreWords.add(keyWord))
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
                Logger.getLogger(MetaObjectProfiSCT.class.getName()).warning("Cannot insert '" + keyWord + "' for object '" + getLocatorURI() + "': " + e.toString());
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
     * Convert a line with comma-separated territories to {@link EnumSet}.
     * @param line the line with territories
     * @return the set of parsed territories
     */
    private EnumSet<Territory> stringToTerritories(String line) {
        if (line == null || line.isEmpty())
            return null;
        EnumSet<Territory> ret = EnumSet.noneOf(Territory.class);
        for (String territory : line.toUpperCase().split("\\W+")) {
            if (territory.isEmpty())
                continue;
            try {
                ret.add(Territory.valueOf(territory));
            } catch (IllegalArgumentException e) {
                Logger.getLogger(MetaObjectProfiSCT.class.getName()).warning("Cannot read territory '" + territory + "' for object '" + getLocatorURI() + "': " + e.toString());
            }
        }
        return ret;
    }

    /**
     * Returns a string joined from the given {@code values} by the given {@code separator}.
     * @param values the list of strings to join
     * @param separator the separator between strings to use
     * @return a joined string
     */
    private static String joinString(String[] values, String separator) {
        if (values == null)
            return null;
        if (values.length == 0)
            return "";
        StringBuilder str = new StringBuilder(values[0]);
        for (int i = 1; i < values.length; i++)
            str.append(separator).append(values[i]);
        return str.toString();
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns list of supported visual descriptor types that this object recognizes.
     * @return list of supported visual descriptor types
     */
    public static String[] getSupportedVisualDescriptorTypes() {
        return descriptorNames;
    }

    /**
     * Returns the title of this object.
     * Note that <tt>null</tt> is returned if the title was already transformed to {@link #keyWords}.
     * @return the title of this object
     */
    public String getTitle() {
        return titleString;
    }

    /**
     * Returns the coma-separated list of keywords for this object.
     * Note that <tt>null</tt> is returned if the title was already transformed to {@link #keyWords}.
     * @return the keywords of this object
     */
    public String getKeywords() {
        return keywordString;
    }

    /**
     * Returns the rights for this object.
     * @return the rights for this object
     */
    public String getRights() {
        return rights != null && rights != Rights.EMPTY ? rights.toString() : null;
    }

    /**
     * Returns the comma-separated list of territories associated with this object.
     * @return the comma-separated list of territories
     */
    public String getTerritories() {
        if (territories == null || territories.isEmpty())
            return "";
        StringBuilder str = new StringBuilder();
        for (Territory territory : territories) {
            if (str.length() > 0)
                str.append(',');
            str.append(territory);
        }
        return str.toString();
    }

    /**
     * Returns the date that this object was added to the collection.
     * @return the date that this object was added to the collection
     */
    public int getAdded() {
        return added;
    }

    /**
     * Returns the ID of the archive from which this object was added.
     * @return the ID of the archive from which this object was added
     */
    public int getArchiveID() {
        return archiveID;
    }

    /**
     * Returns the coma-separated list of attractiveness values for all existing territories.
     * @return the coma-separated list of attractiveness values for all existing territories
     */
    public String getAttractiveness() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < attractiveness.length; i++) {
            if (i > 0)
                str.append(',');
            str.append(attractiveness[i]);
        }
        return str.toString();
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
        rtv += keyWords.dataHashCode();
        return rtv;
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof MetaObjectProfiSCT))
            return false;
        MetaObjectProfiSCT castObj = (MetaObjectProfiSCT)obj;
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
        if (keyWords != null && castObj.keyWords != null && !keyWords.dataEquals(castObj.keyWords))
            return false;
        return true;
    }


    //***************************  Distance computation  *******************************//

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectProfiSCT castObj = (MetaObjectProfiSCT)obj;

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
        return visualWeights;
    }

    @Override
    public float getMaxDistance() {
        float ret = 0;
        for (int i = 0; i < visualWeights.length; i++)
            ret += visualWeights[i];
        return ret;
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
        MetaObjectProfiSCT rtv = (MetaObjectProfiSCT)super.clone(cloneFilterChain);
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
        MetaObjectProfiSCT rtv = (MetaObjectProfiSCT)super.clone(true);
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
    protected void writeData(OutputStream stream) throws IOException {
        // Write a line for every object from the list
        if (colorLayout != null) {
            colorLayout.writeData(stream);
        } else {
            stream.write('\n');
        }

        if (colorStructure != null) {
            colorStructure.writeData(stream);
        } else {
            stream.write('\n');
        }

        if (edgeHistogram != null) {
            edgeHistogram.writeData(stream);
        } else {
            stream.write('\n');
        }

        if (scalableColor != null) {
            scalableColor.writeData(stream);
        } else {
            stream.write('\n');
        }

        if (regionShape != null) {
            regionShape.writeData(stream);
        } else {
            stream.write('\n');
        }

        if (rights != null && !rights.equals(Rights.EMPTY)) {
            stream.write(getRights().getBytes());
        }
        stream.write('\n');

        if (territories != null) {
            stream.write(getTerritories().getBytes());
        }
        stream.write('\n');

        stream.write(Integer.toString(added).getBytes());
        stream.write('\n');

        stream.write(Integer.toString(archiveID).getBytes());
        stream.write('\n');

        ObjectIntVector.writeIntVector(attractiveness, stream);

        if (titleString != null && keywordString != null) {
            stream.write(titleString.getBytes());
            stream.write('\n');
            stream.write(keywordString.getBytes());
            stream.write('\n');
        } else if (keyWords != null) {
            keyWords.writeData(stream);
        } else {
            stream.write('\n');
            stream.write('\n');
        }
    }


    //************ Column setup for DatabaseStorage ************//

    /** Database column definitions for this object */
    public static final Map<String, ColumnConvertor<MetaObjectProfiSCT>> dbColumns;
    static {
        Map<String, ColumnConvertor<MetaObjectProfiSCT>> map = new LinkedHashMap<String, ColumnConvertor<MetaObjectProfiSCT>>();
        BinarySerializator serializator = new CachingSerializator<MetaObjectProfiSCT>(MetaObjectProfiSCT.class,
                messif.objects.keys.AbstractObjectKey.class,
                ObjectColorLayout.class,
                ObjectShortVectorL1.class,
                ObjectVectorEdgecomp.class,
                ObjectIntVectorL1.class,
                ObjectXMRegionShape.class,
                ObjectIntMultiVectorJaccard.class
        );
        map.put("binobj", new BinarySerializableColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, serializator));
        map.put("locator", DatabaseStorage.getLocatorColumnConvertor(MetaObjectProfiSCT.class));
        map.put("color_layout", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "ColorLayoutType"));
        map.put("color_structure", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "ColorStructureType"));
        map.put("edge_histogram", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "EdgeHistogramType"));
        map.put("scalable_color", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "ScalableColorType"));
        map.put("region_shape", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "RegionShapeType"));
        map.put("keyword_id_multivector", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "KeyWordsType"));
        map.put("title", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("title", MetaObjectProfiSCT.class, false, true));
        map.put("keywords", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("keywords", MetaObjectProfiSCT.class, false, true));
        map.put("rights", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("rights", MetaObjectProfiSCT.class, false, true));
        map.put("added", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("added", MetaObjectProfiSCT.class, false, true));
        map.put("archivID", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("archiveID", MetaObjectProfiSCT.class, false, true));
        map.put("attractiveness", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("attractiveness", MetaObjectProfiSCT.class, false, true));
        map.put("territories", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("territories", MetaObjectProfiSCT.class, false, true));
        dbColumns = Collections.unmodifiableMap(map);
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor loaded from binary input buffer.
     *
     * @param input the buffer to read the MetaObjectPixMacShapeAndColor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    @SuppressWarnings("unchecked")
    protected MetaObjectProfiSCT(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        regionShape = serializator.readObject(input, ObjectXMRegionShape.class);
        rights = serializator.readEnum(input, Rights.class);

        // Read territories
        int territoriesCount = serializator.readInt(input);
        Collection<Territory> territoriesRead = new LinkedList<Territory>();
        while (territoriesCount > 0) {
            territoriesRead.add(serializator.readEnum(input, Territory.class));
            territoriesCount--;
        }
        territories = EnumSet.copyOf(territoriesRead);

        added = serializator.readInt(input);
        archiveID = serializator.readInt(input);
        attractiveness = serializator.readIntArray(input);
        titleString = null; // Title is not serialized
        keywordString = null; // Keywords are not serialized
        keyWords = serializator.readObject(input, ObjectIntMultiVectorJaccard.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, scalableColor);
        size += serializator.write(output, regionShape);
        size += serializator.write(output, rights);

        // Territories
        size += serializator.write(output, territories.size());
        for (Territory territory : territories)
            size += serializator.write(output, territory);

        size += serializator.write(output, added);
        size += serializator.write(output, archiveID);
        size += serializator.write(output, attractiveness);
        size += serializator.write(output, keyWords);
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
        size += serializator.getBinarySize(rights);

        // Territories
        size += serializator.getBinarySize(territories.size());
        size += territories.size() * serializator.getBinarySize(Territory.CZ);

        size += serializator.getBinarySize(added);
        size += serializator.getBinarySize(archiveID);
        size += serializator.getBinarySize(attractiveness);
        size += serializator.getBinarySize(keyWords);
        return size;
    }

}
