package net.csibio.propro.algorithm.simulator;

public class DiscriptiveStatistics {
	public float Ntotal;
	public float mean;
	public float standardDeviation;
	public float sum;
	public float minimum;
	public float median;
	public float maximum;

	public DiscriptiveStatistics(float[] stats) {
		Ntotal = stats[0];
		mean = stats[1];
		standardDeviation = stats[2];
		sum = stats[3];
		minimum = stats[4];
		median = stats[5];
		maximum = stats[6];
	}
	public void print(String label,boolean isTitle){
		if(isTitle)
			System.out.println("\tNtotal"+"\t"+"Mean"+"\t"+"standardDeviation\t"+
		"sum\t"+"minimun\t"+"median\t"+"maximum");
		System.out.println(label+"\t"+Ntotal+"\t"+mean+"\t"+standardDeviation+"\t"+sum+"\t"+minimum+"\t"+median+"\t"+maximum);

	}
	


}
