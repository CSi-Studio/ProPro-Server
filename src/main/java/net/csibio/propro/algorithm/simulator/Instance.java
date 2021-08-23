package net.csibio.propro.algorithm.simulator;

class Instance{

	float[][] AA;
	float[][] KR;
	float[] protondis;
	float[] Ndis;
	float[][] dickA;
	float lnR;
	public Instance(){
		this.AA = new float[staticValue.AAend-staticValue.AAbegin+1][20];
		this.KR = new float[2][staticValue.KRend-staticValue.KRbegin+1];
		this.protondis = new float[staticValue.cLen+1];
		this.Ndis = new float[staticValue.nLen+1];
		this.lnR = 0;
		this.dickA = new float[staticValue.dickLen][20];
	}
	public Instance(	float[][] rowAA,float[][] KR,
			float[] rowC,float[] 
					rowN, float[][] dicA,float lnR){
		
		this.AA = rowAA;
		this.KR = KR;
		this.protondis = rowC;
		this.Ndis = rowN;
		
		this.dickA = dicA;
		this.lnR = lnR;
		
	}
	public void print(String split){

		System.out.println(this.toString(split));
	}
	public String toString(String split){
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<AA.length;i++){
			for(int j = 0;j<AA[0].length;j++)
			{
				sb.append(AA[i][j]+split);
			}
		}
//		sb.append("\n");
		
		for(int i=0;i<KR.length;i++){
			for(int j = staticValue.KRbegin;j <= staticValue.KRend;j++)
			{
					sb.append(KR[i][j-staticValue.KRbegin]+split);
			}
//			sb.append("\n");
		}

//		sb.append("\n");
		for(int i=1;i<protondis.length;i++)
		{
			sb.append(protondis[i]+split);
		}
//		sb.append("\n");
		for(int i=1;i<Ndis.length;i++)
		{
			sb.append(Ndis[i]+split);
		}
//		sb.append("\n");
		for (int i = 0; i < dickA.length; i++) {
			for(int j=0;j< dickA[0].length;j++)
				sb.append(dickA[i][j] + split);
		}
//		sb.append("\n");

		sb.append(lnR+"\n");
		
		return sb.toString();
	}
	
}




