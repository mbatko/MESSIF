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
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import messif.objects.LocalAbstractObject;
import messif.objects.LocalAbstractObjectAutoImpl;

/**
 * This class holds an advanced descriptor of face identity robust to variations in pose and illumination conditions.
 * Its strong points are compact representation of facial identity, and high-speed and accurate matching.
 *
 * Extraction remarks:
 * The face images should be normalized before feature extraction.
 * The positions of two eyes should be at (24,16) and (24,31) in the scaled image(56 pixels in height and 46 pixels in width).
 *
 * This descriptor supports scalable represenation of facial feature vector. If you wish to change the dimensionality of
 * the vector, set extraction parameters and matching parameters. The allowed range of the extraction parameters is from
 * 24 to 63 for FourierFeature, and from 0 to 63 for CompositeFeature. The dimensions of the feature vectors in matching
 * must not exceed those of the extracted feature vectors.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectFaceMPEG7AdvancedDescriptor extends LocalAbstractObjectAutoImpl {
    /** Class id for serialization. */
    private static final long serialVersionUID = 2L;
        
    //****************** Attributes ******************//

    /** Flag whether the composite features extension is used */
    protected int extensionFlag;
    /** Face fourier features */
    protected int fourierFeature[];
    /** Face central fourier features */
    protected int centralFourierFeature[];
    /** Face composite features */
    protected int compositeFeature[];
    /** Face central composite features */
    protected int centralCompositeFeature[];

    /** List of fields for the automatic implementation */
    private final static Field[] fields = getFieldsForNames(ObjectFaceMPEG7AdvancedDescriptor.class,
            "extensionFlag",
            "fourierFeature",
            "centralFourierFeature",
            "compositeFeature",
            "centralCompositeFeature");


    //****************** Attributes ******************//

    /**
     * Creates a new instance of ObjectFaceMPEG7AdvancedDescriptor with the given data.
     * 
     * @param extensionFlag the flag whether the composite features extension is used
     * @param fourierFeature the face fourier feature vector
     * @param centralFourierFeature the face central fourier feature vector
     * @param compositeFeature the face composite feature vector
     * @param centralCompositeFeature the face central composite feature vector
     */
    public ObjectFaceMPEG7AdvancedDescriptor(int extensionFlag, int[] fourierFeature, int[] centralFourierFeature, int[] compositeFeature, int[] centralCompositeFeature) {
        this.extensionFlag = extensionFlag;
        
        this.fourierFeature = fourierFeature.clone();
        this.centralFourierFeature = centralFourierFeature.clone();
        this.compositeFeature = compositeFeature.clone();
        this.centralCompositeFeature = centralCompositeFeature.clone();
    }

    /**
     * Creates a new instance of ObjectFaceMPEG7AdvancedDescriptor from stream.
     * @param stream the text stream to read one object from
     * @throws EOFException is thrown when the end-of-file is reached
     * @throws IOException if there is an error during reading from the given stream;
     * @throws IllegalArgumentException if the text stream contains invalid values for this object
     */
    public ObjectFaceMPEG7AdvancedDescriptor(BufferedReader stream) throws EOFException, IOException, IllegalArgumentException {
        super(stream);
    }


    //****************** AutoImpl overrides ******************//

    @Override
    protected Field[] getDataFields() {
        return fields;
    }


    //****************** Distance function ******************//

    /** Weights for the fourier features used in the metric function */
    private static double fourierFeatureWeight[]={1.798081e+001, 6.795809e+000, 5.260557e+000, 3.571177e+000, 3.210394e+000, 2.620535e+000, 2.228538e+000, 2.245985e+000, 1.873352e+000, 1.752597e+000, 1.579577e+000, 1.535900e+000, 1.470712e+000, 1.357127e+000, 1.320019e+000, 1.199018e+000, 1.202859e+000, 1.110766e+000, 1.071195e+000, 1.118303e+000, 1.052979e+000, 1.039572e+000, 9.863494e-001, 9.816257e-001, 9.460065e-001, 9.096861e-001, 9.109148e-001, 8.869273e-001, 8.894892e-001, 8.541995e-001, 8.412409e-001, 7.966286e-001, 8.063573e-001, 7.892404e-001, 7.772063e-001, 7.542841e-001, 7.468508e-001, 7.433246e-001, 7.265750e-001, 7.042917e-001, 7.073574e-001, 6.868307e-001, 7.005851e-001, 6.784358e-001, 6.789272e-001, 6.728051e-001, 6.527822e-001, 6.503947e-001, 6.421589e-001, 6.359661e-001, 6.127091e-001, 6.251180e-001, 6.132433e-001, 6.099040e-001, 6.081947e-001, 5.901578e-001, 5.893763e-001, 5.794061e-001, 5.847705e-001, 5.779241e-001, 5.724861e-001, 5.634636e-001, 5.492996e-001, 5.504729e-001, 5.362453e-001, 5.449241e-001, 5.424120e-001, 5.362397e-001, 5.258737e-001, 5.190015e-001, 5.193156e-001, 5.308644e-001, 5.121491e-001, 5.026056e-001, 5.102172e-001, 4.975407e-001, 4.988692e-001, 5.082213e-001, 4.846210e-001, 4.769746e-001, 4.737751e-001, 4.752826e-001, 4.699635e-001, 4.659951e-001, 4.579795e-001, 4.584783e-001, 4.597742e-001, 4.488027e-001, 4.407231e-001, 4.473011e-001, 4.383086e-001, 4.298794e-001, 4.318727e-001, 4.280597e-001, 4.170053e-001, 4.107486e-001};
    /** Weights for the central fourier features used in the metric function */
    private static double centralFourierFeatureWeight[]={8.324790e+000, 5.539323e+000, 4.597177e+000, 2.626791e+000, 2.365626e+000, 1.980555e+000, 1.815129e+000, 1.617538e+000, 1.507013e+000, 1.395178e+000, 1.259098e+000, 1.232240e+000, 1.155442e+000, 1.087494e+000, 1.059872e+000, 1.024983e+000, 1.006763e+000, 9.428568e-001, 9.249697e-001, 8.752996e-001, 8.762097e-001, 8.300131e-001, 8.221489e-001, 7.912385e-001, 7.670310e-001, 7.467623e-001, 7.630908e-001, 7.285124e-001, 7.365235e-001, 7.226215e-001, 7.041821e-001, 6.960367e-001, 6.749651e-001, 6.760364e-001, 6.712101e-001, 6.582886e-001, 6.697725e-001, 6.330772e-001, 6.534700e-001, 6.211591e-001, 6.302857e-001, 6.338757e-001, 5.915847e-001, 6.177064e-001, 5.754482e-001, 5.901015e-001, 5.789449e-001, 5.666484e-001, 5.614080e-001, 5.775324e-001, 5.712751e-001, 5.541222e-001, 5.500063e-001, 5.468431e-001, 5.372846e-001, 5.350750e-001, 5.427137e-001, 5.075605e-001, 5.080101e-001, 5.166686e-001, 5.095477e-001, 4.911559e-001, 4.896533e-001, 4.889275e-001, 4.933548e-001, 4.860367e-001, 4.816273e-001, 4.880481e-001, 4.763366e-001, 4.721736e-001, 4.706686e-001, 4.713619e-001, 4.555874e-001, 4.517800e-001, 4.695715e-001, 4.391101e-001, 4.423742e-001, 4.406809e-001, 4.321210e-001, 4.413082e-001, 4.361082e-001, 4.341595e-001, 4.281849e-001, 4.152913e-001, 4.165745e-001, 4.203469e-001, 4.176656e-001, 4.087874e-001, 4.031964e-001, 4.057135e-001, 3.986217e-001, 3.925480e-001, 4.035966e-001, 3.896141e-001, 3.866248e-001, 3.961899e-001};
    /** Weights for the composite features used in the metric function */
    private static double compositeFeatureWeight[]={1.638624e+001, 6.362853e+000, 4.895604e+000, 3.484656e+000, 2.970820e+000, 2.424310e+000, 2.201797e+000, 2.073587e+000, 1.771210e+000, 1.629766e+000, 1.515658e+000, 1.435645e+000, 1.389437e+000, 1.335467e+000, 1.217928e+000, 1.173319e+000, 1.098414e+000, 1.056305e+000, 1.082416e+000, 9.982374e-001, 9.993173e-001, 9.663332e-001, 9.537141e-001, 9.290286e-001, 8.903819e-001, 8.735456e-001, 8.808295e-001, 8.344901e-001, 8.279413e-001, 8.091108e-001, 7.903409e-001, 7.856320e-001, 7.563362e-001, 7.354722e-001, 7.222769e-001, 7.217752e-001, 7.017208e-001, 6.955013e-001, 7.041555e-001, 6.594083e-001, 6.412487e-001, 6.454236e-001, 6.432750e-001, 6.510105e-001, 6.217284e-001, 6.119911e-001, 6.199305e-001, 6.079630e-001, 6.148269e-001, 5.890806e-001, 5.845104e-001, 5.705234e-001, 5.642221e-001, 5.620225e-001, 5.604115e-001, 5.531296e-001, 5.504536e-001, 5.353967e-001, 5.406133e-001, 5.369468e-001, 5.215623e-001, 5.215467e-001, 5.157368e-001, 5.143072e-001, 5.150430e-001, 4.880170e-001, 5.006529e-001, 4.935364e-001, 4.908333e-001, 4.745870e-001, 4.860849e-001, 4.693702e-001, 4.689148e-001, 4.658954e-001, 4.562150e-001, 4.573544e-001, 4.568536e-001, 4.505393e-001, 4.499820e-001, 4.515129e-001, 4.304222e-001, 4.381754e-001, 4.202020e-001, 4.270096e-001, 4.264353e-001, 4.156649e-001, 4.148092e-001, 4.179731e-001, 4.099771e-001, 4.004975e-001, 4.032202e-001, 4.071609e-001, 3.952590e-001, 3.812608e-001, 3.860889e-001, 3.780516e-001};
    /** Weights for the central composite features used in the metric function */
    private static double centralCompositeFeatureWeight[]={9.332170e+000, 5.747565e+000, 4.970580e+000, 2.768146e+000, 2.330608e+000, 2.335541e+000, 1.977381e+000, 1.750218e+000, 1.688549e+000, 1.494607e+000, 1.444102e+000, 1.348312e+000, 1.269874e+000, 1.171387e+000, 1.188950e+000, 1.144398e+000, 1.028594e+000, 1.021784e+000, 9.884609e-001, 9.906634e-001, 9.426891e-001, 9.081982e-001, 9.188448e-001, 8.700525e-001, 8.492603e-001, 8.302418e-001, 8.283794e-001, 7.996343e-001, 7.822402e-001, 7.943693e-001, 7.540236e-001, 7.540672e-001, 7.502532e-001, 7.361908e-001, 7.447276e-001, 7.249272e-001, 7.032248e-001, 7.118304e-001, 6.975288e-001, 6.906156e-001, 6.726953e-001, 6.705613e-001, 6.555519e-001, 6.606468e-001, 6.480718e-001, 6.399794e-001, 6.325303e-001, 6.162066e-001, 6.130201e-001, 6.164221e-001, 6.016466e-001, 6.034503e-001, 5.975735e-001, 5.847582e-001, 5.720826e-001, 5.724263e-001, 5.837662e-001, 5.682944e-001, 5.557444e-001, 5.527339e-001, 5.471770e-001, 5.374276e-001, 5.462680e-001, 5.321949e-001, 5.257836e-001, 5.203686e-001, 5.187320e-001, 5.206241e-001, 5.154882e-001, 5.062527e-001, 5.011319e-001, 4.972195e-001, 4.936958e-001, 4.849010e-001, 4.913920e-001, 4.899990e-001, 4.845157e-001, 4.731634e-001, 4.814359e-001, 4.707872e-001, 4.795806e-001, 4.474767e-001, 4.575976e-001, 4.600664e-001, 4.685479e-001, 4.515326e-001, 4.417550e-001, 4.522022e-001, 4.402862e-001, 4.383033e-001, 4.358845e-001, 4.357358e-001, 4.276124e-001, 4.226141e-001, 4.284138e-001, 4.260374e-001};

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectFaceMPEG7AdvancedDescriptor castObj = (ObjectFaceMPEG7AdvancedDescriptor) obj;

        float dist = 0;

        for (int i = 0; i < fourierFeature.length; i++)
            dist += fourierFeatureWeight[i]*Math.pow(fourierFeature[i] - castObj.fourierFeature[i], 2);

        for(int i = 0; i < centralFourierFeature.length; i++)
            dist += centralFourierFeatureWeight[i]*Math.pow(centralFourierFeature[i] - castObj.centralFourierFeature[i], 2);

        for (int i = 0; i < compositeFeature.length; i++)
            dist += compositeFeatureWeight[i]*Math.pow(compositeFeature[i] - castObj.compositeFeature[i], 2);

        for (int i = 0; i < centralCompositeFeature.length; i++)
            dist += centralCompositeFeatureWeight[i]*Math.pow(centralCompositeFeature[i] - castObj.centralCompositeFeature[i], 2);

        return (float)Math.sqrt(dist);
    }


}
