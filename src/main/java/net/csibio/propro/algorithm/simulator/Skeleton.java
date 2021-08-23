package net.csibio.propro.algorithm.simulator;

import java.util.StringTokenizer;

public class Skeleton {
	public String seqAA;
	public float[] massAA;

	public Skeleton() {

	}

	public Skeleton(String ske) {
		/*
		 * AAAAAA(1.000)AAAAAR
		 */
		StringBuffer sb = new StringBuffer();
		/*
		 * ptms is a temp pool to save the ptms.
		 */
		float[] ptms = new float[ske.length()];
		/*
		 * split the ske using ")";
		 * AAAAAA(1.000 and AAAAAR;
		 */
		StringTokenizer st = new StringTokenizer(ske, ")");
		for (; st.hasMoreTokens();) {
			String peptemp = (String) st.nextElement();
			int bracketIndex = peptemp.indexOf('(');
			if (bracketIndex > -1) {
				String frag = peptemp.substring(0, bracketIndex);
				/*
				 * frag is AAAAAA of the AAAAAA(1.000
				 */
				double massPtm = Double.parseDouble(peptemp
						.substring(bracketIndex + 1));
				/*
				 * massPtm is 1.000 of AAAAAA(1.000;
				 */

				sb.append(frag);

				ptms[sb.length() - 1] += massPtm;

			} else {
				sb.append(peptemp);

			}
		}
		this.seqAA = sb.toString();
		massAA = new float[seqAA.length()];
		for (int i = 0; i < seqAA.length(); i++) {
			/*
			System.out.println(Character
					.getNumericValue(seqAA.charAt(i)));
			System.out.println(seqAA.charAt(i));
			*/
			massAA[i] = ConstantValue.amino_acid_molecular_weight[Character
					.getNumericValue(seqAA.charAt(i))] + ptms[i];
		}

	}
	
	public Skeleton(String ske,float[] massaa){
		/*
		 * Set the sequence and the masses of AA;
		 */
		if(ske.length()!=massaa.length){ 
			System.err.println("Constructor Error");
			return;
		}
		this.seqAA = ske;
		this.massAA = massaa;
		
	}
	

	public float getMass() {

		float mass = 0;
		for (int i = 0; i < seqAA.length(); i++) {
			mass += this.massAA[i];
		}
		return mass;
	}

	public int[] getMolecular() {

		int[] atomNo = new int[5];
		char[] aaA = seqAA.toCharArray();
		for (int i = 0; i < aaA.length; i++) {
			for (int j = 0; j < 5; j++) {
				atomNo[j] += ConstantValue.amino_acid_molecular_composition[Character
						.getNumericValue(aaA[i])][j];
			}
		}
		return atomNo;
	}

	public int getRnum() {
		int rNo = 0;
		char[] aaA = seqAA.toCharArray();
		for (int i = 0; i < aaA.length; i++) {
			if (aaA[i] == 'R')
				rNo++;
		}
		return rNo;

	}

	public int getKnum() {
		int kNo = 0;
		char[] aaA = seqAA.toCharArray();
		for (int i = 0; i < aaA.length; i++) {
			if (aaA[i] == 'K')
				kNo++;
		}
		return kNo;

	}

	public float[] getIsotopeRatio() {
		int[] atomNo = new int[5];
		char[] aaA = seqAA.toCharArray();
		for (int i = 0; i < aaA.length; i++) {
			for (int j = 0; j < 5; j++) {
				atomNo[j] += ConstantValue.amino_acid_molecular_composition[Character
						.getNumericValue(aaA[i])][j];
			}
		}
		return (new Molecular(atomNo)).get_isotope_ratio();
	}
}
