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

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;

        // ScalableColorType
        if (objects[0] != null && castObj.objects[0] != null)
            rtv += ((ObjectColorLayout)objects[0]).getDistanceImpl(castObj.objects[0], distThreshold)*2.0/300.0;
        if (objects[1] != null && castObj.objects[1] != null)
            rtv += ((ObjectShortVectorL1)objects[1]).getDistanceImpl(castObj.objects[1], distThreshold)*3.0/40.0/255.0;
        if (objects[2] != null && castObj.objects[2] != null)
            rtv += ((ObjectVectorEdgecomp)objects[2]).getDistanceImpl(castObj.objects[2], distThreshold)*4.0/68.0;
        if (objects[3] != null && castObj.objects[3] != null)
            rtv += ((ObjectHomogeneousTexture)objects[3]).getDistanceImpl(castObj.objects[3], distThreshold)*0.5/25.0;
        if (objects[4] != null && castObj.objects[4] != null)
            rtv += ((ObjectIntVectorL1)objects[4]).getDistanceImpl(castObj.objects[4], distThreshold)*2.0/3000.0;
        
        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 2.0f, 3.0f, 4.0f, 0.5f, 2.0f };
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
