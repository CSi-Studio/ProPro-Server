package net.csibio.propro.algorithm.simulator;

import java.util.ArrayList;

public class PeakMain extends Peak {
	Peak nh3Peak;
	Peak h2oPeak;
	PeakMain complementaryPeak;
	public ArrayList<PeakIso> isoList = new ArrayList<PeakIso>();
	public PeakMain(){
		super();
		
	}

	public PeakMain(Peak peak){
		super(peak.position,peak.charge,peak.intensity);
		this.relative = peak.relative;
	}
	public PeakMain(float pos, int c, float intensity,float relative){
		super(pos,c,intensity,relative);
	}
	public void setNH3Peak(Peak nh3Peak){
		this.nh3Peak=nh3Peak;
	}
	public Peak getNH3Peak(){
		return nh3Peak;
	}
	public void seth2oPeak(Peak peak){
		this.h2oPeak=peak;
	}
	public Peak geth2oPeak(){
		return this.h2oPeak;
	}
	public void addIsoPeak(int isoNum, Peak peak){
		PeakIso peakIso=new PeakIso(peak,isoNum);
		this.isoList.add(peakIso);
	}
	public Peak getComplementaryPeak(){
		return this.complementaryPeak;
	}
	public void setComplementaryPeak(PeakMain peak){
		this.complementaryPeak=peak;
	}

	public void print(){
		super.print("");
		
		for(int i=0;i<isoList.size();i++){
			isoList.get(i).print("\t"+isoList.get(i).isoNum+"\t");
		}
		if(nh3Peak!=null){
			nh3Peak.print("\tNH3\t");
		}
		if(h2oPeak!=null){
			h2oPeak.print("\t\tH2O\t\t");
		}
		if(nh3Peak!=null){
			nh3Peak.print("\tNH3\t");
		}
		if(this.complementaryPeak!=null){
			this.complementaryPeak.print("\t\tcomp\t\t");
		}


	}
}
