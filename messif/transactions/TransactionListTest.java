/*
 * TransactionListTest.java
 *
 * Created on 27. duben 2005, 8:27
 */

package messif.transactions;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Class for testing correct behavior of TransactionList.
 * 
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class TransactionListTest {
    Random rand = new Random();
    int maxRound = 1000;
        
    public void test() {
        TransactionList<Integer> trans;
        List<Integer> list;                 // Original List
        
        // Initialize list with random elements
        list = new ArrayList<Integer>();
        int cnt = rand.nextInt(50) + 25;   // at least 25 elements (50 elements on average)
        for (int i = 0; i < cnt; i++) {
            list.add(rand.nextInt());
        }
        trans = new TransactionList<Integer>(new ArrayList<Integer>(list));
        
        System.out.print("After initialization: ");
        testEquality(list, trans);
        
        // Tests follow:
        maxRound = 1000;
        
        System.out.print("Testing add, set, remove: ");
        testModifByIndex(trans);
        testEquality(list, trans);
        
        
        System.out.print("Testing add, remove on objects: ");
        testDelByObj(trans);
        testEquality(list, trans);
        
                
        System.out.print("Testing addAll, removeAll, clear: ");
        testModifAll(trans);
        testEquality(list, trans);
        
                
        System.out.print("Testing retainAll: ");
        testRetainAll(trans);
        testEquality(list, trans);
        
                        
        System.out.print("Testing iterator, listIterator: ");
        testIterator(trans);
        testEquality(list, trans);
        
                        
        maxRound = 100;
        System.out.print("Testing subList: ");
        testSubList(trans);
        //System.out.print("Verifying after subList: ");
        testEquality(list, trans);
    }
    
    /** Tests folowing methods:
     *   add(index, o)     (also add(o))
     *   set(index, o)
     *   remove(index)
     */
    private void testModifByIndex(List<Integer> list) {
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).beginTransaction();
        
        List<Integer> delList = new ArrayList<Integer>();
        
        // Run sequence of tests
        for (int round = 0; round < maxRound; round ++) {
            int oper = rand.nextInt(3);             // add, set, remove
            int cnt = rand.nextInt(100) + 100;      // on average 150 elements
            
            switch (oper) {
                case 0:         // add
                    for (int i = 0; i < cnt; i++) {
                        int pos = rand.nextInt(list.size() + 1);
                        list.add(pos, rand.nextInt());
                    }
                    break;
                case 1:         // set
                    if (cnt > list.size())
                        cnt = list.size();
                    for (int i = 0; i < cnt; i++) {
                        int pos = rand.nextInt(list.size());
                        list.set(pos, rand.nextInt());
                    }
                    break;
                case 2:         // remove
                    if (cnt > list.size())
                        cnt = list.size();
                    for (int i = 0; i < cnt; i++) {
                        int pos = rand.nextInt(list.size());
                        delList.add(list.remove(pos));
                    }
                    break;
            }
        }
        
        // Readd all deleted elements
        list.addAll(delList);
        
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).rollbackTransaction();
    }
    
    /** Tests folowing methods:
     *   remove(o)
     *   add(o)
     */
    private void testDelByObj(List<Integer> list) {
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).beginTransaction();
        
        // Run sequence of tests
        for (int round = 0; round < maxRound; round ++) {
            // Get random object
            Integer obj;
            try {
                obj = list.get(rand.nextInt(list.size()));
            } catch (IllegalArgumentException e) {
                System.out.println("Exception on random: list size=" + list.size() + ", round=" + round);
                throw e;
            }
            
            // Delete it
            list.remove(obj);
            
            // Append it
            list.add(obj);
        }
        
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).rollbackTransaction();
    }
    
    /** Tests folowing methods:
     *   removeAll(c)
     *   addAll(index, c)   (also addAll(c))
     *   clear()
     */
    private void testModifAll(List<Integer> list) {
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).beginTransaction();
        
        // Run sequence of tests
        for (int round = 0; round < maxRound; round ++) {
            // Get a random list of objects
            List<Integer> rList = new ArrayList<Integer>();
            int cnt = rand.nextInt(100) + 1;
            for (int i = 0; i < cnt; i++) {
                rList.add(list.get(rand.nextInt(list.size())));
            }
            
            // Delete them all
            list.removeAll(rList);
            
            // Re-add them all
            list.addAll(rand.nextInt(list.size() + 1), rList);
        }
        
        list.clear();
        
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).rollbackTransaction();
    }

    /** Tests folowing methods:
     *   retainAll(c)
     *   addAll(index, c)   (also addAll(c))
     */
    private void testRetainAll(List<Integer> list) {
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).beginTransaction();
        
        // If the list is empty, add some random elements
        for (int i = rand.nextInt(50) + 25; i >= 0; i--) {
            list.add(rand.nextInt());
        }
        
        // Run sequence of tests
        for (int round = 0; round < maxRound; round ++) {
            // Get a random list of list's objects
            List<Integer> rList = new ArrayList<Integer>();
            int cnt = rand.nextInt(100) + 1;
            for (int i = 0; i < cnt; i++) {
                rList.add(list.get(rand.nextInt(list.size())));
            }
            
            // Retain them only & delete the others
            list.retainAll(rList);
            
            // Get a random list of random objects
            rList = new ArrayList<Integer>();
            cnt = rand.nextInt(100) + 1;
            for (int i = 0; i < cnt; i++) {
                rList.add(rand.nextInt());
            }
            
            // Readd them all
            list.addAll(rand.nextInt(list.size() + 1), rList);
        }
        
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).rollbackTransaction();
    }
    
    /** Tests folowing methods:
     *   iterator()
     *   listIterator()
     *   listIterator(index)
     */
    private void testIterator(List<Integer> list) {
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).beginTransaction();
        
        // Iterator test
        for (int round = 0; round < maxRound; ++round) {
            List<Integer> delList = new ArrayList<Integer>();
            Iterator<Integer> iter = list.iterator();
            int skip = rand.nextInt(30);
            while (iter.hasNext()) {
                Integer elem = iter.next();
                --skip;
                if (skip <= 0) {
                    skip = rand.nextInt(30);

                    delList.add(elem);
                    iter.remove();
                }
            }
            // append all deleted elements
            list.addAll(delList);
        }
        
        // ListIterator test
        for (int round = 0; round < maxRound; ++round) {
            List<Integer> delList = new ArrayList<Integer>();
            ListIterator<Integer> iter = list.listIterator(rand.nextInt(list.size()));      // Get the iterator starting from a certain position.
            while (delList.size() < list.size()) {
                int skip = rand.nextInt(30) + 1;
                Integer elem = null;
                if ( rand.nextBoolean() ? iter.hasNext() : !iter.hasPrevious() ) {          // if random bool is true, look whether there is any next element (if not use previous), by analogy the other way around.
                    while (iter.hasNext() && skip > 0) {
                        elem = iter.next();
                        --skip;
                    }
                } else {
                    while (iter.hasPrevious() && skip > 0) {
                        elem = iter.previous();
                        --skip;
                    }
                }
                
                switch (rand.nextInt(3)) {
                    case 0:         // add
                        //iter.add(rand.nextInt());
                        break;
                    case 1:         // set
                        iter.set(rand.nextInt());
                        break;
                    case 2:         // del
                        delList.add(elem);
                        iter.remove();
                        break;
                }
            }
            // append all deleted elements
            list.addAll(delList);
        }
        
        if (list instanceof TransactionList)
            ((TransactionList<Integer>)list).rollbackTransaction();
    }
    
    /** Tests folowing methods:
     *   subList(from, to)    (all methods are run on the sublist)
     */
    private void testSubList(TransactionList<Integer> trans) {
        trans.beginTransaction();
        
        for (int round = 0; round < 20; round++) {
            System.out.print(".");
            System.out.flush();
            
            // Get random subList
            int from = rand.nextInt(trans.size() / 2);
            int to = from + rand.nextInt((trans.size() - 10) / 2) + 10;
            List<Integer> list = trans.subList(from, to);
            
            // Perform tests
            //System.out.print("       Testing add, set, remove: \n");
            testModifByIndex(list);
            //System.out.print("       Testing add, remove on objects: \n");
            testDelByObj(list);
            //System.out.print("       Testing addAll, removeAll, clear: \n");
            testModifAll(list);
            //System.out.print("       Testing retainAll: \n");
            testRetainAll(list);
            //System.out.print("       Testing iterator, listIterator: \n");
            testIterator(list);
        }        
        
        trans.rollbackTransaction();
    }

    
    
    /** Decides whether lists are identical and prints a message.
     */
    private void testEquality(List<Integer> orig, TransactionList<Integer> list) {
        try {
            if (list.isTransactionRunning())
                throw new IllegalStateException("Transaction is still running.");
            
            if (orig.size() != list.size())
                throw new IllegalStateException("Different sizes.");
            
            Iterator<Integer> origIter = orig.iterator();
            Iterator<Integer> listIter = list.iterator();
            while (origIter.hasNext() && listIter.hasNext()) {
                if (!origIter.next().equals(listIter.next()))
                    throw new IllegalStateException("Elements are different.");
            }
            
        } catch (IllegalStateException e) {
            System.out.println("FAILED: " + e.getMessage());
            return;
        }
        System.out.println("OK");
    }
    
}
