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
import java.io.IOException;
import java.io.OutputStream;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * Implements GPS coordinates objects. The distance is computed using WGS84 model.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectGPSCoordinate extends LocalAbstractObject implements BinarySerializable {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /** Geographic latitude on WGS84 ellipsiod in degrees */
    private final float latitude;
    /** Geographic longitude on WGS84 ellipsiod in degrees */
    private final float longitude;
    
    /****************** Constructors ******************/

    /**
     * Creates a new instance of ObjectGPSCoordinate for the given
     * latitude and longitude specified in degrees.
     * 
     * @param latitude the geographic latitude on WGS84 ellipsiod in degrees
     * @param longitude the geographic longitude on WGS84 ellipsiod in degrees
     */
    public ObjectGPSCoordinate(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Creates a new instance of ObjectGPSCoordinate from stream.
     * @param stream the text stream from which to read the ObjectGPSCoordinate
     * @throws IOException if there was an error during reading from the given stream
     * @throws NumberFormatException if the data in the stream cannot be converted
     *      to latitude and longitude
     */
    public ObjectGPSCoordinate(BufferedReader stream) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        
        String[] val = line.trim().split("[;,]");
        
        if (val.length == 2) {
            // Only coordinates
            latitude = Float.parseFloat(val[0]);
            longitude = Float.parseFloat(val[1]);
        } else if (val.length == 3) {
            // Url + coordinates
            setObjectKey(new AbstractObjectKey(val[0]));
            latitude = Float.parseFloat(val[1]);
            longitude = Float.parseFloat(val[2]);
        } else
            throw new NumberFormatException("Incorrect format of ObjectGPSCoordinate. The format is \"url;latitute,longitude\" (in degrees), where the url is optional");        
    }

    /**
     * Store this object's data to a text stream.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    @Override
    public void writeData(OutputStream stream) throws IOException {
        stream.write(String.valueOf(latitude).getBytes());
        stream.write(',');
        stream.write(String.valueOf(longitude).getBytes());
        stream.write('\n');
    }

    /****************** Size function ******************/

    @Override
    public int getSize() {
        return Float.SIZE*2/8;
    }


    //****************** Attributes ******************//

    /**
     * Returns the geographic latitude on WGS84 ellipsiod in degrees.
     * @return the geographic latitude on WGS84 ellipsiod in degrees
     */
    public float getLatitude() {
        return latitude;
    }

    /**
     * Returns the geographic longitude on WGS84 ellipsiod in degrees.
     * @return the geographic longitude on WGS84 ellipsiod in degrees
     */
    public float getLongitude() {
        return longitude;
    }


    /****************** Data equality functions ******************/

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectGPSCoordinate))
            return false;
        ObjectGPSCoordinate castObj = (ObjectGPSCoordinate)obj;
        return castObj.latitude == latitude && castObj.longitude == longitude;
    }

    @Override
    public int dataHashCode() {
        return (int)(latitude*1000.0 + 360.0 * longitude*1000.0);
    }


    /****************** Distance function ******************/

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectGPSCoordinate castObj = (ObjectGPSCoordinate)obj;

        return (float)ReferenceEllipsoid.WGS84.distanceDegree(latitude, longitude, castObj.latitude, castObj.longitude);
    }
    
    
    /********************  Cloning ***************************/
    
    /**
     * Creates and returns a randomly modified copy of this GPS coordinates.
     * 
     * Not implemented yet!
     *
     * @param args No parameters are required to pass.
     * @return A randomly modified clone of this instance.
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) {
        throw new UnsupportedOperationException("Random cloning is not implemented in ObjectGPSCoordinate class.");
    }


    /****************** String representation ******************/

    /**
     * Returns a string representation of this abstract object.
     * Basically, this method returns the object type plus object locator
     * (or ID if locator is <tt>null</tt>) in brackets.
     * @return a string representation of this abstract object
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(super.toString());
        // Add GPS coordinates
        rtv.append(" [");
        rtv.append(latitude).append(",").append(longitude);
        rtv.append("]");
        return rtv.toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectGPSCoordinate loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectGPSCoordinate(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.latitude = serializator.readFloat(input);
        this.longitude = serializator.readFloat(input);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the data output this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, latitude) +
               serializator.write(output, longitude);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + 4 + 4;
    }

    
    /********************  Internal class for computing the geographic distance **************/
    
    /*
     * This following code is adopted from:
     * 
     * JScience - Java(TM) Tools and Libraries for the Advancement of Sciences.
     * Copyright (C) 2006 - JScience (http://jscience.org/)
     * All rights reserved.
     *
     * Permission to use, copy, modify, and distribute this software is
     * freely granted, provided that this notice is preserved.
     */

    /**
     * <p> The ReferenceEllipsoid class defines a geodetic reference ellipsoid
     *     used as a standard for geodetic measurements. The World Geodetic System
     *     1984 (WGS84) ellipsoid is the current standard for most geographic and
     *     geodetic coordinate systems, including GPS. The WGS84 ellipsoid is
     *     provided as a static instance of this class.</p>
     *
     * <p> The ellipsoid (actually an oblate spheroid) is uniquely specified by
     *     two parameters, the semimajor (or equatorial) radius and the ellipticity
     *     or flattening. In practice, the reciprocal of the flattening is
     *     specified.</p>
     *
     * <p> The ellipsoid is an approximation of the shape of the earth. Although
     *     not exact, the ellipsoid is much more accurate than a spherical
     *     approximation and is still mathematically simple. The <i>geoid</i> is
     *     a still closer approximation of the shape of the earth (intended to
     *     represent the mean sea level), and is generally specified by it's
     *     deviation from the ellipsoid.</p>
     *
     * <p> Different reference ellipsoids give more or less accurate results at
     *     different locations, so it was previously common for different nations
     *     to use ellipsoids that were more accurate for their areas. More recent
     *     efforts have provided ellipsoids with better overall global accuracy,
     *     such as the WGS84 ellipsiod, and these have now largely supplanted
     *     the others.</p>
     *
     * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
     * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
     * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
     * @version 3.0, February 18, 2006
     */
    protected static class ReferenceEllipsoid {

        /**
         * The World Geodetic System 1984 reference ellipsoid.
         */
        public static final ReferenceEllipsoid WGS84
            = new ReferenceEllipsoid(6378137.0, 298.257223563);
//        /**
//         * Geodetic Reference System 1980 ellipsoid.
//         */
//        public static final ReferenceEllipsoid GRS80
//            = new ReferenceEllipsoid(6378137.0, 298.257222101);
//        /**
//         * The World Geodetic System 1972 reference ellipsoid.
//         */
//        public static final ReferenceEllipsoid WGS72
//            = new ReferenceEllipsoid(6378135.0, 298.26);
//        /**
//         * The International 1924 reference ellipsoid, one of the earliest
//         * "global" ellipsoids.
//         */
//        public static final ReferenceEllipsoid INTERNATIONAL1924
//            = new ReferenceEllipsoid(6378388.0, 297.0);

        /** WGS84 ellipsoid parameter */
        private final double a;

        /** WGS84 ellipsoid parameter */
        private final double b;

        /** WGS84 ellipsoid parameter */
        private final double f;

        /** WGS84 ellipsoid parameter */
        private final double ea2;

        /** WGS84 ellipsoid parameter */
        private final double e;

        /** WGS84 ellipsoid parameter */
        private final double eb2;

        //private Measurable<Length> _semimajorAxis;

        //private Measurable<Length> _semiminorAxis;

        /**
         *  Constructs an instance of a reference ellipsoid.
         *
         * @param semimajorAxis The semimajor or equatorial radius of this
         * reference ellipsoid, in meters.
         * @param inverseFlattening The reciprocal of the ellipticity or flattening
         * of this reference ellipsoid (dimensionless).
         */
        public ReferenceEllipsoid(double semimajorAxis, double inverseFlattening) {
            a = semimajorAxis;
            f = 1.0 / inverseFlattening;
            b = semimajorAxis * (1.0 - f);
            ea2 = f * (2.0 - f);
            e = Math.sqrt(ea2);
            eb2 = ea2 / (1.0 - ea2);
        }

        /**
         * Returns square of x.
         * @param x the number to compute the square for
         * @return the square of the specified number
         */
        private static double sqr(final double x) {
            return x * x;
        }

        /**
         * Returns the degree angle in radians.
         * @param degree the angle in degrees
         * @return the angle in radians
         */
        private double degreeToRadian(double degree) {
            return degree * Math.PI / 180.0D;
        }        
        
        /**
         * Returns the semimajor or equatorial radius of this reference ellipsoid.
         *
         * @return The semimajor radius.
         */
        public double getSemimajorAxis() {
            return a;
        }

        /**
         * Returns the semiminor or polar radius of this reference ellipsoid.
         *
         * @return  The semiminor radius.
         */
        public double getsSemiminorAxis() {
            return b;
        }

        /**
         * Returns the flattening or ellipticity of this reference ellipsoid.
         *
         * @return The flattening.
         */
        public double getFlattening() {
            return f;
        }

        /**
         * Returns the (first) eccentricity of this reference ellipsoid.
         *
         * @return The eccentricity.
         */
        public double getEccentricity() {
            return e;
        }

        /**
         * Returns the square of the (first) eccentricity. This number is frequently
         * used in ellipsoidal calculations.
         *
         * @return The square of the eccentricity.
         */
        public double getEccentricitySquared() {
            return ea2;
        }

        /**
         * Returns the square of the second eccentricity of this reference ellipsoid.
         * This number is frequently used in ellipsoidal calculations.
         *
         * @return The square of the second eccentricity.
         */
        public double getSecondEccentricitySquared() {
            return eb2;
        }

        /**
          * Returns the <i>radius of curvature in the prime vertical</i>
          * for this reference ellipsoid at the specified latitude.
          *
          * @param phi The local latitude (radians).
          * @return The radius of curvature in the prime vertical (meters).
          */
         public double verticalRadiusOfCurvature(final double phi) {
             return a / Math.sqrt(1.0 - (ea2 * sqr(Math.sin(phi))));
         }

        /**
         *  Returns the <i>radius of curvature in the meridian<i>
         *  for this reference ellipsoid at the specified latitude.
         *
         * @param phi The local latitude (in radians).
         * @return  The radius of curvature in the meridian (in meters).
         */
        public double meridionalRadiusOfCurvature(final double phi) {
            return verticalRadiusOfCurvature(phi)
                   / (1.0 + eb2 * sqr(Math.cos(phi)));
        }

        /**
         *  Returns the meridional arc, the true meridional distance on the
         * ellipsoid from the equator to the specified latitude, in meters.
         *
         * @param phi   The local latitude (in radians).
         * @return  The meridional arc (in meters).
         */
        public double meridionalArc(final double phi) {
            final double sin2Phi = Math.sin(2.0 * phi);
            final double sin4Phi = Math.sin(4.0 * phi);
            final double sin6Phi = Math.sin(6.0 * phi);
            final double sin8Phi = Math.sin(8.0 * phi);
            final double n = f / (2.0 - f);
            final double n2 = n * n;
            final double n3 = n2 * n;
            final double n4 = n3 * n;
            final double n5 = n4 * n;
            final double n1n2 = n - n2;
            final double n2n3 = n2 - n3;
            final double n3n4 = n3 - n4;
            final double n4n5 = n4 - n5;
            final double ap = a * (1.0 - n + (5.0 / 4.0) * (n2n3) + (81.0 / 64.0) * (n4n5));
            final double bp = (3.0 / 2.0) * a * (n1n2 + (7.0 / 8.0) * (n3n4) + (55.0 / 64.0) * n5);
            final double cp = (15.0 / 16.0) * a * (n2n3 + (3.0 / 4.0) * (n4n5));
            final double dp = (35.0 / 48.0) * a * (n3n4 + (11.0 / 16.0) * n5);
            final double ep = (315.0 / 512.0) * a * (n4n5);
            return ap * phi - bp * sin2Phi + cp * sin4Phi - dp * sin6Phi + ep * sin8Phi;
        }

        /**
         *  Returns the parallel arc, the true parallel distance on the
         * ellipsoid from the meridian lambda1 to the meridian lambda2 at the specified latitude phi, in meters.
         *
         * @param phi      The local latitude (in radians).
         * @param lambda1  The source longitude (in radians).
         * @param lambda2  The destination longitude (in radians).
         * @return         The meridional arc (in meters).
         */
        public double parallelArc(double phi, double lambda1, double lambda2) {
                double radius = verticalRadiusOfCurvature(phi) * Math.cos(phi);
                double sector = Math.abs(lambda1 - lambda2);

                return radius * sector;
        }

        /**
         *  Returns the prime vertical arc, the true prime vertical distance on the
         * ellipsoid from the meridian lambda1 to the meridian lambda2 at the specified latitude phi, in meters.
         *
         * @param phi      The local latitude (in radians).
         * @param lambda1  The source longitude (in radians).
         * @param lambda2  The destination longitude (in radians).
         * @return         The meridional arc (in meters).
         */
        public double verticalArc(double phi, double lambda1, double lambda2) {
                double radius = verticalRadiusOfCurvature(phi);
                double sector = Math.abs(lambda1 - lambda2);

                return radius * sector;
        }

        /**
         *  Returns the distance in meters between two points in geographical coordinates.
         *
         * @param phi1     The source latitude (in radians).
         * @param lambda1  The source longitude (in radians).
         * @param phi2     The destination latitude (in radians).
         * @param lambda2  The destination longitude (in radians).
         * @return         The ellipsiod arc (in meters).
         */
        public double distanceRadian(double phi1, double lambda1, double phi2, double lambda2) {
                double middleLat = (phi1 + phi2) / 2.0;
                double vertDist = parallelArc(middleLat, lambda1, lambda2);
                double horzDist = Math.abs(meridionalArc(phi1) - meridionalArc(phi2));

                return Math.sqrt(sqr(vertDist) + sqr(horzDist));
        }

        /**
         *  Returns the distance in meters between two points in geographical coordinates.
         *
         * @param phi1     The source latitude (in degrees).
         * @param lambda1  The source longitude (in degrees).
         * @param phi2     The destination latitude (in degrees).
         * @param lambda2  The destination longitude (in degrees).
         * @return         The ellipsiod arc (in meters).
         */
        public double distanceDegree(double phi1, double lambda1, double phi2, double lambda2) {
                return distanceRadian(degreeToRadian(phi1), degreeToRadian(lambda1), 
                                      degreeToRadian(phi2), degreeToRadian(lambda2));
        }
    }
}
