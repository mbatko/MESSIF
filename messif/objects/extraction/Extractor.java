/*
 * Extractor
 */

package messif.objects.extraction;

import java.io.IOException;
import messif.objects.LocalAbstractObject;

/**
 * Interface for extractors that can create {@link LocalAbstractObject objects}
 * from binary data.
 *
 * @param <T> the type of {@link LocalAbstractObject object} that is extracted by this extractor
 * @author xbatko
 */
public interface Extractor<T extends LocalAbstractObject> {
    /**
     * Extracts the {@link LocalAbstractObject} from the specified binary data.
     * @param dataSource the source of binary data for the extraction
     * @return a new instance of object extracted from the binary data
     * @throws ExtractorException if the extractor encountered problem
     *          extracting the object from the binary data
     * @throws IOException if there was a problem reading data from the {@code dataSource}
     */
    public T extract(ExtractorDataSource dataSource) throws ExtractorException, IOException;
}
