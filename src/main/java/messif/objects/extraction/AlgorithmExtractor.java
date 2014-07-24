/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.extraction;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import messif.algorithms.Algorithm;
import messif.objects.LocalAbstractObject;
import messif.operations.AbstractOperation;
import messif.operations.SingletonQueryOperation;
import messif.utility.Convert;

/**
 * Implementation of {@link Extractor} that creates objects by
 * executing a {@link SingletonQueryOperation}. The type of the query
 * operation as well as its parameters are specified in the constructor.
 * Note that the parameters map of the {@link ExtractorDataSource} is
 * used as the named instances map, so the parameters can be used as the
 * operation arguments.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class AlgorithmExtractor implements Extractor<LocalAbstractObject> {

    //****************** Attributes ******************//

    /** Algorithm on which to execute the operation */
    private final Algorithm algorithm;
    /** Constructor of the operation that is executed */
    private final Constructor<? extends SingletonQueryOperation> operationConstructor;
    /** Operation string arguments (converted when the operation is executed) */
    private final String[] operationArguments;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of algorithm extractor.
     * @param algorithm the algorithm on which to execute the operation
     * @param operationClass the class of the operation that is executed
     * @param operationArguments the operation string arguments (converted when the operation is executed)
     * @throws NoSuchMethodException if the operation constructor is not found for the given number of arguments
     */
    public AlgorithmExtractor(Algorithm algorithm, Class<? extends SingletonQueryOperation> operationClass, String... operationArguments) throws NoSuchMethodException {
        this.algorithm = algorithm;
        this.operationConstructor = AbstractOperation.getAnnotatedConstructor(operationClass, operationArguments.length);
        this.operationArguments = operationArguments;
    }


    //****************** Extractor implementation ******************//

    @Override
    public LocalAbstractObject extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
        SingletonQueryOperation op;
        Object[] parameters;
        try {
            parameters = Convert.parseTypesFromString(
                    operationArguments,
                    operationConstructor.getParameterTypes(),
                    false, // do not use var-args, since number of constructor parameters is given
                    0,
                    dataSource.getParameterMap()
            );
            op = operationConstructor.newInstance(parameters);
        } catch (Exception e) {
            throw new ExtractorException("Cannot create operation", e);
        }
        try {
            op = algorithm.executeOperation(op);
        } catch (Exception e) {
            throw new ExtractorException("Cannot execute operation", e);
        }

        LocalAbstractObject object = (LocalAbstractObject)op.getAnswerObject();
        if (object == null)
            throw new ExtractorException("No object found for " + Arrays.toString(parameters));

        return object;
    }

    @Override
    public Class<? extends LocalAbstractObject> getExtractedClass() {
        return LocalAbstractObject.class;
    }

}
