package net.csibio.propro.algorithm.simulator;


public class Ion extends Skeleton{
	public int charge=0;
	public ConstantValue.ionTypes mainType;
	public ConstantValue.derivativeTypes deriType;
	public int deriLossNum = 0;
	
	public Ion(){
		
	}
	public Ion(String aaseq,float[] massaa,int cha,ConstantValue.ionTypes ionT,
			ConstantValue.derivativeTypes deriT,int deriLNum){
		this.seqAA = aaseq;
		this.massAA = massaa;
		this.charge = cha;
		this.mainType = ionT;
		this.deriType = deriT;
		this.deriLossNum = deriLNum;
	}
	public float getPosition(){
		return getMass()/this.charge+1;
		
	}
	public float getMass(){
		float mass = super.getMass();
		
		if(mainType == ConstantValue.ionTypes.B){
		}
		else if(mainType == ConstantValue.ionTypes.Y){
			mass += 2*1.0079+15.9994;
		}
		if(this.deriType==ConstantValue.derivativeTypes.iso){
			mass += ConstantValue.derivativeMass[1]*this.deriLossNum;
		}
		if(this.deriType==ConstantValue.derivativeTypes.NH3){
			mass += ConstantValue.derivativeMass[2]*this.deriLossNum;
		}
		if(this.deriType==ConstantValue.derivativeTypes.H2O){
			mass += ConstantValue.derivativeMass[3]*this.deriLossNum;
		}
		if(this.deriType==ConstantValue.derivativeTypes.CO2){
			mass += ConstantValue.derivativeMass[4]*this.deriLossNum;
		}
		return mass;


	}
	
	public float getMDivC(){
		return (this.getMass()+charge)/this.charge;
	}

	
	public int[] getMolecular(){
		int[] atomNo = super.getMolecular();
		return atomNo;
		
	}
	public void print(String prefix){
		System.out.println(prefix+this.seqAA+"\t"+this.mainType+"\t"+
				this.deriType+"\t"+this.charge+"\t"+this.getPosition());
	}
	
	
}