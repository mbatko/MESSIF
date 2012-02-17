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
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * This class encapsulates a Luxand FaceSDK recognition descriptor.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectFaceLuxandDescriptor extends ObjectByteVector {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** External library initialization ******************//

    /** Flag that represents the state of the native functions */
    private static final boolean isLibraryLoaded;

    static {
        boolean libraryLoaded;
        try {
            System.loadLibrary("LuxandDescriptor");
            int err = activateLibrary("2454FBA2A0CEFC4C5D0EFF26AA136E29D265CF84C69BF95057CFBA254F0C504EB4F94EBA3DFBAA6EE3C068CDAF94EF45CAB2190F76A208EBF88C698C9FA665C6");
            if (err == 0) {
                libraryLoaded = true;
            } else {
                libraryLoaded = false;
                Logger.getLogger(ObjectFaceLuxandDescriptor.class.getName()).log(Level.WARNING, "Cannot activate Luxand FaceSDK library: err code {0}", err);
            }
        } catch (UnsatisfiedLinkError e) {
            Logger.getLogger(ObjectFaceLuxandDescriptor.class.getName()).log(Level.WARNING, "Cannot load Luxand FaceSDK library: {0}", (Object)e);
            libraryLoaded = false;
        }
        isLibraryLoaded = libraryLoaded;
    }

    /**
     * Returns <tt>true</tt> if the Luxand FaceSDK library was successfully loaded.
     * If this method returns <tt>false</tt>, the {@link #getDistanceImpl(messif.objects.LocalAbstractObject, float) distance}
     * method will throw exception.
     * @return <tt>true</tt> if the Luxand FaceSDK library was successfully loaded
     */
    public static boolean isIsLibraryLoaded() {
        return isLibraryLoaded;
    }


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFaceLuxandDescriptor from provided data.
     * @param data the Luxand data for recognition
     */
    public ObjectFaceLuxandDescriptor(byte[] data) {
        super(data);
    }

    /**
     * Creates a new instance of ObjectFaceLuxandDescriptor from stream.
     * @param stream the stream to read object's data from
     * @throws IOException if there was an error during reading from the given stream
     * @throws EOFException when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line read from given stream does not consist of comma-separated or space-separated numbers
     * @throws IllegalArgumentException if the read data is not valid
     */
    public ObjectFaceLuxandDescriptor(BufferedReader stream) throws IOException, EOFException, NumberFormatException, IllegalArgumentException {
        super(stream, true);
    }

    /**
     * Creates a new instance of ObjectFaceLuxandDescriptor loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectFaceLuxandDescriptor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    public ObjectFaceLuxandDescriptor(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Text file store/retrieve methods ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        writeByteHexString(data, stream, '\n');
    }


    //****************** Distance function ******************//

    /**
     * Distance function for Luxand descriptors.
     * Note that this function is <em>not</em> a metric.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold.
     *         Otherwise the returned value is not guaranteed to be exact, but in this respect the returned value
     *         must be greater than the threshold distance.
     * @throws IllegalStateException if the Luxand library was not loaded
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) throws IllegalStateException {
        if (!isLibraryLoaded)
            throw new IllegalStateException("Cannot compute distance - the FaceSDK library was not loaded");
        return 1f - getSimilarityImpl(data, ((ObjectFaceLuxandDescriptor)obj).data);
    }


    //****************** External library methods ******************//

    /**
     * Implementation of the similarity measure in an external Luxand library.
     * @param obj1 the first object for which to compute the distance
     * @param obj2 the second object for which to compute the distance
     * @return the distance between obj1 and obj2
     */
    private static native float getSimilarityImpl(byte[] obj1, byte[] obj2);

    /**
     * Activation method of the Luxand library.
     * This is called only once when the library is loaded.
     * @param activationKey the license key
     * @return zero if the activation was successful, a Luxand error code is returned otherwise
     */
    private static native int activateLibrary(String activationKey);
}
