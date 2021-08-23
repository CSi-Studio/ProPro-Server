package net.csibio.propro.algorithm.simulator;

public class PeakIso extends Peak{
	public int isoNum=0;
	public PeakIso(Peak peak,int isoNum){
		this.position=peak.position;
		this.charge=peak.charge;
		this.isoNum = isoNum;
		this.intensity = peak.intensity;
		this.relative=peak.relative;
	}
}
