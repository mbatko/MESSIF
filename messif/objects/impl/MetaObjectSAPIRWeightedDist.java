/*
 * MetaObjectSAPIRWeightedDist.java
 *
 * Created on 2. kveten 2007, 18:08
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectSAPIR.XMLHandlerSAPIR;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinarySerializator;
import org.xml.sax.SAXException;

/**
 *
 * @author xbatko
 */
public class MetaObjectSAPIRWeightedDist extends MetaObjectSAPIR {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;    
    
    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI, objects, cloneObjects);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI, objects);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist(BufferedReader stream) throws IOException {
        super(stream);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist(BufferedReader stream, Set<String> restrictedNames) throws IOException {
        super(stream, restrictedNames);
    }

    public MetaObjectSAPIRWeightedDist(BufferedReader stream, String restrictedName) throws IOException {
        this(stream, Collections.singleton(restrictedName));
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the given File */
    public static MetaObjectSAPIRWeightedDist create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed URI */
    public static MetaObjectSAPIRWeightedDist create(String uri) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(uri, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed InputStream */
    public static MetaObjectSAPIRWeightedDist create(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(is, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;
        LocalAbstractObject obj1, obj2;
        
        // ScalableColorType
        if ((obj1 = getObject("ScalableColorType")) != null && (obj2 = castObj.getObject("ScalableColorType")) != null)
            rtv += ((ObjectIntVectorL1)obj1).getDistanceImpl(obj2, distThreshold)*2.0/3000.0;
        if ((obj1 = getObject("ColorStructureType")) != null && (obj2 = castObj.getObject("ColorStructureType")) != null)
            rtv += ((ObjectShortVectorL1)obj1).getDistanceImpl(obj2, distThreshold)*3.0/40.0/255.0;
        if ((obj1 = getObject("ColorLayoutType")) != null && (obj2 = castObj.getObject("ColorLayoutType")) != null)
            rtv += ((ObjectColorLayout)obj1).getDistanceImpl(obj2, distThreshold)*2.0/300.0;
        if ((obj1 = getObject("EdgeHistogramType")) != null && (obj2 = castObj.getObject("EdgeHistogramType")) != null)
            rtv += ((ObjectVectorEdgecomp)obj1).getDistanceImpl(obj2, distThreshold)*4.0/68.0;
        if ((obj1 = getObject("HomogeneousTextureType")) != null && (obj2 = castObj.getObject("HomogeneousTextureType")) != null)
            rtv += ((ObjectHomogeneousTexture)obj1).getDistanceImpl(obj2, distThreshold)*0.5/25.0;
        
        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 2.0f, 3.0f, 2.0f, 4.0f, 0.5f };
    }

    public static String[] getVisualDescriptorNames() {
        return new String[] { "ScalableColorType", "ColorStructureType", "ColorLayoutType", "EdgeHistogramType", "HomogeneousTextureType" };
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectSAPIRWeightedDist loaded from binary input stream.
     * 
     * @param input the stream to read the MetaObjectSAPIRWeightedDist from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected MetaObjectSAPIRWeightedDist(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}
