package edu.jhu.optimize;

public class FunctionAsBatchFunction extends AbstractDifferentiableBatchFunction implements DifferentiableBatchFunction {

    private DifferentiableFunction fn;
    private int numExamples;
    
    public FunctionAsBatchFunction(DifferentiableFunction fn, int numExamples) {
        this.fn = fn;
        this.numExamples = numExamples;
    }
    
    @Override
    public void setPoint(double[] point) {
        fn.setPoint(point);
    }

    @Override
    public double getValue(int[] batch) {
        return fn.getValue();
    }

    @Override
    public void getGradient(int[] batch, double[] gradient) {
        fn.getGradient(gradient);
    }

    @Override
    public int getNumDimensions() {
        return fn.getNumDimensions();
    }
    
    @Override
    public int getNumExamples() {
        return numExamples;
    }

    public void setNumExamples(int numExamples) {
        this.numExamples = numExamples;
    }

}
