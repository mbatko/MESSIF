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
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.utility.ModifiableParametric;

/**
 * This class encapsulates a PittPatt recognition descriptor.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectFacePittPattDescriptor extends ObjectByteVector implements ModifiableParametric<Serializable> {
    /** class id for serialization */
    private static final long serialVersionUID = 2L;

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
            Logger.getLogger(ObjectFacePittPattDescriptor.class.getName()).log(Level.WARNING, "Cannot load PittPatt library: {0}", (Object)e);
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


    //****************** Attributes ******************//

    /** Encapsulated {@link Map} that provides the parameter values */
    private Map<String, Serializable> additionalParameters;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFacePittPattDescriptor from provided data.
     * @param data the PittPatt data for recognition
     */
    public ObjectFacePittPattDescriptor(byte[] data) {
        this(null, data);
    }

    /**
     * Creates a new instance of ObjectFacePittPattDescriptor from provided data.
     * @param additionalParameters additional parameters for this meta object
     * @param data the PittPatt data for recognition
     */
    public ObjectFacePittPattDescriptor(Map<String, ? extends Serializable> additionalParameters, byte[] data) {
        super(data);
        this.additionalParameters = (additionalParameters == null) ? null : new HashMap<String, Serializable>(additionalParameters);
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
        this(stream, null);
    }

    /**
     * Creates a new instance of ObjectFacePittPattDescriptor from stream.
     * @param stream the stream to read object's data from
     * @param additionalParameters additional parameters for this meta object
     * @throws IOException if there was an error during reading from the given stream
     * @throws EOFException when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line read from given stream does not consist of comma-separated or space-separated numbers
     * @throws IllegalArgumentException if the read data is not valid
     */
    public ObjectFacePittPattDescriptor(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters) throws IOException, EOFException, NumberFormatException, IllegalArgumentException {
        super(stream, true);
        this.additionalParameters = (additionalParameters == null) ? null : new HashMap<String, Serializable>(additionalParameters);
    }


    //****************** Attribute access method ******************//

    /**
     * Returns whether this object has a PittPatt face template for recognition.
     * If <tt>false</tt> is returned, this object cannot be used to compute distance.
     * @return <tt>true</tt> if this object has a PittPatt face template for recognition
     */
    public boolean hasFace() {
        return data != null && data.length > 0;
    }


    //****************** Parametric interface implementation ******************//

    @Override
    public int getParameterCount() {
        return additionalParameters != null ? additionalParameters.size() : 0;
    }

    @Override
    public Collection<String> getParameterNames() {
        if (additionalParameters == null)
            return Collections.emptyList();
        return Collections.unmodifiableCollection(additionalParameters.keySet());
    }

    @Override
    public boolean containsParameter(String name) {
        return additionalParameters != null && additionalParameters.containsKey(name);
    }

    @Override
    public Serializable getParameter(String name) {
        return additionalParameters != null ? additionalParameters.get(name) : null;
    }

    @Override
    public Serializable getRequiredParameter(String name) throws IllegalArgumentException {
        Serializable parameter = getParameter(name);
        if (parameter == null)
            throw new IllegalArgumentException("The parameter '" + name + "' is not set");
        return parameter;
    }

    @Override
    public <T> T getRequiredParameter(String name, Class<? extends T> parameterClass) throws IllegalArgumentException, ClassCastException {
        return parameterClass.cast(getRequiredParameter(name));
    }

    @Override
    public <T> T getParameter(String name, Class<? extends T> parameterClass, T defaultValue) {
        Object value = getParameter(name);
        return value != null && parameterClass.isInstance(value) ? parameterClass.cast(value) : defaultValue; // This cast IS checked by isInstance
    }

    @Override
    public <T> T getParameter(String name, Class<? extends T> parameterClass) {
        return getParameter(name, parameterClass, null);
    }

    @Override
    public Map<String, ? extends Serializable> getParameterMap() {
        if (additionalParameters == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(additionalParameters);
    }

    @Override
    public Serializable removeParameter(String name) {
        if (additionalParameters == null)
            return null;
        return additionalParameters.remove(name);
    }

    @Override
    public void setParameter(String name, Serializable value) {
        if (additionalParameters == null)
            additionalParameters = new HashMap<String, Serializable>();
        additionalParameters.put(name, value);
    }


    //****************** Text file store/retrieve methods ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        writeByteHexString(data, stream);
        stream.write('\n');
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
        ObjectFacePittPattDescriptor castObj = (ObjectFacePittPattDescriptor)obj;
        if (!hasFace())
            throw new IllegalStateException("No face present in " + this);
        if (!castObj.hasFace())
            throw new IllegalStateException("No face present in " + obj);
        float score = getSimilarityImpl(data, castObj.data);
        if (score < 0) // Zero is 1% false acceptance rate, one is 0.1% false acceptance rate
            return 1f;
        if (score < 1)
            return 1f - score * 0.5f;
        return (1f -  score / 20.0f) * 0.5f;
    }

    @Override
    public float getMaxDistance() {
        return 1f;
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


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFacePittPattDescriptor loaded from binary input.
     *
     * @param input the input to read the ObjectFacePittPattDescriptor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    public ObjectFacePittPattDescriptor(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        int additionaParametersCount = serializator.readInt(input);
        if (additionaParametersCount == -1) {
            this.additionalParameters = null;
        } else {
            Map<String, Serializable> internalMap = new HashMap<String, Serializable>(additionaParametersCount);
            for (; additionaParametersCount > 0; additionaParametersCount--)
                internalMap.put(serializator.readString(input), serializator.readObject(input, Serializable.class));
            this.additionalParameters = internalMap;
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        if (additionalParameters == null) {
            size += serializator.write(output, -1);
        } else {
            size += serializator.write(output, additionalParameters.size());
            for (Map.Entry<String, ? extends Serializable> entry : additionalParameters.entrySet()) {
                size += serializator.write(output, entry.getKey());
                size += serializator.write(output, entry.getValue());
            }
        }

        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        if (additionalParameters == null) {
            size += serializator.getBinarySize(-1);
        } else {
            size += serializator.getBinarySize(additionalParameters.size());
            for (Map.Entry<String, ? extends Serializable> entry : additionalParameters.entrySet()) {
                size += serializator.getBinarySize(entry.getKey());
                size += serializator.getBinarySize(entry.getValue());
            }
        }

        return size;
    }

}
