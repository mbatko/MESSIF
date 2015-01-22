/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.algorithms.impl;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import messif.algorithms.Algorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectMap;
import messif.objects.impl.ObjectIntVectorL1;
import messif.utility.CoreApplication;

/**
 *
 * @author Michal Batko <batko@fi.muni.cz>
 */
public class TestVarargAlgorithm extends Algorithm {
    private final Object[] args;
    @AlgorithmConstructor(description = "test", arguments = {"vararg"})
    public TestVarargAlgorithm(String a, String... args) throws IllegalArgumentException {
        super("Test algotithm");
        this.args = args;
        System.out.println(Arrays.toString(args));
    }

    public static void testObjectFactory() throws InvocationTargetException {
        LocalAbstractObject.TextStreamFactory<MetaObjectMap> factory = new LocalAbstractObject.TextStreamFactory<>(
                MetaObjectMap.class, true, null, new Object[] { "moje, test" }
        );
        System.out.println(factory.create("moje;messif.objects.impl.ObjectIntVectorL1\n1,2,3,4,5"));
    }

    public static void main(String[] args) throws Exception {
        System.setIn(new ByteArrayInputStream((
                "actions = alg\n"
              + "alg = algorithmStart\n"
              + "alg.param.1 = " + TestVarargAlgorithm.class.getName() + "\n"
              + "alg.param.2 = jedna\n"
              + "alg.param.3 = dva\n"
//              + "alg.param.4 = tri\n"
                ).getBytes()));
        CoreApplication.main(new String[] {"-"});
        testObjectFactory();
    }
}
