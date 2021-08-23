package net.csibio.propro.algorithm.simulator;

public class DiscriptiveLinearRegression {
	/*
	 * get the parameters of the linear regression
	 * Y=aX+b
	 * parameters[0] is the a(slope)
	 * parameters[1] is the b(intercept)
	 * parameters[2] is the 
	 * parameters[3] is the correlation
	 */
	public double slope;
	public double intercept;
	public double correlation;
	
	public DiscriptiveLinearRegression(){
		this.slope = 0;
		this.intercept = 0;
		this.correlation = 0;

	}
	public DiscriptiveLinearRegression(double slope,double intercept,double correlation){
		this.slope = slope;
		this.intercept = intercept;
		this.correlation = correlation;
	}

}
