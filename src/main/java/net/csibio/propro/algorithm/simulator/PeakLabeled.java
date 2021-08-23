package net.csibio.propro.algorithm.simulator;

import java.util.ArrayList;

public class PeakLabeled {
	public Peak peak;
	public ArrayList<Ion> ionList;
	public float[] iso_ratio;
	public PeakLabeled(){
		
	}
	
	public PeakLabeled(Peak peak, ArrayList<Ion> ionlist){
		this.peak = peak;
		this.ionList = ionlist;
	}
	public PeakLabeled(Peak peak,ArrayList<Ion> ionlist,float[] iso_ratio)
	{
		this.peak=peak;
		this.ionList=ionlist;
		this.iso_ratio=iso_ratio;
	}

}
