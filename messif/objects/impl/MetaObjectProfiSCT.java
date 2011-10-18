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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.BucketStorageException;
import messif.buckets.StorageFailureException;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.buckets.storage.impl.DatabaseStorage;
import messif.buckets.storage.impl.DatabaseStorage.BinarySerializableColumnConvertor;
import messif.buckets.storage.impl.DatabaseStorage.ColumnConvertor;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.extraction.Extractor;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.extraction.ExtractorException;
import messif.objects.extraction.Extractors;
import messif.objects.impl.ObjectIntMultiVector.SortedDataIterator;
import messif.objects.impl.ObjectIntMultiVectorJaccard.WeightProvider;
import messif.objects.text.Stemmer;
import messif.objects.text.TextConversion;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.CachingSerializator;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.utility.Convert;
import messif.utility.ExtendedDatabaseConnection;

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

    //****************** Constants ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {
        "ColorLayoutType","ColorStructureType","EdgeHistogramType",
        "ScalableColorType","RegionShapeType","KeyWordsType"
    };

    /** Weights for the visual descriptors */
    protected static float[] visualWeights = { 2.0f, 2.0f, 5.0f, 2.0f, 4.0f };

    /** Regular expression used to split title */
    public static final String TITLE_SPLIT_REGEXP = "[^\\p{javaLowerCase}\\p{javaUpperCase}]+";
    /** Regular expression used to split keywords */
    public static final String KEYWORD_SPLIT_REGEXP = "[^\\p{javaLowerCase}\\p{javaUpperCase}]+";
    /** Regular expression used to split search string */
    public static final String SEARCH_SPLIT_REGEXP = "[^\\p{javaLowerCase}\\p{javaUpperCase}]+";


    //****************** Enumeration classes ******************//

    /** List of rights */
    public static enum Rights {
        /** No right set */
        EMPTY,
        /** RM right */
        RM,
        /** RF right */
        RF;

        /**
         * Convert the given string to Rights enum value.
         * Similar to {@link #valueOf(java.lang.String)} except for the fact
         * that <tt>null</tt> or empty string is converted to {@link #EMPTY}.
         * @param string the string to convert
         * @return a converted enum value
         */
        public static Rights valueOfWithEmpty(String string) {
            return (string == null || string.isEmpty()) ? Rights.EMPTY : valueOf(string);
        }
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
        HR;

        /**
         * Convert a line with comma-separated territories to {@link EnumSet}.
         * @param string the string with territories
         * @return the set of parsed territories
         * @throws IllegalArgumentException if there was a problem converting a string to territory enum value
         */
        public static EnumSet<Territory> stringToTerritories(String string) throws IllegalArgumentException {
            EnumSet<Territory> ret = EnumSet.noneOf(Territory.class);
            if (string != null && !string.isEmpty()) {
                for (String territory : string.toUpperCase().split("\\W+")) {
                    if (territory.isEmpty())
                        continue;
                    ret.add(Territory.valueOf(territory));
                }
            }

            return ret;
        }
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
     * @param keyWords all words descriptor (title words, keyword words, search words)
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
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObjectProfiSCT}.
     * The locator, the attributes and the encapsulated objects from the source {@code object} are
     * taken.
     *
     * @param object the source metaobject from which to get the data
     * @param copyIntKeyWords if <tt>true</tt>, the {@link #keyWords} object is copied
     *          otherwise the keyWords are <tt>null</tt>
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, boolean copyIntKeyWords) {
        this(object.getLocatorURI(), object.colorLayout, object.colorStructure, object.edgeHistogram,
                object.scalableColor, object.regionShape, object.keyWords, object.rights,
                object.territories, object.added, object.archiveID, object.attractiveness);
        this.titleString = object.titleString;
        this.keywordString = object.keywordString;
        if (copyIntKeyWords)
            this.keyWords = object.keyWords;
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObjectProfiSCT}.
     * The locator, the attributes and the encapsulated objects from the source {@code object} are
     * taken. Keyword object is not copied and will be <tt>null</tt>.
     *
     * @param object the source metaobject from which to get the data
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object) {
        this(object, false);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObjectProfiSCT}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @param titleString the title to set for the new object
     * @param keywordString the keywords to set for the new object
     * @param searchString the searched string to set for the new object
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, Stemmer stemmer, IntStorageIndexed<String> wordIndex, String titleString, String keywordString, String searchString) {
        this(object, false);
        if (titleString != null || keywordString != null || searchString != null)
            this.keyWords = convertWordsToIdentifiers(stemmer, wordIndex, titleString, keywordString, searchString);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObjectProfiSCT}.
     * All the encapsulated objects and the locator are taken from the source {@code object}.
     * @param object the source metaobject from which to get the data
     * @param searchWordIds the identifiers of the searched words to set for the new object (in addition to the copied keyword and title words)
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, int[] searchWordIds) {
        this(object, false);
        if (searchWordIds == null)
            searchWordIds = new int[0];
        if (object.keyWords != null && object.keyWords.getVectorDataCount() >= 2)
            this.keyWords = new ObjectIntMultiVectorJaccard(
                object.keyWords.getVectorData(0),
                object.keyWords.getVectorData(1),
                searchWordIds
            );
        else
            this.keyWords = new ObjectIntMultiVectorJaccard(
                new int[0],
                new int[0],
                searchWordIds
            );
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObjectProfiSCT}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @param titleString the title words to set for the new object
     * @param keywordString the keyword words to set for the new object
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, Stemmer stemmer, IntStorageIndexed<String> wordIndex, String titleString, String keywordString) {
        this(object, stemmer, wordIndex, titleString, keywordString, (String)null);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given {@link MetaObjectProfiSCT}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @param searchString the searched string to set for the new object
     */
    public MetaObjectProfiSCT(MetaObjectProfiSCT object, Stemmer stemmer, IntStorageIndexed<String> wordIndex, String searchString) {
        this(object, (searchString == null || searchString.isEmpty()) ? null :
            TextConversion.textToWordIdentifiers(searchString, SEARCH_SPLIT_REGEXP, null, stemmer, wordIndex));
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT from the given text stream.
     * The stream may contain the '#...' lines with object key and/or precomputed distances
     * and a mandatory line for each descriptor name, from which the respective
     * descriptor {@link LocalAbstractObject} is loaded.
     *
     * @param stream the stream from which the data are read
     * @param haveWords flag whether the data contains title and keywords lines
     * @param wordsConverted flag whether to read the title and keyword word lines
     *                          as integer vectors (<tt>true</tt>) or strings (<tt>false</tt>)
     * @throws IOException if there was an error reading the data from the stream
     */
    public MetaObjectProfiSCT(BufferedReader stream, boolean haveWords, boolean wordsConverted) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        colorLayout = new ObjectColorLayout(new BufferedReader(new StringReader(line)));
        colorStructure = new ObjectShortVectorL1(stream);
        edgeHistogram = new ObjectVectorEdgecomp(stream);
        scalableColor = new ObjectIntVectorL1(stream);
        regionShape = new ObjectXMRegionShape(stream);
        rights = Rights.valueOfWithEmpty(stream.readLine());
        territories = Territory.stringToTerritories(stream.readLine());
        line = stream.readLine();
        added = (line == null || line.isEmpty()) ? 0 : Integer.parseInt(line);
        line = stream.readLine();
        archiveID = (line == null || line.isEmpty()) ? 0 : Integer.parseInt(line);
        line = stream.readLine();
        attractiveness = line == null ? null : ObjectIntVector.parseIntVector(line);
        if (haveWords) {
            if (wordsConverted) {
                titleString = null;
                keywordString = null;
                keyWords = new ObjectIntMultiVectorJaccard(stream, 2);
            } else {
                titleString = stream.readLine();
                keywordString = stream.readLine();
                keyWords = null;
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
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @param searchString the additional keywords that will be encapsulated in the keyWords object as the third array
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectProfiSCT(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> wordIndex, String searchString) throws IOException {
        this(stream, true, false);
        // Note that the additional words are added AS THE LAST LINE OF THE OBJECT!
        keyWords = convertWordsToIdentifiers(stemmer, wordIndex, titleString, keywordString, searchString);
    }

    /**
     * Creates a new instance of MetaObjectProfiSCT.
     * A keyword index is used to translate keywords to addresses.
     *
     * @param stream stream to read the data from
     * @param stemmer a {@link Stemmer} for word transformation
     * @param wordIndex the index for translating words to addresses
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectProfiSCT(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IOException {
        this(stream, stemmer, wordIndex, null);
    }


    //****************** Conversion methods ******************//

    /**
     * Convert the given title, key and additional words to a int multi-vector
     * object with Jaccard distance function.
     *
     * @param stemmer the stemmer to use for stemming of the words
     * @param wordIndex the index used to transform the words into integers
     * @param titleString the title string to convert
     * @param keywordString the keywords string to convert
     * @param searchString the search string to convert
     * @return a new instance of int multi-vector object with Jaccard distance function
     */
    private ObjectIntMultiVectorJaccard convertWordsToIdentifiers(Stemmer stemmer, IntStorageIndexed<String> wordIndex, String titleString, String keywordString, String searchString) {
        try {
            int[][] data = new int[searchString != null ? 3 : 2][];
            Set<String> ignoreWords = new HashSet<String>();
            if (searchString != null)
                data[2] = TextConversion.textToWordIdentifiers(searchString, SEARCH_SPLIT_REGEXP, ignoreWords, stemmer, wordIndex);
            data[0] = TextConversion.textToWordIdentifiers(titleString, TITLE_SPLIT_REGEXP, ignoreWords, stemmer, wordIndex);
            data[1] = TextConversion.textToWordIdentifiers(keywordString, KEYWORD_SPLIT_REGEXP, ignoreWords, stemmer, wordIndex);
            return new ObjectIntMultiVectorJaccard(data);
        } catch (Exception e) {
            Logger.getLogger(MetaObjectProfiSCT.class.getName()).log(Level.WARNING, "Cannot create keywords for object ''{0}'': {1}", new Object[]{getLocatorURI(), e.toString()});
            return new ObjectIntMultiVectorJaccard(new int[][] {{},{}}, false);
        }
    }

    /**
     * Implementation of {@link WeightProvider} that has a single weight for every data array of the {@link ObjectIntMultiVector}
     *  and it ignores a specified list of integers (created from a given list of keywords).
     */
    public static class MultiWeightIgnoreProviderProfi extends ObjectIntMultiVectorJaccard.MultiWeightIgnoreProvider {

        /** Class id for serialization. */
        private static final long serialVersionUID = 51201L;

        /**
         * Creates a new instance of MultiWeightProvider with the the given array of weights.
         * @param weights the weights for the data arrays
         * @param ignoreWeight weight used for the {@code ignoredKeywords}
         * @param ignoredKeywords array of keywords to be ignored (before stemming and other corrections)
         * @param stemmer a {@link Stemmer} for word transformation
         * @param keyWordIndex typically database storage to convert keywords to IDs and other parameters
         */
        public MultiWeightIgnoreProviderProfi(float[] weights, float ignoreWeight, String[] ignoredKeywords, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex) {
            super(weights, ignoreWeight, getIgnoredIDs(ignoredKeywords, stemmer, keyWordIndex));
        }

        /**
         * Internal method to create a set of integer IDs for specified keywords given a PixMac keyword -> ID index.
         * @param ignoredKeywords array of keywords to be ignored (before stemming and other corrections)
         * @param stemmer a {@link Stemmer} for word transformation
         * @param keyWordIndex typically database storage to convert keywords to IDs and other parameters
         * @return
         */
        private static Set<Integer> getIgnoredIDs(String[] ignoredKeywords, Stemmer stemmer, IntStorageIndexed<String> keyWordIndex) {
            HashSet<Integer> retVal = new HashSet<Integer>();
            int[] keywordsToIdentifiers = TextConversion.wordsToIdentifiers(ignoredKeywords, new HashSet<String>(), stemmer, keyWordIndex, true);
            Logger.getLogger(MetaObjectProfiSCT.class.getName()).log(Level.INFO, "the following words will be ignored: ''{0}'' with the following IDs: {1}", new Object[]{Arrays.deepToString(ignoredKeywords), Arrays.toString(keywordsToIdentifiers)});
            for (int id : keywordsToIdentifiers) {
                retVal.add(id);
            }
            return retVal;
        }
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns list of supported visual descriptor types that this object recognizes.
     * @return list of supported visual descriptor types
     */
    public static String[] getSupportedVisualDescriptorTypes() {
        return descriptorNames.clone();
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
     * Returns the title words of this object.
     * Note that if the title is already transformed to {@link #keyWords},
     * the {@code wordIndex} is used to transform it back.
     * @param wordIndex the index used to transform the integers to words
     * @return the title words of this object
     * @throws IllegalArgumentException if there was an error reading a word with a given identifier from the index
     */
    public String[] getTitleWords(IntStorageIndexed<String> wordIndex) throws IllegalArgumentException {
        if (titleString != null)
            return titleString.split(TITLE_SPLIT_REGEXP);
        else if (keyWords != null && wordIndex != null)
            return TextConversion.identifiersToWords(wordIndex, keyWords.getVectorData(0));
        else
            return null;
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
     * Returns the key words of this object.
     * Note that if the key words are already transformed to {@link #keyWords},
     * the {@code wordIndex} is used to transform them back.
     * @param wordIndex the index used to transform the integers to words
     * @return the key words of this object
     * @throws IllegalArgumentException if there was an error reading a word with a given identifier from the index
     */
    public String[] getKeywordWords(IntStorageIndexed<String> wordIndex) throws IllegalArgumentException {
        if (keywordString != null)
            return keywordString.split(KEYWORD_SPLIT_REGEXP);
        else if (keyWords != null && wordIndex != null)
            return TextConversion.identifiersToWords(wordIndex, keyWords.getVectorData(1));
        else
            return null;
    }

    /**
     * Returns the search words of this object.
     * @param wordIndex the index used to transform the integers to words
     * @return the search words of this object
     * @throws IllegalArgumentException if there was an error reading a word with a given identifier from the index
     */
    public String[] getSearchWords(IntStorageIndexed<String> wordIndex) throws IllegalArgumentException {
        if (keyWords != null && keyWords.getVectorDataCount() == 3 && wordIndex != null)
            return TextConversion.identifiersToWords(wordIndex, keyWords.getVectorData(2));
        else
            return null;
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
     * Returns whether this object contains the given territory.
     * @param territory the territory to check
     * @return <tt>true</tt>, if this object contains the given territory or <tt>false</tt>, if it does not
     */
    public boolean containsTerritory(Territory territory) {
        return territories != null && territories.contains(territory);
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
        if (attractiveness == null)
            return "";
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
        if (getLocatorURI() != null && castObj.getLocatorURI() != null && !getLocatorURI().equals(castObj.getLocatorURI()))
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
    @Override
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

        if (attractiveness == null)
            stream.write('\n');
        else
            ObjectIntVector.writeIntVector(attractiveness, stream, ',', '\n');

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

    //****************** Database storage and extraction support ******************//

    /**
     * Utility class that allows to read/store the necessary data of the Profi objects
     * in a database.
     */
    public static class DatabaseSupport extends ExtendedDatabaseConnection {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        //****************** Database column definition ******************//

        /**
         * Returns the database column name for creating the {@link MetaObjectProfiSCT} object
         * from the text stream.
         * @param useLinkTable flag whether to use the title and keyword link tables
         * @return the database column name
         */
        public static String getTextStreamColumnName(boolean useLinkTable) {
            return useLinkTable ?
                    "cast(concat_ws('', color_layout,'\n', color_structure,'\n', edge_histogram,'\n', scalable_color,'\n', region_shape,'\n', rights,'\n', territories,'\n', added,'\n', archivID,'\n', attractiveness,'\n', f_profimedia_title_ids(id),'\n', f_profimedia_keyword_ids(id),'\n') as char)":
                    "cast(concat_ws('', color_layout,'\n', color_structure,'\n', edge_histogram,'\n', scalable_color,'\n', region_shape,'\n', rights,'\n', territories,'\n', added,'\n', archivID,'\n', attractiveness,'\n', title,'\n', keywords,'\n') as char)";
        }

        /**
         * Returns the database column convertor for creating the {@link MetaObjectProfiSCT} object
         * from the text stream.
         * @param stemmer an instance that provides a {@link Stemmer} for word transformation
         * @param wordIndex the index for translating words to addresses
         * @param useLinkTable flag whether to use the title and keyword link tables
         * @return the database column convertor
         */
        public static ColumnConvertor<MetaObjectProfiSCT> getTextStreamColumnConvertor(Stemmer stemmer, IntStorageIndexed<String> wordIndex, boolean useLinkTable) {
            return DatabaseStorage.wrapConvertor(
                    useLinkTable ?
                        new DatabaseStorage.LocalAbstractObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, true, false, true, true) :
                        new DatabaseStorage.LocalAbstractObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, true, false, stemmer, wordIndex),
                    true, false, true
            );
        }

        /**
         * Returns the database column definitions for the {@link MetaObjectProfiSCT} object.
         * @param addTextStreamColumn flag whether to add the metaobject stream column to the resulting map
         * @param stemmer an instance that provides a {@link Stemmer} for word transformation
         * @param wordIndex the index for translating words to addresses
         * @param useLinkTable flag whether to use the title and keyword link tables
         * @return the database column definitions for the {@link MetaObjectProfiSCT} object
         */
        public static Map<String, ColumnConvertor<MetaObjectProfiSCT>> getDBColumnMap(boolean addTextStreamColumn, Stemmer stemmer, IntStorageIndexed<String> wordIndex, boolean useLinkTable) {
            Map<String, ColumnConvertor<MetaObjectProfiSCT>> map = new LinkedHashMap<String, ColumnConvertor<MetaObjectProfiSCT>>();
            // id -- primary key
            // thumbfile -- location of the thumbnail image file
            map.put("binobj", new BinarySerializableColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, defaultBinarySerializator));
            if (addTextStreamColumn)
                map.put(getTextStreamColumnName(useLinkTable), getTextStreamColumnConvertor(stemmer, wordIndex, useLinkTable));
            map.put("locator", DatabaseStorage.getLocatorColumnConvertor(MetaObjectProfiSCT.class));
            map.put("color_layout", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "ColorLayoutType"));
            map.put("color_structure", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "ColorStructureType"));
            map.put("edge_histogram", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "EdgeHistogramType"));
            map.put("scalable_color", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "ScalableColorType"));
            map.put("region_shape", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "RegionShapeType"));
            map.put("rights", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("rights", MetaObjectProfiSCT.class, false, true));
            map.put("territories", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("territories", MetaObjectProfiSCT.class, false, true));
            map.put("added", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("added", MetaObjectProfiSCT.class, false, true));
            map.put("archivID", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("archiveID", MetaObjectProfiSCT.class, false, true));
            map.put("attractiveness", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("attractiveness", MetaObjectProfiSCT.class, false, true));
            map.put("title", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("title", MetaObjectProfiSCT.class, false, true));
            map.put("keywords", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("keywords", MetaObjectProfiSCT.class, false, true));
            map.put("keyword_id_multivector", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCT.class, "KeyWordsType"));

            return map;
        }


        //****************** Attributes ******************//

        /** Random number generator for {@link #randomLocators(int)} */
        private final Random randomGenerator;
        /** Instance that provides a {@link Stemmer} for word transformation */
        private final Stemmer stemmer;
        /** Index for translating words to addresses */
        private final IntStorageIndexed<String> wordIndex;
        /** SQL command to retrieve the thumbnail path for the given locator */
        private final String locatorToThumbnailSQL;
        /** SQL command to retrieve the locator for the given id */
        private final String locatorByIdSQL;
        /** SQL command to retrieve the current maximal id */
        private final String maxIdSQL;
        /** SQL command to insert word links */
        private final String insertWordLinkSQL;
        /** SQL command to delete word links */
        private final String deleteObjectWordLinksSQL;
        /** SQL command for retrieving keyword weights */
        private final String keywordWeightSQL;
        /** Database storage used to add objects extracted by image extractor */
        private final DatabaseStorage<MetaObjectProfiSCT> databaseStorage;
        /** List of stopwords */
        private final List<Integer> stopwords;


        /**
         * Creates a new instance of DatabaseSupport.
         * @param dbConnUrl the database connection URL (e.g. "jdbc:mysql://localhost/somedb")
         * @param dbConnInfo additional parameters of the connection (e.g. "user" and "password")
         * @param dbDriverClass class of the database driver to use (can be <tt>null</tt> if the driver is already registered)
         * @param tableName the name of the table in the database
         * @param wordLinkTable the name of the table in the database that the word links are inserted into
         * @param wordFrequencyTable the name of the table in the database that the word frequencies are taken from
         *      (optional, if not set, the wordLinkTable is used)
         * @param stopwordTable the name of the table in the database where the stopwords are kept
         * @param stopwordCategories list of stopword categories to load, all categories are loaded if <tt>null</tt>
         * @param stemmer an instance that provides a {@link Stemmer} for word transformation
         * @param wordIndex the index for translating words to addresses
         * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
         * @throws SQLException if there was a problem connecting to the database
         */
        public DatabaseSupport(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, String tableName, String wordLinkTable, String wordFrequencyTable, String stopwordTable, String[] stopwordCategories, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IllegalArgumentException, SQLException {
            super(dbConnUrl, dbConnInfo, dbDriverClass);
            this.randomGenerator = new Random();
            this.stemmer = stemmer;
            this.wordIndex = wordIndex;
            this.locatorToThumbnailSQL = "select thumbfile from " + tableName + " where locator = ?";
            this.locatorByIdSQL = "select locator from " + tableName + " where id > ? limit 1";
            this.maxIdSQL = "select max(id) from " + tableName;
            this.databaseStorage = new DatabaseStorage<MetaObjectProfiSCT>(
                    MetaObjectProfiSCT.class,
                    dbConnUrl, dbConnInfo, dbDriverClass,
                    tableName, "id", getDBColumnMap(true, stemmer, wordIndex, wordLinkTable != null)
            );
            if (wordLinkTable != null) {
                this.insertWordLinkSQL = "insert into " + wordLinkTable + "(object_id, keyword_id) values (?, ?)";
                this.deleteObjectWordLinksSQL = "delete from " + wordLinkTable + " where object_id = ?";
            } else {
                this.insertWordLinkSQL = null;
                this.deleteObjectWordLinksSQL = null;
            }
            if (wordFrequencyTable != null) {
                this.keywordWeightSQL = "log((select count(*) from " + tableName + ")/keyword_frequency) as idf from " + wordFrequencyTable;
            } else if (wordLinkTable != null) {
                this.keywordWeightSQL = "log((select count(*) from " + tableName + ")/count(*)) as idf from " + wordLinkTable;
            } else {
                this.keywordWeightSQL = null;
            }
            if (stopwordTable != null) {
                stopwords = loadStopWords(stopwordTable, stopwordCategories);
            } else {
                stopwords = null;
            }
        }

        /**
         * Creates a new instance of DatabaseSupport.
         * @param dbConnUrl the database connection URL (e.g. "jdbc:mysql://localhost/somedb")
         * @param dbConnInfo additional parameters of the connection (e.g. "user" and "password")
         * @param dbDriverClass class of the database driver to use (can be <tt>null</tt> if the driver is already registered)
         * @param tableName the name of the table in the database
         * @param wordLinkTable the name of the table in the database that the word links are inserted into
         * @param stemmer an instance that provides a {@link Stemmer} for word transformation
         * @param wordIndex the index for translating words to addresses
         * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
         * @throws SQLException if there was a problem connecting to the database
         */
        public DatabaseSupport(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, String tableName, String wordLinkTable, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IllegalArgumentException, SQLException {
            this(dbConnUrl, dbConnInfo, dbDriverClass, tableName, wordLinkTable, null, null, null, stemmer, wordIndex);
        }

        /**
         * Creates a new instance of DatabaseSupport.
         * @param dbConnUrl the database connection URL (e.g. "jdbc:mysql://localhost/somedb")
         * @param dbConnInfo additional parameters of the connection (e.g. "user" and "password")
         * @param dbDriverClass class of the database driver to use (can be <tt>null</tt> if the driver is already registered)
         * @param tableName the name of the table in the database
         * @param stemmer an instance that provides a {@link Stemmer} for word transformation
         * @param wordIndex the index for translating words to addresses
         * @throws IllegalArgumentException if the connection url is <tt>null</tt> or the driver class cannot be registered
         * @throws SQLException if there was a problem connecting to the database
         */
        public DatabaseSupport(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, String tableName, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IllegalArgumentException, SQLException {
            this(dbConnUrl, dbConnInfo, dbDriverClass, tableName, null, stemmer, wordIndex);
        }

        /**
         * Retrieves the stopwords from the database.
         * @param tableName the table where the stop words are stored
         *      (it should have a keyword_id integer column and category string column)
         * @param categories the list of categories for which to retrieve the stop words
         * @return the stopword list
         * @throws IllegalArgumentException if there was a database problem loading the stopwords
         */
        public final List<Integer> loadStopWords(String tableName, String[] categories) throws IllegalArgumentException {
            // Prepare SQL string
            StringBuilder sql = new StringBuilder("SELECT keyword_id FROM ").append(tableName);
            if (categories != null && categories.length > 0) {
                sql.append(" WHERE category IN ('");
                for (int i = 0; i < categories.length; i++) {
                    if (i > 0)
                        sql.append("','");
                    sql.append(categories[i]);
                }
                sql.append("')");
            }

            // Execute SQL and retrieve the data
            try {
                PreparedStatement objectsCursor = prepareAndExecute(null, sql.toString(), false, (Object[])null);
                ResultSet rs = objectsCursor.getResultSet();
                List<Integer> sw = new ArrayList<Integer>();
                while (rs.next()) {
                    sw.add(rs.getInt(1));
                }
                rs.close();
                objectsCursor.close();
                return sw;
            } catch (SQLException e) {
                throw new IllegalArgumentException("Error reading the data: " + e, e);
            }
        }

        /**
         * Returns the stopwords loaded from database.
         * If the stopword table was not specified in constructor, <tt>null</tt> is returned.
         * @return the list of stopwords
         */
        public Collection<Integer> getStopwords() {
            return Collections.unmodifiableCollection(stopwords);
        }

        /**
         * Returns the current {@link Stemmer} instance.
         * @return the current {@link Stemmer} instance
         */
        public Stemmer getStemmer() {
            return stemmer;
        }

        /**
         * Returns the current word-index instance.
         * @return the current word-index instance
         */
        public IntStorageIndexed<String> getWordIndex() {
            return wordIndex;
        }

        /**
         * Transforms a list of words into array of identifiers.
         * Note that unknown words are added to the index.
         * All items from the list are removed during the process, so
         * do not pass an unmodifiable list!
         *
         * @param words the list of keywords to transform
         * @return array of translated addresses
         * @throws IllegalStateException if there was a problem reading the index
         */
        public int[] wordsToIdentifiers(String[] words) throws IllegalStateException {
            return TextConversion.wordsToIdentifiers(words, null, stemmer, wordIndex, true);
        }

        /**
         * Returns the thumbnail path of the object with the given locator.
         * @param locator the locator of the object for which to get the thumbnail
         * @return the thumbnail path
         * @throws SQLException if there was a problem executing the SQL command
         */
        public String locatorToThumbnail(String locator) throws SQLException {
            return (String)executeSingleValue(locatorToThumbnailSQL, locator);
        }

        /**
         * Returns a list of randomly generated locators from the database.
         * @param count the number of random locators to retrieve
         * @return a list of randomly generated locators
         * @throws SQLException if there was a problem executing the SQL command
         */
        public synchronized List<String> randomLocators(int count) throws SQLException {
            List<String> ret = new ArrayList<String>(count);
            int maxid = ((Number)executeSingleValue(maxIdSQL)).intValue();
            while (ret.size() < count) {
                try {
                    ret.add((String)executeSingleValue(locatorByIdSQL, randomGenerator.nextInt(maxid + 1)));
                } catch (NoSuchElementException ignore) {
                }
            }
            return ret;
        }

        /**
         * Returns the object with given {@code locator}.
         * The object is retrieved from the database.
         * @param locator the locator of the object to return
         * @param searchWords the search words that will be encapsulated in the keyWords object as the third array
         * @return the created instance of the object
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public MetaObjectProfiSCT locatorToObject(String locator, String searchWords) throws ExtractorException {
            return locatorToObject(locator, searchWords, false);
        }
        
        /**
         * Returns the object with given {@code locator}.
         * The object is retrieved from the database.
         * @param locator the locator of the object to return
         * @param searchWords the search words that will be encapsulated in the keyWords object as the third array
         * @param remove if <tt>true</tt>, the object is removed from the database after it is retrieved
         * @return the created instance of the object
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public MetaObjectProfiSCT locatorToObject(String locator, String searchWords, boolean remove) throws ExtractorException {
            Exception causeException = null;
            try {
                IntStorageSearch<MetaObjectProfiSCT> search = databaseStorage.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, locator);
                if (search.next()) {
                    MetaObjectProfiSCT object = search.getCurrentObject();
                    if (remove)
                        search.remove();
                    if (searchWords != null)
                        object = new MetaObjectProfiSCT(object, stemmer, wordIndex, searchWords);
                    return object;
                }
            } catch (Exception e) {
                causeException = e;
            }
            throw new ExtractorException("Cannot read object '" + locator + "' from database", causeException);
        }

        /**
         * Returns a collection of objects with given {@code locators}.
         * The objects are retrieved from the database.
         * @param locators the locators of the objects to return
         * @return the collection of object instances
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public Collection<MetaObjectProfiSCT> locatorsToObject(String[] locators) throws ExtractorException {
            try {
                IntStorageSearch<MetaObjectProfiSCT> search = databaseStorage.search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, Arrays.asList(locators));
                Collection<MetaObjectProfiSCT> ret = new ArrayList<MetaObjectProfiSCT>(locators.length);
                while (search.next())
                    ret.add(search.getCurrentObject());
                return ret;
            } catch (Exception e) {
                throw new ExtractorException("Cannot read objects for " + Arrays.toString(locators) + " from database", e);
            }
        }

        /**
         * Returns the object with given {@code locator}.
         * The object is retrieved from the database.
         * @param locator the locator of the object to return
         * @return the created instance of the object
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public MetaObjectProfiSCT locatorToObject(String locator) throws ExtractorException {
            return locatorToObject(locator, null);
        }

        /**
         * Returns a collection of objects found by the text search.
         * @param object the query object which provides the text
         * @param weights the weigths to use for title, keyword, and search words
         * @param useIdf flag whether to use inverse-document-frequencies of the words (<tt>true</tt>) or a simpler weighted sum search (<tt>false</tt>)
         * @param count the number of objects to retrieve
         * @return an iterator over objects found by the text search
         * @throws SQLException if there was a problem executing the search on the database
         */
        public Collection<RankedAbstractObject> searchByText(MetaObjectProfiSCT object, float[] weights, boolean useIdf, int count) throws SQLException {
            return searchByText(object.getTitleWords(wordIndex), object.getKeywordWords(wordIndex), object.getSearchWords(wordIndex), weights, useIdf, count);
        }

        /**
         * Returns a collection of objects found by the text search.
         * @param text the text to search for
         * @param useIdf flag whether to use inverse-document-frequencies of the words (<tt>true</tt>) or a simpler weighted sum search (<tt>false</tt>)
         * @param count the number of objects to retrieve
         * @return an iterator over objects found by the text search
         * @throws IllegalArgumentException if there was a problem executing the search on the database
         */
        public Collection<RankedAbstractObject> searchByText(String text, boolean useIdf, int count) throws IllegalArgumentException {
            if (text == null || text.isEmpty())
                return null;
            return searchByText(text.split(SEARCH_SPLIT_REGEXP), useIdf, count);
        }

        /**
         * Returns a collection of objects found by the text search.
         * @param searchKeywords the words to search for
         * @param useIdf flag whether to use inverse-document-frequencies of the words (<tt>true</tt>) or a simpler weighted sum search (<tt>false</tt>)
         * @param count the number of objects to retrieve
         * @return an iterator over objects found by the text search
         * @throws IllegalArgumentException if there was a problem executing the search on the database
         */
        public Collection<RankedAbstractObject> searchByText(String[] searchKeywords, boolean useIdf, int count) throws IllegalArgumentException {
            return searchByText(null, null, searchKeywords, new float[]{0f, 0f, 1f}, useIdf, count);
        }

        /**
         * Returns a collection of objects found by the text search.
         * Note that the keywords are automatically {@link TextConversion#unifyWords unified} using
         * the stemmer and also duplicate keywords are removed.
         *
         * @param titleWords the title words to search for
         * @param keywordWords the keyword words to search for
         * @param searchWords the search words to search for
         * @param weights the weights to use for title, keyword, and search words
         * @param useIdf flag whether to use inverse-document-frequencies of the words (<tt>true</tt>) or a simpler weighted sum search (<tt>false</tt>)
         * @param count the number of objects to retrieve
         * @return an iterator over objects found by the text search
         * @throws IllegalArgumentException if there was a problem executing the search on the database
         */
        public Collection<RankedAbstractObject> searchByText(String[] titleWords, String[] keywordWords, String[] searchWords, float[] weights, boolean useIdf, int count) throws IllegalArgumentException {
            Collection<String> processedTitleWords = (titleWords != null ? TextConversion.unifyWords(titleWords, null, stemmer, true) : new ArrayList<String>());
            Collection<String> processedSearchWords = (searchWords != null ? TextConversion.unifyWords(searchWords, null, stemmer, true) : new ArrayList<String>());
            if (processedTitleWords.isEmpty() && processedSearchWords.isEmpty())
                return null;

            StringBuilder str = new StringBuilder();
            String objectTable = "profimedia";
            String keywordsTable = "profimedia_keywords";
            String keywordLinkTable = "profimedia_keyword_links";
            String keywordCountTable = "profimedia_kwcount";
            String frequencyTable = "profimedia_keyword_frequency";

            if (useIdf) {
                /*
                 * UPDATE `profimedia_kwcount` SET `weighted_idf_sum`=(SELECT sum(log( (SELECT count( * ) FROM profimedia ) / keyword_frequency )*CASE WHEN word_source = 'TITLE' THEN 3.0 ELSE 1.0 END )
                 * FROM profimedia_keyword_links keyword_links INNER JOIN profimedia_keyword_frequency freq ON (keyword_links.keyword_id=freq.keyword_id)
                 * WHERE  keyword_links.object_id=profimedia_kwcount.object_id);
                 */
                str.append("select locator, ");
                str.append(getTextStreamColumnName(true));
                str.append(" as metaobject, distance from (SELECT keyword_links.object_id, ( 1 / ( weighted_idf_sum +");
                //subquery for qo idfsum
                str.append("5*ifnull(qo_keyw_idfs, 0) + 3*ifnull(qo_titlew_idfs, 0)");
                //end subquery
                str.append(") ) * (sum( idf * CASE WHEN word_source = 'TITLE' THEN 3.0 ELSE 1.0 END ) + sum( idf * (qo_keyword_weight+qo_titleword_weight) ) ) AS distance ");
                str.append("FROM ( SELECT ").append(keywordsTable).append(".id, CASE WHEN keyword IN ('");
                str.append(Convert.iterableToString(processedTitleWords.iterator(), "','"));
                str.append("') THEN 3 ELSE 0 END AS qo_titleword_weight, CASE WHEN keyword IN ('");
                str.append(Convert.iterableToString(processedSearchWords.iterator(), "','"));
                str.append("') THEN 5 ELSE 0 END AS qo_keyword_weight, log( (SELECT count( * ) FROM ").append(objectTable).append(" ) / keyword_frequency ) AS idf ");
                str.append("FROM ").append(frequencyTable).append(" freq RIGHT OUTER JOIN ").append(keywordsTable).append(" ON ( ").append(keywordsTable).append(".id = freq.keyword_id ) ");
                str.append("WHERE ").append(keywordsTable).append(".keyword IN ( '");
                str.append(Convert.iterableToString(processedTitleWords.iterator(), "','"));
                if (processedTitleWords.size() > 0 && processedSearchWords.size() > 0) {
                    str.append("','");
                }
                str.append(Convert.iterableToString(processedSearchWords.iterator(), "','"));
                str.append("') )keywords INNER JOIN ").append(keywordLinkTable).append(" AS keyword_links ON ( keywords.id = keyword_links.keyword_id ) ");
                str.append("LEFT OUTER JOIN ").append(keywordCountTable).append(" ON ( keyword_links.object_id = ").append(keywordCountTable).append(".object_id ), ");
                str.append("(SELECT sum(log( (SELECT count( * ) FROM ").append(objectTable).append(" ) / keyword_frequency)) as qo_keyw_idfs ");
                str.append("FROM ").append(keywordsTable).append(" INNER JOIN ").append(frequencyTable).append(" ON ( ").append(keywordsTable).append(".id = ").append(frequencyTable).append(".keyword_id ) ");
                str.append("WHERE ").append(keywordsTable).append(".keyword IN ( '");
                str.append(Convert.iterableToString(processedSearchWords.iterator(), "','"));
                str.append("')) A, "
                        + "(SELECT sum(log( (SELECT count( * ) FROM ").append(objectTable).append(" ) / keyword_frequency)) as qo_titlew_idfs ");
                str.append("FROM ").append(keywordsTable).append(" INNER JOIN ").append(frequencyTable).append(" ON ( ").append(keywordsTable).append(".id = ").append(frequencyTable).append(".keyword_id ) ");
                str.append("WHERE ").append(keywordsTable).append(".keyword IN ( '");
                str.append(Convert.iterableToString(processedTitleWords.iterator(), "','"));
                str.append("')) B ");
                str.append("GROUP BY object_id ORDER BY distance DESC LIMIT ");
                str.append(count);
                str.append(") oids inner join ").append(objectTable).append(" on (oids.object_id = ").append(objectTable).append(".id)");
            } else {
                str.append("select locator, ");
                str.append(getTextStreamColumnName(true));
                str.append(" as metaobject, distance from (SELECT keyword_links.object_id, ( 1 / ( 2*kwcount_title + kwcount +5*");
                str.append(processedSearchWords.size());
                str.append(" + 3*");
                str.append(processedTitleWords.size());
                str.append(") ) * (sum( CASE WHEN word_source = 'TITLE' THEN 3.0 ELSE 1.0 END ) + sum( (qo_keyword_weight+qo_titleword_weight) ) ) AS distance ");
                str.append("FROM ( SELECT ").append(keywordsTable).append(".id, CASE WHEN keyword IN ('");
                str.append(Convert.iterableToString(processedTitleWords.iterator(), "','"));
                str.append("') THEN 3 ELSE 0 END AS qo_titleword_weight, CASE WHEN keyword IN ('");
                str.append(Convert.iterableToString(processedSearchWords.iterator(), "','"));
                str.append("') THEN 5 ELSE 0 END AS qo_keyword_weight ");
                str.append("FROM ").append(keywordsTable).append(" ");
                str.append("WHERE ").append(keywordsTable).append(".keyword IN ( '");
                str.append(Convert.iterableToString(processedTitleWords.iterator(), "','"));
                if (processedTitleWords.size() > 0 && processedSearchWords.size() > 0) {
                    str.append("','");
                }
                str.append(Convert.iterableToString(processedSearchWords.iterator(), "','"));
                str.append("') )keywords INNER JOIN ").append(keywordLinkTable).append(" AS keyword_links ON ( keywords.id = keyword_links.keyword_id ) ");
                str.append("LEFT OUTER JOIN ").append(keywordCountTable).append(" ON ( keyword_links.object_id = ").append(keywordCountTable).append(".object_id ) ");
                str.append("GROUP BY object_id ORDER BY distance DESC LIMIT ");
                str.append(count);
                str.append(") oids inner join ").append(objectTable).append(" on (oids.object_id = ").append(objectTable).append(".id)");
            }

            Collection<RankedAbstractObject> ret = new ArrayList<RankedAbstractObject>(count);
            try {
                PreparedStatement objectsCursor = prepareAndExecute(null, str.toString(), false, (Object[])null);
                ColumnConvertor<MetaObjectProfiSCT> textStreamColumnConvertor = getTextStreamColumnConvertor(stemmer, wordIndex, true);
                ResultSet rs = objectsCursor.getResultSet();
                while (rs.next()) {
                    MetaObjectProfiSCT obj = textStreamColumnConvertor.convertFromColumnValue(null, rs.getObject(2));
                    obj.setObjectKey(new AbstractObjectKey(rs.getString(1)));
                    ret.add(new RankedAbstractObject(obj, rs.getFloat(3)));
                }
                objectsCursor.close();
            } catch (BucketStorageException e) {
                throw new IllegalArgumentException("Error reading the data: " + e.getMessage(), e);
            } catch (SQLException e) {
                throw new IllegalArgumentException("Error reading the data: " + e, e);
            }
            return ret;
        }

        /**
         * Computes the weighted Jaccard keyword distance with word-frequency weights.
         * The {@code keyWordWeights} array provides additional weights for the
         * different layers of the keywords (title, etc.).
         *
         * @param o1 the first keyword object to compare
         * @param o2 the first keyword object to compare
         * @param keyWordWeights the weights for different layers of keywords (title, etc.)
         * @return the keywords distance
         * @throws SQLException if there was a problem retrieving keyword frequencies from the database
         */
        private float keywordDistanceTfIdf(ObjectIntMultiVector o1, ObjectIntMultiVector o2, float[] keyWordWeights) throws SQLException {
            return ObjectIntMultiVectorJaccard.getWeightedDistance(o1, getKeywordWeightProvider(o1, keyWordWeights), o2, getKeywordWeightProvider(o2, keyWordWeights));
        }

        /**
         * Returns a collection of ranked objects given by the {@code iterator} with
         * the distances provided by the weighted Jaccard keyword distance
         * with word-frequency weights.
         * An iterator of already ranked objects is provided and a weight for combining the original weight can be provided.
         * @param queryObject the query object with keywords using which the objects in the collection are ranked
         * @param keyWordWeights the weights for different layers of keywords (title, etc.)
         * @param originalRankWeight weight of the original object distance rank (if zero, only the new ranking based on tf-idf is used)
         * @param iterator the iterator that provides the objects to rank
         * @return a collection of ranked objects
         * @throws SQLException if there was a problem retrieving keyword frequencies from the database
         */
        public Collection<RankedAbstractObject> rerankByKeywords(MetaObjectProfiSCT queryObject, float[] keyWordWeights, float originalRankWeight, Iterator<? extends RankedAbstractObject> iterator) throws SQLException {
            RankedSortedCollection ret = new RankedSortedCollection();
            while (iterator.hasNext()) {
                RankedAbstractObject rankedObj = iterator.next();
                float kwdist = keywordDistanceTfIdf(queryObject.getKeyWords(), ((MetaObjectProfiSCT)rankedObj.getObject()).getKeyWords(), keyWordWeights);
                ret.add(rankedObj.clone(kwdist + rankedObj.getDistance() * originalRankWeight));
            }
            return ret;
        }

        /**
         * Returns a collection of ranked objects given by the {@code iterator} with
         * the distances provided by the weighted Jaccard keyword distance
         * with word-frequency weights.
         * @param queryObject the query object with keywords using which the objects in the collection are ranked
         * @param keyWordWeights the weights for different layers of keywords (title, etc.)
         * @param iterator the iterator that provides the objects to rank
         * @return a collection of ranked objects
         * @throws SQLException if there was a problem retrieving keyword frequencies from the database
         */
        public Collection<RankedAbstractObject> rankByKeywords(MetaObjectProfiSCT queryObject, float[] keyWordWeights, Iterator<? extends MetaObjectProfiSCT> iterator) throws SQLException {
            RankedSortedCollection ret = new RankedSortedCollection();
            while (iterator.hasNext()) {
                MetaObjectProfiSCT obj = iterator.next();
                ret.add(new RankedAbstractObject(obj, keywordDistanceTfIdf(queryObject.getKeyWords(), obj.getKeyWords(), keyWordWeights)));
            }
            return ret;
        }

        /**
         * Returns a collection of ranked objects given by {@code iterator} with
         * the distances provided by the weighted Jaccard keyword distance
         * with word-frequency weights.
         * The given reference keywords are provided in different layers (title, etc.)
         * in the respective subarrays. Note that the size of the outer array should
         * not be bigger than the size of the {@code keyWordWeights}.
         *
         * @param referenceKeywords the reference keywords using which the objects in the collection are ranked
         * @param keyWordWeights the weights for different layers of keywords (title, etc.)
         * @param iterator the iterator that provides the objects to rank
         * @return a collection of newly ranked objects
         * @throws SQLException if there was a problem retrieving keyword frequencies from the database
         */
        public Collection<RankedAbstractObject> rankByKeywords(String[][] referenceKeywords, float[] keyWordWeights, Iterator<? extends MetaObjectProfiSCT> iterator) throws SQLException {
            int[][] keywordIds = new int[referenceKeywords.length][];
            for (int i = 0; i < referenceKeywords.length; i++)
                keywordIds[i] = wordsToIdentifiers(referenceKeywords[i]);
            ObjectIntMultiVectorJaccard queryObject = new ObjectIntMultiVectorJaccard(keywordIds);
            RankedSortedCollection ret = new RankedSortedCollection();
            while (iterator.hasNext()) {
                MetaObjectProfiSCT obj = iterator.next();
                ret.add(new RankedAbstractObject(obj, keywordDistanceTfIdf(queryObject, obj.getKeyWords(), keyWordWeights)));
            }
            return ret;
        }

        /**
         * Insert words link for the given object id.
         * This method does nothing if the {@link #insertWordLinkSQL} is <tt>null</tt>.
         * @param objectId the identifier of the object with the keywords
         * @param wordIds the identifiers of the keywords in the object
         * @throws SQLException if there was an error inserting word links
         */
        protected void insertWordLinks(int objectId, ObjectIntMultiVector wordIds) throws SQLException {
            if (insertWordLinkSQL == null)
                return;

            PreparedStatement insertWordLink = getConnection().prepareStatement(insertWordLinkSQL);
            try {
                insertWordLink.setInt(1, objectId);
                for (int kwVectorId = 0; kwVectorId < wordIds.getVectorDataCount(); kwVectorId++) {
                    int[] kwVectorData = wordIds.getVectorData(kwVectorId);
                    for (int i = 0; i < kwVectorData.length; i++) {
                        insertWordLink.setInt(2, kwVectorData[i]);
                        insertWordLink.execute();
                    }
                }
            } finally {
                insertWordLink.close();
            }
        }

        /**
         * Delete all word links for the given object id.
         * This method does nothing if the {@link #deleteObjectWordLinksSQL} is <tt>null</tt>.
         * @param objectId the identifier of the object with the keywords
         * @throws SQLException if there was an error deleting word links
         */
        protected void deleteWordLinks(int objectId) throws SQLException {
            if (deleteObjectWordLinksSQL == null)
                return;

            prepareAndExecute(null, deleteObjectWordLinksSQL, false, objectId).close();
        }

        /**
         * Store the object into the database storage.
         *
         * @param object the object to store
         * @return the identifier of the stored object
         * @throws BucketStorageException if there was an error adding the object to storage
         */
        public int storeObject(MetaObjectProfiSCT object) throws BucketStorageException {
            int id = databaseStorage.store(object).getAddress();
            try {
                insertWordLinks(id, object.getKeyWords());
                return id;
            } catch (SQLException e) {
                throw new StorageFailureException("Cannot insert word links for " + object + ": " + e, e);
            }
        }

        /**
         * Updates the object stored in the database storage.
         *
         * @param objectId the identifier of the object with the keywords
         * @param object the object to store
         * @throws BucketStorageException if there was an error adding the object to storage
         */
        public void updateObject(int objectId, MetaObjectProfiSCT object) throws BucketStorageException {
            databaseStorage.update(objectId, object);
            try {
                deleteWordLinks(objectId);
                insertWordLinks(objectId, object.getKeyWords());
            } catch (SQLException e) {
                throw new StorageFailureException("Cannot update word links for " + object + ": " + e, e);
            }
        }

        /**
         * Remove the object from the database storage.
         *
         * @param objectId the identifier of the object with the keywords
         * @throws BucketStorageException if there was an error adding the object to storage
         */
        public void removeObject(int objectId) throws BucketStorageException {
            try {
                deleteWordLinks(objectId);
            } catch (SQLException e) {
                throw new StorageFailureException("Cannot insert word links for " + objectId + ": " + e, e);
            }
            databaseStorage.remove(objectId);
        }

        /**
         * Returns a search over all objects in the storage.
         * @return a search over all objects in the storage
         */
        public IntStorageSearch<MetaObjectProfiSCT> getAllObjects() {
            return databaseStorage.search();
        }

        /**
         * Returns a search over all objects in the storage.
         * @param fromId identifier of the starting object
         * @param toId identifier of the ending object
         * @return a search over all objects in the storage
         */
        public IntStorageSearch<MetaObjectProfiSCT> getAllObjects(Integer fromId, Integer toId) {
            if (fromId == null || toId == null)
                return getAllObjects();
            return databaseStorage.search(null, fromId, toId);
        }

        /**
         * Creates a new extractor that uses locator parameter of the
         * {@link ExtractorDataSource} to get the respective object from the database.
         * @param locatorParamName the name of the {@link ExtractorDataSource} parameter that contains the locator
         * @param additionalKeyWordsParamName the name of the {@link ExtractorDataSource} parameter that contains the additional keywords
         * @param removeObjects if <tt>true</tt>, the object is removed from the database after it is retrieved
         * @return a new extractor instance
         */
        public Extractor<? extends MetaObjectProfiSCT> createLocatorExtractor(String locatorParamName, String additionalKeyWordsParamName, boolean removeObjects) {
            return new LocatorExtractor(locatorParamName, additionalKeyWordsParamName, removeObjects);
        }

        /**
         * Creates a new extractor that uses external image extractor and additional parameters
         * to create instances of {@link MetaObjectProfiSCT}.
         * @param extractorCommand the external extractor command for extracting binary images
         * @param storeObjects if <tt>true</tt> every object successfully extracted by this extractor
         *          is added to the encapsulated storage via {@link #storeObject(messif.objects.impl.MetaObjectProfiSCT)}
         * @param dataLineParameterNames a list of names of the {@link ExtractorDataSource} parameters that are appended to the extracted descriptors
         * @return a new extractor instance
         */
        public Extractor<? extends MetaObjectProfiSCT> createImageExtractor(String extractorCommand, boolean storeObjects, String[] dataLineParameterNames) {
            return new ImageExtractor(extractorCommand, dataLineParameterNames, storeObjects);
        }

        /**
         * Internal class that provides object extractor that uses locator
         * parameter of the {@link ExtractorDataSource} to get the respective
         * object from the database. Note that additional keywords can be
         * added to the object.
         */
        private class LocatorExtractor implements Extractor<MetaObjectProfiSCT> {
            /** Name of the {@link ExtractorDataSource} parameter that contains the locator */
            private final String locatorParamName;
            /** Name of the {@link ExtractorDataSource} parameter that contains the additional keywords */
            private final String additionalKeyWordsParamName;
            /** Flag whether to remove successfully retrieved objects from database */
            private final boolean removeObjects;

            /**
             * Creates a new instance of LocatorExtractor.
             * @param locatorParamName the name of the {@link ExtractorDataSource} parameter that contains the locator
             * @param additionalKeyWordsParamName the name of the {@link ExtractorDataSource} parameter that contains the additional keywords
             * @param removeObjects if <tt>true</tt>, the object is removed from the database after it is retrieved
             */
            LocatorExtractor(String locatorParamName, String additionalKeyWordsParamName, boolean removeObjects) {
                this.locatorParamName = locatorParamName;
                this.additionalKeyWordsParamName = additionalKeyWordsParamName;
                this.removeObjects = removeObjects;
            }

            @Override
            public MetaObjectProfiSCT extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                return locatorToObject(
                        dataSource.getRequiredParameter(locatorParamName).toString(),
                        dataSource.getParameter(additionalKeyWordsParamName, String.class),
                        removeObjects
                );
            }

            @Override
            public Class<? extends MetaObjectProfiSCT> getExtractedClass() {
                return MetaObjectProfiSCT.class;
            }
        }

        /**
         * Internal class that provides external image extractor and additional parameters
         * to create instances of {@link MetaObjectProfiSCT}.
         */
        private class ImageExtractor implements Extractor<MetaObjectProfiSCT> {
            /** External extractor command */
            private final String extractorCommand;
            /** Names of the {@link ExtractorDataSource} parameters that are appended to the extracted descriptors */
            private final String[] dataLineParameterNames;
            /** Flag whether to store successfully extracted objects to database */
            private final boolean storeObjects;

            /**
             * Creates a new instance of ImageExtractor.
             * @param extractorCommand the external extractor command for extracting binary images
             * @param dataLineParameterNames a list of names of the {@link ExtractorDataSource} parameters that are appended to the extracted descriptors
             * @param storeObjects if <tt>true</tt> every object successfully extracted by this extractor
             *          is added to the encapsulated storage via {@link #storeObject(messif.objects.impl.MetaObjectProfiSCT)}
             */
            ImageExtractor(String extractorCommand, String[] dataLineParameterNames, boolean storeObjects) {
                this.extractorCommand = extractorCommand;
                this.dataLineParameterNames = dataLineParameterNames;
                this.storeObjects = storeObjects;
            }

            @Override
            public MetaObjectProfiSCT extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
                StringBuilder str = new StringBuilder();

                // Read data from the extractor
                if (extractorCommand != null) {
                    str = Convert.readStringData(Extractors.callExternalExtractor(extractorCommand, false, dataSource), str);
                    if (str.length() == 0)
                        throw new IllegalArgumentException("There were no data extracted");
                    if (str.charAt(str.length() - 1) != '\n')
                        str.append('\n');
                }

                // Add data from the parameters
                if (dataLineParameterNames != null)
                    for (int i = 0; i < dataLineParameterNames.length; i++)
                        str.append(dataSource.getParameter(dataLineParameterNames[i], String.class, "")).append('\n');

                // Create the object
                try {
                    MetaObjectProfiSCT obj = new MetaObjectProfiSCT(new BufferedReader(new StringReader(str.toString())), stemmer, wordIndex);

                    // Set object key
                    String key = dataSource.getRequiredParameter("locator").toString();
                    if (key != null)
                        obj.setObjectKey(new AbstractObjectKey(key));

                    // Store object into database
                    if (storeObjects)
                        storeObject(obj);

                    return obj;
                } catch (Exception e) {
                    throw new ExtractorException("Cannot extract the input data: " + e, e);
                }
            }

            @Override
            public Class<? extends MetaObjectProfiSCT> getExtractedClass() {
                return MetaObjectProfiSCT.class;
            }
        }

        /**
         * Creates a map of key-value pairs from a given result set.
         * The keys are integers and they are read from the result set's first column.
         * The values are floats and read from the second column.
         * @param rs the result set to get the key-value pairs from
         * @return a map of key-value pairs
         * @throws SQLException if there was an error reading values from the result set
         */
        private Map<Integer, Float> rsToMap(ResultSet rs) throws SQLException {
            Map<Integer, Float> ret = new HashMap<Integer, Float>();
            while (rs.next())
                ret.put(rs.getInt(1), rs.getFloat(2));
            return ret;
        }

        /**
         * Returns weights for keywords based on tf-idf.
         * @param keywords the keywords to read the weights for
         * @return the weight provider for keywords
         * @throws SQLException if there was an error reading the keyword weights from the database
         */
        public Map<Integer, Float> getKeywordWeights(ObjectIntMultiVector keywords) throws SQLException {
            if (keywordWeightSQL == null)
                throw new SQLException("Keyword links table was not set for this database support");
            if (keywords.getDimensionality() == 0)
                return null;

            // Prepare SQL statement
            StringBuilder sql = new StringBuilder("select keyword_id, ");
            sql.append(keywordWeightSQL).append(" where keyword_id in (");
            for (int i = 0; i < keywords.getVectorDataCount(); i++) {
                int[] vector = keywords.getVectorData(i);
                for (int j = 0; j < vector.length; j++)
                    sql.append(vector[j]).append(",");
            }
            sql.setCharAt(sql.length() -1, ')'); // Replace last coma
            sql.append(" group by keyword_id");

            // Execute SQL and get weight for the keywords
            PreparedStatement crs = prepareAndExecute(null, sql.toString(), false, (Object[])null);
            try {
                return rsToMap(crs.getResultSet());
            } finally {
                crs.close();
            }
        }

        /**
         * Returns the weight provider for keywords based on tf-idf.
         * The idf weights are computed only for the provided keywords.
         * The returned object is serializable and does not require the database access.
         *
         * @param keywords the keywords to read the weights for
         * @param weights the weights for different layers of keywords (title, etc.)
         * @return the weight provider for keywords
         * @throws SQLException if there was an error reading the keyword weights from the database
         */
        public WeightProvider getKeywordWeightProvider(ObjectIntMultiVector keywords, float[] weights) throws SQLException {
            return new KeywordWeightProvider(getKeywordWeights(keywords), weights);
        }

        /**
         * Returns the weight provider for keywords based on tf-idf that uses
         * pre-loaded static keyword weights.
         * Note that {@link #initializeKeywordIdfWeights()} must be used
         * to load the static keyword weights. The returned object is serializable,
         * but it requires that the static keyword weights are initialized on the
         * other side.
         *
         * @param weights the weights for different layers of keywords (title, etc.)
         * @return the weight provider for keywords
         * @throws SQLException if there was an error reading the keyword weights from the database
         */
        public static WeightProvider getStaticKeywordWeightProvider(float[] weights) throws SQLException {
            return new KeywordWeightProvider(null, weights);
        }

        /**
         * Initializes the internal keyword idf weights.
         * @return the map of keyword idf weights
         * @throws SQLException if there was an error reading the keyword weights from the database
         */
        public Map<Integer, Float> initializeKeywordIdfWeights() throws SQLException {
            if (keywordWeightSQL == null)
                throw new SQLException("Keyword links table was not set for this database support");
            PreparedStatement crs = prepareAndExecute(null, "select keyword_id, " + keywordWeightSQL + " group by keyword_id", false, (Object[])null);
            KeywordWeightProvider.staticKeywordWeights = rsToMap(crs.getResultSet());
            crs.close();

            return KeywordWeightProvider.staticKeywordWeights;
        }

        /**
         * Implements a database provider for keyword weights.
         */
        private static class KeywordWeightProvider implements WeightProvider, java.io.Serializable {
            /** Class id for serialization. */
            private static final long serialVersionUID = 1L;
            /** Static weights for the respective keyword IDs - keys is the keyword id and value is its idf weight */
            private static Map<Integer, Float> staticKeywordWeights;
            /** Weights for the respective keyword IDs - keys is the keyword id and value is its idf weight */
            private final Map<Integer, Float> keywordWeights;
            /** Weights for different layers of keywords */
            private final float[] weights;

            /**
             * Creates a new weight provider with the given map of keyword weights.
             * @param keywordWeights the weights for the respective keyword IDs - key is the keyword id and value is its idf weight
             * @param weights the weights for different layers of keywords (title, etc.)
             */
            protected KeywordWeightProvider(Map<Integer, Float> keywordWeights, float[] weights) {
                this.keywordWeights = keywordWeights;
                this.weights = weights;
            }

            /**
             * Returns the weight of the keyword using the tf-idf algorithm.
             * @param keywordId the id of the keyword in the database
             * @param documentKwCount the number of keywords in the respective document
             * @param dataVectorIndex the index into {@link #weights} array
             * @return the weight
             */
            private float getWeight(int keywordId, int documentKwCount, int dataVectorIndex) {
                // Read the keyword idf weight from either local map or the static map
                Float kwWeight;
                if (keywordWeights != null)
                    kwWeight = keywordWeights.get(keywordId);
                else if (staticKeywordWeights != null)
                    kwWeight = staticKeywordWeights.get(keywordId);
                else
                    kwWeight = null;

                if (kwWeight == null)
                    return 0; // Ignore unknown keywords
                if (weights != null)
                    return kwWeight.floatValue() * weights[dataVectorIndex] / documentKwCount;
                else
                    return kwWeight.floatValue() / documentKwCount;
            }

            @Override
            public float getWeight(SortedDataIterator iterator) {
                return getWeight(iterator.currentInt(), iterator.getIteratedObject().getDimensionality(), iterator.getCurrentVectorDataIndex());
            }

            @Override
            public float getWeightSum(ObjectIntMultiVector obj) {
                float sum = 0;
                int documentKwCount = obj.getDimensionality();
                for (int i = 0; i < obj.getVectorDataCount(); i++) {
                    int[] vector = obj.getVectorData(i);
                    for (int j = 0; j < vector.length; j++)
                        sum += getWeight(vector[j], documentKwCount, i);
                }
                return sum;
            }
        }
    }

    /**
     * Object that holds only keywords and measures the distance as the
     * weighted jaccard with weights based on tf-idf algorithm. Note
     * that the other object for the distance must be {@link MetaObjectProfiSCT}.
     */
    public static class MetaObjectProfiSCTKwdist extends MetaObjectProfiSCT {
        /** Class id for serialization. */
        private static final long serialVersionUID = 2L;

        //****************** Attributes ******************//

        /** Weight for combining the keywords distance with the visual descriptors distance */
        private final Float keywordsWeight;
        /** Internal keyword weight provider based on tf-idf */
        private final WeightProvider kwWeightProvider;


        //****************** Constructor ******************//

        /**
         * Creates a new instance of MetaObjectProfiSCTKwdist from the given {@link MetaObjectProfiSCT}.
         * The locator and the encapsulated objects from the source {@code object} are
         * taken.
         *
         * @param object the source metaobject from which to get the data
         * @param keywordsWeight the weight for combining the keywords distance with the visual descriptors distance,
         *          if <tt>null</tt>, only the text distance is used
         * @param keywordLayerWeights the weights for different layers of keywords (title, etc.)
         */
        public MetaObjectProfiSCTKwdist(MetaObjectProfiSCT object, Float keywordsWeight, float[] keywordLayerWeights) {
            super(object, true);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = new DatabaseSupport.KeywordWeightProvider(null, keywordLayerWeights);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCT.
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
        public MetaObjectProfiSCTKwdist(BufferedReader stream, Stemmer stemmer, IntStorageIndexed<String> wordIndex, Float keywordsWeight, float[] keywordLayerWeights) throws IOException {
            super(stream, stemmer, wordIndex);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = new DatabaseSupport.KeywordWeightProvider(null, keywordLayerWeights);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTKwdist from the given text stream.
         * Note that the keywords are expected to be present and already converted to IDs.
         *
         * @param stream the stream from which the data are read
         * @param keywordsWeight the weight for combining the keywords distance with the visual descriptors distance,
         *          if <tt>null</tt>, only the text distance is used
         * @param keywordLayerWeights the weights for different layers of keywords (title, etc.)
         * @throws IOException if there was an error reading the data from the stream
         */
        public MetaObjectProfiSCTKwdist(BufferedReader stream, Float keywordsWeight, float[] keywordLayerWeights) throws IOException {
            super(stream, true, true);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = new DatabaseSupport.KeywordWeightProvider(null, keywordLayerWeights);
        }

        /**
         * Creates a new instance of MetaObjectProfiSCTKwdist from the given text stream.
         * Note that the keywords are expected to be present and already converted to IDs.
         * Null keyword weight is applied, i.e. the distance is computed by text only.
         *
         * @param stream the stream from which the data are read
         * @throws IOException if there was an error reading the data from the stream
         */
        public MetaObjectProfiSCTKwdist(BufferedReader stream) throws IOException {
            this(stream, null, null);
        }


        //****************** Distance function ******************//

        @Override
        protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
            try {
                float distance = keyWords.getWeightedDistance(((MetaObjectProfiSCT)obj).keyWords, kwWeightProvider, kwWeightProvider);
                if (keywordsWeight != null)
                    distance = super.getDistanceImpl(obj, metaDistances, distThreshold) + keywordsWeight * distance;
                return distance;
            } catch (RuntimeException e) {
                throw new IllegalStateException("Error computing distance between " + this + " and " + obj + ": " + e, e);
            }
        }
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
        if (territoriesCount == -1) {
            territories = null;
        } else if (territoriesCount == 0) {
            territories = EnumSet.noneOf(Territory.class);
        } else {
            Collection<Territory> territoriesRead = new LinkedList<Territory>();
            while (territoriesCount > 0) {
                territoriesRead.add(serializator.readEnum(input, Territory.class));
                territoriesCount--;
            }
            territories = EnumSet.copyOf(territoriesRead);
        }

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
        if (territories == null) {
            size += serializator.write(output, -1);
        } else {
            size += serializator.write(output, territories.size());
            for (Territory territory : territories)
                size += serializator.write(output, territory);
        }

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
        if (territories == null) {
            size += serializator.getBinarySize(-1);
        } else {
            size += serializator.getBinarySize(territories.size());
            size += territories.size() * serializator.getBinarySize(Territory.CZ);
        }

        size += serializator.getBinarySize(added);
        size += serializator.getBinarySize(archiveID);
        size += serializator.getBinarySize(attractiveness);
        size += serializator.getBinarySize(keyWords);
        return size;
    }

    /** Binary serializator for this object with default caching classes */
    public static final BinarySerializator defaultBinarySerializator = new CachingSerializator<MetaObjectProfiSCT>(MetaObjectProfiSCT.class,
                messif.objects.keys.AbstractObjectKey.class,
                ObjectColorLayout.class,
                ObjectShortVectorL1.class,
                ObjectVectorEdgecomp.class,
                ObjectIntVectorL1.class,
                ObjectXMRegionShape.class,
                ObjectIntMultiVectorJaccard.class
        );

}
