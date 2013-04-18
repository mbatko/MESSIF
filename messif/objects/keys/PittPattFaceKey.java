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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * Object key for the faces detected by the PittPatt library.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class PittPattFaceKey extends FaceKey {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** X-axis position of the face center (in pixels on the original image) */
    private final int centerX;
    /** Y-axis position of the face center (in pixels on the original image) */
    private final int centerY;
    /** Width of the face (in pixels) */
    private final int width;
    /** Height of the face (in pixels) */
    private final int height;
    /** In-plane face rotation angle (in degrees) */
    private final float rollAngle;
    /** Left-right face rotation angle (in degrees) */
    private final float yawAngle;
    /** Table of landmark positions */
    private final Map<FaceLandmark, Point2D> landmarkPoints;


    //****************** Constructors ******************//

    /**
     * Creates a new LuxandFaceKey from the given string parts.
     * The string features parts should contain the following:
     * <ul>
     * <li>coma-separated x and y axis coordinate of the face center,
     *     width and height of the face, and in-plane and left-right rotation of the face;</li>
     * <li>nine coma-separated x and y axis values of the face features;</li>
     * <li>face image locator.</li>
     * </ul>
     * @param stringData the string feature parts
     */
    private PittPattFaceKey(String[] stringData) {
        super(stringData[stringData.length - 1]);
        // Parse position (center x,center y,width,height,roll,yaw)
        String[] positionData = stringData[0].split(",");
        this.centerX = Integer.parseInt(positionData[0]);
        this.centerY = Integer.parseInt(positionData[1]);
        this.width = Integer.parseInt(positionData[2]);
        this.height = Integer.parseInt(positionData[3]);
        this.rollAngle = Float.parseFloat(positionData[4]);
        this.yawAngle = Float.parseFloat(positionData[5]);

        this.landmarkPoints = new EnumMap<FaceLandmark, Point2D>(FaceLandmark.class);
        for (int i = 1; i < stringData.length - 1; i++) {
            String[] landmarkData = stringData[i].split(" ");
            this.landmarkPoints.put(labelTolandmark(landmarkData[0]), new Point(Integer.parseInt(landmarkData[1]), Integer.parseInt(landmarkData[2])));
        }
    }

    /**
     * Creates a new PittPattFaceKey from the given string data.
     * The string data should contain the following semicolon-separated parts:
     * <ul>
     * <li>coma-separated x and y axis coordinate of the face center,
     *     width and height of the face, and in-plane and left-right rotation of the face;</li>
     * <li>nine coma-separated x and y axis values of the face features;</li>
     * <li>face image locator.</li>
     * </ul>
     * @param stringData the data string with the position, features and locator
     */
    public PittPattFaceKey(String stringData) {
        this(stringData.split(";"));
    }


    //****************** Landmark conversion functions ******************//

    /**
     * Returns the face landmark based on the given label.
     * @param label the landmark label to transform
     * @return the face landmark
     */
    private static FaceLandmark labelTolandmark(String label) {
        if (label.equals("el"))
            return FaceLandmark.EYE_LEFT;
        else if (label.equals("er"))
            return FaceLandmark.EYE_RIGHT;
        else if (label.equals("nt"))
            return FaceLandmark.NOSE_TIP;
        else if (label.equals("nb"))
            return FaceLandmark.NOSE_BRIDGE;
        else if (label.equals("ne"))
            return FaceLandmark.NOSE_EYE;
        else if (label.equals("clt"))
            return FaceLandmark.CHEEK_LEFT_TOP;
        else if (label.equals("clb"))
            return FaceLandmark.CHEEK_LEFT_BOTTOM;
        else if (label.equals("crt"))
            return FaceLandmark.CHEEK_RIGHT_TOP;
        else if (label.equals("crb"))
            return FaceLandmark.CHEEK_RIGHT_BOTTOM;
        else
            throw new IllegalArgumentException("Unknown landmark label: " + label);
    }

    /**
     * Returns the face landmark label.
     * @param landmark the face landmark for which to get the label
     * @return the face landmark label
     */
    private static String landmarkToLabel(FaceLandmark landmark) {
        switch (landmark) {
            case EYE_LEFT: return "el";
            case EYE_RIGHT: return "er";
            case NOSE_TIP: return "nt";
            case NOSE_BRIDGE: return "nb";
            case NOSE_EYE: return "ne";
            case CHEEK_LEFT_TOP: return "clt";
            case CHEEK_LEFT_BOTTOM: return "clb";
            case CHEEK_RIGHT_TOP: return "crt";
            case CHEEK_RIGHT_BOTTOM: return "crb";
            default:
                return null;
        }
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
        return height;
    }

    @Override
    public float getRollAngle() {
        return rollAngle;
    }

    @Override
    public float getYawAngle() {
        return yawAngle;
    }

    @Override
    public Shape getContour() {
        return new Rectangle(
                centerX - width / 2,
                centerY - height / 2,
                width, height
        );
    }

    @Override
    public Point2D getLandmarkPosition(FaceLandmark landmark) {
        return landmarkPoints.get(landmark);
    }


    //****************** Textual serialization ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        StringBuilder str = new StringBuilder();
        str.append(centerX).append(',').append(centerY).append(',');
        str.append(width).append(',').append(height).append(',');
        str.append(rollAngle).append(',').append(yawAngle).append(';');
        for (Map.Entry<FaceLandmark, Point2D> entry : landmarkPoints.entrySet()) {
            str.append(landmarkToLabel(entry.getKey())).append(' ');
            str.append((int)entry.getValue().getX()).append(' ');
            str.append((int)entry.getValue().getY()).append(';');
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
    protected PittPattFaceKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        centerX = serializator.readInt(input);
        centerY = serializator.readInt(input);
        width = serializator.readInt(input);
        height = serializator.readInt(input);
        rollAngle = serializator.readFloat(input);
        yawAngle = serializator.readFloat(input);
        landmarkPoints = new EnumMap<FaceLandmark, Point2D>(FaceLandmark.class);
        int landmarkCount = serializator.readInt(input);
        for (; landmarkCount > 0; landmarkCount--)
            landmarkPoints.put(serializator.readEnum(input, FaceLandmark.class), new Point(serializator.readInt(input), serializator.readInt(input)));
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) +
                serializator.getBinarySize(centerX) +
                serializator.getBinarySize(centerY) +
                serializator.getBinarySize(width) +
                serializator.getBinarySize(height) +
                serializator.getBinarySize(rollAngle) +
                serializator.getBinarySize(yawAngle) +
                serializator.getBinarySize(landmarkPoints.size());
        for (Map.Entry<FaceLandmark, Point2D> entry : landmarkPoints.entrySet()) {
            size += serializator.getBinarySize(entry.getKey());
            size += serializator.getBinarySize((int)entry.getValue().getX());
            size += serializator.getBinarySize((int)entry.getValue().getY());
        }
        return size;
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, centerX);
        size += serializator.write(output, centerY);
        size += serializator.write(output, width);
        size += serializator.write(output, height);
        size += serializator.write(output, rollAngle);
        size += serializator.write(output, yawAngle);
        size += serializator.write(output, landmarkPoints.size());
        for (Map.Entry<FaceLandmark, Point2D> entry : landmarkPoints.entrySet()) {
            size += serializator.write(output, entry.getKey());
            size += serializator.write(output, (int)entry.getValue().getX());
            size += serializator.write(output, (int)entry.getValue().getY());
        }
        return size;
    }
}
