package net.csibio.propro.algorithm.simulator;

import java.util.ArrayList;

public class staticValue {
	static int cLen = 10;
	static int AAbegin = -2;
	static int AAend = 1;
	static int KRbegin = -8;
	static int KRend = 5;
	static int nLen = 7;
	static int dickLen = 3;

	static char[] aa_array = { 'A', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'K',
			'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'Y' };
	static int[] mapAAIndex = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4,
			5, 6, 7, 0, 8, 9, 10, 11, 0, 12, 13, 14, 15, 16, 0, 17, 18, 0, 19,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    static public Parameter parameter;

	static public float[] posProbs = { 0, (float) 0, (float) 0, (float) 0,
			(float) 0, (float) 0.13, (float) 0.12, (float) 0.17, (float) 0.27,
			(float) 0.36, (float) 0.47, (float) 0.56, (float) 0.65,
			(float) 0.72, (float) 0.79, (float) 0.86, (float) 0.93,
			(float) 0.93 };
	static public float[] lAAProbs = { (float) 0.62, (float) 0.42,
			(float) 0.85, (float) 0.69, (float) 0.28, (float) 0.92,
			(float) 1.0, (float) 0.23, (float) 1.0, (float) 0.26, (float) 0.28,
			(float) 0.61, (float) 0.21, (float) 0.45, (float) 1.0,
			(float) 0.76, (float) 0.59, (float) 0.32, (float) 0.14,
			(float) 0.24 };
	static public float[] rAAProbs = { (float) 0.49, (float) 0.41,
			(float) 0.40, (float) 0.51, (float) 0.46, (float) 0.40,
			(float) 0.97, (float) 0.62, (float) 1.0, (float) 0.54,
			(float) 0.52, (float) 0.50, (float) 0.20, (float) 0.66,
			(float) 0.88, (float) 0.41, (float) 0.51, (float) 0.63,
			(float) 0.48, (float) 0.46 };

	static int getIndex(char a) {
		// System.out.println("getindex:"+a);
		return mapAAIndex[Character.getNumericValue(a)];
	}

	static public Instance getInstance(Peptide pep, int fragPos, float lnR) {
		float[][] rowAA = getAA(pep, fragPos);

		float[][] krA = getKR(pep, fragPos);

		float[] rowC = getCproton(pep, fragPos);

		float[] rowN = getNterm(pep, fragPos);

		float[][] dicA = getDikeA(pep, fragPos);

		// SunArray.print(rowAA);
		// SunArray.print(rowC);
		// SunArray.print(rowN);
		// SunArray.print(dicA);
		// System.out.println("\n\n");
		return new Instance(rowAA, krA, rowC, rowN, dicA, lnR);

	}

	static float[][] getAA(Peptide pep, int fragPos) {
		float[][] aaA = new float[staticValue.AAend - staticValue.AAbegin + 1][20];
		char[] aa = pep.seqAA.toCharArray();
		for (int i = staticValue.AAbegin; i <= staticValue.AAend; i++) {
			int nn = fragPos + i;
			if (nn >= 0 && nn < aa.length - 1) {
				char a = pep.seqAA.charAt(nn);
				int index = staticValue.mapAAIndex[Character.getNumericValue(a)];
				aaA[i - staticValue.AAbegin][index] = 1;
			}
		}

		// for (int i = staticValue.AAbegin+1; i <= staticValue.AAend+1; i++) {
		// int nn = fragPos + i;
		// if (nn >= 0 && nn < aa.length - 1) {
		// char a = pep.seqAA.charAt(nn);
		// int index = staticValue.mapAAIndex[Character.getNumericValue(a)];
		// aaA[i - staticValue.AAbegin-1][index] = 1;
		// }
		// }

		return aaA;
	}

	static float[][] getKR(Peptide pep, int fragPos) {
		float[][] krA = new float[2][staticValue.KRend - staticValue.KRbegin
				+ 1];
		char[] aa = pep.seqAA.toCharArray();
		for (int i = staticValue.KRbegin; i <= staticValue.KRend; i++) {

			int nn = fragPos + i;
			if (i >= -2 && i < 1)
				continue;
			if (i ==1 && nn != pep.seqAA.length()-1){
				continue;
			}
			if (nn >= 0 && nn < aa.length) {
				char a = pep.seqAA.charAt(nn);
				if (a == 'K') {
					krA[0][i - staticValue.KRbegin] = 1;
				}
				if (a == 'R') {
					krA[1][i - staticValue.KRbegin] = 1;
				}

			}
		}
		return krA;

	}

	static float[] getCproton(Peptide pep, int fragPos) {
		/*
		 * ABCKD*EFGR
		 */
		float[] cA = new float[staticValue.cLen + 1];
		int proton = pep.seqAA.length();
		if (proton - fragPos <= staticValue.cLen)

			cA[proton - fragPos] = 1;
		return cA;
	}

	static float[] getNterm(Peptide pep, int fragPos) {
		/*
		 * ABCKD*EFGR
		 */
		float[] nA = new float[staticValue.nLen + 1];
		// if (fragPos <= staticValue.nLen)
		// nA[fragPos]=1;
		int ndis = (int) (staticValue.nLen * fragPos / pep.seqAA.length());
		nA[ndis] = 1;

		return nA;
	}

	// static float[] getDikeA(Peptide pep, int fragPos) {
	// float[] dikeA = new float[20];
	// if (fragPos >= staticValue.dickLen)
	// return dikeA;
	// char ch = pep.seqAA.charAt(fragPos - 1);
	// dikeA[staticValue.getIndex(ch)] -= 1;
	// ch = pep.seqAA.charAt(fragPos);
	// dikeA[staticValue.getIndex(ch)] += 1;
	//
	// return dikeA;
	// }

	static float[][] getDikeA(Peptide pep, int fragPos) {
		float[][] dikeA = new float[staticValue.dickLen][20];
		if (fragPos >= 4)
			return dikeA;
		if (fragPos - 1 < staticValue.dickLen) {
			char ch = pep.seqAA.charAt(fragPos - 1);
			dikeA[fragPos - 1][staticValue.getIndex(ch)] = 1;
		}

		if (fragPos < staticValue.dickLen) {
			char ch = pep.seqAA.charAt(fragPos);
			dikeA[fragPos][staticValue.getIndex(ch)] = 1;
		}

		return dikeA;
	}

	static ArrayList<String> getNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (int p = staticValue.AAbegin; p <= staticValue.AAend; p++) {
			for (int i = 0; i < aa_array.length; i++) {
				char a = aa_array[i];
				names.add("AA_" + a + "_" + p);
			}
		}
		for (int i = 0; i < 2; i++) {
			char aa = 'K';
			if (i == 1) {
				aa = 'R';
			}
			for (int j = staticValue.KRbegin; j <= staticValue.KRend; j++) {
				names.add("KR" + "_" + aa + "_" + j);
			}
		}

		for (int c = 1; c <= staticValue.cLen; c++) {
			names.add("Cproton_X_" + c);
		}
		for (int c = 1; c <= staticValue.nLen; c++) {
			names.add("Ndis_X_" + c);
		}
		for (int j = 0; j < staticValue.dickLen; j++)
			for (int i = 0; i < staticValue.aa_array.length; i++) {
				names.add("dR_" + staticValue.aa_array[i] + "_" + j);
			}

		return names;

	}

}
