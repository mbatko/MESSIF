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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
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

    /**
     * Returns <tt>true</tt> if the FaceSDK library was successfuly loaded.
     * If this method returns <tt>false</tt>, the {@link #getDistanceImpl(messif.objects.LocalAbstractObject, float) distance}
     * method will throw exception.
     * @return <tt>true</tt> if the FaceSDK library was successfuly loaded
     */
    public static boolean isIsLibraryLoaded() {
        return isLibraryLoaded;
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


    //****************** Face object key ******************//

    /**
     * Object key for the faces detected by the FaceSDK library.
     */
    public static class FaceKey extends AbstractObjectKey {
        /** class id for serialization */
        private static final long serialVersionUID = 1L;

        //****************** FaceSDK Constants ******************//

        /** Facial feature constant LEFT_EYE as defined by FaceSDK */
        public static final int LEFT_EYE = 0;
        /** Facial feature constant RIGHT_EYE as defined by FaceSDK */
        public static final int RIGHT_EYE = 1;
        /** Facial feature constant LEFT_EYE_INNER_CORNER as defined by FaceSDK */
        public static final int LEFT_EYE_INNER_CORNER = 24;
        /** Facial feature constant LEFT_EYE_OUTER_CORNER as defined by FaceSDK */
        public static final int LEFT_EYE_OUTER_CORNER = 23;
        /** Facial feature constant RIGHT_EYE_INNER_CORNER as defined by FaceSDK */
        public static final int RIGHT_EYE_INNER_CORNER = 25;
        /** Facial feature constant RIGHT_EYE_OUTER_CORNER as defined by FaceSDK */
        public static final int RIGHT_EYE_OUTER_CORNER = 26;

        /** Facial feature constant LEFT_EYEBROW_INNER_CORNER as defined by FaceSDK */
        public static final int LEFT_EYEBROW_INNER_CORNER = 17;
        /** Facial feature constant LEFT_EYEBROW_MIDDLE as defined by FaceSDK */
        public static final int LEFT_EYEBROW_MIDDLE = 34;
        /** Facial feature constant LEFT_EYEBROW_OUTER_CORNER as defined by FaceSDK */
        public static final int LEFT_EYEBROW_OUTER_CORNER = 16;

        /** Facial feature constant RIGHT_EYEBROW_INNER_CORNER as defined by FaceSDK */
        public static final int RIGHT_EYEBROW_INNER_CORNER = 18;
        /** Facial feature constant RIGHT_EYEBROW_MIDDLE as defined by FaceSDK */
        public static final int RIGHT_EYEBROW_MIDDLE = 35;
        /** Facial feature constant RIGHT_EYEBROW_OUTER_CORNER as defined by FaceSDK */
        public static final int RIGHT_EYEBROW_OUTER_CORNER = 19;

        /** Facial feature constant NOSE_TIP as defined by FaceSDK */
        public static final int NOSE_TIP = 2;
        /** Facial feature constant NOSE_BRIDGE as defined by FaceSDK */
        public static final int NOSE_BRIDGE = 22;
        /** Facial feature constant NOSE_LEFT_WING as defined by FaceSDK */
        public static final int NOSE_LEFT_WING = 27;
        /** Facial feature constant NOSE_RIGHT_WING as defined by FaceSDK */
        public static final int NOSE_RIGHT_WING = 28;

        /** Facial feature constant MOUTH_RIGHT_CORNER as defined by FaceSDK */
        public static final int MOUTH_RIGHT_CORNER = 3;
        /** Facial feature constant MOUTH_LEFT_CORNER as defined by FaceSDK */
        public static final int MOUTH_LEFT_CORNER = 4;
        /** Facial feature constant MOUTH_TOP as defined by FaceSDK */
        public static final int MOUTH_TOP = 20;
        /** Facial feature constant MOUTH_BOTTOM as defined by FaceSDK */
        public static final int MOUTH_BOTTOM = 21;
        /** Facial feature constant MOUTH_LEFT_TOP as defined by FaceSDK */
        public static final int MOUTH_LEFT_TOP = 36;
        /** Facial feature constant MOUTH_RIGHT_TOP as defined by FaceSDK */
        public static final int MOUTH_RIGHT_TOP = 37;
        /** Facial feature constant MOUTH_LEFT_BOTTOM as defined by FaceSDK */
        public static final int MOUTH_LEFT_BOTTOM = 38;
        /** Facial feature constant MOUTH_RIGHT_BOTTOM as defined by FaceSDK */
        public static final int MOUTH_RIGHT_BOTTOM = 39;

        /** Facial feature constant CHIN_BOTTOM as defined by FaceSDK */
        public static final int CHIN_BOTTOM = 29;
        /** Facial feature constant CHIN_LEFT as defined by FaceSDK */
        public static final int CHIN_LEFT = 14;
        /** Facial feature constant CHIN_RIGHT as defined by FaceSDK */
        public static final int CHIN_RIGHT = 15;

        /** Facial feature constant FACE_CONTOUR1 as defined by FaceSDK */
        public static final int FACE_CONTOUR1 = 12;
        /** Facial feature constant FACE_CONTOUR2 as defined by FaceSDK */
        public static final int FACE_CONTOUR2 = 30;
        /** Facial feature constant FACE_CONTOUR3 as defined by FaceSDK */
        public static final int FACE_CONTOUR3 = 32;
        /** Facial feature constant FACE_CONTOUR4 as defined by FaceSDK */
        public static final int FACE_CONTOUR4 = 10;
        /** Facial feature constant FACE_CONTOUR5 as defined by FaceSDK */
        public static final int FACE_CONTOUR5 = 8;
        /** Facial feature constant FACE_CONTOUR6 as defined by FaceSDK */
        public static final int FACE_CONTOUR6 = 6;
        /** Facial feature constant FACE_CONTOUR7 as defined by FaceSDK */
        public static final int FACE_CONTOUR7 = 5;
        /** Facial feature constant FACE_CONTOUR8 as defined by FaceSDK */
        public static final int FACE_CONTOUR8 = 7;
        /** Facial feature constant FACE_CONTOUR9 as defined by FaceSDK */
        public static final int FACE_CONTOUR9 = 9;
        /** Facial feature constant FACE_CONTOUR10 as defined by FaceSDK */
        public static final int FACE_CONTOUR10 = 11;
        /** Facial feature constant FACE_CONTOUR11 as defined by FaceSDK */
        public static final int FACE_CONTOUR11 = 33;
        /** Facial feature constant FACE_CONTOUR12 as defined by FaceSDK */
        public static final int FACE_CONTOUR12 = 31;
        /** Facial feature constant FACE_CONTOUR13 as defined by FaceSDK */
        public static final int FACE_CONTOUR13 = 13;

        /** List of facial feature constants that represent the face contour as defined by FaceSDK */
        private static final int[] contourFeatures = {
            FACE_CONTOUR1, FACE_CONTOUR2, FACE_CONTOUR3, FACE_CONTOUR4, FACE_CONTOUR5,
            FACE_CONTOUR6, FACE_CONTOUR7, FACE_CONTOUR8, FACE_CONTOUR9, FACE_CONTOUR10,
            FACE_CONTOUR11, FACE_CONTOUR12, FACE_CONTOUR13, CHIN_RIGHT, CHIN_BOTTOM, CHIN_LEFT
        };


        //****************** Attributes ******************//

        /** X-axis position of the face center (in pixels on the original image) */
        private final int centerX;
        /** Y-axis position of the face center (in pixels on the original image) */
        private final int centerY;
        /** Width of the face (in pixels) */
        private final int width;
        /** In-plane face rotation angle (in degrees) */
        private final double angle;
        /** X-axis position of the face features (in pixels on the original image) */
        private final int[] featureXCoordinates;
        /** Y-axis position of the face features (in pixels on the original image) */
        private final int[] featureYCoordinates;


        //****************** Constructors ******************//

        /**
         * Creates a new FaceKey from the given data.
         * @param locatorURI the face image locator URI
         * @param position the face bounding box in the original image
         * @param features the position of fourty face features in the original image
         */
        public FaceKey(String locatorURI, Rectangle position, Point[] features) {
            super(locatorURI);
            this.width = position.width;
            this.angle = 0;
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

        /**
         * Creates a new FaceKey from the given string parts.
         * The string features parts should contain the following:
         * <ul>
         * <li>coma-separated x and y axis coordinate of the face center,
         *     width of the face and in-plane rotation of the face;</li>
         * <li>forty coma-separated x and y axis values of the face features;</li>
         * <li>face image locator.</li>
         * </ul>
         * @param stringData the string feature parts
         */
        private FaceKey(String[] stringData) {
            super(stringData[stringData.length - 1]);
            // Parse position (center x,y and width)
            String[] positionData = stringData[0].split(",");
            this.centerX = Integer.parseInt(positionData[0]);
            this.centerY = Integer.parseInt(positionData[1]);
            this.width = Integer.parseInt(positionData[2]);
            this.angle = Double.parseDouble(positionData[3]);

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
         * Creates a new FaceKey from the given string data.
         * The string data should contain the following colon-separated parts:
         * <ul>
         * <li>coma-separated x and y axis coordinate of the face center,
         *     width of the face and in-plane rotation of the face;</li>
         * <li>forty coma-separated x and y axis values of the face features;</li>
         * <li>face image locator.</li>
         * </ul>
         * @param stringData the data string with the position, features and locator
         */
        public FaceKey(String stringData) {
            this(stringData.split(";"));
        }


        //****************** Attribute access ******************//

        /**
         * Returns the original image file name.
         * This method returns the {@link #getLocatorURI() locatorURI} without
         * the face number, i.e. without the two digits before the file extension.
         * @return the original image file name
         */
        public String getOriginalFileName() {
            String locator = getLocatorURI();
            int comaPos = locator.lastIndexOf('.');
            if (comaPos == -1)
                return locator;
            return locator.substring(0, comaPos - 2) + locator.substring(comaPos);
        }

        /**
         * Returns the face bounding rectangle.
         * @return the face bounding rectangle
         */
        public Rectangle getRect() {
            return new Rectangle(
                centerX - width / 2,
                centerY - width * 3 / 4,
                width, width * 3 / 2
            );
        }

        /**
         * Returns the face contour.
         * Specifically, the contour is a {@link Polygon} defined by 13
         * points as defined by FaceSDK.
         * @return the face contour
         */
        public Shape getFaceContour() {
            Polygon polygon = new Polygon();
            for (int i = 0; i < contourFeatures.length; i++)
                polygon.addPoint(featureXCoordinates[contourFeatures[i]], featureYCoordinates[contourFeatures[i]]);
            return polygon;
        }

        /**
         * Returns the position of the given feature.
         * @param feature identifier the feature the position of which to get
         * @return the feature position
         * @throws IndexOutOfBoundsException if the given feature identifier is not valid
         */
        public Point getFeaturePosition(int feature) throws IndexOutOfBoundsException {
            return new Point(featureXCoordinates[feature], featureYCoordinates[feature]);
        }


        //****************** Drawing functions ******************//

        /**
         * Returns the intersection of the image rectange (i.e. its width and height)
         * with the given rectangle.
         * @param image the image to use for intersection
         * @param rect the rectangle to use for intersection
         * @return an intersected rectangle
         */
        protected static Rectangle intersectRectWithImage(BufferedImage image, Rectangle rect) {
            return rect.intersection(new Rectangle(image.getWidth(), image.getHeight()));
        }

        /**
         * Draws a face oval in the given graphic context.
         * Note that the coordinate system of the original image is assumed.
         * @param g the graphic context in which to draw
         * @param size the size of the oval line in pixels
         * @return the graphic context that was passed (to allow streamlining of the drawing operations)
         */
        public Graphics2D drawFaceOval(Graphics2D g, int size) {
            AffineTransform transform = g.getTransform();

            // Translate to face center and rotate
            g.translate(centerX, centerY);
            g.rotate(angle * Math.PI / 180);

            // Draw oval
            Stroke origStroke = g.getStroke();
            g.setStroke(new BasicStroke(size));
            g.drawOval(- width / 2, - width * 3 / 4, width, width * 3 / 2);
            g.setStroke(origStroke);

            g.setTransform(transform);

            return g;
        }

        /**
         * Draws a face oval in the given image.
         * Note that the coordinate system of the original image is assumed.
         * @param image the image in which to draw
         * @param size the size of the oval line in pixels
         * @return the image that was passed (to allow streamlining of the drawing operations)
         */
        public BufferedImage drawFaceOval(BufferedImage image, int size) {
            drawFaceOval(image.createGraphics(), size);
            return image;
        }

        /**
         * Draws a face contour in the given graphic context.
         * Note that the coordinate system of the original image is assumed.
         * @param g the graphic context in which to draw
         * @param size the size of the contour line in pixels
         * @return the graphic context that was passed (to allow streamlining of the drawing operations)
         */
        public Graphics2D drawFaceContour(Graphics2D g, int size) {
            Stroke origStroke = g.getStroke();
            g.setStroke(new BasicStroke(size));
            g.draw(getFaceContour());
            g.setStroke(origStroke);
            return g;
        }

        /**
         * Draws a face contour in the given image.
         * Note that the coordinate system of the original image is assumed.
         * @param image the image in which to draw
         * @param size the size of the contour line in pixels
         * @return the image that was passed (to allow streamlining of the drawing operations)
         */
        public BufferedImage drawFaceContour(BufferedImage image, int size) {
            drawFaceContour(image.createGraphics(), size);
            return image;
        }

        /**
         * Draws a face feature point in the given graphic context.
         * Note that the coordinate system of the original image is assumed.
         * @param g the graphic context in which to draw
         * @param feature the feature identifier the position of which to get
         * @param size the size of the dot in pixels
         * @return the graphic context that was passed (to allow streamlining of the drawing operations)
         * @throws IndexOutOfBoundsException if the given feature identifier is not valid
         */
        public Graphics2D drawFaceFeaturePoint(Graphics2D g, int feature, int size) throws IndexOutOfBoundsException {
            g.fillOval(featureXCoordinates[feature] - (size + 1)/2, featureYCoordinates[feature] - (size + 1)/2, size + 2, size + 2);
            return g;
        }

        /**
         * Draws a face feature point in the given image.
         * The point is drawn as 3-pixels dot.
         * Note that the coordinate system of the original image is assumed.
         * @param image the image in which to draw
         * @param feature the feature identifier the position of which to get
         * @param size the size of the dot in pixels
         * @return the image that was passed (to allow streamlining of the drawing operations)
         * @throws IndexOutOfBoundsException if the given feature identifier is not valid
         */
        public BufferedImage drawFaceFeaturePoint(BufferedImage image, int feature, int size) throws IndexOutOfBoundsException {
            drawFaceFeaturePoint(image.createGraphics(), feature, size);
            return image;
        }

        /**
         * Draws all face feature points in the given image.
         * The points are drawn as 3-pixels dots.
         * Note that the coordinate system of the original image is assumed.
         * @param image the image in which to draw
         * @param size the size of the dot in pixels
         * @return the image that was passed (to allow streamlining of the drawing operations)
         * @throws IndexOutOfBoundsException if the given feature identifier is not valid
         */
        public BufferedImage drawFaceFeatures(BufferedImage image, int size) throws IndexOutOfBoundsException {
            Graphics2D g = image.createGraphics();
            for (int i = 0; i < featureXCoordinates.length; i++)
                drawFaceFeaturePoint(g, i, size);
            return image;
        }

        /**
         * Loads the original image where the this key's face was detected.
         * The original image file name is derived from the locator (see {@link #getOriginalFileName()}).
         * @param imageDir the root directory from which the detector was run;
         *          it can be <tt>null</tt> if the locator contains valid absolute path
         *          or relative path the the current working directory
         * @return the loaded image
         * @throws IOException if there was a problem loading the image (e.g. file was not found)
         */
        public BufferedImage loadFaceImage(File imageDir) throws IOException {
            return ImageIO.read(new File(imageDir, getOriginalFileName()));
        }

        /**
         * Saves the given image into the {@code destDir} as a file named
         * by this key's {@link #getLocatorURI() locator}.
         * This method is usually used as a final step of a serie of drawing operations.
         * @param image the image to save
         * @param destDir the destination directory where the image is stored
         * @param imageFormatName a string containg the informal name of the format (e.g. "jpeg")
         * @throws IOException if there was a problem loading the image (e.g. file was not found)
         */
        public void saveFaceImage(BufferedImage image, String imageFormatName, File destDir) throws IOException {
            ImageIO.write(image, imageFormatName, new File(destDir, getLocatorURI()));
        }

        /**
         * Returns the sub-image that contains the face.
         * @param originalImage the original image where the face was detected
         * @param enlargementFactor the enlargement factor of the face bounding box;
         *          zero means no enlargement
         * @return the sub-image with the face
         */
        public BufferedImage extractFaceImage(BufferedImage originalImage, double enlargementFactor) {
            Rectangle rect = getRect();
            if (enlargementFactor > 0)
                rect.grow((int)(rect.getWidth() * enlargementFactor), (int)(rect.getHeight() * enlargementFactor));
            rect = intersectRectWithImage(originalImage, rect);
            return originalImage.getSubimage(rect.x, rect.y, rect.width, rect.height);
        }

        /**
         * Returns the sub-image that contains the face.
         * The original face image is loaded from the {@code imageDir} directory.
         * @param imageDir the root directory from which the detector was run;
         *          it can be <tt>null</tt> if the locator contains valid absolute path
         *          or relative path the the current working directory
         * @param enlargementFactor the enlargement factor of the face bounding box;
         *          zero means no enlargement
         * @return the sub-image with the face
         * @throws IOException if there was a problem loading the image (e.g. file was not found)
         */
        public BufferedImage extractFaceImage(File imageDir, double enlargementFactor) throws IOException {
            return extractFaceImage(loadFaceImage(imageDir), enlargementFactor);
        }

        /**
         * Extracts the sub-image that contains the face and saves it into a file.
         * The original face image is loaded from the {@code imageDir} directory.
         * The face sub-image is placed into the {@code destDir} as a file named
         * by this key's {@link #getLocatorURI() locator}.
         * @param imageDir the root directory from which the detector was run;
         *          it can be <tt>null</tt> if the locator contains valid absolute path
         *          or relative path the the current working directory
         * @param destDir the destination directory where the face sub-image is stored
         * @param imageFormatName a string containg the informal name of the format (e.g. "jpeg")
         * @param enlargementFactor the enlargement factor of the face bounding box;
         *          zero means no enlargement
         * @throws IOException
         */
        public void extractFaceImage(File imageDir, File destDir, String imageFormatName, double enlargementFactor) throws IOException {
            ImageIO.write(
                    extractFaceImage(imageDir, enlargementFactor),
                    imageFormatName,
                    new File(destDir, getLocatorURI())
            );
        }

        /**
         * Extracts the face image represented by this key from the given image.
         * The face is transformed so that the eyes are level, the left eye is
         * at the given corredinates (and the right eye at "newWidth - leftEyeX").
         *
         * @param image the original image from which to extract the face
         * @param leftEyeX the left eye x-coordinate in the resulting image
         * @param leftEyeY the left eye y-coordinate in the resulting image
         * @param newWidth the resulting image width
         * @param newHeight the resulting image height
         * @return the extracted face image
         */
        public BufferedImage extractTransformedFaceImage(BufferedImage image, int leftEyeX, int leftEyeY, int newWidth, int newHeight) {
            // Compute the rotation and scale based on the eyes position
            double rotation = Math.atan2(
                    featureYCoordinates[LEFT_EYE] - featureYCoordinates[RIGHT_EYE],
                    featureXCoordinates[RIGHT_EYE] - featureXCoordinates[LEFT_EYE]
            );
            double scale = (newWidth - 2 * leftEyeX) / Math.sqrt(
                    Math.pow(featureXCoordinates[LEFT_EYE] - featureXCoordinates[RIGHT_EYE], 2) +
                    Math.pow(featureYCoordinates[LEFT_EYE] - featureYCoordinates[RIGHT_EYE], 2)
            );

            // Prepare the transformation (note that the operations are applied down-to-top)
            AffineTransform transform = new AffineTransform();
            transform.translate(leftEyeX, leftEyeY);
            transform.scale(scale, scale);
            transform.rotate(rotation);
            transform.translate(-featureXCoordinates[LEFT_EYE], -featureYCoordinates[LEFT_EYE]);

            BufferedImage outImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(image, new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC), 0, 0);
            return outImage;
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
        protected FaceKey(BinaryInput input, BinarySerializator serializator) throws IOException {
            super(input, serializator);
            centerX = serializator.readInt(input);
            centerY = serializator.readInt(input);
            width = serializator.readInt(input);
            angle = serializator.readDouble(input);
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


    //////////////////////////// HACKS BELOW THIS LINE ////////////////////////////
    //////////////////////////// YOU HAVE BEEN WARNED  ////////////////////////////

    /**
     * Convenience method for processing the images of the detected faces.
     *
     * @param imageDir the directory with the original images
     * @param destDir the directory where the new face images will be stored
     * @param datafile the data file with {@link ObjectFaceSDKDescriptor}s
     * @param locatorRegexp regular expression for matching the object locators
     * @param enlargementFactor the enlargement factor of the face bounding box;
     *          zero means no enlargement, negative value means that the original
     *          image is not cropped by the face bounding box
     * @param featureSize the size of the features dots and lines in pixels;
     *          if zero, no features are drawn
     * @throws IOException if there was a problem reading the datafile, reading
     *              an image from imageDir or writing to a file in destDir
     */
    public static void processImages(File imageDir, File destDir, String datafile, String locatorRegexp, double enlargementFactor, int featureSize) throws IOException {
        StreamGenericAbstractObjectIterator<ObjectFaceSDKDescriptor> objects = new StreamGenericAbstractObjectIterator<ObjectFaceSDKDescriptor>(ObjectFaceSDKDescriptor.class, datafile);
        try {
            while (true) {
                FaceKey key = objects.getObjectByLocatorRegexp(locatorRegexp).getObjectKey();
                BufferedImage image = key.loadFaceImage(imageDir);
                if (featureSize > 0)
                    image = key.drawFaceFeatures(key.drawFaceContour(image, featureSize), featureSize);
                if (enlargementFactor >= 0) // Zero means no enlargement, negative value means no cropping
                    image = key.extractFaceImage(image, enlargementFactor);
                else if (enlargementFactor < -2) // Large negative value means that the image is transformed for the ObjectAdvancedFaceDescriptor
                    image = key.extractTransformedFaceImage(image, 16, 24, 46, 56);
                key.saveFaceImage(image, "jpeg", destDir);
            }
        } catch (NoSuchElementException ignore) {
        }
        objects.close();
    }

    /**
     * Calls the {@link #processImages(java.io.File, java.io.File, java.lang.String)} method.
     * Usage: java -classpath MESSIF.jar messif.objects.impl.ObjectFaceSDKDescriptor ...
     *
     * @param args the following arguments are expected: imageDir, destDir, datafile,
     *          locatorRegexp, enlargementFactor, and featureSize
     * @throws IOException if there was a problem reading the datafile, reading
     *              an image from imageDir or writing to a file in destDir
     */
    public static void main(String[] args) throws IOException {
        try {
            int argIndex = 0;
            File imageDir = new File(args[argIndex++]);
            File destDir = new File(args[argIndex++]);
            String datafile = args[argIndex++];
            String locatorRegexp = argIndex < args.length ? args[argIndex++] : ".*";
            double enlargementFactor = argIndex < args.length ? Double.parseDouble(args[argIndex++]) :  0.05;
            int featureSize = argIndex < args.length ? Integer.parseInt(args[argIndex++]) :  0;
            processImages(imageDir, destDir, datafile, locatorRegexp, enlargementFactor, featureSize);
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: " + ObjectFaceSDKDescriptor.class.getName() + " <imageDir> <destDir> <datafile> [<locatorRegexp> [<enlargementFactor> [<featureSize>]]]");
        }
    }
}
