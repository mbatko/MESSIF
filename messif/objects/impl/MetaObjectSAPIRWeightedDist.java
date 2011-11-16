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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectSAPIR.XMLHandlerSAPIR;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectSAPIR castObj = (MetaObjectSAPIR)obj;
        
        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null) {
            if (metaDistances != null) {
                metaDistances[0] = colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)/300.0f;
                rtv += metaDistances[0]*2.0;
            } else {
                rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)*2.0/300.0f;
            }
        }

        if (colorStructure != null && castObj.colorStructure != null) {
            if (metaDistances != null) {
                metaDistances[1] = colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)/40.0f/255.0f;
                rtv += metaDistances[1]*3.0;
            } else {
                rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)*3.0/40.0/255.0;
            }
        }

        if (edgeHistogram != null && castObj.edgeHistogram != null) {
            if (metaDistances != null) {
                metaDistances[2] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)/68.0f;
                rtv += metaDistances[2]*4.0;
            } else {            
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*4.0/68.0;
            }
        }

        if (homogeneousTexture != null && castObj.homogeneousTexture != null) {
            if (metaDistances != null) {
                metaDistances[3] = homogeneousTexture.getDistanceImpl(castObj.homogeneousTexture, distThreshold)/25.0f;
                rtv += metaDistances[3]*0.5;
            } else {
                rtv += homogeneousTexture.getDistanceImpl(castObj.homogeneousTexture, distThreshold)*0.5/25.0;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[4] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)/3000.0f;
                rtv += metaDistances[4]*2.0;
            } else {
                rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)*2.0/3000.0;
            }
        }
        
        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 2.0f, 3.0f, 4.0f, 0.5f, 2.0f, 0.0f };
    }

    @Override
    public float getMaxDistance() {
        return 16f;
    }

    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectSAPIRWeightedDist loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObjectSAPIRWeightedDist from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectSAPIRWeightedDist(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}
