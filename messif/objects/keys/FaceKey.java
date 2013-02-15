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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Object key for the faces.
 * Object supports cropping and drawing the facial features.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class FaceKey extends AbstractObjectKey {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** Facial landmark constants */
    public static enum FaceLandmark {
        /** Facial feature constant EAR_LEFT */
        EAR_LEFT,
        /** Facial feature constant EAR_RIGHT */
        EAR_RIGHT,
        /** Facial feature constant EYE_LEFT */
        EYE_LEFT,
        /** Facial feature constant EYE_LEFT_OUTER_CORNER */
        EYE_LEFT_OUTER_CORNER,
        /** Facial feature constant EYE_LEFT_INNER_CORNER */
        EYE_LEFT_INNER_CORNER,
        /** Facial feature constant EYE_RIGHT */
        EYE_RIGHT,
        /** Facial feature constant EYE_RIGHT_INNER_CORNER */
        EYE_RIGHT_INNER_CORNER,
        /** Facial feature constant EYE_RIGHT_OUTER_CORNER */
        EYE_RIGHT_OUTER_CORNER,
        /** Facial feature constant EYEBROW_LEFT_OUTER_CORNER */
        EYEBROW_LEFT_OUTER_CORNER,
        /** Facial feature constant EYEBROW_LEFT_INNER_CORNER */
        EYEBROW_LEFT_INNER_CORNER,
        /** Facial feature constant EYEBROW_LEFT_MIDDLE */
        EYEBROW_LEFT_MIDDLE,
        /** Facial feature constant EYEBROW_RIGHT_INNER_CORNER */
        EYEBROW_RIGHT_INNER_CORNER,
        /** Facial feature constant EYEBROW_RIGHT_OUTER_CORNER */
        EYEBROW_RIGHT_OUTER_CORNER,
        /** Facial feature constant EYEBROW_RIGHT_MIDDLE */
        EYEBROW_RIGHT_MIDDLE,
        /** Facial feature constant NOSE_TIP */
        NOSE_TIP,
        /** Facial feature constant NOSE_BRIDGE */
        NOSE_BRIDGE,
        /** Facial feature constant NOSE_EYE */
        NOSE_EYE,
        /** Facial feature constant NOSE_LEFT_WING */
        NOSE_LEFT_WING,
        /** Facial feature constant NOSE_RIGHT_WING */
        NOSE_RIGHT_WING,
        /** Facial feature constant MOUTH_CENTER */
        MOUTH_CENTER,
        /** Facial feature constant MOUTH_TOP */
        MOUTH_TOP,
        /** Facial feature constant MOUTH_BOTTOM */
        MOUTH_BOTTOM,
        /** Facial feature constant MOUTH_LEFT_CORNER */
        MOUTH_LEFT_CORNER,
        /** Facial feature constant MOUTH_RIGHT_CORNER */
        MOUTH_RIGHT_CORNER,
        /** Facial feature constant MOUTH_LEFT_TOP */
        MOUTH_LEFT_TOP,
        /** Facial feature constant MOUTH_RIGHT_TOP */
        MOUTH_RIGHT_TOP,
        /** Facial feature constant MOUTH_LEFT_BOTTOM */
        MOUTH_LEFT_BOTTOM,
        /** Facial feature constant MOUTH_RIGHT_BOTTOM */
        MOUTH_RIGHT_BOTTOM,
        /** Facial feature constant CHIN_LEFT */
        CHIN_LEFT,
        /** Facial feature constant CHIN_RIGHT */
        CHIN_RIGHT,
        /** Facial feature constant CHIN_BOTTOM */
        CHIN_BOTTOM,
        /** Facial feature constant CHEEK_LEFT_TOP */
        CHEEK_LEFT_TOP,
        /** Facial feature constant CHEEK_RIGHT_TOP */
        CHEEK_RIGHT_TOP,
        /** Facial feature constant CHEEK_LEFT_BOTTOM */
        CHEEK_LEFT_BOTTOM,
        /** Facial feature constant CHEEK_RIGHT_BOTTOM */
        CHEEK_RIGHT_BOTTOM;
    }


    //****************** Constructors ******************//

    /**
     * Creates a new FaceKey with the given the locator URI.
     * @param locatorURI the face image locator URI
     */
    protected FaceKey(String locatorURI) {
        super(locatorURI);
    }

    //****************** Attribute access ******************//

    /**
     * Returns the locator URI of the source image this face was extracted from.
     * The actual {@link #getLocatorURI() locator URI} is returned by default.
     * @return the locator URI of the face source image
     */
    public String getSourceImageLocatorURI() {
        return getLocatorURI();
    }

    /**
     * Returns the x-coordinate (in pixels) of the face center point
     * in the {@link #getSourceImageLocatorURI() source image}.
     * @return the x-coordinate of the face center point
     */
    public abstract int getCenterX();

    /**
     * Returns the y-coordinate (in pixels) of the face center point
     * in the {@link #getSourceImageLocatorURI() source image}.
     * @return the y-coordinate of the face center point
     */
    public abstract int getCenterY();

    /**
     * Returns the face width in pixels in the {@link #getSourceImageLocatorURI() source image}.
     * @return the face width in pixels
     */
    public abstract int getWidth();

    /**
     * Returns the face height in pixels in the {@link #getSourceImageLocatorURI() source image}.
     * @return the face height in pixels
     */
    public abstract int getHeight();

    /**
     * Returns the face roll angle in degrees in the {@link #getSourceImageLocatorURI() source image}.
     * That is the in-plane rotation of the face along the face center.
     * Straight face have zero roll angle.
     * @return the face roll angle
     */
    public abstract float getRollAngle();

    /**
     * Returns the face yaw angle in degrees in the {@link #getSourceImageLocatorURI() source image}.
     * That is the left-right rotation of the face along the head vertical axis.
     * Full-frontal face have zero yaw angle.
     * @return the face yaw angle
     */
    public abstract float getYawAngle();

    /**
     * Returns the face contour in the {@link #getSourceImageLocatorURI() source image}.
     * @return the face contour
     */
    public abstract Shape getContour();

    /**
     * Returns the position of the given face landmark.
     * @param  landmark the face landmark the position of which to get
     * @return the landmark position
     * @throws NullPointerException if the {@code feature} parameter is <tt>null</tt>
     */
    public abstract Point2D getLandmarkPosition(FaceLandmark landmark) throws NullPointerException;


    //****************** Intersection functions ******************//

    /**
     * Returns whether this face key has an intersection with the given face key.
     * @param otherFaceKey the other face key to intersect with
     * @param threshold the intersection threshold, i.e. the percentage of the intersection area with respect to the face key areas
     * @return <tt>true</tt> if this face key intersects with the given face key
     */
    public boolean intersect(FaceKey otherFaceKey, double threshold) {
        Rectangle2D thisBoundBox = getContour().getBounds2D();
        Rectangle2D otherBoundBox = otherFaceKey.getContour().getBounds2D();
        Rectangle2D intersection = thisBoundBox.createIntersection(otherBoundBox);
        if (intersection.isEmpty())
            return false;
        int thisArea = getWidth() * getHeight();
        int otherArea = otherFaceKey.getWidth() * otherFaceKey.getHeight();
        double intersectionArea = intersection.getWidth() * intersection.getHeight();
        assert intersectionArea <= thisArea && intersectionArea <= otherArea;
        return (2d * intersectionArea / (thisArea + otherArea)) >= threshold;
    }


    //****************** Drawing functions ******************//

    /**
     * Returns the intersection of the image rectangle (i.e. its width and height)
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
        g.translate(getCenterX(), getCenterY());
        g.rotate(-getRollAngle() * Math.PI / 180.0);

        // Draw oval
        Stroke origStroke = g.getStroke();
        g.setStroke(new BasicStroke(size));
        g.drawOval(- getWidth() / 2, - getHeight() / 2, getWidth(), getHeight());
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
        g.draw(getContour());
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
     * Draws a face landmark point in the given graphic context.
     * Note that the coordinate system of the original image is assumed.
     * @param g the graphic context in which to draw
     * @param landmark the landmark identifier the position of which to get
     * @param size the size of the dot in pixels
     * @return the graphic context that was passed (to allow streamlining of the drawing operations)
     */
    public Graphics2D drawFaceLandmarkPoint(Graphics2D g, FaceLandmark landmark, int size) {
        Point2D pos = getLandmarkPosition(landmark);
        if (pos != null)
            g.fillOval((int)(pos.getX() - (size + 1)/2), (int)(pos.getY() - (size + 1)/2), size + 2, size + 2);
        return g;
    }

    /**
     * Draws a face landmark point in the given image.
     * The point is drawn as {@code size}-pixels wide dot.
     * Note that the coordinate system of the original image is assumed.
     * @param image the image in which to draw
     * @param landmark the landmark identifier the position of which to get
     * @param size the size of the dot in pixels
     * @return the image that was passed (to allow streamlining of the drawing operations)
     */
    public BufferedImage drawFaceLandmarkPoint(BufferedImage image,FaceLandmark landmark, int size) {
        drawFaceLandmarkPoint(image.createGraphics(), landmark, size);
        return image;
    }

    /**
     * Draws all face landmark points in the given image.
     * The points are drawn as {@code size}-pixels wide dots.
     * Note that the coordinate system of the original image is assumed.
     * @param image the image in which to draw
     * @param size the size of the dot in pixels
     * @return the image that was passed (to allow streamlining of the drawing operations)
     */
    public BufferedImage drawFaceLandmarks(BufferedImage image, int size) {
        Graphics2D g = image.createGraphics();
        for (FaceLandmark landmark : FaceLandmark.values())
            drawFaceLandmarkPoint(g, landmark, size);
        return image;
    }

    /**
     * Draws a given face label at the bottom of the face chin.
     * @param g the graphic context in which to draw
     * @param label the label to draw
     * @return the graphic context that was passed (to allow streamlining of the drawing operations)
     */
    public Graphics2D drawFaceLabel(Graphics2D g, String label) {
        FontMetrics fontMetrics = g.getFontMetrics();
        g.drawString(label,
                getCenterX() - fontMetrics.stringWidth(label) / 2,
                getCenterY() + getHeight() / 2 + fontMetrics.getMaxAscent());
        return g;
    }

    /**
     * Draws a given face label at the bottom of the face chin.
     * Note that the coordinate system of the original image is assumed.
     * @param image the image in which to draw
     * @param label the label to draw
     * @return the image that was passed (to allow streamlining of the drawing operations)
     */
    public BufferedImage drawFaceLabel(BufferedImage image, String label) {
        drawFaceLabel(image.createGraphics(), label);
        return image;
    }

    /**
     * Loads the original image where the this face key was detected.
     * The original image file name is derived from the locator (see {@link #getSourceImageLocatorURI()}).
     * @param imageDir the root directory from which the detector was run;
     *          it can be <tt>null</tt> if the locator contains valid absolute path
     *          or relative path the the current working directory
     * @return the loaded image
     * @throws IOException if there was a problem loading the image (e.g. file was not found)
     */
    public BufferedImage loadFaceImage(File imageDir) throws IOException {
        return ImageIO.read(new File(imageDir, getSourceImageLocatorURI()));
    }

    /**
     * Saves the given image into the {@code destDir} as a file named
     * by the {@link #getLocatorURI() locator} of this key.
     * This method is usually used as a final step of drawing operations sequence.
     * @param image the image to save
     * @param destDir the destination directory where the image is stored
     * @param imageFormatName a string containing the informal name of the format (e.g. "jpeg")
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
        Rectangle rect = getContour().getBounds();
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
     * by the {@link #getLocatorURI() locator} of this key.
     * @param imageDir the root directory from which the detector was run;
     *          it can be <tt>null</tt> if the locator contains valid absolute path
     *          or relative path the the current working directory
     * @param destDir the destination directory where the face sub-image is stored
     * @param imageFormatName a string containing the informal name of the format (e.g. "jpeg")
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
     * The face rotated, translated and cropped.
     *
     * @param image the original image from which to extract the face
     * @param scale the scaling factor of the resulting face
     * @param rotation the rotation angle of the resulting face (in radians)
     * @param rotationCenter the point of the rotation center
     * @param centerTranslation the destination point where the rotationCenter will be in the new image
     * @param newWidth the resulting image width
     * @param newHeight the resulting image height
     * @return the extracted face image
     */
    public BufferedImage extractTransformedFaceImage(BufferedImage image, double scale, double rotation, Point2D rotationCenter, Point2D centerTranslation, int newWidth, int newHeight) {
        // Prepare the transformation (note that the operations are applied down-to-top)
        AffineTransform transform = new AffineTransform();
        transform.translate(centerTranslation.getX(), centerTranslation.getY());
        transform.scale(scale, scale);
        transform.rotate(rotation);
        transform.translate(-rotationCenter.getX(), -rotationCenter.getY());

        BufferedImage outImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = outImage.createGraphics();
        g.setBackground(Color.white);
        g.fillRect(0, 0, newWidth, newHeight);
        g.drawImage(image, new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC), 0, 0);
        return outImage;        
    }

    /**
     * Extracts the face image represented by this key from the given image.
     * The face is transformed so that the eyes are level, the left eye is
     * at the given coordinates (and the right eye at "newWidth - leftEye.getX()").
     *
     * @param image the original image from which to extract the face
     * @param newLeftEyePos the coordinates of the left eye in the resulting image
     * @param newWidth the resulting image width
     * @param newHeight the resulting image height
     * @return the extracted face image
     */
    public BufferedImage extractTransformedFaceImageByEyes(BufferedImage image, Point2D newLeftEyePos, int newWidth, int newHeight) {
        // Compute the rotation and scale based on the eyes position
        Point2D leftEyePos = getLandmarkPosition(FaceLandmark.EYE_LEFT);
        Point2D rightEyePos = getLandmarkPosition(FaceLandmark.EYE_RIGHT);
        double rotation = Math.atan2(
                leftEyePos.getY() - rightEyePos.getY(),
                rightEyePos.getX() - leftEyePos.getX()
        );
        double scale = (newWidth - 2 * newLeftEyePos.getX()) / Math.sqrt(
                Math.pow(leftEyePos.getX() - rightEyePos.getX(), 2) +
                Math.pow(leftEyePos.getY() - rightEyePos.getY(), 2)
        );
        return extractTransformedFaceImage(image, scale, rotation, leftEyePos, newLeftEyePos, newWidth, newHeight);
    }

    /**
     * Extracts the face image represented by this key from the given image.
     * The face is transformed so that the eyes are level, the left eye is
     * at the given coordinates (and the right eye at "newWidth - leftEyeX").
     *
     * @param image the original image from which to extract the face
     * @param leftEye the coordinates of the left eye in the resulting image
     * @param newWidth the resulting image width
     * @param newHeight the resulting image height
     * @return the extracted face image
     * @throws IllegalStateException if this face key has neither left nor right eye landmark detected 
     */
    public BufferedImage extractTransformedFaceImageByPos(BufferedImage image, Point2D leftEye, int newWidth, int newHeight) throws IllegalStateException {
        // Compute the rotation and scale based on the eyes position
        Point2D origPos = getLandmarkPosition(FaceLandmark.EYE_LEFT);
        Point2D newPos;
        if (origPos != null) {
            newPos = leftEye;
        } else {
            origPos = getLandmarkPosition(FaceLandmark.EYE_RIGHT);
            newPos = (Point2D)leftEye.clone();
            newPos.setLocation(newWidth - newPos.getX(), newPos.getY());
        }
        if (origPos == null)
            throw new IllegalStateException("Cannot extract face with neither left nor right eye");
        return extractTransformedFaceImage(image, (double)newWidth / (double)getWidth(), getRollAngle() * Math.PI / 180, origPos, newPos, newWidth, newHeight);
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
    }
}
