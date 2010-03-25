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
package messif.transactions;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.ListIterator;
import java.util.AbstractList;
import java.util.NoSuchElementException;

/**
 * An implementation of List interface which takes a list as an argument and provides transactional behavior on it.
 * Specifically, this wrapping class provides three methods for initiating, commiting and aborting a transaction.
 * After beginTransaction() call, all changes in the passed instance of list are caught and stored for the sake of any
 * upcomming roll-back. When commit takes places all internal arrays of changes get emptied. When roll-back is requested,
 * the internal arrays are taken and all elements that were added/deleted get deleted/added to get back to the original
 * state of the list before beginTransaction() was issued.
 *
 * Notice: This class inherits from AbstractList only for the reason of having fully function subList() method. Other 
 *         methods are overwritten.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class TransactionList<E> extends AbstractList<E> implements List<E> {
    /** List on which transactions can be run */
    private List<E> monitoredList = null;
    /** Flag denoting whether a transaction is active */
    private boolean transactionActive = false;
    /** Flag telling whether changes were made to the list during the last transaction */
    private boolean changedInTransaction = false;
    
    /** Elements added/deleted/modified in the list */
    private List<ListMember> changes = new ArrayList<ListMember>();

    /** Creates a new instance of TransactionList */
    public TransactionList(List<E> list) {
        monitoredList = list;
    }
    
    
    /**********************************
     * TRANSACTIONS
     **********************************/
        
    /** Start a new transaction.
     *
     * @param blocking Flag whether the start of a new transaction is blocking. We cannot run more transactions at a time.
     *
     * @return Returns true if the transaction has been started.
     *         Returns false if there is an active transaction and we would block.
     *         Returns false if there is an active transaction but the waiting for its end was terminated.
     */
    public synchronized boolean beginTransaction(boolean blocking) {
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
        // Set the changed flag to false
        changedInTransaction = false;
        
        return true;
    } 
    
    /** Start a new transaction. 
     *
     * @return Returns true if the transaction has been started.
     *         Returns false if there is an active transaction that has been started earlier.
     */
    public synchronized boolean beginTransaction() {
        return beginTransaction(false);
    } 
    
    /** Returns current transaction state
     */
    public boolean isTransactionRunning() {
        return transactionActive;
    }
    
    /** End transaction of pivot processing, commiting changes
     */
    public synchronized void commitTransaction() {
        // Set the flag to report that changes were made
        changedInTransaction = (changes.size() != 0);
        // Clear the auxiliary list
        changes.clear();
        // Reset the flag
        transactionActive = false;
    }
    
    /** End transaction of pivot processing, undoing changes
     */
    public synchronized void rollbackTransaction() {
        // Recover the previous list's state using caught changes
        for (int i = changes.size() - 1; i >= 0; i--) {
            ListMember change = changes.get(i);
            
            if (change.isAddOperation()) {
                // We inserted a new element, delete it.
                monitoredList.remove(change.isLastElement() ? monitoredList.size() - 1 : change.getIndex());
            } else if (change.isDelOperation()) {
                // We deleted an object, re-insert it.
                monitoredList.add(change.isLastElement() ? monitoredList.size() : change.getIndex(), change.getValue());
            } else if (change.isSetOperation()) {
                // We modified an object, undo the modification
                monitoredList.set(change.getIndex(), change.getValue());
            }
        }
        
        // Clear the auxiliary list
        changes.clear();
        // Reset the flag
        transactionActive = false;
        // Set the flag to report that no changes were made
        changedInTransaction = false;
    }
    
    /** Reports whether the list was changed during the last transaction or not.
     * The state is retained until a new transaction is started.
     * If the list was changed and the transaction was rolled back, returns false, 
     * which is obvious.
     * When a transaction is pending, returns true/false based on the changes made or not.
     */
    public boolean changedDuringTransaction() {
        if (isTransactionRunning())
            return changes.size() != 0;
        else
            return changedInTransaction;
    }

    
    /**********************************
     * LIST STUB METHODS
     **********************************/
    
    // Query Operations

    /**
     * Returns the number of elements in this list.  If this list contains
     * more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this list.
     */
    public int size() { return monitoredList.size(); }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements.
     */
    public boolean isEmpty() { return monitoredList.isEmpty(); }

    /**
     * 
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested.
     * @return <tt>true</tt> if this list contains the specified element.
     * @throws ClassCastException if the type of the specified element
     * 	       is incompatible with this list (optional).
     * @throws NullPointerException if the specified element is null and this
     *         list does not support null elements (optional).
     */
    public boolean contains(Object o) { return monitoredList.contains(o); }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence.
     */
    public Iterator<E> iterator() {
        return new TransactionIterator(monitoredList.listIterator());
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence.  Obeys the general contract of the
     * <tt>Collection.toArray</tt> method.
     *
     * @return an array containing all of the elements in this list in proper
     *	       sequence.
     * @see java.util.Arrays#asList(Object[])
     */
    public Object[] toArray() { return monitoredList.toArray(); }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence; the runtime type of the returned array is that of the
     * specified array.  Obeys the general contract of the
     * <tt>Collection.toArray(Object[])</tt> method.
     *
     * @param a the array into which the elements of this list are to
     *		be stored, if it is big enough; otherwise, a new array of the
     * 		same runtime type is allocated for this purpose.
     * @return  an array containing the elements of this list.
     * 
     * @throws ArrayStoreException if the runtime type of the specified array
     * 		  is not a supertype of the runtime type of every element in
     * 		  this list.
     * @throws NullPointerException if the specified array is <tt>null</tt>.
     */
    public <T> T[] toArray(T[] a) { return monitoredList.toArray(a); }


    // Modification Operations

    /**
     * Appends the specified element to the end of this list (optional
     * operation). <p>
     *
     * Lists that support this operation may place limitations on what
     * elements may be added to this list.  In particular, some
     * lists will refuse to add null elements, and others will impose
     * restrictions on the type of elements that may be added.  List
     * classes should clearly specify in their documentation any restrictions
     * on what elements may be added.
     *
     * @param o element to be appended to this list.
     * @return <tt>true</tt> (as per the general contract of the
     *            <tt>Collection.add</tt> method).
     * 
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not
     * 		  supported by this list.
     * @throws ClassCastException if the class of the specified element
     * 		  prevents it from being added to this list.
     * @throws NullPointerException if the specified element is null and this
     *           list does not support null elements.
     * @throws IllegalArgumentException if some aspect of this element
     *            prevents it from being added to this list.
     */
    public boolean add(E o) {
        boolean res = monitoredList.add(o);
        if (transactionActive)
            changes.add(new ListMember(ListMember.LAST_ELEMENT, ListMember.ADD, null));
        return res;
    }

    /**
     * Removes the first occurrence in this list of the specified element 
     * (optional operation).  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index i
     * such that <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt> (if
     * such an element exists).
     *
     * @param o element to be removed from this list, if present.
     * @return <tt>true</tt> if this list contained the specified element.
     * @throws ClassCastException if the type of the specified element
     * 	          is incompatible with this list (optional).
     * @throws NullPointerException if the specified element is null and this
     *            list does not support null elements (optional).
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is
     *		  not supported by this list.
     */
    public boolean remove(Object o) {
        // Get the index of the object o
        int index = monitoredList.indexOf(o);
        if (index != -1) {
            // Delete the object
            E obj = monitoredList.remove(index);
            // Store changes
            if (transactionActive)
                changes.add(new ListMember(index, ListMember.DEL, obj));
            return true;
        }
        return false;
    }


    // Bulk Modification Operations

    /**
     * 
     * Returns <tt>true</tt> if this list contains all of the elements of the
     * specified collection.
     *
     * @param  c collection to be checked for containment in this list.
     * @return <tt>true</tt> if this list contains all of the elements of the
     * 	       specified collection.
     * @throws ClassCastException if the types of one or more elements
     *         in the specified collection are incompatible with this
     *         list (optional).
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this list does not support null
     *         elements (optional).
     * @throws NullPointerException if the specified collection is
     *         <tt>null</tt>.
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) { return containsAll(c); }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator (optional operation).  The behavior of this
     * operation is unspecified if the specified collection is modified while
     * the operation is in progress.  (Note that this will occur if the
     * specified collection is this list, and it's nonempty.)
     *
     * @param c collection whose elements are to be added to this list.
     * @return <tt>true</tt> if this list changed as a result of the call.
     * 
     * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
     *         not supported by this list.
     * @throws ClassCastException if the class of an element in the specified
     * 	       collection prevents it from being added to this list.
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this list does not support null
     *         elements, or if the specified collection is <tt>null</tt>.
     * @throws IllegalArgumentException if some aspect of an element in the
     *         specified collection prevents it from being added to this
     *         list.
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        // Add all elements
        boolean res = monitoredList.addAll(c);
        // Store changes
        if (transactionActive) {
            for (int i = 0; i < c.size(); i++) {
                changes.add(new ListMember(ListMember.LAST_ELEMENT, ListMember.ADD, null));
            }
        }
        return res;
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list at the specified position (optional operation).  Shifts the
     * element currently at that position (if any) and any subsequent
     * elements to the right (increases their indices).  The new elements
     * will appear in this list in the order that they are returned by the
     * specified collection's iterator.  The behavior of this operation is
     * unspecified if the specified collection is modified while the
     * operation is in progress.  (Note that this will occur if the specified
     * collection is this list, and it's nonempty.)
     *
     * @param index index at which to insert first element from the specified
     *	            collection.
     * @param c elements to be inserted into this list.
     * @return <tt>true</tt> if this list changed as a result of the call.
     * 
     * @throws UnsupportedOperationException if the <tt>addAll</tt> method is
     *		  not supported by this list.
     * @throws ClassCastException if the class of one of elements of the
     * 		  specified collection prevents it from being added to this
     * 		  list.
     * @throws NullPointerException if the specified collection contains one
     *           or more null elements and this list does not support null
     *           elements, or if the specified collection is <tt>null</tt>.
     * @throws IllegalArgumentException if some aspect of one of elements of
     *		  the specified collection prevents it from being added to
     *		  this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *		  &lt; 0 || index &gt; size()).
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        // Add all elements
        boolean res = monitoredList.addAll(index, c);
        // Store changes
        if (transactionActive) {
            for (int i = 0; i < c.size(); i++) {
                changes.add(new ListMember(index + i, ListMember.ADD, null));  // (index+i) is necessary since object of c are added to monitoredList in the same order.
            }
        }
        return res;
    }

    /**
     * Removes from this list all the elements that are contained in the
     * specified collection (optional operation).
     *
     * @param c collection that defines which elements will be removed from
     *          this list.
     * @return <tt>true</tt> if this list changed as a result of the call.
     * 
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
     * 		  is not supported by this list.
     * @throws ClassCastException if the types of one or more elements
     *            in this list are incompatible with the specified
     *            collection (optional).
     * @throws NullPointerException if this list contains one or more
     *            null elements and the specified collection does not support
     *            null elements (optional).
     * @throws NullPointerException if the specified collection is
     *            <tt>null</tt>.
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean removeAll(Collection<?> c) {
	boolean modified = false;
        
        // Go through all elements to be deleted and store changes
        ListIterator<E> iter = monitoredList.listIterator();
        while (iter.hasNext()) {
            int idx = iter.nextIndex();
            E obj = iter.next();
            
            if (c.contains(obj)) {
                iter.remove();
                modified = true;
                // Store changes
                if (transactionActive)
                    changes.add(new ListMember(idx, ListMember.DEL, obj));
            }
        }
	return modified;
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection (optional operation).  In other words, removes
     * from this list all the elements that are not contained in the specified
     * collection.
     *
     * @param c collection that defines which elements this set will retain.
     * 
     * @return <tt>true</tt> if this list changed as a result of the call.
     * 
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method
     * 		  is not supported by this list.
     * @throws ClassCastException if the types of one or more elements
     *            in this list are incompatible with the specified
     *            collection (optional).
     * @throws NullPointerException if this list contains one or more
     *            null elements and the specified collection does not support
     *            null elements (optional).
     * @throws NullPointerException if the specified collection is
     *         <tt>null</tt>.
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public boolean retainAll(Collection<?> c) {
	boolean modified = false;

        ListIterator<E> iter = monitoredList.listIterator();
        while (iter.hasNext()) {
            int idx = iter.nextIndex();
            E obj = iter.next();
            
	    if (!c.contains(obj)) {
		iter.remove();
		modified = true;
                // Store changes
                if (transactionActive)
                    changes.add(new ListMember(idx, ListMember.DEL, obj));
	    }
	}
	return modified;
    }

    /**
     * Removes all of the elements from this list (optional operation).  This
     * list will be empty after this call returns (unless it throws an
     * exception).
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> method is
     * 		  not supported by this list.
     */
    public void clear() {
        if (!transactionActive) {
            monitoredList.clear();
            return;
        }
        
        // Store the last index of changes (for the reason of storing delete changes in the reverse order)
        int index = changes.size();
        // Go trough all elements to clear.
        for (Iterator<E> iter = monitoredList.iterator(); iter.hasNext(); ) {
            // Store changes (in reverse order)
            changes.add(index, new ListMember(ListMember.LAST_ELEMENT, ListMember.DEL, iter.next()));
        }
        // Clear the list
        // Catching exceptions is needed here because we save changes before elements get deleted in the monitored list
        try {
            monitoredList.clear();
        } catch (UnsupportedOperationException e) {
            // Delete all previously stored changes
            changes.subList(index, changes.size()).clear();
            throw e;
        }
    }


    // Comparison and hashing

    /**
     * Compares the specified object with this list for equality.  Returns
     * <tt>true</tt> if and only if the specified object is also a list, both
     * lists have the same size, and all corresponding pairs of elements in
     * the two lists are <i>equal</i>.  (Two elements <tt>e1</tt> and
     * <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
     * e1.equals(e2))</tt>.)  In other words, two lists are defined to be
     * equal if they contain the same elements in the same order.  This
     * definition ensures that the equals method works properly across
     * different implementations of the <tt>List</tt> interface.
     *
     * @param o the object to be compared for equality with this list.
     * @return <tt>true</tt> if the specified object is equal to this list.
     */
    public boolean equals(Object o) { return monitoredList.equals(o); }

    /**
     * Returns the hash code value for this list.  The hash code of a list
     * is defined to be the result of the following calculation:
     * <pre>
     *  hashCode = 1;
     *  Iterator i = list.iterator();
     *  while (i.hasNext()) {
     *      Object obj = i.next();
     *      hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
     *  }
     * </pre>
     * This ensures that <tt>list1.equals(list2)</tt> implies that
     * <tt>list1.hashCode()==list2.hashCode()</tt> for any two lists,
     * <tt>list1</tt> and <tt>list2</tt>, as required by the general
     * contract of <tt>Object.hashCode</tt>.
     *
     * @return the hash code value for this list.
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    public int hashCode() { return monitoredList.hashCode(); }


    // Positional Access Operations

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of element to return.
     * @return the element at the specified position in this list.
     * 
     * @throws IndexOutOfBoundsException if the index is out of range (index
     * 		  &lt; 0 || index &gt;= size()).
     */
    public E get(int index) { return monitoredList.get(index); }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element (optional operation).
     *
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position.
     * 
     * @throws UnsupportedOperationException if the <tt>set</tt> method is not
     *		  supported by this list.
     * @throws    ClassCastException if the class of the specified element
     * 		  prevents it from being added to this list.
     * @throws    NullPointerException if the specified element is null and
     *            this list does not support null elements.
     * @throws    IllegalArgumentException if some aspect of the specified
     *		  element prevents it from being added to this list.
     * @throws    IndexOutOfBoundsException if the index is out of range
     *		  (index &lt; 0 || index &gt;= size()).
     */
    public E set(int index, E element) {
        // Change the element
        E orig = monitoredList.set(index, element);
        // Store change
        if (transactionActive)
            changes.add(new ListMember(index, ListMember.SET, orig));
        return orig;
    }

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation).  Shifts the element currently at that position
     * (if any) and any subsequent elements to the right (adds one to their
     * indices).
     *
     * @param index index at which the specified element is to be inserted.
     * @param element element to be inserted.
     * 
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not
     *		  supported by this list.
     * @throws    ClassCastException if the class of the specified element
     * 		  prevents it from being added to this list.
     * @throws    NullPointerException if the specified element is null and
     *            this list does not support null elements.
     * @throws    IllegalArgumentException if some aspect of the specified
     *		  element prevents it from being added to this list.
     * @throws    IndexOutOfBoundsException if the index is out of range
     *		  (index &lt; 0 || index &gt; size()).
     */
    public void add(int index, E element) {
        // Add the element
        monitoredList.add(index, element);
        // Store change
        if (transactionActive)
            changes.add(new ListMember(index, ListMember.ADD, null));
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.
     *
     * @param index the index of the element to removed.
     * @return the element previously at the specified position.
     * 
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is
     *		  not supported by this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *            &lt; 0 || index &gt;= size()).
     */
    public E remove(int index) {
        // Remove the element
        E orig = monitoredList.remove(index);
        // Store change
        if (transactionActive)
            changes.add(new ListMember(index, ListMember.DEL, orig));
        return orig;
    }


    // Search Operations

    /**
     * Returns the index in this list of the first occurrence of the specified
     * element, or -1 if this list does not contain this element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for.
     * @return the index in this list of the first occurrence of the specified
     * 	       element, or -1 if this list does not contain this element.
     * @throws ClassCastException if the type of the specified element
     * 	       is incompatible with this list (optional).
     * @throws NullPointerException if the specified element is null and this
     *         list does not support null elements (optional).
     */
    public int indexOf(Object o) { return monitoredList.indexOf(o); }

    /**
     * Returns the index in this list of the last occurrence of the specified
     * element, or -1 if this list does not contain this element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for.
     * @return the index in this list of the last occurrence of the specified
     * 	       element, or -1 if this list does not contain this element.
     * @throws ClassCastException if the type of the specified element
     * 	       is incompatible with this list (optional).
     * @throws NullPointerException if the specified element is null and this
     *         list does not support null elements (optional).
     */
    public int lastIndexOf(Object o) { return monitoredList.lastIndexOf(o); }


    // List Iterators

    /**
     * Returns a list iterator of the elements in this list (in proper
     * sequence).
     *
     * @return a list iterator of the elements in this list (in proper
     * 	       sequence).
     */
    public ListIterator<E> listIterator() {
        return new TransactionListIterator(0, monitoredList.listIterator(0));
    }

    /**
     * Returns a list iterator of the elements in this list (in proper
     * sequence), starting at the specified position in this list.  The
     * specified index indicates the first element that would be returned by
     * an initial call to the <tt>next</tt> method.  An initial call to
     * the <tt>previous</tt> method would return the element with the
     * specified index minus one.
     *
     * @param index index of first element to be returned from the
     *		    list iterator (by a call to the <tt>next</tt> method).
     * @return a list iterator of the elements in this list (in proper
     * 	       sequence), starting at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index
     *         &lt; 0 || index &gt; size()).
     */
    public ListIterator<E> listIterator(int index) {
        return new TransactionListIterator(index, monitoredList.listIterator(index));
    }

    // View

    // subList is not implemented. It is inherited from the AbstractList class, which returns a corresponding sub-list
    // but the backend of it is this class (TransactionList). So, any updates are handled correctly and changes are stored
    // automatically.
//    public List<E> subList(int fromIndex, int toIndex);
    
    
    
    private class TransactionIterator implements Iterator<E> {
        /** Iterator of the monitored list */
        protected ListIterator<E> monitoredIter;
        
        // The following two attributes are used to carry two pieces of information important to
        // transaction feature of the TransactionList. They are used to store correct index of modification
        // and the valid object that gets deleted or replaced.
        // Their states are independent of states of internal attributes (cursor, lastRet) of Iterator 
        // in AbstractList class.
        
        /** Object returned by next() or by previous() (see TransactionListIterator) */
        protected E returnedObject = null;
        
      	/**
	 * Index of element returned by most recent call to next or
	 * previous.  Reset to -1 if this element is deleted by a call
	 * to remove or get invalid after a call to add.
	 */
	protected int lastRet = -1;
        
        
        
        /** Constructor for encapsulating an iterator of the monitored list */
        public TransactionIterator(ListIterator<E> iter) {
            monitoredIter = iter;
        }
        
        /** Iterator interface methods */
        
	public boolean hasNext() {
            return monitoredIter.hasNext();
	}

	public E next() {
            lastRet = monitoredIter.nextIndex();        // This element is gonna be returned by next().
            returnedObject = monitoredIter.next();

            return returnedObject;
	}

	public void remove() {                
            // Remove the object via Iterator
            monitoredIter.remove();
            // Store changes
            if (transactionActive)
                changes.add(new ListMember(lastRet, ListMember.DEL, returnedObject));
            // Set the last returned index to undefined and the returned object as well
            lastRet = -1;
            returnedObject = null;
	}
    }

    private class TransactionListIterator extends TransactionIterator implements ListIterator<E> {
        
	TransactionListIterator(int index, ListIterator<E> iter) {
            super(iter);
	}

	public boolean hasPrevious() {
            return monitoredIter.hasPrevious();
	}

        public E previous() {
            returnedObject = monitoredIter.previous();
            lastRet = monitoredIter.nextIndex();        // save index of the element just returned

            return returnedObject;
        }

	public int nextIndex() {
            return monitoredIter.nextIndex();
	}

	public int previousIndex() {
            return monitoredIter.previousIndex();
	}

	public void set(E o) {
            // Update the object via Iterator
            monitoredIter.set(o);
            // Store changes
            if (transactionActive)
                changes.add(new ListMember(lastRet, ListMember.SET, returnedObject));
            // Do not change the index of last returned object
	}

	public void add(E o) {
            // Get the index where the add() takes place
            int index = monitoredIter.nextIndex();
            // Add the object via Iterator
            monitoredIter.add(o);
            // Store changes
            if (transactionActive)
                changes.add(new ListMember(index, ListMember.ADD, null));
            // Set the last returned index to undefined and the returned object as well
            lastRet = -1;
            returnedObject = null;
	}
    }
    
    
    /**********************************
     * INTERNALS
     **********************************/
        
    /** Private class for storing pairs of indexes to the list and list members */
    private class ListMember {
        public static final int LAST_ELEMENT = -1;
        public static final int ADD = 0;
        public static final int DEL = 1;
        public static final int SET = 2;
        
        private int index;
        private int oper;
        private E value;
        
        public ListMember(int index, int oper, E value) {
            this.index = index;
            this.oper = oper;
            this.value = value;
        }
        
        public int getIndex() {
            return index;
        }
        
        public E getValue() {
            return value;
        }
        
        public boolean isAddOperation() {
            return (oper == ADD);
        }
        
        public boolean isDelOperation() {
            return (oper == DEL);
        }
        
        public boolean isSetOperation() {
            return (oper == SET);
        }
        
        /** Returns true if the stored changed was made on the last element in the list
         */
        public boolean isLastElement() {
            return (index == LAST_ELEMENT);
        }
    }
}
