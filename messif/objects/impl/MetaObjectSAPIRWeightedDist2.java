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
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectSAPIR.XMLHandlerSAPIR;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinarySerializator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author xbatko
 */
public class MetaObjectSAPIRWeightedDist2 extends MetaObjectSAPIR {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;    
    
    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist2(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectHomogeneousTexture homogeneousTexture, ObjectIntVectorL1 scalableColor, ObjectGPSCoordinate location) {
        super(locatorURI, colorLayout, colorStructure, edgeHistogram, homogeneousTexture, scalableColor, location);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist2(BufferedReader stream) throws IOException {
        super(stream);
    }

    public MetaObjectSAPIRWeightedDist2(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI, objects, cloneObjects);
    }

    public MetaObjectSAPIRWeightedDist2(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI, objects);
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the given File */
    public static MetaObjectSAPIRWeightedDist2 create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        return new MetaObjectSAPIRWeightedDist2(xmlHandler.getLocatorURI(),xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed URI */
    public static MetaObjectSAPIRWeightedDist2 create(String uri) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(uri, xmlHandler);
        return new MetaObjectSAPIRWeightedDist2(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed InputStream */
    public static MetaObjectSAPIRWeightedDist2 create(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(is, xmlHandler);
        return new MetaObjectSAPIRWeightedDist2(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed InputSource */
    public static MetaObjectSAPIRWeightedDist2 create(InputSource is) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(is, xmlHandler);
        return new MetaObjectSAPIRWeightedDist2(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null)
            rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)*1.5/300.0;
        if (colorStructure != null && castObj.colorStructure != null)
            rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)*2.5/40.0/255.0;
        if (edgeHistogram != null && castObj.edgeHistogram != null)
            rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*4.5/68.0;
        if (homogeneousTexture != null && castObj.homogeneousTexture != null)
            rtv += homogeneousTexture.getDistanceImpl(castObj.homogeneousTexture, distThreshold)*0.5/25.0;
        if (scalableColor != null && castObj.scalableColor != null)
            rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)*2.5/3000.0;
        
        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 1.5f, 2.5f, 4.5f, 0.5f, 2.5f };
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectSAPIRWeightedDist2 loaded from binary input stream.
     * 
     * @param input the stream to read the MetaObjectSAPIRWeightedDist2 from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected MetaObjectSAPIRWeightedDist2(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


}
