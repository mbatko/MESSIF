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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.FaceKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * This class encapsulates a PittPatt recognition descriptor.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectFacePittPattDescriptor extends ObjectByteVector {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** External library initialization ******************//

    /** Flag that represents the state of the native functions */
    private static final boolean isLibraryLoaded;

    static {
        boolean libraryLoaded;
        try {
            System.loadLibrary("PittPattDescriptor");
            int err = activateLibrary("batko_michal", new int[] { 0x467d4373, 0x454e82da, 0x68ef8f47, 0x6111dd46, 0x46b81901, 0x69253faf, 0x49389048, 0x54dea4f2, 0x59dbae86, 0x48bb5f7e, 0x8084942e, 0xeae9a8dd, 0xe8ac5919, 0xf93acd57, 0xe8ac3d45, 0xb2dd2273, 0x3e6ca727, 0xcae1d99a, 0xc42f8eb0, 0x01a046f1, 0x7bd3005f, 0x2ebeb878, 0x8bad2675, 0xbebfbf73, 0xd01d14e9, 0x8da3e673, 0x0bec729a, 0xbb716fcc, 0xa5684970, 0x09e900c8, 0x4f6047a1, 0xe1b3925b, 0x1dbe737a });
            if (err == 0) {
                libraryLoaded = true;
            } else {
                libraryLoaded = false;
                Logger.getLogger(ObjectFacePittPattDescriptor.class.getName()).log(Level.WARNING, "Cannot activate PittPatt library: err code {0}", err);
            }
        } catch (UnsatisfiedLinkError e) {
            Logger.getLogger(ObjectFacePittPattDescriptor.class.getName()).log(Level.WARNING, "Cannot load PittPatt library: {0}", e);
            libraryLoaded = false;
        }
        isLibraryLoaded = libraryLoaded;
    }

    /**
     * Returns <tt>true</tt> if the PittPatt library was successfully loaded.
     * If this method returns <tt>false</tt>, the {@link #getDistanceImpl(messif.objects.LocalAbstractObject, float) distance}
     * method will throw exception.
     * @return <tt>true</tt> if the PittPatt library was successfully loaded
     */
    public static boolean isIsLibraryLoaded() {
        return isLibraryLoaded;
    }


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFacePittPattDescriptor from provided data.
     * @param data the PittPatt data for recognition
     */
    public ObjectFacePittPattDescriptor(byte[] data) {
        super(data);
    }
    
    /**
     * Creates a new instance of ObjectFacePittPattDescriptor from stream.
     * @param stream the stream to read object's data from
     * @throws IOException if there was an error during reading from the given stream
     * @throws EOFException when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line read from given stream does not consist of comma-separated or space-separated numbers
     * @throws IllegalArgumentException if the read data is not valid
     */
    public ObjectFacePittPattDescriptor(BufferedReader stream) throws IOException, EOFException, NumberFormatException, IllegalArgumentException {
        super(stream, true);
    }


    //****************** Text file store/retrieve methods ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        writeByteHexString(data, stream, '\n');
    }


    //****************** Distance function ******************//

    /**
     * Distance function for PittPatt descriptors.
     * Note that this function is <em>not</em> a metric.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     * @throws IllegalStateException if the PittPatt library was not loaded
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) throws IllegalStateException {
        if (!isLibraryLoaded)
            throw new IllegalStateException("Cannot compute distance - the PittPatt library was not loaded");
        return 1f - getSimilarityImpl(data, ((ObjectFacePittPattDescriptor)obj).data);
    }


    //****************** External library methods ******************//

    /**
     * Implementation of the similarity measure in an external PittPatt library.
     * @param obj1 the first object for which to compute the distance
     * @param obj2 the second object for which to compute the distance
     * @return the distance between obj1 and obj2
     */
    private static native float getSimilarityImpl(byte[] obj1, byte[] obj2);

    /**
     * Activation method of the PittPatt library.
     * This is called only once when the library is loaded.
     * @param licenseId the name of the licensed party
     * @param licenseKey the license key
     * @return zero if the activation was successful, a PittPatt error code is returned otherwise
     */
    private static native int activateLibrary(String licenseId, int[] licenseKey);


    //****************** Face object key ******************//

    /**
     * Object key for the faces detected by the PittPatt library.
     */
    public static class PittPattFaceKey extends FaceKey {
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
}
