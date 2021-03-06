package edu.jhu.hlt.optimize.functions;

import edu.jhu.hlt.optimize.function.Function;
import junit.framework.Assert;

/**
 * A function wrapper with expected results and tolerances.
 * 
 * @author Nicholas Andrews
 */
public class TestFunction {
	double value_at_optima;
	double value_tolerance;
	double [] param_at_optima;
	double param_tolerance;
	public boolean maximize;
	public Function f;
	
	public TestFunction(Function f, boolean maximize, double value_at_optima, double value_tolerance, double [] param_at_optima, double param_tolerance) {
		this.f = f;
		this.value_at_optima = value_at_optima;
		this.value_tolerance = value_tolerance;
		this.param_at_optima = param_at_optima;
		this.param_tolerance = param_tolerance;
	}
	
	public Function getFunction() { return f; }
	
	public boolean maximize() { return maximize; };
	
	public void checkValue(double value) {
		//Assert.assertEquals(value, value_at_optima, value_tolerance);
		Assert.assertEquals(value_at_optima, value, value_tolerance);
	}
	
	public void checkParam(double [] param) {
		for(int i=0; i<param.length; i++) {
			//Assert.assertEquals(param[i], param_at_optima[i], param_tolerance);
			Assert.assertEquals(param_at_optima[i], param[i], param_tolerance);
		}
	}
	
}
