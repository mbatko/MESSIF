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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * Object key for the faces detected by the PittPatt library.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class OCVFaceKey extends FaceKey {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** X-axis position of the face left-top corner (in pixels on the original image) */
    private final int x;
    /** Y-axis position of the face left-top corner (in pixels on the original image) */
    private final int y;
    /** Width of the face (in pixels) */
    private final int width;
    /** Height of the face (in pixels) */
    private final int height;


    //****************** Constructors ******************//

    /**
     * Creates a new OCVFaceKey from the given string parts.
     * The string features parts should contain the following:
     * <ul>
     * <li>coma-separated x and y axis coordinate of the face left-top corner, and
     *     width and height of the face;</li>
     * <li>face image locator.</li>
     * </ul>
     * @param stringData the string feature parts
     */
    private OCVFaceKey(String[] stringData) {
        super(stringData[stringData.length - 1]);
        // Parse position (center x,center y,width,height)
        String[] positionData = stringData[0].split(",");
        this.x = Integer.parseInt(positionData[0]);
        this.y = Integer.parseInt(positionData[1]);
        this.width = Integer.parseInt(positionData[2]);
        this.height = Integer.parseInt(positionData[3]);
    }

    /**
     * Creates a new OCVFaceKey from the given string data.
     * The string data should contain the following semicolon-separated parts:
     * <ul>
     * <li>coma-separated x and y axis coordinate of the face left-top corner, and
     *     width and height of the face;</li>
     * <li>face image locator.</li>
     * </ul>
     * @param stringData the data string with the position, features and locator
     */
    public OCVFaceKey(String stringData) {
        this(stringData.split(";"));
    }


    //****************** Attribute access methods ******************//

    @Override
    public String getSourceImageLocatorURI() {
        String locator = getLocatorURI();
        return locator.substring(0, locator.length() - 3);
    }

    @Override
    public int getCenterX() {
        return x + width/2;
    }

    @Override
    public int getCenterY() {
        return y + height/2;
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
        return 0;
    }

    @Override
    public float getYawAngle() {
        return 0;
    }

    @Override
    public Shape getContour() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public Point2D getLandmarkPosition(FaceLandmark landmark) {
        return null;
    }


    //****************** Textual serialization ******************//

    /**
     * Append face position data to the given string.
     * @param str the string builder to which the data are appended
     * @return the given string builder to allow chaining
     */
    private StringBuilder appendFaceData(StringBuilder str) {
        str.append(x).append(',').append(y).append(',');
        str.append(width).append(',').append(height).append(';');
        return str;
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        stream.write(appendFaceData(new StringBuilder()).toString().getBytes());
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
    protected OCVFaceKey(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        x = serializator.readInt(input);
        y = serializator.readInt(input);
        width = serializator.readInt(input);
        height = serializator.readInt(input);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) +
                serializator.getBinarySize(x) +
                serializator.getBinarySize(y) +
                serializator.getBinarySize(width) +
                serializator.getBinarySize(height);
        return size;
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, x);
        size += serializator.write(output, y);
        size += serializator.write(output, width);
        size += serializator.write(output, height);
        return size;
    }
}
