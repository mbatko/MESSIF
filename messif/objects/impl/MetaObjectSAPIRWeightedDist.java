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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author xbatko
 */
public class MetaObjectSAPIRWeightedDist extends MetaObjectSAPIR {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;    
    
    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectHomogeneousTexture homogeneousTexture, ObjectIntVectorL1 scalableColor, ObjectGPSCoordinate location) {
        super(locatorURI, colorLayout, colorStructure, edgeHistogram, homogeneousTexture, scalableColor, location);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist(BufferedReader stream) throws IOException {
        super(stream);
    }

    public MetaObjectSAPIRWeightedDist(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI, objects, cloneObjects);
    }

    public MetaObjectSAPIRWeightedDist(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI, objects);
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the given File */
    public static MetaObjectSAPIRWeightedDist create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(),xmlHandler.getObjects());
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

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed InputSource */
    public static MetaObjectSAPIRWeightedDist create(InputSource is) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(is, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null)
            rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)*2.0/300.0;
        if (colorStructure != null && castObj.colorStructure != null)
            rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)*3.0/40.0/255.0;
        if (edgeHistogram != null && castObj.edgeHistogram != null)
            rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*4.0/68.0;
        if (homogeneousTexture != null && castObj.homogeneousTexture != null)
            rtv += homogeneousTexture.getDistanceImpl(castObj.homogeneousTexture, distThreshold)*0.5/25.0;
        if (scalableColor != null && castObj.scalableColor != null)
            rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)*2.0/3000.0;
        
        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 2.0f, 3.0f, 4.0f, 0.5f, 2.0f };
    }

}
