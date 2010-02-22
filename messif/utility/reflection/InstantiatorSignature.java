/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.utility.reflection;

/**
 * Parse a given string signature and provide methods for creating
 * {@link Instantiator}s that match the signature or instances directly.
 * @author xbatko
 */
public class InstantiatorSignature {
    /** List of arguments parsed from the signature, i.e. coma separated values in parenthesis */
    private final String[] args;
    /** Parsed class of the constructor/method/field */
    private final Class<?> objectClass;
    /** Parsed name of the constructor/method/field */
    private final String name;

    /**
     * Creates a new instance of ParsedSignature.
     * The internal information about arguments and names are initialized.
     * @param signature a fully specified constructor/method/field signature
     */
    public InstantiatorSignature(String signature) {
        // Find left parenthesis
        int leftParenthesis = signature.indexOf('(');
        Class<?> constructorClass = null;

        // Parse arguments and constructor class is appropriate
        if (leftParenthesis == -1) {
            this.args = null;
        } else {
            // Check right parenthesis (must be last character)
            if (signature.charAt(signature.length() - 1) != ')')
                throw new IllegalArgumentException("Missing or invalid closing parenthesis: " + signature);
            // Parse arguments
            if (leftParenthesis == signature.length() - 2)
                this.args = new String[0];
            else
                this.args = signature.substring(leftParenthesis + 1, signature.length() - 1).split("\\s*,\\s*");
            // Remove arguments from the signature (already parsed)
            signature = signature.substring(0, leftParenthesis);
            try {
                // Parse signature as constructor
                constructorClass = Class.forName(signature.substring(0, leftParenthesis));
            } catch (ClassNotFoundException ignore) {
            }
        }

        // Parse class and name
        if (constructorClass == null) {
            // Try method or field (i.e. last dot position denotes method/field name
            int dotPos = signature.lastIndexOf('.');
            if (dotPos == -1)
                throw new IllegalArgumentException("Class not found: " + signature);
            try {
                this.objectClass = Class.forName(signature.substring(0, dotPos));
            } catch (ClassNotFoundException ignore) {
                throw new IllegalArgumentException("Class not found: " + signature.substring(0, dotPos));
            }
            this.name = signature.substring(dotPos + 1);
        } else {
            // We have parsed constructor class
            this.objectClass = constructorClass;
            this.name = null;
        }
    }

    /**
     * Return arguments parsed by this signature.
     * If a field signature was parsed, <tt>null</tt> is returned.
     * @return arguments parsed by this signature
     */
    public String[] getParsedArgs() {
        return args;
    }

    /**
     * Returns the parsed declaring class of the constructor/method/field.
     * @return the parsed declaring class
     */
    public Class<?> getParsedClass() {
        return objectClass;
    }

    /**
     * Returns the parsed declaring class of the constructor/method/field.
     * Generic-safe typecast is performed using the given {@code checkClass}.
     *
     * @param <T> the super class of the declaring class agains which to check
     * @param checkClass the super class of the declaring class agains which to check
     * @return the parsed declaring class
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getParsedClass(Class<? extends T> checkClass) {
        if (checkClass.isAssignableFrom(objectClass))
            return (Class<T>)objectClass; // This cast IS checked on the previous line
        else
            throw new IllegalArgumentException("Cannot cast " + objectClass + " to " + checkClass);
    }

    /**
     * Returns the parsed name of the method/field.
     * If a constructor signature was parsed, <tt>null</tt> is returned.
     * @return the parsed name
     */
    public String getParsedName() {
        return name;
    }

    /**
     * Returns <tt>true</tt> if a constructor signature was parsed.
     * @return <tt>true</tt> if a constructor signature was parsed
     */
    public boolean isConstructorSignature() {
        return args != null && name == null;
    }

    /**
     * Returns <tt>true</tt> if a method signature was parsed.
     * @return <tt>true</tt> if a method signature was parsed
     */
    public boolean isMethodSignature() {
        return args != null && name != null;
    }

    /**
     * Returns <tt>true</tt> if a field signature was parsed.
     * @return <tt>true</tt> if a field signature was parsed
     */
    public boolean isFieldSignature() {
        return args == null;
    }

    /**
     * Creates instantiator for the parsed signature.
     * @param <T> the class of instances that will be created by the returned instantiator
     * @param checkClass the class of instances that will be created by the returned instantiator
     * @return a new instantiator for the parsed signature
     * @throws IllegalArgumentException if the instantiator cannot be created
     */
    public <T> Instantiator<T> createInstantiator(Class<? extends T> checkClass) throws IllegalArgumentException {
        if (args == null) {
            return new FieldInstantiator<T>(checkClass, objectClass, name);
        } else if (name == null) {
            return new ConstructorInstantiator<T>(getParsedClass(checkClass), args.length);
        } else {
            return new FactoryMethodInstantiator<T>(checkClass, objectClass, name, args.length);
        }
    }

}
