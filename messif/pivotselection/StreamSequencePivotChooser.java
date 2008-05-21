/*
 * StreamSequencePivotChooser.java
 *
 * Created on 16. kveten 2008, 10:05
 */

package messif.pivotselection;

import java.io.IOException;
import java.io.Serializable;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.StreamGenericAbstractObjectIterator;


/**
 *
 * @author  xbatko
 */
public class StreamSequencePivotChooser extends AbstractPivotChooser implements Serializable {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Stream to read pivots from */
    protected final StreamGenericAbstractObjectIterator<LocalAbstractObject> stream;


    /****************** Construcotrs ******************/

    /**
     * Creates a new instance of StreamSequencePivotChooser.
     * @param objClass the class of objects to read from the stream
     * @param fileName the file to read objects from
     * @throws IllegalArgumentException if the provided class does not have a proper "stream" constructor
     * @throws IOException if there was an error opening the file
     */
    public StreamSequencePivotChooser(Class<? extends LocalAbstractObject> objClass, String fileName) throws IllegalArgumentException, IOException {
        stream = new StreamGenericAbstractObjectIterator<LocalAbstractObject>(objClass, fileName);
    }
    
    
    /****************** Overrides ******************/

    /**
     * This method carries out the actual pivot selection, that is, read specified
     * number of objects from the stream.
     * @param count number of pivots to generate
     * @param sampleSetIterator ignored by this chooser, since the pivots are provided externally
     */
    protected void selectPivot(int count, GenericObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        for (;count > 0;count--)
            addPivot(stream.next());
    }

}
