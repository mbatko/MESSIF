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
package messif.objects.keys;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * Object key for the faces detected by the Luxand FaceSDK library.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class LuxandFaceKey extends FaceKey {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Luxand FaceSDK Constants ******************//

    /** Facial feature constant LEFT_EYE as defined by FaceSDK */
    private static final int LEFT_EYE = 0;
    /** Facial feature constant RIGHT_EYE as defined by FaceSDK */
    private static final int RIGHT_EYE = 1;
    /** Facial feature constant LEFT_EYE_INNER_CORNER as defined by FaceSDK */
    private static final int LEFT_EYE_INNER_CORNER = 24;
    /** Facial feature constant LEFT_EYE_OUTER_CORNER as defined by FaceSDK */
    private static final int LEFT_EYE_OUTER_CORNER = 23;
    /** Facial feature constant RIGHT_EYE_INNER_CORNER as defined by FaceSDK */
    private static final int RIGHT_EYE_INNER_CORNER = 25;
    /** Facial feature constant RIGHT_EYE_OUTER_CORNER as defined by FaceSDK */
    private static final int RIGHT_EYE_OUTER_CORNER = 26;

    /** Facial feature constant LEFT_EYEBROW_INNER_CORNER as defined by FaceSDK */
    private static final int LEFT_EYEBROW_INNER_CORNER = 17;
    /** Facial feature constant LEFT_EYEBROW_MIDDLE as defined by FaceSDK */
    private static final int LEFT_EYEBROW_MIDDLE = 34;
    /** Facial feature constant LEFT_EYEBROW_OUTER_CORNER as defined by FaceSDK */
    private static final int LEFT_EYEBROW_OUTER_CORNER = 16;

    /** Facial feature constant RIGHT_EYEBROW_INNER_CORNER as defined by FaceSDK */
    private static final int RIGHT_EYEBROW_INNER_CORNER = 18;
    /** Facial feature constant RIGHT_EYEBROW_MIDDLE as defined by FaceSDK */
    private static final int RIGHT_EYEBROW_MIDDLE = 35;
    /** Facial feature constant RIGHT_EYEBROW_OUTER_CORNER as defined by FaceSDK */
    private static final int RIGHT_EYEBROW_OUTER_CORNER = 19;

    /** Facial feature constant NOSE_TIP as defined by FaceSDK */
    private static final int NOSE_TIP = 2;
    /** Facial feature constant NOSE_BRIDGE as defined by FaceSDK */
    private static final int NOSE_BRIDGE = 22;
    /** Facial feature constant NOSE_LEFT_WING as defined by FaceSDK */
    private static final int NOSE_LEFT_WING = 27;
    /** Facial feature constant NOSE_RIGHT_WING as defined by FaceSDK */
    private static final int NOSE_RIGHT_WING = 28;

    /** Facial feature constant MOUTH_RIGHT_CORNER as defined by FaceSDK */
    private static final int MOUTH_RIGHT_CORNER = 3;
    /** Facial feature constant MOUTH_LEFT_CORNER as defined by FaceSDK */
    private static final int MOUTH_LEFT_CORNER = 4;
    /** Facial feature constant MOUTH_TOP as defined by FaceSDK */
    private static final int MOUTH_TOP = 20;
    /** Facial feature constant MOUTH_BOTTOM as defined by FaceSDK */
    private static final int MOUTH_BOTTOM = 21;
    /** Facial feature constant MOUTH_LEFT_TOP as defined by FaceSDK */
    private static final int MOUTH_LEFT_TOP = 36;
    /** Facial feature constant MOUTH_RIGHT_TOP as defined by FaceSDK */
    private static final int MOUTH_RIGHT_TOP = 37;
    /** Facial feature constant MOUTH_LEFT_BOTTOM as defined by FaceSDK */
    private static final int MOUTH_LEFT_BOTTOM = 38;
    /** Facial feature constant MOUTH_RIGHT_BOTTOM as defined by FaceSDK */
    private static final int MOUTH_RIGHT_BOTTOM = 39;

    /** Facial feature constant CHIN_BOTTOM as defined by FaceSDK */
    private static final int CHIN_BOTTOM = 29;
    /** Facial feature constant CHIN_LEFT as defined by FaceSDK */
    private static final int CHIN_LEFT = 14;
    /** Facial feature constant CHIN_RIGHT as defined by FaceSDK */
    private static final int CHIN_RIGHT = 15;

    /** Facial feature constant FACE_CONTOUR1 as defined by FaceSDK */
    private static final int FACE_CONTOUR1 = 12;
    /** Facial feature constant FACE_CONTOUR2 as defined by FaceSDK */
    private static final int FACE_CONTOUR2 = 30;
    /** Facial feature constant FACE_CONTOUR3 as defined by FaceSDK */
    private static final int FACE_CONTOUR3 = 32;
    /** Facial feature constant FACE_CONTOUR4 as defined by FaceSDK */
    private static final int FACE_CONTOUR4 = 10;
    /** Facial feature constant FACE_CONTOUR5 as defined by FaceSDK */
    private static final int FACE_CONTOUR5 = 8;
    /** Facial feature constant FACE_CONTOUR6 as defined by FaceSDK */
    private static final int FACE_CONTOUR6 = 6;
    /** Facial feature constant FACE_CONTOUR7 as defined by FaceSDK */
    private static final int FACE_CONTOUR7 = 5;
    /** Facial feature constant FACE_CONTOUR8 as defined by FaceSDK */
    private static final int FACE_CONTOUR8 = 7;
    /** Facial feature constant FACE_CONTOUR9 as defined by FaceSDK */
    private static final int FACE_CONTOUR9 = 9;
    /** Facial feature constant FACE_CONTOUR10 as defined by FaceSDK */
    private static final int FACE_CONTOUR10 = 11;
    /** Facial feature constant FACE_CONTOUR11 as defined by FaceSDK */
    private static final int FACE_CONTOUR11 = 33;
    /** Facial feature constant FACE_CONTOUR12 as defined by FaceSDK */
    private static final int FACE_CONTOUR12 = 31;
    /** Facial feature constant FACE_CONTOUR13 as defined by FaceSDK */
    private static final int FACE_CONTOUR13 = 13;

    /** List of facial feature constants that represent the face contour as defined by FaceSDK */
    private static final int[] contourFeatures = {
        FACE_CONTOUR1, FACE_CONTOUR2, FACE_CONTOUR3, FACE_CONTOUR4, FACE_CONTOUR5,
        FACE_CONTOUR6, FACE_CONTOUR7, FACE_CONTOUR8, FACE_CONTOUR9, FACE_CONTOUR10,
        FACE_CONTOUR11, FACE_CONTOUR12, FACE_CONTOUR13, CHIN_LEFT, CHIN_BOTTOM, CHIN_RIGHT
    };

    /**
     * Returns the index of the FaceSDK facial feature for the given landmark.
     * @param landmark the face landmark the feature index of which to get
     * @return the index of the FaceSDK facial feature or -1 if unknown
     */
    private static int getLandmarkFeatureIndex(FaceLandmark landmark) {
        switch (landmark) {
            case EYE_LEFT: return LuxandFaceKey.RIGHT_EYE;
            case EYE_LEFT_OUTER_CORNER: return LuxandFaceKey.RIGHT_EYE_OUTER_CORNER;
            case EYE_LEFT_INNER_CORNER: return LuxandFaceKey.RIGHT_EYE_INNER_CORNER;
            case EYEBROW_LEFT_OUTER_CORNER: return LuxandFaceKey.RIGHT_EYEBROW_OUTER_CORNER;
            case EYEBROW_LEFT_INNER_CORNER: return LuxandFaceKey.RIGHT_EYEBROW_INNER_CORNER;
            case EYEBROW_LEFT_MIDDLE: return LuxandFaceKey.RIGHT_EYEBROW_MIDDLE;
            case EYE_RIGHT: return LuxandFaceKey.LEFT_EYE;
            case EYE_RIGHT_INNER_CORNER: return LuxandFaceKey.LEFT_EYE_INNER_CORNER;
            case EYE_RIGHT_OUTER_CORNER: return LuxandFaceKey.LEFT_EYE_OUTER_CORNER;
            case EYEBROW_RIGHT_INNER_CORNER: return LuxandFaceKey.LEFT_EYEBROW_INNER_CORNER;
            case EYEBROW_RIGHT_OUTER_CORNER: return LuxandFaceKey.LEFT_EYEBROW_OUTER_CORNER;
            case EYEBROW_RIGHT_MIDDLE: return LuxandFaceKey.LEFT_EYEBROW_MIDDLE;
            case NOSE_TIP: return LuxandFaceKey.NOSE_TIP;
            case NOSE_BRIDGE: return LuxandFaceKey.NOSE_BRIDGE;
            case NOSE_LEFT_WING: return LuxandFaceKey.NOSE_RIGHT_WING;
            case NOSE_RIGHT_WING: return LuxandFaceKey.NOSE_LEFT_WING;
            case MOUTH_RIGHT_CORNER: return LuxandFaceKey.MOUTH_LEFT_CORNER;
            case MOUTH_LEFT_CORNER: return LuxandFaceKey.MOUTH_RIGHT_CORNER;
            case MOUTH_TOP: return LuxandFaceKey.MOUTH_TOP;
            case MOUTH_BOTTOM: return LuxandFaceKey.MOUTH_BOTTOM;
            case MOUTH_LEFT_TOP: return LuxandFaceKey.MOUTH_RIGHT_TOP;
            case MOUTH_RIGHT_TOP: return LuxandFaceKey.MOUTH_LEFT_TOP;
            case MOUTH_LEFT_BOTTOM: return LuxandFaceKey.MOUTH_RIGHT_BOTTOM;
            case MOUTH_RIGHT_BOTTOM: return LuxandFaceKey.MOUTH_LEFT_BOTTOM;
            case CHIN_LEFT: return LuxandFaceKey.CHIN_RIGHT;
            case CHIN_RIGHT: return LuxandFaceKey.CHIN_LEFT;
            case CHIN_BOTTOM: return LuxandFaceKey.CHIN_BOTTOM;
            default:
                return -1;
        }
    }


    //****************** Attributes ******************//

    /** X-axis position of the face center (in pixels on the original image) */
    private final int centerX;
    /** Y-axis position of the face center (in pixels on the original image) */
    private final int centerY;
    /** Width of the face (in pixels) */
    private final int width;
    /** In-plane face rotation angle (in degrees) */
    private final float angle;
    /** X-axis position of the face features (in pixels on the original image) */
    private final int[] featureXCoordinates;
    /** Y-axis position of the face features (in pixels on the original image) */
    private final int[] featureYCoordinates;


    //****************** Constructors ******************//

    /**
     * Creates a new LuxandFaceKey from the given string parts.
     * The string features parts should contain the following:
     * <ul>
     * <li>coma-separated x and y axis coordinate of the face center,
     *     width of the face, and in-plane rotation of the face;</li>
     * <li>forty coma-separated x and y axis values of the face features;</li>
     * <li>face image locator.</li>
     * </ul>
     * @param stringData the string feature parts
     */
    private LuxandFaceKey(String[] stringData) {
        super(stringData[stringData.length - 1]);
        // Parse position (center x,y and width)
        String[] positionData = stringData[0].split(",");
        this.centerX = Integer.parseInt(positionData[0]);
        this.centerY = Integer.parseInt(positionData[1]);
        this.width = Integer.parseInt(positionData[2]);
        this.angle = Float.parseFloat(positionData[3]);

        // Parse feature coordinates
        featureXCoordinates = new int[stringData.length - 2];
        featureYCoordinates = new int[stringData.length - 2];
        for (int i = 0; i < stringData.length - 2; i++) {
            int comaPos = stringData[i + 1].indexOf(',');
            featureXCoordinates[i] = Integer.parseInt(stringData[i + 1].substring(0, comaPos));
            featureYCoordinates[i] = Integer.parseInt(stringData[i + 1].substring(comaPos + 1));
        }
    }

    /**
     * Creates a new LuxandFaceKey from the given string data.
     * The string data should contain the following semicolon-separated parts:
     * <ul>
     * <li>coma-separated x and y axis coordinate of the face center,
     *     width of the face, and in-plane rotation of the face;</li>
     * <li>forty coma-separated x and y axis values of the face features;</li>
     * <li>face image locator.</li>
     * </ul>
     * @param stringData the data string with the position, features and locator
     */
    public LuxandFaceKey(String stringData) {
        this(stringData.split(";"));
    }


    //****************** Attribute access methods ******************//

    @Override
    public String getSourceImageLocatorURI() {
        String locator = getLocatorURI();
        int dotPos = locator.lastIndexOf('.');
        if (dotPos == -1)
            return locator;
        return locator.substring(0, dotPos - 2) + locator.substring(dotPos);
    }

    @Override
    public String getFaceIdentifierLocatorURI() {
        String currentLocator = getLocatorURI();
        int dotPos = currentLocator.lastIndexOf('.');
        if (dotPos == -1)
            return "";
        return currentLocator.substring(dotPos - 2, dotPos);
    }

    @Override
    public int getCenterX() {
        return centerX;
    }

    @Override
    public int getCenterY() {
        return centerY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return width * 3 / 2;
    }

    @Override
    public float getRollAngle() {
        return angle;
    }

    @Override
    public float getYawAngle() {
        return 0;
    }

    @Override
    public Shape getContour() {
        Polygon polygon = new Polygon();
        for (int i = 0; i < contourFeatures.length; i++)
            polygon.addPoint(featureXCoordinates[contourFeatures[i]], featureYCoordinates[contourFeatures[i]]);
        return polygon;            
    }

    @Override
    public Point2D getLandmarkPosition(FaceLandmark landmark) throws NullPointerException {
        int featureIndex = getLandmarkFeatureIndex(landmark);
        if (featureIndex < 0)
            return null;
        return new Point(featureXCoordinates[featureIndex], featureYCoordinates[featureIndex]);
    }


    //****************** Textual serialization ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        StringBuilder str = new StringBuilder();
        str.append(centerX).append(',').append(centerY).append(',');
        str.append(width).append(',').append(angle).append(';');
        for (int i = 0; i < featureXCoordinates.length; i++) {
            str.append(featureXCoordinates[i]).append(',');
            str.append(featureYCoordinates[i]).append(';');
        }
        stream.write(str.toString().getBytes());
        super.writeData(stream);
    }


    //****************** Binary serialization implementation ******************//

    /**
     * Creates a new instance of FaceKey loaded from binary input.
     *
     * @param input the input to read the AbstractObjectKey from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected LuxandFaceKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        centerX = serializator.readInt(input);
        centerY = serializator.readInt(input);
        width = serializator.readInt(input);
        angle = serializator.readFloat(input);
        featureXCoordinates = serializator.readIntArray(input);
        featureYCoordinates = serializator.readIntArray(input);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) +
                serializator.getBinarySize(centerX) +
                serializator.getBinarySize(centerY) +
                serializator.getBinarySize(width) +
                serializator.getBinarySize(angle) +
                serializator.getBinarySize(featureXCoordinates) +
                serializator.getBinarySize(featureYCoordinates);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
                serializator.write(output, centerX) +
                serializator.write(output, centerY) +
                serializator.write(output, width) +
                serializator.write(output, angle) +
                serializator.write(output, featureXCoordinates) +
                serializator.write(output, featureYCoordinates);
    }
}
