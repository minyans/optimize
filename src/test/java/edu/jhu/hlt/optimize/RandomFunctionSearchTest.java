package edu.jhu.hlt.optimize;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;

import edu.jhu.hlt.optimize.function.Bounds;
import edu.jhu.hlt.optimize.function.Function;
import edu.jhu.hlt.optimize.functions.XSquared;


public class RandomFunctionSearchTest {
    static Logger log = LoggerFactory.getLogger(RandomFunctionSearch.class);

    @Test
	public void testXSquaredSearch(){
	Function x2 =new XSquared();
	Bounds bounds; 
	RandomFunctionSearch rfs;

	double d[] = new double[]{5,1,.25};
	for(double dd : d){
	    bounds = new Bounds(new double[]{-dd}, new double[]{dd});
	    rfs = new RandomFunctionSearch(x2, bounds);
	    boolean good = rfs.sample(1000);
	    double guesses[] = rfs.getBestGuessPerIteration();
	    // for(double d : guesses){
	    //     System.out.print(d +"\t");
	    
	    // }
	    if(!good) System.exit(1);	
	}
    }
}
