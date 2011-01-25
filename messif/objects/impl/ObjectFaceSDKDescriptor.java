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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.StreamGenericAbstractObjectIterator;

/**
 * This class encapsulates a FaceSDK recognition descriptor.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectFaceSDKDescriptor extends LocalAbstractObject {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** External library initialization ******************//

    /** Flag that represents the state of the native functions */
    private static final boolean isLibraryLoaded;

    static {
        boolean libraryLoaded;
        try {
            System.loadLibrary("FaceSDKDescriptor");
            int err = activateLibrary("2454FBA2A0CEFC4C5D0EFF26AA136E29D265CF84C69BF95057CFBA254F0C504EB4F94EBA3DFBAA6EE3C068CDAF94EF45CAB2190F76A208EBF88C698C9FA665C6");
            if (err == 0) {
                libraryLoaded = true;
            } else {
                libraryLoaded = false;
                Logger.getLogger(ObjectFaceSDKDescriptor.class.getName()).log(Level.WARNING, "Cannot activate FaceSDK library: err code " + err);
            }
        } catch (UnsatisfiedLinkError e) {
            Logger.getLogger(ObjectFaceSDKDescriptor.class.getName()).log(Level.WARNING, "Cannot load FaceSDK library: " + e);
            libraryLoaded = false;
        }
        isLibraryLoaded = libraryLoaded;
    }


    //****************** Attributes ******************//

    /** Internal FaceSDK data for recognition */
    private final byte[] data;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFaceSDKDescriptor from provided data.
     * @param data the FaceSDK data for recognition
     */
    public ObjectFaceSDKDescriptor(byte[] data) {
        this.data = data;
    }
    
    /**
     * Creates a new instance of ObjectFaceSDKDescriptor from stream.
     * @param stream the stream to read object's data from
     * @throws IOException if there was an error during reading from the given stream
     * @throws EOFException when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line read from given stream does not consist of comma-separated or space-separated numbers
     * @throws IllegalArgumentException if the read data is not valid
     */
    public ObjectFaceSDKDescriptor(BufferedReader stream) throws IOException, EOFException, NumberFormatException, IllegalArgumentException {
        String line = readObjectComments(stream);
        int len = line.length();
        data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte)((Character.digit(line.charAt(i), 16) << 4) + Character.digit(line.charAt(i + 1), 16));
    }


    //****************** Text file store/retrieve methods ******************//

    /** Internal array of hexadecimal digits */
    private final static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        for (int i = 0; i < data.length; i++) {
            stream.write(hexDigits[data[i] >> 4 & 0xf]);
            stream.write(hexDigits[data[i] & 0xf]);
        }
        stream.write('\n');
    }


    //****************** Data functions ******************//

    @Override
    public int getSize() {
        return data.length * Byte.SIZE / 8;
    }

    @Override
    public boolean dataEquals(Object obj) {
        return (obj instanceof ObjectFaceSDKDescriptor) && Arrays.equals(data, ((ObjectFaceSDKDescriptor)obj).data);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public FaceKey getObjectKey() {
        return (FaceKey)super.getObjectKey();
    }


    //****************** Distance function ******************//

    /**
     * Distance function for FaceSDK descriptors.
     * Note that this function is <em>not</em> a metric.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     * @throws IllegalStateException if the FaceSDK library was not loaded
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) throws IllegalStateException {
        if (!isLibraryLoaded)
            throw new IllegalStateException("Cannot compute distance - the FaceSDK library was not loaded");
        return 1f - getSimilarityImpl(data, ((ObjectFaceSDKDescriptor)obj).data);
    }


    //****************** External library methods ******************//

    /**
     * Implementation of the similarity measure in an external FaceSDK library.
     * @param obj1 the first object for which to compute the distance
     * @param obj2 the second object for which to compute the distance
     * @return the distance between obj1 and obj2
     */
    private static native float getSimilarityImpl(byte[] obj1, byte[] obj2);

    /**
     * Activation method of the FaceSDK library.
     * This is called only once when the library is loaded.
     * @param activationKey the license key
     * @return zero if the activation was successful, a FaceSDK error code is returned otherwise
     */
    private static native int activateLibrary(String activationKey);


    //****************** Extraction ******************//

    //////////////////////////// HACKS BELOW THIS LINE ////////////////////////////
    //////////////////////////// YOU HAVE BEEN WARNED  ////////////////////////////

    public static class FaceKey extends AbstractObjectKey {
        /** class id for serialization */
        private static final long serialVersionUID = 1L;

        public static enum FeatureType {
            LEFT_EYE, RIGHT_EYE, NOSE_TIP, MOUTH_RIGHT_CORNER, MOUTH_LEFT_CORNER,
            FACE_CONTOUR7, FACE_CONTOUR6, FACE_CONTOUR8, FACE_CONTOUR5, FACE_CONTOUR9, FACE_CONTOUR4, FACE_CONTOUR10, FACE_CONTOUR1, FACE_CONTOUR13,
            CHIN_LEFT, CHIN_RIGHT, LEFT_EYEBROW_OUTER_CORNER, LEFT_EYEBROW_INNER_CORNER, RIGHT_EYEBROW_INNER_CORNER, RIGHT_EYEBROW_OUTER_CORNER,
            MOUTH_TOP, MOUTH_BOTTOM, NOSE_BRIDGE, LEFT_EYE_OUTER_CORNER, LEFT_EYE_INNER_CORNER, RIGHT_EYE_INNER_CORNER, RIGHT_EYE_OUTER_CORNER,
            NOSE_LEFT_WING, NOSE_RIGHT_WING, CHIN_BOTTOM, FACE_CONTOUR2, FACE_CONTOUR12, FACE_CONTOUR3, FACE_CONTOUR11,
            LEFT_EYEBROW_MIDDLE, RIGHT_EYEBROW_MIDDLE, MOUTH_LEFT_TOP, MOUTH_RIGHT_TOP, MOUTH_LEFT_BOTTOM, MOUTH_RIGHT_BOTTOM
        };

        private final int centerX;
        private final int centerY;
        private final int width;
        private final int[] featureXCoordinates;
        private final int[] featureYCoordinates;

        public FaceKey(String locatorURI, Rectangle position, Point[] features) {
            super(locatorURI);
            this.width = position.width;
            this.centerX = (int)position.getCenterX();
            this.centerY = (int)position.getCenterY();
            if (features == null) {
                featureXCoordinates = null;
                featureYCoordinates = null;
            } else {
                featureXCoordinates = new int[features.length];
                featureYCoordinates = new int[features.length];
                for (int i = 0; i < features.length; i++) {
                    featureXCoordinates[i] = features[i].x;
                    featureYCoordinates[i] = features[i].y;
                }
            }
        }

        private FaceKey(String[] stringData) {
            super(stringData[stringData.length - 1]);
            // Parse position (center x,y and width)
            String[] positionData = stringData[0].split(",");
            this.centerX = Integer.parseInt(positionData[0]);
            this.centerY = Integer.parseInt(positionData[1]);
            this.width = Integer.parseInt(positionData[2]);

            // Parse feature coordinates
            featureXCoordinates = new int[stringData.length - 2];
            featureYCoordinates = new int[stringData.length - 2];
            for (int i = 0; i < stringData.length - 2; i++) {
                int comaPos = stringData[i + 1].indexOf(',');
                featureXCoordinates[i] = Integer.parseInt(stringData[i + 1].substring(0, comaPos));
                featureYCoordinates[i] = Integer.parseInt(stringData[i + 1].substring(comaPos + 1));
            }
        }

        public FaceKey(String stringData) {
            this(stringData.split(";"));
        }

        public String getFileName() {
            String locator = getLocatorURI();
            return locator.substring(0, locator.length() - 6) + ".jpg";
            /*
            int comaPos = locator.lastIndexOf(',');
            if (comaPos == -1)
                return locator;
            else
                return locator.substring(0, comaPos);
            */
        }

        public Rectangle getRect() {
            return new Rectangle(
                centerX - (width + 1) / 2,
                centerY - width * 3 / 4,
                width, width * 3 / 2
            );
        }

        public int getFeaturePosX(FeatureType feature) {
            return featureXCoordinates[feature.ordinal()];
        }
        public int getFeaturePosY(FeatureType feature) {
            return featureYCoordinates[feature.ordinal()];
        }

        @Override
        protected void writeData(OutputStream stream) throws IOException {
            StringBuilder str = new StringBuilder();
            str.append(centerX).append(',').append(centerY).append(',');
            str.append(width).append(';');
            for (int i = 0; i < featureXCoordinates.length; i++) {
                str.append(featureXCoordinates[i]).append(',');
                str.append(featureYCoordinates[i]).append(';');
            }
            stream.write(str.toString().getBytes());
            super.writeData(stream);
        }

        protected FaceKey(BinaryInput input, BinarySerializator serializator) throws IOException {
            super(input, serializator);
            centerX = serializator.readInt(input);
            centerY = serializator.readInt(input);
            width = serializator.readInt(input);
            featureXCoordinates = serializator.readIntArray(input);
            featureYCoordinates = serializator.readIntArray(input);
        }

        @Override
        public int getBinarySize(BinarySerializator serializator) {
            return super.getBinarySize(serializator) +
                    serializator.getBinarySize(centerX) +
                    serializator.getBinarySize(centerY) +
                    serializator.getBinarySize(width) +
                    serializator.getBinarySize(featureXCoordinates) +
                    serializator.getBinarySize(featureYCoordinates);
        }

        @Override
        public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
            return super.binarySerialize(output, serializator) +
                    serializator.write(output, centerX) +
                    serializator.write(output, centerY) +
                    serializator.write(output, width) +
                    serializator.write(output, featureXCoordinates) +
                    serializator.write(output, featureYCoordinates);
        }
    }


    //****************** Image manipulation ******************//

    public static BufferedImage drawOval(BufferedImage image, Rectangle rectangle, double rotation) throws IOException {
        Graphics2D g = image.createGraphics();

        // Translate to face center and rotate
        g.translate(rectangle.getCenterX(), rectangle.getCenterY());
        g.rotate(-rotation);

        // Draw oval
        g.drawOval(-rectangle.width/2, -rectangle.height/2, rectangle.width, rectangle.height);

        return image;
    }

    public static BufferedImage drawEyes(BufferedImage image, int leftEyeX, int leftEyeY, int rightEyeX, int rightEyeY) {
        Graphics2D g = image.createGraphics();
        g.fillOval(leftEyeX - 1, leftEyeY - 1, 3, 3);
        g.fillOval(rightEyeX - 1, rightEyeY - 1, 3, 3);
        return image;
    }

    public static void saveFace(File imageDir, FaceKey key, String format, File destFile) throws IOException {
        BufferedImage image = ImageIO.read(new File(imageDir, key.getFileName()));
        Rectangle rect = key.getRect();
        BufferedImage subImage = image.getSubimage(rect.x - rect.width / 20, rect.y - rect.height / 20, rect.width + rect.width / 10, rect.height + rect.height / 10);
        ImageIO.write(subImage, format, destFile);
    }

    public static BufferedImage drawFace(File imageDir, FaceKey key) throws IOException {
        // Read image
        BufferedImage image = ImageIO.read(new File(imageDir, key.getFileName()));
        return drawEyes(drawOval(image, key.getRect(), 0),
                key.getFeaturePosX(FaceKey.FeatureType.LEFT_EYE), key.getFeaturePosY(FaceKey.FeatureType.LEFT_EYE),
                key.getFeaturePosX(FaceKey.FeatureType.RIGHT_EYE), key.getFeaturePosY(FaceKey.FeatureType.RIGHT_EYE)
        );
    }

    public static void processImages(File imageDir, File destDir, String datafile) throws IOException {
        StreamGenericAbstractObjectIterator<ObjectFaceSDKDescriptor> objects = new StreamGenericAbstractObjectIterator<ObjectFaceSDKDescriptor>(ObjectFaceSDKDescriptor.class, datafile);
        while (objects.hasNext()) {
            FaceKey key = objects.next().getObjectKey();
            saveFace(imageDir, key, "jpg", new File(destDir, key.getLocatorURI() + ".jpg"));
        }
        objects.close();
    }
}
