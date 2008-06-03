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
public class MetaObjectSAPIRWeightedDist2 extends MetaObjectSAPIR {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;    
    
    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist2(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI, objects, cloneObjects);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist2(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI, objects);
    }

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist2(BufferedReader stream) throws IOException {
        super(stream);
    }

    /** Factory method that creates MetaObjects from SAPIR XML files */
    public static MetaObjectSAPIRWeightedDist2 create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        return new MetaObjectSAPIRWeightedDist2(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;

        // ScalableColorType
        if (objects[0] != null && castObj.objects[0] != null)
            rtv += ((ObjectColorLayout)objects[0]).getDistanceImpl(castObj.objects[0], distThreshold)*1.5/300.0;
        if (objects[1] != null && castObj.objects[1] != null)
            rtv += ((ObjectShortVectorL1)objects[1]).getDistanceImpl(castObj.objects[1], distThreshold)*2.5/40.0/255.0;
        if (objects[2] != null && castObj.objects[2] != null)
            rtv += ((ObjectVectorEdgecomp)objects[2]).getDistanceImpl(castObj.objects[2], distThreshold)*4.5/68.0;
        if (objects[3] != null && castObj.objects[3] != null)
            rtv += ((ObjectHomogeneousTexture)objects[3]).getDistanceImpl(castObj.objects[3], distThreshold)*0.5/25.0;
        if (objects[4] != null && castObj.objects[4] != null)
            rtv += ((ObjectIntVectorL1)objects[4]).getDistanceImpl(castObj.objects[4], distThreshold)*2.5/3000.0;
        
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
