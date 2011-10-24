package messif.objects.impl;

import java.util.Comparator;
import messif.objects.LocalAbstractObject;

public class ObjectLocalFeatureComparatorX implements Comparator<LocalAbstractObject> {

    public int compare (LocalAbstractObject o1, LocalAbstractObject o2)
    {
        return (int) Math.signum(((ObjectFeature) o1).getX() - ((ObjectFeature) o2).getX());
    }

}
