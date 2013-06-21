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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.buckets.BucketStorageException;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.storage.IntStorageIndexed;
import messif.buckets.storage.IntStorageSearch;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
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
public class MetaObjectPixMacShapeAndColor extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 2L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"ColorLayoutType","ColorStructureType","EdgeHistogramType","ScalableColorType","RegionShapeType","KeyWordsType"};


    /** The list of the names for the encapsulated objects - without the KeyWordsType, because it's not in the text stream by default. */
    protected static final String[] textStreamDescriptorNames = {"ColorLayoutType","ColorStructureType","EdgeHistogramType","ScalableColorType","RegionShapeType"};


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
    protected ObjectRegionShape regionShape;
    /** Object for the KeyWordsType */
    protected ObjectIntSortedVectorJaccard keyWords;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param colorLayout color layout descriptor
     * @param colorStructure  color structure descriptor
     * @param edgeHistogram edge histogram descriptor
     * @param scalableColor scalable color descriptor
     * @param regionShape region shape descriptor
     * @param keyWords key words descriptor
     */
    public MetaObjectPixMacShapeAndColor(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectIntVectorL1 scalableColor, ObjectRegionShape regionShape, ObjectIntSortedVectorJaccard keyWords) {
        super(locatorURI);
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.edgeHistogram = edgeHistogram;
        this.scalableColor = scalableColor;
        this.regionShape = regionShape;
        this.keyWords = keyWords;
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects map of objects with the {@link #descriptorNames} as keys
     * @param cloneObjects if <tt>true</tt>, the {@link Object#clone() cloned} objects from the map will be stored in this metaobject
     * @throws CloneNotSupportedException if there was a problem cloning an object from the map
     */
    public MetaObjectPixMacShapeAndColor(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        this(locatorURI, objects);
        if (cloneObjects) {
            this.colorLayout = (ObjectColorLayout)this.colorLayout.clone(getObjectKey());
            this.colorStructure = (ObjectShortVectorL1)this.colorStructure.clone(getObjectKey());
            this.edgeHistogram = (ObjectVectorEdgecomp)this.edgeHistogram.clone(getObjectKey());
            this.scalableColor = (ObjectIntVectorL1)this.scalableColor.clone(getObjectKey());
            this.regionShape = (ObjectRegionShape)this.regionShape.clone(getObjectKey());
            this.keyWords = (ObjectIntSortedVectorJaccard)this.keyWords.clone(getObjectKey());
        }
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given key and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects map of objects with the {@link #descriptorNames} as keys
     */
    public MetaObjectPixMacShapeAndColor(String locatorURI, Map<String, ? extends LocalAbstractObject> objects) {
        super(locatorURI);
        this.colorLayout = (ObjectColorLayout)objects.get("ColorLayoutType");
        this.colorStructure = (ObjectShortVectorL1)objects.get("ColorStructureType");
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get("EdgeHistogramType");
        this.scalableColor = (ObjectIntVectorL1)objects.get("ScalableColorType");
        this.regionShape = (ObjectRegionShape)objects.get("RegionShapeType");
        this.keyWords = (ObjectIntSortedVectorJaccard)objects.get("KeyWordsType");
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given {@link MetaObject}.
     * The locator and the encapsulated objects from the source {@code object} are
     * taken.
     *
     * @param object the source metaobject from which to get the data
     */
    public MetaObjectPixMacShapeAndColor(MetaObject object) {
        this(object.getLocatorURI(), object.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor from the given {@link MetaObject}
     * and given set of keywords. The locator and the encapsulated objects from the source
     * {@code object} are taken.
     * @param object the source metaobject from which to get the data
     * @param keyWordIndex the index for translating keywords to addresses
     * @param keyWords the keywords to set for the new object
     */
    public MetaObjectPixMacShapeAndColor(MetaObject object, IntStorageIndexed<String> keyWordIndex, String... keyWords) {
        this(object);
        if (keyWords != null)
            this.keyWords = new ObjectIntSortedVectorJaccard(
                    keywordsToIdentifiers(new ArrayList<String>(Arrays.asList(keyWords)), keyWordIndex)
            );
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
     * @throws IOException if there was an error reading the data from the stream
     */
    public MetaObjectPixMacShapeAndColor(BufferedReader stream, boolean haveKeyWords, boolean readKeyWords) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        colorLayout = new ObjectColorLayout(new BufferedReader(new StringReader(line)));
        colorStructure = new ObjectShortVectorL1(stream);
        edgeHistogram = new ObjectVectorEdgecomp(stream);
        scalableColor = new ObjectIntVectorL1(stream);
        regionShape = new ObjectRegionShape(stream);
        if (haveKeyWords) {
            if (readKeyWords)
                keyWords = new ObjectIntSortedVectorJaccard(stream);
            else
                stream.readLine(); // Skip the line with keywords
        }
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor.
     *
     * @param stream stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectPixMacShapeAndColor(BufferedReader stream) throws IOException {
        this(stream, true, false);
    }

    /**
     * Creates a new instance of MetaObjectPixMacShapeAndColor.
     * A keyword index is used to translate keywords to addresses.
     *
     * @param stream stream to read the data from
     * @param keyWordIndex the index for translating keywords to addresses
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectPixMacShapeAndColor(BufferedReader stream, IntStorageIndexed<String> keyWordIndex) throws IOException {
        this(stream, false, false);
        try {
            keyWords = new ObjectIntSortedVectorJaccard(keywordsToIdentifiers(stream.readLine(), ';', keyWordIndex));
        } catch (Exception e) {
            Logger.getLogger(MetaObjectPixMacShapeAndColor.class.getName()).log(Level.WARNING, "Cannot create keywords for object ''{0}'': {1}", new Object[]{getLocatorURI(), e.toString()});
            keyWords = new ObjectIntSortedVectorJaccard(new int[0]);
        }
    }

    /**
     * Returns list of supported visual descriptor types that this object recognizes in XML.
     * @return list of supported visual descriptor types
     */
    public static String[] getSupportedVisualDescriptorTypes() {
        return descriptorNames.clone();
    }

    /**
     * Transforms a line with keywords into array of addresses.
     * Note that unknown keywords are added to the index.
     *
     * @param keyWordsLine the line that contains the keywords
     * @param separator separates the keywords on the {@code keyWordsLine}
     * @param keyWordIndex the index for translating keywords to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     */
    private int[] keywordsToIdentifiers(String keyWordsLine, char separator, IntStorageIndexed<String> keyWordIndex) {
        if (keyWordsLine == null || keyWordsLine.trim().length() == 0)
            return new int[0];

        // Parse all key words from the given string (not using String.split)
        List<String> processedKeyWords = new ArrayList<String>();
        int lastPos = -1;
        int nextPos;
        while ((nextPos = keyWordsLine.indexOf(separator, lastPos + 1)) != -1) {
            processedKeyWords.add(keyWordsLine.substring(lastPos + 1, nextPos).trim().toLowerCase());
            lastPos = nextPos;
        }
        processedKeyWords.add(keyWordsLine.substring(lastPos + 1).trim().toLowerCase());

        return keywordsToIdentifiers(processedKeyWords, keyWordIndex);
    }

    /**
     * Transforms a list of keywords into array of addresses.
     * Note that unknown keywords are added to the index.
     * All items from the list are removed during the process, so
     * do not pass an unmodifiable list!
     *
     * @param processedKeyWords the list of keywords to transform
     * @param keyWordIndex the index for translating keywords to addresses
     * @return array of translated addresses
     * @throws IllegalStateException if there was a problem reading the index
     */
    private int[] keywordsToIdentifiers(List<String> processedKeyWords, IntStorageIndexed<String> keyWordIndex) {
        // Search the index
        int[] ret = new int[processedKeyWords.size()];
        int i;
        IntStorageSearch<String> search = keyWordIndex.search(LocalAbstractObjectOrder.trivialObjectComparator, processedKeyWords);
        for (i = 0; search.next(); i++) {
            processedKeyWords.remove(search.getCurrentObject());
            ret[i] = search.getCurrentObjectIntAddress();
        }
        for (; !processedKeyWords.isEmpty(); i++) {
            String keyWord = processedKeyWords.remove(processedKeyWords.size() - 1);
            try {
                ret[i] = keyWordIndex.store(keyWord).getAddress();
            } catch (BucketStorageException e) {
                Logger.getLogger(MetaObjectPixMacShapeAndColor.class.getName()).log(Level.WARNING, "Cannot insert ''{0}'' for object ''{1}'': {2}", new Object[]{keyWord, getLocatorURI(), e.toString()});
                i--;
            }
        }
        if (i != ret.length) {
            int[] saved = ret;
            ret = new int[i];
            System.arraycopy(saved, 0, ret, 0, i);
        }

        return ret;
    }


    //****************** MetaObject overrides ******************//

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    @Override
    public int getObjectCount() {
        return 5;
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    @Override
    public LocalAbstractObject getObject(String name) {
        if ("ColorLayoutType".equals(name))
            return colorLayout;
        else if ("ColorStructureType".equals(name))
            return colorStructure;
        else if ("EdgeHistogramType".equals(name))
            return edgeHistogram;
        else if ("ScalableColorType".equals(name))
            return scalableColor;
        else if ("RegionShapeType".equals(name))
            return regionShape;
        else if ("KeyWordsType".equals(name))
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
    public ObjectIntSortedVectorJaccard getKeyWords() {
        return keyWords;
    }

    @Override
    public int dataHashCode() {
        int rtv = colorLayout.dataHashCode();
        rtv += colorStructure.dataHashCode();
        rtv += edgeHistogram.dataHashCode();
        rtv += scalableColor.dataHashCode();
        rtv += regionShape.dataHashCode();
        return rtv;
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof MetaObjectPixMacShapeAndColor))
            return false;
        MetaObjectPixMacShapeAndColor castObj = (MetaObjectPixMacShapeAndColor)obj;
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
        return true;
    }


    //***************************  Distance computation  *******************************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectPixMacShapeAndColor castObj = (MetaObjectPixMacShapeAndColor)obj;

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
                metaDistances[3] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)/68.0f;
                rtv += metaDistances[3]*5.0f;
            } else {
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*5.0f/68.0f;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[2] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)/3000.0f;
                rtv += metaDistances[2]*2.0f;
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
        return new float[] { 2.0f, 2.0f, 2.0f, 5.0f, 4.0f};
    }

    @Override
    public float getMaxDistance() {
        return 16f;
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
        MetaObjectPixMacShapeAndColor rtv = (MetaObjectPixMacShapeAndColor)super.clone(cloneFilterChain);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.clone(cloneFilterChain);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.clone(cloneFilterChain);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.clone(cloneFilterChain);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.clone(cloneFilterChain);
        if (regionShape != null)
            rtv.regionShape = (ObjectRegionShape)regionShape.clone(cloneFilterChain);
        if (keyWords != null)
            rtv.keyWords = (ObjectIntSortedVectorJaccard)keyWords.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectPixMacShapeAndColor rtv = (MetaObjectPixMacShapeAndColor)super.clone(true);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.cloneRandomlyModify(args);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.cloneRandomlyModify(args);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.cloneRandomlyModify(args);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.cloneRandomlyModify(args);
        if (regionShape != null)
            rtv.regionShape = (ObjectRegionShape)regionShape.cloneRandomlyModify(args);
        if (keyWords != null)
            rtv.keyWords = (ObjectIntSortedVectorJaccard)keyWords.cloneRandomlyModify(args);
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
    protected MetaObjectPixMacShapeAndColor(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        regionShape = serializator.readObject(input, ObjectRegionShape.class);
        keyWords = serializator.readObject(input, ObjectIntSortedVectorJaccard.class);
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
        return size;
    }
}
