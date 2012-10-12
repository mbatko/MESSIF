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
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.impl.DatabaseStorage;
import messif.buckets.storage.impl.DatabaseStorage.BinarySerializableColumnConvertor;
import messif.buckets.storage.impl.DatabaseStorage.ColumnConvertor;
import messif.objects.LocalAbstractObject;
import messif.objects.extraction.ExtractorException;
import messif.objects.nio.BinaryInput;
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
    protected static float[] visualWeights = { 4.0f, 6.0f, 7.5f, 7.5f, 3.0f };


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

    public MetaObjectProfiSCTiDIM(BufferedReader stream, boolean haveWords, boolean wordsConverted) throws IOException {
        super(stream, haveWords, wordsConverted);
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
        MetaObjectProfiSCTiDIM castObj = (MetaObjectProfiSCTiDIM)obj;

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


    //****************** Database storage and extraction support ******************//

    /**
     * Utility class that allows to read/store the necessary data of the Profi objects
     * in a database.
     */
    public static class DatabaseSupport extends MetaObjectProfiSCT.DatabaseSupport {
        /** Class id for serialization. */
        private static final long serialVersionUID = 162001L;

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
                        new DatabaseStorage.LocalAbstractObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, true, false, true, true) :
                        new DatabaseStorage.LocalAbstractObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, true, false, stemmer, wordIndex),
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
            map.put("binobj", new BinarySerializableColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, defaultBinarySerializator));
            if (addTextStreamColumn)
                map.put(getTextStreamColumnName(useLinkTable), getTextStreamColumnConvertor(stemmer, wordIndex, useLinkTable));
            map.put("locator", DatabaseStorage.getLocatorColumnConvertor(MetaObjectProfiSCT.class));
            map.put("color_layout", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, "ColorLayoutType"));
            map.put("color_structure", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, "ColorStructureType"));
            map.put("edge_histogram", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, "EdgeHistogramType"));
            map.put("scalable_color", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, "ScalableColorType"));
            map.put("region_shape", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, "RegionShapeType"));
            map.put("rights", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("rights", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("territories", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("territories", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("added", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("added", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("archivID", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("archiveID", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("attractiveness", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("attractiveness", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("title", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("title", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("keywords", new DatabaseStorage.BeanPropertyColumnConvertor<MetaObjectProfiSCT>("keywords", MetaObjectProfiSCTiDIM.class, false, true));
            map.put("keyword_id_multivector", new DatabaseStorage.MetaObjectTextStreamColumnConvertor<MetaObjectProfiSCT>(MetaObjectProfiSCTiDIM.class, "KeyWordsType"));

            return map;
        }

        public DatabaseSupport(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, String tableName, String wordLinkTable, String wordFrequencyTable, String stopwordTable, String[] stopwordCategories, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IllegalArgumentException, SQLException {
            super(dbConnUrl, dbConnInfo, dbDriverClass, tableName, wordLinkTable, wordFrequencyTable, stopwordTable, stopwordCategories, stemmer, wordIndex);
        }

        public DatabaseSupport(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, String tableName, String wordLinkTable, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IllegalArgumentException, SQLException {
            super(dbConnUrl, dbConnInfo, dbDriverClass, tableName, wordLinkTable, stemmer, wordIndex);
        }

        public DatabaseSupport(String dbConnUrl, Properties dbConnInfo, String dbDriverClass, String tableName, Stemmer stemmer, IntStorageIndexed<String> wordIndex) throws IllegalArgumentException, SQLException {
            super(dbConnUrl, dbConnInfo, dbDriverClass, tableName, stemmer, wordIndex);
        }

        


        /**
         * Returns the object with given {@code locator}.
         * The object is retrieved from the database.
         * @param locator the locator of the object to return
         * @param remove if <tt>true</tt>, the object is removed from the database after it is retrieved
         * @param searchWords the search words that will be encapsulated in the keyWords object as the third array
         * @param expander instance for expanding the list of search words
         * @return the created instance of the object
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public MetaObjectProfiSCTiDIM locatorToObject(String locator, boolean remove, String searchWords, WordExpander expander) throws ExtractorException {
            return (MetaObjectProfiSCTiDIM) super.locatorToObject(locator, remove, searchWords, expander);
        }

        /**
         * Returns the object with given {@code locator}.
         * The object is retrieved from the database.
         * @param locator the locator of the object to return
         * @param searchWords the search words that will be encapsulated in the keyWords object as the third array
         * @param expander instance for expanding the list of search words
         * @return the created instance of the object
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public MetaObjectProfiSCTiDIM locatorToObject(String locator, String searchWords, WordExpander expander) throws ExtractorException {
            return locatorToObject(locator, false, searchWords, expander);
        }
        
        /**
         * Returns the object with given {@code locator}.
         * The object is retrieved from the database.
         * @param locator the locator of the object to return
         * @return the created instance of the object
         * @throws ExtractorException if there was a problem retrieving or instantiating the data
         */
        public MetaObjectProfiSCTiDIM locatorToObject(String locator) throws ExtractorException {
            return locatorToObject(locator, null, null);
        }

    }


}
