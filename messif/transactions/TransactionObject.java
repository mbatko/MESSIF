/*
 * TransactionObject.java
 *
 * Created on 5. kveten 2005, 14:42
 */

package messif.transactions;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides a transaction encapsulation on any object.
 * 
 * @param <E> the type of encapsulated object
 * @author xbatko
 */
public class TransactionObject<E> implements Serializable {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Stored object for encapsulation */
    protected final E encapsulatedObject;

    /** Active transaction flag */
    protected transient boolean transactionActive;
    
    /** Flag storing the switch between array and normal class */
    protected final boolean isArrayClass;
    
    /** Stored state of primitive fields of this encapsulated object */
    protected final Map<String, Object> objectPrimitiveFieldsState;

    /** Stored state of object fields wrapped in TransactionObject of this encapsulated object */
    protected final Map<String, TransactionObject<Object>> objectEncapsulatedFieldsState;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of TransactionObject.
     * @param encapsulatedObject the object to encapsulate
     */
    public TransactionObject(E encapsulatedObject) {
        this.encapsulatedObject = encapsulatedObject;
        this.isArrayClass = encapsulatedObject.getClass().isArray();
        this.objectPrimitiveFieldsState = new HashMap<String, Object>();
        this.objectEncapsulatedFieldsState = new HashMap<String, TransactionObject<Object>>();
    }
    

    //****************** Data access ******************//

    /**
     * Returns the encapsulated object.
     * @return the encapsulated object
     */
    public E get() {
        synchronized (encapsulatedObject) {
            return encapsulatedObject;
        }
    }
    

    /****************** Transactions ******************/
    
    
    /**
     * Returns <tt>true</tt> if there is a transaction running currently.
     * @return <tt>true</tt> if there is a transaction running currently
     */
    public boolean isTransactionRunning() {
        return transactionActive;
    }
    
    /**
     * Start a new transaction.
     * @param blocking flag whether the start of a new transaction is blocking. We cannot run more transactions at a time.
     *
     * @return <tt>true</tt> if the transaction has been started or
     *         <tt>false</tt> if there is an active transaction and we would block or
     *         there is an active transaction but the waiting for its end was terminated
     * @throws IllegalAccessException 
     */
    public synchronized boolean beginTransaction(boolean blocking) throws IllegalAccessException {
        // If another transaction is running
        if (transactionActive) {
            if (!blocking)
                return false;
            
            // Block until transaction ends
            while (isTransactionRunning()) {
                try { wait(); }
                catch (InterruptedException e) { return false; }
            }
        }
        
        // Set the flag
        transactionActive = true;
        
        synchronized (encapsulatedObject) {
            // Copy all the object attributes
            for (Class objClass = encapsulatedObject.getClass(); objClass != null; objClass = objClass.getSuperclass())
                for (Field objField : objClass.getDeclaredFields())
                    storeField(objField, blocking);
        }
        
        return true;
    } 
    
    /** Start a new transaction. 
     *
     * @return Returns true if the transaction has been started.
     *         Returns false if there is an active transaction that has been started earlier.
     */
    public synchronized boolean beginTransaction() throws IllegalAccessException {
        return beginTransaction(false);
    } 
    
    /** End transaction of pivot processing, commiting changes
     */
    public synchronized void commitTransaction() {
        // Clear the auxiliary primitive list
        objectPrimitiveFieldsState.clear();
        
        // Send commit transaction on all encapsulated fields and clear the list
        for (TransactionObject<Object> tranField : objectEncapsulatedFieldsState.values())
            tranField.commitTransaction();
        objectEncapsulatedFieldsState.clear();
        
        // Reset the flag
        transactionActive = false;
        
        // Inform others
        notifyAll();
    }
    
    /** End transaction of pivot processing, undoing changes
     */
    public synchronized void rollbackTransaction() {
        // Recover previous state of the encapsulated object
        synchronized (encapsulatedObject) {
            for (String fieldName : objectPrimitiveFieldsState.keySet())
                try {
                    restoreField(encapsulatedObject.getClass().getDeclaredField(fieldName));
                } catch (NoSuchFieldException e) {} // ignore (cannot happen)
                
            for (TransactionObject tranField : objectEncapsulatedFieldsState.values())
                tranField.rollbackTransaction();
        }
        
        // Clear the auxiliary lists
        objectPrimitiveFieldsState.clear();
        objectEncapsulatedFieldsState.clear();
        
        // Reset the flag
        transactionActive = false;
        
        // Inform others
        notifyAll();
    }
    
    protected synchronized void storeField(Field field, boolean blocking) throws IllegalAccessException {
        // Encapsulate the field attribute
        encapsulateStoreObject(
                field.getName(),
                field.get(encapsulatedObject), 
                field.getModifiers(), 
                blocking
        );
    }
    
    protected synchronized void encapsulateStoreObject(String name, Object object, int modifiers, boolean blocking) throws IllegalAccessException {
        // Ignore static attributes
        if (Modifier.isStatic(modifiers))
            return;
        
        // Check if primitive type
        if (!object.getClass().isPrimitive() && !object.getClass().equals(String.class)) {
            // Field is a regular object or array, encapsulate into transaction
            TransactionObject obj = new TransactionObject<Object>(object);
            obj.beginTransaction(blocking);
        } else if (!Modifier.isFinal(modifiers))// Ignore final primitive values
            objectPrimitiveFieldsState.put(name, object);
    }

    protected synchronized void restoreField(Field field) {
    
    }

}
