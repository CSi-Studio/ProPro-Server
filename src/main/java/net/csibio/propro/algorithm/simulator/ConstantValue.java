package net.csibio.propro.algorithm.simulator;
public class ConstantValue {
	//static public float precusorError=2.0;
	//static public float fragmentError=0.01;
	//static public float intensityThres=100;
	//static public float[] derivative = {0,1,2,3,21,22,-35,-36,-17,-18,-28};
	public static String spectraDir = "/Users/ictsun/Documents" +
			"/MyDocuments/SpectraLib/swedCAD_msms_data/CAD/allSpectra/";
	public static int zero_pos=100;
	public static float isoDelta = (float) 0.02;
	public static float lossDelta = (float) 0.05;
	public static float complementaryDelta = (float) 0.05;
	public static float pairDelta = (float) 0.1;
	public static float mergeDelta = (float) 0.1;
	public static float posDelta = (float) 0.2;
	public static float precusorDelta = (float) 0.1;
	public static float intensityTres = 0;
	public static float relativeTres = 0;
	static public char[] aa_array = {'A','C','D','E','F','G','H','I','K','L','M','N','P','Q','R','S','T','V','W','Y','Z'};




	static public float[] derivativeMass = {0,1,-17,-18,-44};
	public static enum derivativeTypes{main,iso,NH3,H2O,CO2};
	public static enum ionTypes{Noise,B,Y};

	//amino_acid_molecular_weight[Character.getNumericValue('X')][0]=57.02147;
	static public float[] amino_acid_molecular_weight={(float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)
		71.03711, (float)0.0, (float)103.00919, (float)
		115.02694, (float)129.04259, (float) 147.06841, (float)57.02146, (float)
		137.05891, (float)113.08406, (float)0.0, (float)128.09496, (float)113.08406, (float)131.04049, (float)
		114.04293, (float)0.0, (float)97.05276, (float)128.05858, (float)156.10111, (float)87.03203, (float)
		101.04768, (float)0.0, (float)99.06841, (float)186.07931, (float)0.0, (float)163.06333, (float)0.0, (float)0.0, (float)
		0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0};
	static public float[] amino_acid_proton_affinity={(float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)
		71.0317, (float)0.0, (float)160.00919, (float)
		115.02695, (float)129.0426, (float)147.06842, (float)57.02147, (float)
		137.05891, (float)113.08407, (float)0.0, (float)128.09497, (float)113.08407, (float)131.04049, (float)
		114.04293, (float)0.0, (float)97.05277, (float)128.05858, (float)156.10112, (float)87.03203, (float)
		101.04768, (float)0.0, (float)99.06842, (float)186.07932, (float)0.0, (float)163.06333, (float)0.0, (float)0.0, (float) 
		0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0, (float)0.0};
	static public float isotope_ratio_array[][] = { 
		{(float)0.9889, (float)0.0111, (float)0},  //C
		{(float)0.99985 , (float) 1.5E-4, (float) 0} , //H 
		{(float)0.99634, (float)0.00366, (float)0} ,   //N
		{(float)0.99756, (float)3.9E-4, (float)0.00205} ,//O
		{(float)0.9502, (float)0.0075, (float)0.0421}  //S
	};
	static public float isoSlope= (float)5.5E-4;
	static public float isoIntercept= (float)-0.005;
	static public float iso2Slope= (float)2.3E-4;
	static public float iso2Intercept= (float)-0.07;

	
	static public int amino_acid_molecular_composition[][]= {{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
		{3,5,1,1,0},{0,0,0,0,0},{5,8,2,2,1},{4,5,1,3,0},{5,7,1,3,0},{9,9,1,1,0},{2,3,1,1,0},
		{6,7,3,1,0},{6,11,1,1,0},{0,0,0,0,0},{6,12,2,1,0},{6,11,1,1,0},{5,9,1,1,1},
		{4,6,2,2,0},{0,0,0,0,0},{5,7,1,1,0},{5,8,2,2,0},{6,12,4,1,0},{3,5,1,2,0},
		{4,7,1,2,0},{0,0,0,0,0},{5,9,1,1,0},{11,10,2,1,0},{0,0,0,0,0},{9,9,1,2,0},{0,0,0,0,0},{0,0,0,0,0},
		{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},
		{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0}};
	
	
}