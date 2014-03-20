package edu.jhu.hlt.optimize;

import java.util.Date;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;

import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Timer;

/**
 * Stochastic gradient descent with minibatches.
 * 
 * We use the learning rate suggested in Leon Bottou's (2012) SGD Tricks paper.
 * 
 * @author mgormley
 */
public class SGD implements Optimizer<DifferentiableBatchFunction> {

    /** Options for this optimizer. */
    public static class SGDPrm {
        /**
         * The initial learning rate. (i.e. \gamma_0 in where \gamma_t =
         * \frac{\gamma_0}{1 + \gamma_0 \lambda t})
         */
        public double initialLr = 0.1;
        /**
         * Learning rate scaler. (i.e. \lambda in where \gamma_t =
         * \frac{\gamma_0}{1 + \gamma_0 \lambda t})
         * 
         * According to Leon Bottou's (2012) SGD tricks paper, when using an L2
         * regularizer of the form \frac{\lambda}{2} ||w||^2, where w is the
         * weight vector, this should be set to the value \lambda. If the L2
         * regularizer is instead parameterized by the variance of the L2 (i.e.
         * Guassian) prior, then we should set \lambda = 1 / \sigma^2.
         */
        public double lambda = 1.0;
        /** The number of passes over the dataset to perform. */
        public double numPasses = 10;
        /** The batch size to use at each step. */
        public int batchSize = 15;
        /** Whether batches should be sampled with replacement. */
        public boolean withReplacement = false;
        /** Date by which to stop. */
        public Date stopBy = null;
        /** Whether to compute the function value on the 0th iteration. */
        public boolean computeValueOnIterZero = true;
        public SGDPrm() { } 
        public SGDPrm(double initialLr, int numPasses, int batchSize) {
            this.initialLr = initialLr;
            this.numPasses = numPasses;
            this.batchSize = batchSize;
        }
    }
    
    private static final Logger log = Logger.getLogger(SGD.class);

    /** The number of gradient steps to run. */   
    private int iterations;
    /** The number of iterations performed thus far. */
    private int iterCount;
    /** The sampler of the indices for each batch. */
    private BatchSampler batchSampler;
   

    private SGDPrm prm;
    
    /**
     * Constructs an SGD optimizer.
     */
    public SGD(SGDPrm prm) {
        this.prm = prm;
    }
    
    /**
     * Initializes all the parameters for optimization.
     */
    protected void init(DifferentiableBatchFunction function) {
        int numExamples = function.getNumExamples();

        // Variables
        iterCount = 0;
        batchSampler = new BatchSampler(prm.withReplacement, numExamples, prm.batchSize);
                    
        // Constants
        iterations = (int) Math.ceil((double) prm.numPasses * numExamples / prm.batchSize);
        log.info("Setting number of batch gradient steps: " + iterations);
    }

    /**
     * Updates the learning rate for the next iteration.
     * @param iterCount The current iteration.
     * @param i The index of the current model parameter. 
     */
    protected double getLearningRate(int iterCount, int i) {
        // We use the learning rate suggested in Leon Bottou's (2012) SGD Tricks paper.
        // 
        // \gamma_t = \frac{\gamma_0}{1 + \gamma_0 \lambda t})
        //
        return prm.initialLr / (1 + prm.initialLr * prm.lambda * iterCount);
    }

    /**
     * Maximize the function starting at the given initial point.
     */
    @Override
    public boolean maximize(DifferentiableBatchFunction function, IntDoubleVector point) {
        return optimize(function, point, true);
    }

    /**
     * Minimize the function starting at the given initial point.
     */
    public boolean minimize(DifferentiableBatchFunction function, IntDoubleVector point) {
        return optimize(function, point, false);
    }

    private boolean optimize(DifferentiableBatchFunction function, final IntDoubleVector point, final boolean maximize) {
        init(function);
        if (prm.stopBy != null) {
            log.debug("Max time alloted (hr): " + (prm.stopBy.getTime() - new Date().getTime()) / 1000. / 3600.);  
        }

        int passCount = 0;
        double passCountFrac = 0;

        if (prm.computeValueOnIterZero) {
            double value = function.getValue(point);
            log.info(String.format("Function value on all examples = %g at iteration = %d on pass = %.2f", value, iterCount, passCountFrac));
        }
        
        // TODO: This used to be possible: assert (function.getNumDimensions() == point.length);

        Timer timer = new Timer();
        timer.start();
        for (iterCount=0; iterCount < iterations; iterCount++) {
            int[] batch = batchSampler.sampleBatch();
            
            // Get the current value and gradient of the function.
            ValueGradient vg = function.getValueGradient(point, batch);
            double value = vg.getValue();
            final IntDoubleVector gradient = vg.getGradient();
            log.trace(String.format("Function value on batch = %g at iteration = %d", value, iterCount));
            // TODO: This used to be possible: assert (gradient.length == point.length);            
            takeNoteOfGradient(gradient);
            
            // Scale the gradient by the parameter-specific learning rate.
            gradient.apply(new FnIntDoubleToDouble() {
                @Override
                public double call(int index, double value) {
                    double lr = getLearningRate(iterCount, index);
                    if (maximize) {
                        return lr * value;
                    } else {
                        return - lr * value;
                    }
                }
            });
            
            // Take a step in the direction of the gradient.
            point.add(gradient);

            // Compute the average learning rate and the average step size.
            final MutableDouble avgLr = new MutableDouble(0.0);
            final MutableDouble avgStep = new MutableDouble(0d);
            final MutableInt numNonZeros = new MutableInt(0);
            gradient.apply(new FnIntDoubleToDouble() {
                @Override
                public double call(int index, double value) {
                    double lr = getLearningRate(iterCount, index);
                    assert !Double.isNaN(point.get(index));
                    if (value != 0.0) {
                        avgLr.add(lr);
                        avgStep.add(gradient.get(index));
                        numNonZeros.increment();
                    }
                    return value;
                }
            });
            avgLr.setValue(avgLr.doubleValue() / numNonZeros.doubleValue());
            avgStep.setValue(avgStep.doubleValue() / numNonZeros.doubleValue());
            
            // If a full pass through the data has been completed...
            passCountFrac = (double) iterCount * prm.batchSize / function.getNumExamples();
            if ((int) Math.floor(passCountFrac) > passCount || iterCount == iterations - 1) {
                // Another full pass through the data has been completed or we're on the last iteration.
                // Get the value of the function on all the examples.
                value = function.getValue(point);
                log.info(String.format("Function value on all examples = %g at iteration = %d on pass = %.2f", value, iterCount, passCountFrac));
                log.debug("Average learning rate: " + avgLr);
                log.debug("Average step size: " + avgStep);
                log.debug(String.format("Average time per pass (min): %.2g", timer.totSec() / 60.0 / passCountFrac));
            }
            if ((int) Math.floor(passCountFrac) > passCount) {
                // Another full pass through the data has been completed.
                passCount++;
            }
            
            if (prm.stopBy != null) {
                Date now = new Date();
                if (now.after(prm.stopBy)) {
                    log.info(String.format("Current time is after stop-by time. now=%s, stopBy=%s", now.toString(), prm.stopBy.toString()));
                    log.info("Stopping training early.");
                    break;
                }
            }
        }
        
        // We don't test for convergence.
        return false;
    }

    /** A tie-in for subclasses such as AdaGrad. */
    protected void takeNoteOfGradient(IntDoubleVector gradient) {
        // Do nothing. This is just for subclasses.
    }
    
}
