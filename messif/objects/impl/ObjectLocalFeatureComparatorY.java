package messif.objects.impl;

import java.util.Comparator;
import messif.objects.LocalAbstractObject;

public class ObjectLocalFeatureComparatorY implements Comparator<LocalAbstractObject> {

    public int compare (LocalAbstractObject o1, LocalAbstractObject o2)
    {
        return (int) Math.signum(((ObjectFeature) o1).getY() - ((ObjectFeature) o2).getY());
    }
    
}
