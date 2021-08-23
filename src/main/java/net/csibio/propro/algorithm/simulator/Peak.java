package net.csibio.propro.algorithm.simulator;
public class Peak {
	public float position;
	public int charge;
	public float intensity;
	public float relative;
	public Peak(){
		
	}

	public Peak(float pos, int c, float intensity) {
		
		this.position = pos;
		this.charge = c;
		this.intensity = intensity;
	}
	public Peak(float pos, int c, float intensity,float relative){
		this.position = pos;
		this.charge = c;
		this.intensity = intensity;
		this.relative = relative;
	}
	public float getMass(){
		return this.position*this.charge-this.charge;
	}
	public void mergeWith(Peak op){
		this.position = (this.position+op.position)/2;
		this.intensity = this.intensity+op.intensity;
		this.relative = this.relative + op.relative;
	}


	public void print(String prefix){
		System.out.println(prefix +position
				+ "\t" + charge 
				+ "\t"
				+ intensity+"\t"+relative);
		

	}
	/*
	 * get score of the peak and its same peak.
	 * The formula is Sum(0.5^i*Math.log(peak.relative);
	 */
	public float getScore(){
		float score = 0;
		return score;
	}
}
