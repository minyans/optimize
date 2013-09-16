package edu.jhu.optimize;

import edu.jhu.util.math.Vectors;

public class FunctionOpts {

    /** Wrapper which negates the input function. */
    public static class NegateFunction extends ScaleFunction implements DifferentiableFunction {
    
        public NegateFunction(DifferentiableFunction function) {
            super(function, -1.0);
        }
        
    }
    
    /** Wrapper which scales the input function. */
    public static class ScaleFunction implements DifferentiableFunction {
    
        private DifferentiableFunction function;
        private double multiplier;
        
        public ScaleFunction(DifferentiableFunction function, double multiplier) {
            this.function = function;
            this.multiplier = multiplier;
        }
        
        @Override
        public void setPoint(double[] point) {
            function.setPoint(point);
        }
        
        @Override
        public double getValue() {
            return multiplier * function.getValue();
        }
    
        @Override
        public void getGradient(double[] gradient) {
            function.getGradient(gradient);
            Vectors.scale(gradient, multiplier);
        }
    
        @Override
        public int getNumDimensions() {
            return function.getNumDimensions();
        }
    
    }
    
    /** Wrapper which adds the input functions. */
    public static class AddFunctions implements DifferentiableFunction {
    
        private DifferentiableFunction[] functions;
        
        public AddFunctions(DifferentiableFunction... functions) {
            int numDims = functions[0].getNumDimensions();
            for (DifferentiableFunction f : functions) {
                if (numDims != f.getNumDimensions()) {
                    throw new IllegalArgumentException("Functions have different dimension.");
                }
            }
            this.functions = functions;
        }
        
        @Override
        public void setPoint(double[] point) {
            for (DifferentiableFunction function : functions) {
                function.setPoint(point);
            }
        }
        
        @Override
        public double getValue() {
            double sum = 0.0;
            for (DifferentiableFunction f : functions) {
                sum += f.getValue();                
            }
            return sum;
        }
    
        @Override
        public void getGradient(double[] gradient) {
            double[] g = new double[getNumDimensions()];
            for (DifferentiableFunction f : functions) {
                f.getGradient(g);
                Vectors.add(gradient, g);
            }
        }
    
        @Override
        public int getNumDimensions() {
            return functions[0].getNumDimensions();
        }
    
    }

}
