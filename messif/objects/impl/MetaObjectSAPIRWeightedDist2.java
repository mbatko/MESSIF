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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectSAPIR.XMLHandlerSAPIR;
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

    /** Creates a new instance of MetaObjectSAPIRWeightedDist */
    public MetaObjectSAPIRWeightedDist2(BufferedReader stream, Set<String> restrictedNames) throws IOException {
        super(stream, restrictedNames);
    }

    public MetaObjectSAPIRWeightedDist2(BufferedReader stream, String restrictedName) throws IOException {
        this(stream, Collections.singleton(restrictedName));
    }

    /** Factory method that creates MetaObjects from SAPIR XML files */
    public static MetaObjectSAPIRWeightedDist2 create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        return new MetaObjectSAPIRWeightedDist2(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;
        LocalAbstractObject obj1, obj2;
        
        // ScalableColorType
        if ((obj1 = getObject("ScalableColorType")) != null && (obj2 = castObj.getObject("ScalableColorType")) != null)
            rtv += ((ObjectIntVectorL1)obj1).getDistanceImpl(obj2, distThreshold)*1.5/3000.0;
        if ((obj1 = getObject("ColorStructureType")) != null && (obj2 = castObj.getObject("ColorStructureType")) != null)
            rtv += ((ObjectShortVectorL1)obj1).getDistanceImpl(obj2, distThreshold)*2.5/40.0/255.0;
        if ((obj1 = getObject("ColorLayoutType")) != null && (obj2 = castObj.getObject("ColorLayoutType")) != null)
            rtv += ((ObjectColorLayout)obj1).getDistanceImpl(obj2, distThreshold)*2.5/300.0;
        if ((obj1 = getObject("EdgeHistogramType")) != null && (obj2 = castObj.getObject("EdgeHistogramType")) != null)
            rtv += ((ObjectVectorEdgecomp)obj1).getDistanceImpl(obj2, distThreshold)*4.5/68.0;
        if ((obj1 = getObject("HomogeneousTextureType")) != null && (obj2 = castObj.getObject("HomogeneousTextureType")) != null)
            rtv += ((ObjectHomogeneousTexture)obj1).getDistanceImpl(obj2, distThreshold)*0.5/25.0;
        
        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 1.5f, 2.5f, 2.5f, 4.5f, 0.5f };
    }

    public static String[] getVisualDescriptorNames() {
        return new String[] { "ScalableColorType", "ColorStructureType", "ColorLayoutType", "EdgeHistogramType", "HomogeneousTextureType" };
    }

}
