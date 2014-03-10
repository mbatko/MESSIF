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
package messif.objects;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.utility.Convert;
import messif.utility.ModifiableParametric;
import messif.utility.ParametricBase;

/**
 * Extension of the standard {@link MetaObject} that allows to store (in addition
 * to encapsulated {@link LocalAbstractObject}s) additional named parameters too.
 * A parameter can be any {@link Serializable} object. If the stored objects
 * implement also {@link messif.objects.nio.BinarySerializable}, it is used
 * for serialization.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class LocalAbstractObjectParametric extends LocalAbstractObject implements ModifiableParametric {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** Pattern for parsing param comment lines */
    private static final Pattern paramCommentPattern = Pattern.compile("^#param \"(.*?)(?<!\\\\)\" (\\S+)\\s+(.*)", Pattern.DOTALL);


    //****************** Attributes ******************//

    /** Additional parameters for this meta object */
    private Map<String, Serializable> additionalParameters;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectParametric.
     * A new unique object ID is generated and the
     * object's key is set to <tt>null</tt>.
     * @param additionalParameters additional parameters for this meta object
     */
    public LocalAbstractObjectParametric(Map<String, ? extends Serializable> additionalParameters) {
        this.additionalParameters = (additionalParameters == null || additionalParameters.isEmpty()) ? null : new HashMap<String, Serializable> (additionalParameters);
    }

    /**
     * Creates a new instance of MetaObjectParametric.
     * A new unique object ID is generated and the
     * object's key is set to the specified key.
     * @param objectKey the key to be associated with this object
     * @param additionalParameters additional parameters for this meta object
     */
    public LocalAbstractObjectParametric(AbstractObjectKey objectKey, Map<String, ? extends Serializable> additionalParameters) {
        super(objectKey);
        this.additionalParameters = (additionalParameters == null || additionalParameters.isEmpty()) ? null : new HashMap<String, Serializable> (additionalParameters);
    }

    /**
     * Creates a new instance of MetaObjectParametric.
     * A new unique object ID is generated and a
     * new {@link AbstractObjectKey} is generated for
     * the specified <code>locatorURI</code>.
     * @param locatorURI the locator URI for the new object
     * @param additionalParameters additional parameters for this meta object
     */
    public LocalAbstractObjectParametric(String locatorURI, Map<String, ? extends Serializable> additionalParameters) {
        super(locatorURI);
        this.additionalParameters = (additionalParameters == null || additionalParameters.isEmpty()) ? null : new HashMap<String, Serializable>(additionalParameters);
    }


    //****************** Parametric implementation ******************//

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

    /**
     * @inheritDoc
     * @throws ClassCastException if the given value is not {@link Serializable}
     */
    @Override
    public Serializable setParameter(String name, Object value) throws ClassCastException {
        if (additionalParameters == null)
            additionalParameters = new HashMap<String, Serializable>();
        return additionalParameters.put(name, (Serializable)value);
    }

    @Override
    public Serializable removeParameter(String name) {
        if (additionalParameters == null)
            return null;
        return additionalParameters.remove(name);
    }

    /**
     * {@inheritDoc }
     * 
     * Note that {@link messif.utility.Parametric parameters} that are {@link messif.utility.Clearable clearable}
     * are {@link messif.utility.Clearable#clearSurplusData() cleared} and the other non-primitive
     * parameters are <em>removed</em> from the parameters.
     */
    @Override
    public void clearSurplusData() {
        super.clearSurplusData();
        if (additionalParameters != null)
            ParametricBase.clearSurplusData(additionalParameters.values().iterator(), true, true);
    }


    //****************** Text stream I/O helper methods for parametric ******************//

    @Override
    protected boolean parseObjectComment(String line) throws IllegalArgumentException {
        if (super.parseObjectComment(line))
            return true;
        Matcher matcher = paramCommentPattern.matcher(line);
        if (!matcher.matches())
            return false;
        try {
            if (additionalParameters == null)
                additionalParameters = new HashMap<String, Serializable>();
            additionalParameters.put(matcher.group(1),
                    Convert.stringToType(matcher.group(3), Convert.getClassForName(matcher.group(2), Serializable.class)));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found: " + e.getMessage());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot instantiate value: " + e.getMessage());
        }
        return true;
    }

    @Override
    protected final void writeData(OutputStream stream) throws IOException {
        if (additionalParameters != null) {
            StringBuilder str = new StringBuilder("#param \"");
            for (Map.Entry<String, ? extends Serializable> entry : additionalParameters.entrySet()) {
                str.replace(8, str.length(), entry.getKey());
                str.append("\" ").append(entry.getValue().getClass().getName());
                str.append(' ').append(Convert.typeToString(entry.getValue())).append('\n');
                stream.write(str.toString().getBytes());
            }
        }
        writeDataImpl(stream);
    }

    /**
     * Store this object's data to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    protected abstract void writeDataImpl(OutputStream stream) throws IOException;


    //************ Protected methods of BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectParametric loaded from binary input.
     *
     * @param input the input to read the MetaObjectParametric from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected LocalAbstractObjectParametric(BinaryInput input, BinarySerializator serializator) throws IOException {
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

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    protected int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
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

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    protected int getBinarySize(BinarySerializator serializator) {
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
