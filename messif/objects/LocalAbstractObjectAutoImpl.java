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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import messif.utility.Convert;

/**
 * This class eases the task of implementing data read/write methods.
 * The data fields of the subclass needs to be marked by overriding the {@link #getDataFields} method.
 *
 * If the newly created object cannot be descendant of this class, the static methods for
 * reading/writing can be used.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class LocalAbstractObjectAutoImpl extends LocalAbstractObject {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of LocalAbstractObjectAutoImpl.
     * New object ID is automatically created.
     */
    protected LocalAbstractObjectAutoImpl() {
    }


    //****************** Text-stream serialization ******************//

    /**
     * Creates a new instance of object from a text stream.
     * @param stream the text stream to read one object from
     * @throws EOFException is thrown when the end-of-file is reached
     * @throws IOException if there is an error during reading from the given stream;
     * @throws IllegalArgumentException if the text stream contains invalid values for this object
     */
    protected LocalAbstractObjectAutoImpl(BufferedReader stream) throws EOFException, IOException, IllegalArgumentException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);

        // Read object data into specific fields
        readAttributesFromStream(line, getAttributesRegexp(), getArrayItemsRegexp(), this, getDataFields());
    }       

    /**
     * Returns the character that separates attributes in a text stream.
     * @return the character that separates attributes in a text stream
     */
    protected char getAttributesSeparator() {
        return ';';
    }

    /**
     * Returns the regular expression used to separate attributes in a text stream.
     * Defaults to {@link #getAttributesSeparator()} plus any number of spaces.
     * @return the regular expression used to separate attributes in a text stream
     */
    protected String getAttributesRegexp() {
        return getAttributesSeparator() + "\\p{Space}*";
    }

    /**
     * Returns the character that separates items of an array attribute.
     * @return the character that separates items of an array attribute
     */
    protected char getArrayItemsSeparator() {
        return ' ';
    }

    /**
     * Returns the regular expression used to separate items of an array attribute.
     * Defaults to {@link #getArrayItemsSeparator()} plus any number of spaces.
     * @return the regular expression used to separate items of an array attribute
     */
    protected String getArrayItemsRegexp() {
        return getArrayItemsSeparator() + "\\p{Space}*";
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        writeAttributesToStream(stream, getAttributesSeparator(), getArrayItemsSeparator(), this, getDataFields());
    }


    //****************** Size function ******************//

    @Override
    public int getSize() throws IllegalArgumentException {
        try {
            int rtv = 0;

            for (Field field : getDataFields()) {
                Class<?> fieldClass = field.getType();
                if (fieldClass.isArray())
                    rtv += (Convert.getPrimitiveTypeSize(fieldClass.getComponentType()) * Array.getLength(field.get(this)))/8;
                else if (fieldClass.equals(String.class))
                    rtv += (Character.SIZE * ((String)field.get(this)).length())/8;
                else rtv += Convert.getPrimitiveTypeSize(fieldClass)/8;
            }

            return rtv;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }


    //****************** Equality driven by object data ******************//

    @Override
    public boolean dataEquals(Object obj) {
        try {
            for (Field field : getDataFields()) {
                // Get field values (if the objects are incompatible, exception is thrown)
                Object ourField = field.get(this);
                Object objField = field.get(obj);

                // Check if field data are equal (special handling for arrays)
                if (field.getType().isArray()) {
                    // Check if array lengths match
                    int length = Array.getLength(ourField);
                    if (length != Array.getLength(objField))
                        return false;
                    for (int i = 0; i < length; i++)
                        if (!Array.get(ourField, i).equals(Array.get(objField, i)))
                            return false;
                } else if (!ourField.equals(objField))
                    return false;
            }
            return true;
        } catch (IllegalAccessException e) {
            // Thrown if the provided object is incompatible with this one
            return false;
        } catch (IllegalArgumentException e) {
            // Thrown if the provided object is incompatible with this one
            return false;
        } catch (NullPointerException e) {
            // Thrown if there is a null value in some field
            return false;
        }
    }

    @Override
    public int dataHashCode() throws IllegalArgumentException {
        try {
            int rtv = 0;
            for (Field field : getDataFields()) {
                // Get field value
                Object ourField = field.get(this);
                // Compute hash code
                if (ourField != null)
                    rtv += ourField.hashCode();
            }
            return rtv;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }


    //****************** Clonning ******************//

    /** The clone method reflection accessor */
    private static final Method cloneMethod;
    static { // Initialized for the cloneMethod static field
        try {
            cloneMethod = Object.class.getDeclaredMethod("clone");
            cloneMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // This will never happen since Object has a clone method.
            throw new RuntimeException(e);
        }
    }

    /** 
     * Creates and returns a randomly modified copy of this object. 
     * One randomly selected field is modified. If the selected field is array, one randomly selected
     * item of the array is modified.
     *
     * @param args two objects with the miminal and the maximal possible values and optional third integer parameter with index of the field to modify
     * @return a randomly modified clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        // Get a clone of this object
        LocalAbstractObject rtv = clone();
        
        // Clone all data fields
        try {
            // Get the data fields descriptor array
            Field[] fields = getDataFields();
            // Get the modified field index - either the last (odd) argument or choose one randomly
            int modifiedFieldIndex = (args.length >= 3)?(Integer)args[2]:((int)(Math.random() * fields.length));
            for (int i = 0; i < fields.length; i++) {
                // Get field value
                Object fieldData = fields[i].get(rtv);
                if (fieldData == null)
                    continue;

                // Get field type
                Class<?> fieldClass = fields[i].getType();
                
                // Clone the data if it is not a primitive type (which doesn't need clonning)
                if (!fieldClass.isPrimitive())
                    fieldData = cloneMethod.invoke(fieldData);

                // Make the random adjustment if this is the selected field
                if (i == modifiedFieldIndex) {
                    if (fieldClass.isArray()) {
                        // We have array thus a random index is selected
                        int arrayIndex = (int)(Math.random() * Array.getLength(fieldData));
                        Array.set(fieldData, arrayIndex, Convert.getRandomValue(
                                Array.get(fields[i].get(args[0]), arrayIndex),
                                Array.get(fields[i].get(args[1]), arrayIndex)
                        ));
                    } else fieldData = Convert.getRandomValue(fields[i].get(args[0]), fields[i].get(args[1]));
                }

                // Set the field to clonned value
                fields[i].set(rtv, fieldData);
            }

            return rtv;
        } catch (IllegalAccessException e) {
            throw new CloneNotSupportedException("Can't clone data field: Illegal access to " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new CloneNotSupportedException("Can't clone data field: " + e.getCause());
        } catch (IndexOutOfBoundsException e) {
            throw new CloneNotSupportedException("Invalid minimal/maximal data while data field clonning");
        }
    }


    //****************** Serialization implementation methods ******************//

    /**
     * Helper method for writing object primitive/array attributes to a text stream.
     * If the attribute is a primitive type, the value of that attribute is written.
     * If the attribute is an instance of some class, {@link Object#toString toString} method result is written.
     * If the attribute is an array, a <code>arrayItemsSeparator</code>-separated list of items is written.
     *
     * @param stream the stream to output the text to
     * @param attributesSeparator the character written to separate attributes
     * @param arrayItemsSeparator the character written to separate items of an array attribute
     * @param dataObject the object whose data are written
     * @param dataFields the list of <code>dataObject</code> attribute fields to write to the stream
     * @throws IOException if there was an error writing to the stream
     * @throws IllegalArgumentException if one of the specified fields is invalid
     */
    public static void writeAttributesToStream(OutputStream stream, char attributesSeparator, char arrayItemsSeparator, LocalAbstractObject dataObject, Field... dataFields) throws IOException, IllegalArgumentException {
        // Print all specified attributes
        for (int i = 0; i < dataFields.length; i++) {
            // Write separator
            if (i > 0)
                stream.write(attributesSeparator);

            // Get attribute for the spefied field
            Object attribute;
            try {
                attribute = dataFields[i].get(dataObject);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e.toString());
            }

            // Null values are written as empty
            if (attribute == null)
                continue;

            // Get class of this attribute
            if (attribute.getClass().isArray()) {
                int length = Array.getLength(attribute);
                for (int j = 0; j < length; j++) {
                    if (j > 0)
                        stream.write(arrayItemsSeparator);
                    stream.write(Array.get(attribute, j).toString().getBytes());
                }
            } else stream.write(attribute.toString().getBytes());
        }
    }

    /**
     * Helper method for reading object primitive/array attributes from a text stream.
     * If the field is a primitive type, the value of that attribute is read into respective attribute.
     * If the field is an instance of some class, a static <code>Object valueOf(String value)</code> method is used to create a respective attribute object.
     * If the field is an array, a <code>arrayItemsSeparator</code>-separated list of items is read into respective attribute array.
     *
     * @param line a line of text representing the data
     * @param attributesRegexp the regular expression that is used to separate attributes
     * @param arrayItemsRegexp the regular expression that is used to separate items of arrays
     * @param dataObject the object whose data are read
     * @param dataFields the list of <code>dataObject</code> attribute fields to read from the stream
     * @throws IOException if there was an error reading from the stream
     * @throws IllegalArgumentException if one of the specified fields is invalid or the value specified for a field can't be converted to correct type
     */
    public static void readAttributesFromStream(String line, String attributesRegexp, String arrayItemsRegexp, LocalAbstractObject dataObject, Field... dataFields) throws IOException, IllegalArgumentException {
        // Split the line
        String[] attributes = line.trim().split(attributesRegexp, dataFields.length);
        if (attributes.length != dataFields.length)
            throw new IllegalArgumentException("There was not enough attributes to create " + dataObject.getClass().getSimpleName());

        // Process fields
        for (int i = 0; i < dataFields.length; i++)
            try {
                Class<?> fieldClass = dataFields[i].getType();
                if (attributes[i].length() == 0) {
                    // The value is empty text, set the attribute to null
                    dataFields[i].set(dataObject, null);
                } else if (fieldClass.isArray()) {
                    // The field is array, split the values and fill them into array
                    String[] itemStrings = attributes[i].split(arrayItemsRegexp);
                    
                    // Shift to the class of the components
                    fieldClass = fieldClass.getComponentType();
                    // Create new instance of array and fill its items
                    Object fieldArray = Array.newInstance(fieldClass, itemStrings.length);
                    for (int j = 0; j < itemStrings.length; j++)
                        Array.set(fieldArray, j, Convert.stringToType(itemStrings[j], fieldClass, null));
                    // Finally, assign the created array to the respective attribute
                    dataFields[i].set(dataObject, fieldArray);
                } else {
                    // It is a primitive value (or class with valueOf)
                    dataFields[i].set(dataObject, Convert.stringToType(attributes[i], fieldClass, null));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Can't read value for " + dataObject.getClass().getSimpleName() + "." + dataFields[i].getName(), e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Can't read value for " + dataObject.getClass().getSimpleName() + "." + dataFields[i].getName(), e);
            } 
    }


    //****************** Data fields handling ******************//

    /**
     * Returns the list of fields that should be automatically managed by AutoImpl class as data.
     * Recommended usage in a subclass:
     * <code>
     *    private final static Field[] fields = getFieldsFor????(<the subclass name>.class, ...);
     *    protected Field[] getDataFields() {
     *        return fields;
     *    }
     * </code>
     * See {@link #getFieldsForNames} for explanation how to get field lists.
     *
     * @return the list of fields that hold data
     */
    protected abstract Field[] getDataFields();

    /**
     * Returns a list of fields of the specified class that match the provided names.
     * @param forClass the class to get fields for
     * @param fieldName the list of field names
     * @return a list of fields of the specified class that match the provided names
     * @throws IllegalArgumentException if there is no attribute field with the specified name
     */
    protected static Field[] getFieldsForNames(Class<? extends LocalAbstractObject> forClass, String... fieldName) throws IllegalArgumentException {
        // Prepare fields
        Field[] fields = new Field[fieldName.length];
        try {
            // Get fileds with the specified names
            for (int i = 0; i < fieldName.length; i++) {
                fields[i] = forClass.getDeclaredField(fieldName[i]);
                fields[i].setAccessible(true); // Allow access
            }
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(e.toString());
        }
        
        return fields;
    }

}
