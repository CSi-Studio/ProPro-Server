package net.csibio.propro.algorithm.simulator;

/*
 * Peptide format is AAAAA[nn]AAAAc;
 * pep_string is the complete string including the PTM information;
 * Skelton_string is the AA string.
 * the PTM info is also saved in the pif.
 */
public class Peptide extends Skeleton {
	public int charge = 0;
	public String pepString;
	public Peptide() {

	}

	public Peptide(String pep, int cha) {
		super(pep);
		this.pepString = pep;
		this.charge = cha;
	}

	public float getBmass(int pos) {
		if (pos > this.seqAA.length())
			return -1;
		return getBion(pos, 1).getMass();
	}

	public Ion getBion(int bi, int cha) {
		Ion bion = new Ion(this.seqAA.substring(0, bi), 
				SunArray.subArray(this.massAA, 0, bi),cha,
				ConstantValue.ionTypes.B, ConstantValue.derivativeTypes.main, 0);

		return bion;
	}
	/*
	 * get Yi;
	 */
	public Ion getYion(int yi, int cha) {
		Ion yion = new Ion(this.seqAA.substring(seqAA.length()-yi), 
				SunArray.subArray(this.massAA, seqAA.length()-yi, massAA.length),cha,
				ConstantValue.ionTypes.Y, ConstantValue.derivativeTypes.main, 0);
		return yion;
	}
	
	public Ion getYion_iso(int yi,int cha)
	{
		Ion yion=getYion(yi,cha);
		Ion yion_iso=new Ion(yion.seqAA,yion.massAA,yion.charge,yion.mainType,ConstantValue.derivativeTypes.iso,0);
		return yion_iso;
	}

	public float[] getYion_iso_ratio(int yi)
	{
		float[] iso_ratio=new Peptide(this.seqAA.substring(seqAA.length()-yi),2).getIsotopeRatio();
		return iso_ratio;
	}
	public float getYmass(int pos) {
		if (pos > this.seqAA.length())
			return -1;
		return getYion(pos, 1).getMass();
	}

	public float getPrecursorMass() {
		float pMass = (float) (this.charge * 1.0079 + 15.9994 + 2*1.00728);
		pMass = pMass + this.getMass();
		return pMass;

	}
	public int countRKH() {
		char[] s1c = seqAA.toCharArray();
		int nk = 0;
		for (int i = 0; i < s1c.length; i++) {
			if (s1c[i] == 'K' || s1c[i] == 'R' || s1c[i] == 'H')
				nk++;
		}
		return nk;
	}
	
	public static void main(String[] args) throws Exception {
		Peptide pep = new Peptide
				("EHGEPLFSSHM(+15.99)LDLSEETDEENISTC(+57.02)VK",2);
		System.out.println(pep.seqAA);
		SunArray.print(pep.massAA);
		for(int i=1;i<pep.seqAA.length();i++){
			System.out.println(i+"\t"+pep.getYion(i, 1).getPosition());
		}
		System.out.println(pep.getPrecursorMass());
	}

}
