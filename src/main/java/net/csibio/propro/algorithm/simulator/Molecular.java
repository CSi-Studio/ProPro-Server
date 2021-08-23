package net.csibio.propro.algorithm.simulator;

public class Molecular {
	
	int[] atomic_composition = new int[5];
	
	public Molecular(int[] atomNo){
		this.atomic_composition=atomNo;
	}
	
	public float[] get_isotope_ratio(){
		float[] iso4Mol=new float[4];
		iso4Mol[0]=1;
		//compute isotope 0 
		for(int i=0;i<5;i++){
			iso4Mol[0]*=Math.pow(ConstantValue.isotope_ratio_array[i][0], this.atomic_composition[i]);
		}

		//compute isotope 1
		for(int i=0;i<5;i++){
			double f=1;
			for(int j=0;j<5;j++){
				if(j==i){
					f*=atomic_composition[j]*ConstantValue.isotope_ratio_array[j][1]*
					Math.pow(ConstantValue.isotope_ratio_array[j][0], atomic_composition[j]-1);
				}
				else  {
					f*=Math.pow(ConstantValue.isotope_ratio_array[j][0], atomic_composition[j]);
				}
				
			}
			iso4Mol[1]+=f;
		}

		//compute isotope 2
		for(int i=0;i<5;i++){
			
			for(int j=i;j<5;j++){
				double f=1;
				if(j==i){
					for(int k=0;k<5;k++){
					
						if(k==i){
							f*=0.5*atomic_composition[k]*(atomic_composition[k]-1)*
							ConstantValue.isotope_ratio_array[k][1]*ConstantValue.isotope_ratio_array[k][1]*
							Math.pow(ConstantValue.isotope_ratio_array[k][0], atomic_composition[k]-2);
						}
						else{
							f*=Math.pow(ConstantValue.isotope_ratio_array[k][0], atomic_composition[k]);
						}
					}
				}
					
				else {
					for(int k=0;k<5;k++){
						if(k==i||k==j){
							f*=atomic_composition[k]*ConstantValue.isotope_ratio_array[k][1]*
							Math.pow(ConstantValue.isotope_ratio_array[k][0], atomic_composition[k]-1);
						}
						else{
							f*=Math.pow(ConstantValue.isotope_ratio_array[k][0], atomic_composition[k]);
						}
					}
				}
				iso4Mol[2]+=f;
			}
			
		}

		for(int i=0;i<5;i++){
			double f=1;
			for(int j=0;j<5;j++){
				if(j==i){
					f*=atomic_composition[j]*ConstantValue.isotope_ratio_array[j][2]*
					Math.pow(ConstantValue.isotope_ratio_array[j][0], atomic_composition[j]-1);

				}
				else  {
					f*=Math.pow(ConstantValue.isotope_ratio_array[j][0], atomic_composition[j]);

				}
				
			}
			iso4Mol[2]+=f;
		}
		return iso4Mol;
	}
}
