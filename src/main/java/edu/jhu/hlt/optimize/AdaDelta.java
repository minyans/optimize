package edu.jhu.hlt.optimize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
import edu.jhu.hlt.util.Prm;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.vector.IntDoubleHashVector;
import edu.jhu.prim.vector.IntDoubleVector;

/**
 * AdaDelta -- Tweaks the AdaGrad (Duchi et al., 2010) method by adding momentum
 * to the gradient updates. On Neural Net training it seems to be far less
 * sensitive to the learning rate parameters (even though there are two) than
 * SGD or AdaGrad.
 * 
 * Matthew D. Zeiler (2012) "ADADELTA: An Adaptive Learning Rate Method"
 * http://arxiv.org/abs/1212.5701
 * 
 * @author mgormley
 */
public class AdaDelta implements GainSchedule {

    private static final long serialVersionUID = 1L;

    /** Options for this optimizer. */
    public static class AdaDeltaPrm extends Prm {
        private static final long serialVersionUID = 1L;
        /** The decay rate (rho) for exponential decay averaging. */
        public double decayRate = 0.95;
        /** The amount added (epsilon) to the sum of squares inside the square root. */
        public double constantAddend = 1e-6;
        /**
         * Whether to initialize the sums-of-squares to zeros. Note: Most other implementations of
         * AdaDelta have initSumToZeros = true and no option to do otherwise.
         */
        public boolean initSumsToZeros = false;
    }
    
    private static final Logger log = LoggerFactory.getLogger(AdaDelta.class);

    private AdaDeltaPrm prm;
    // Accumulator for gradient.
    private double[] gradAccum;
    // Accumulator for updates.
    private double[] updAccum;
    // Cache of the learning rate for each parameter.
    private double[] lr;
    // Whether the accumulators have been initialized.
    private boolean initialized;
    
    /**
     * Constructs an SGD optimizer.
     */
    public AdaDelta(AdaDeltaPrm prm) {
        this.prm = prm;
        if (prm.constantAddend <= 0) {
            throw new IllegalArgumentException("Constant added must be positive: " + prm.constantAddend);
        }
    }
    
    @Override
    public void init(DifferentiableBatchFunction function) {
        gradAccum = new double[function.getNumDimensions()];
        lr = new double[function.getNumDimensions()];
        updAccum = new double[function.getNumDimensions()];
        initialized = false;
    }

    @Override
    public void takeNoteOfGradient(IntDoubleVector g) {
        // If this is the first iteration, use the value of the current gradient only. 
        double gamma = !initialized && prm.initSumsToZeros ? 0.0 : prm.decayRate;
        initialized = true;
        
        // TODO: This update is NOT sparse.
        for (int i=0; i<gradAccum.length; i++) {
            double g_i = g.get(i);
            gradAccum[i] = gamma * gradAccum[i] + (1.0 - gamma) * g_i * g_i;
            lr[i] = computeLearningRate(i);
            double update = lr[i] * g_i;
            updAccum[i] = gamma * updAccum[i] + (1.0 - gamma) * update * update;
            
            if (log.isTraceEnabled()) {
                log.trace(String.format("i=%3d g=%7.2g g2=%7.2g u2=%7.2g lr=%7.2g dx=%7.2g", i, g_i, gradAccum[i],
                        lr[i], updAccum[i], update));
            }
            assert !Double.isNaN(gradAccum[i]);
            assert !Double.isNaN(lr[i]);
            assert !Double.isNaN(updAccum[i]);
        }
    }
    
    /**
     * The entire point of this method is to carefully compute the following:
     * <p>
     * Math.sqrt(updAccum[i] + prm.constantAddend) / Math.sqrt(gradAccum[i] + prm.constantAddend);
     * </p>
     * without running into boundary cases.
     *  
     * @param i The index of the parameter for which to compute the learning rate.
     * @return The learning rate for that parameter.
     */
    private double computeLearningRate(int i) {
        if (gradAccum[i] < 0) {
            throw new RuntimeException("Gradient accumulator is < 0: " + gradAccum[i]);
        }
        if (updAccum[i] < 0) {
            throw new RuntimeException("Update accumulator is < 0: " + updAccum[i]);
        }

        double learningRate = Math.sqrt((updAccum[i] + prm.constantAddend) / (gradAccum[i] + prm.constantAddend));        
        assert !Double.isNaN(learningRate);
        // We shouldn't ever worry about infinities because of the constantAdded being > 0.
        assert !Double.isInfinite(learningRate);
        
        return learningRate;
    }
    
    /**
     * Gets the learning rate for the current iteration.
     * @param iterCount The current iteration.
     * @param i The index of the current model parameter. 
     */
    @Override
    public double getLearningRate(int iterCount, int i) {
        return lr[i];
    }
    
    @Override
    public GainSchedule copy() {
        AdaDeltaPrm otherPrm = Prm.clonePrm(this.prm);
        AdaDelta other = new AdaDelta(otherPrm);
        other.gradAccum = DoubleArrays.copyOf(this.gradAccum);
        other.lr = DoubleArrays.copyOf(this.lr);
        other.updAccum = DoubleArrays.copyOf(this.updAccum);
        return other;
    }

    @Override
    public double getEta0() {
        throw new IllegalStateException("This gain schedule has no eta0 parameter.");
    }

    @Override
    public void setEta0(double eta0) {
        throw new IllegalStateException("This gain schedule has no eta0 parameter.");
    }

    @Override
    public boolean isSameForAllParameters() {
        return false;
    }
    
}
