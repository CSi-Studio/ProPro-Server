package net.csibio.propro.algorithm.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/*
 * The peaks in i_a or the dta should be ranked by Mass/Charge. 
 */
public class Spectrum {
	public ArrayList<PeakMain> peakList = new ArrayList<PeakMain>();
	public float precuPosition = 0;
	public float MH = 0;
	public int charge = 0;
	public float fragmentError;
	public float precusorError;
	public float intensityThres;
	public float maxMass = 0;
	public float relative;
	public float maxPosition = 0;
	private float maxIntensity = 0;
	public float allRelative = 0;

	public Spectrum(String dtaFileName) {

		this.fragmentError = ConstantValue.posDelta;

		this.precusorError = ConstantValue.precusorDelta;
		this.intensityThres = ConstantValue.intensityTres;

		try {
			BufferedReader sq = new BufferedReader(new FileReader(dtaFileName));

			String a = sq.readLine();
			StringTokenizer st;

			if (a.indexOf("\t") > -1) {
				st = new StringTokenizer(a, "\t");
			} else {
				st = new StringTokenizer(a, " ");
			}
			this.MH = Float.parseFloat((String) st.nextToken());

			this.charge = Integer.parseInt((String) st.nextToken());
			this.precuPosition = (MH + charge) / charge;
			a = sq.readLine();

			while (a != null) {

				if (a.indexOf("\t") > -1) {
					st = new StringTokenizer(a, "\t");
				} else {
					st = new StringTokenizer(a, " ");
				}
				// add the peak to IA;
				// System.out.println(a);
				float pos = Float.parseFloat((String) st.nextToken());

				float intensity = Float.parseFloat((String) st.nextElement());

				PeakMain perP = new PeakMain(pos, 1, intensity, 0);

				this.addPeakToPeakList(perP);
				a = sq.readLine();
			}
			// System.out.println("hello");
			sq.close();

		} catch (Exception e) {

			System.out.println("Sorry, the type of file " + " isn't correct");
			e.printStackTrace();

		}
		this.maxMass = this.precuPosition * this.charge + this.charge;
		// System.out.println("maxIntensity\t:"+this.maxIntensity);

		parseCharge();
		this.relativePeakList();
		this.clusterIsotope();
		this.parsePeaksRelation();
		// this.print();

		// this.parsePeaksRelation();

	}

	public Spectrum(float[][] spectrumContent) {

		this.fragmentError = ConstantValue.posDelta;

		this.precusorError = ConstantValue.precusorDelta;
		this.intensityThres = ConstantValue.intensityTres;
		this.MH = spectrumContent[0][0];
		this.charge = (int) spectrumContent[0][1];
		this.precuPosition = (MH + charge) / charge;

		for (int i = 1; i < spectrumContent.length; i++) {
			PeakMain perP = new PeakMain(spectrumContent[i][0], 1,
					spectrumContent[i][1], 0);
			this.addPeakToPeakList(perP);
		}
		this.maxMass = this.precuPosition * this.charge + this.charge;
		// System.out.println("maxIntensity\t:"+this.maxIntensity);

		parseCharge();
		this.relativePeakList();
		this.clusterIsotope();
		this.parsePeaksRelation();
		// this.print();

		// this.parsePeaksRelation();

	}

	private void relativePeakList() {
		for (int i = 2; i < this.peakList.size(); i++) {
			float rela = 100 * peakList.get(i).intensity / maxIntensity;
			this.peakList.get(i).relative = rela;
			this.allRelative += rela;
		}
	}

	private void parseCharge() {
		// System.out.println("parsee");
		for (int i = 0; i < this.peakList.size(); i++) {
			PeakMain ggpi = this.peakList.get(i);
			float isoShift = ConstantValue.derivativeMass[1];
			float[][] isoA = new float[charge + 1][3];
			int[][] index = new int[charge + 1][3];
			for (int c = 1; c < isoA.length; c++) {
				isoA[c][0] = ggpi.intensity;
				index[c][0] = i;
			}
			for (int j = i + 1; j < this.peakList.size(); j++) {
				PeakMain ggpj = this.peakList.get(j);
				// float posiDe= ;
				if (ggpj.position - ggpi.position > 4.5) {
					break;
				}
				for (int c = 1; c < isoA.length; c++) {
					for (int s = 1; s < 3; s++) {
						if (Math.abs(ggpj.position - ggpi.position - s
								* isoShift / c) < ConstantValue.isoDelta) {
							if (isoA[c][s - 1] > 0) {
								isoA[c][s] = ggpj.intensity;
								index[c][s] = j;
							}
						}
					}
				}

			}
			float sim1 = SunArray.getSimilarity(getIsoDis(ggpi.position, 1),
					isoA[1]);
			int cha1pn = SunArray.getNonZeroNum(isoA[1]);
			float score = sim1 * cha1pn;
			int candiCh = 1;
			for (int c = 2; c < isoA.length; c++) {
				float sim = SunArray.getSimilarity(getIsoDis(ggpi.position, c),
						isoA[c]);
				int chacpn = SunArray.getNonZeroNum(isoA[c]);
				if (chacpn * sim > score) {
					score = chacpn * sim;
					candiCh = c;

				}
			}
			// System.out.println(ggpi.peakPosition+"\t"+ggpi.intensity+"\t");
			for (int s = 0; s < index[candiCh].length; s++) {
				if (index[candiCh][s] > 0) {
					peakList.get(index[candiCh][s]).charge = candiCh;
					i = index[candiCh][s];
					// System.out.println(peakList.get(index[candiCh][s]).peakPosition);
				}
				//
			}

		}

		for (int i = 0; i < this.peakList.size(); i++) {
			PeakMain pi = this.peakList.get(i);
			if (pi.charge > 1) {
				this.peakList.remove(i);
				i--;
				pi.position = pi.position * pi.charge - pi.charge + 1;
				pi.charge = 1;
				this.addPeakToPeakList(pi);
			}
		}

	}

	private void clusterIsotope() {
		for (int i = 0; i < this.peakList.size(); i++) {
			int[] cClu = this.selectIsoClusterAround(i);
			int cMi = this.selectMainPeakFromCluster(cClu);

			if (cMi >= 0) {
				PeakMain pmi = this.peakList.get(cClu[cMi]);
				for (int c = cMi + 1; c < cClu.length; c++) {
					if (cClu[c] > 0) {
						PeakMain pmc = this.peakList.get(cClu[c]);
						pmi.isoList.add(new PeakIso(pmc, c - cMi));
					}
				}
				for (int c = cClu.length - 1; c > cMi; c--) {
					if (cClu[c] > 0)
						this.peakList.remove(cClu[c]);
				}

			}
		}
	}

	private int selectMainPeakFromCluster(int[] peakInds) {
		if (peakInds[1] < 1)
			return -1;
		float[] intens = new float[peakInds.length];
		for (int i = 0; i < peakInds.length; i++) {
			intens[i] = this.peakList.get(peakInds[i]).intensity;
		}
		float[] theoDis = this.getIsoDis(
				this.peakList.get(peakInds[0]).position, 1);
		int mainIndex = 0;
		float sim = 0;
		// SunArray.print(peakInds);
		for (int i = 0; i < 3; i++) {
			float simi = SunArray.getPartialSimilarity(intens, theoDis, i);

			// System.out.println(peakList.get(peakInds[0]).peakPosition+"\t"+i+"\t"+simi);

			if (simi > sim) {
				sim = simi;
				mainIndex = i;
				if (sim > 0.95) {
					break;
				}
			}

		}
		if (sim > 0.95 && peakInds[mainIndex + 1] > 0)
			return mainIndex;
		return -1;
	}

	private int[] selectIsoClusterAround(int peakIndex) {
		int[] inds = new int[10];
		inds[0] = peakIndex;
		PeakMain peak0 = this.peakList.get(peakIndex);
		for (int i = peakIndex; i > 0; i--) {
			PeakMain pi = this.peakList.get(i);
			if (Math.abs(pi.position - peak0.position
					+ ConstantValue.derivativeMass[1]) < ConstantValue.isoDelta) {
				return inds;
			}

		}
		for (int i = peakIndex; i < this.peakList.size(); i++) {
			PeakMain pi = this.peakList.get(i);
			for (int s = 1; s < 6; s++) {
				if (Math.abs(pi.position - peak0.position - s
						* ConstantValue.derivativeMass[1]) < ConstantValue.isoDelta) {
					inds[s] = i;
				}
				if (Math.abs(pi.position - peak0.position) > 4) {
					break;
				}
			}
		}
		// SunArray.print(inds);

		return inds;

	}

	private float[] getIsoDis(float mass, int cha) {
		float[] dis = new float[3];
		dis[0] = 100;
		dis[1] = 100 * (ConstantValue.isoSlope * (mass * cha) + ConstantValue.isoIntercept);
		if (dis[1] < 0) {
			dis[1] = 0;
		}
		dis[2] = 100 * (ConstantValue.iso2Slope * (mass * cha) + ConstantValue.iso2Intercept);
		if (dis[2] < 0) {
			dis[2] = 0;
		}

		return dis;
	}

	private void parsePeaksRelation() {
		for (int i = 0; i < this.peakList.size(); i++) {
			PeakMain ggpi = this.peakList.get(i);
			float compPos = this.precuPosition * charge - ggpi.position
					* ggpi.charge;
			// System.out.println(compPos);
			for (int j = 0; j < this.peakList.size(); j++) {
				PeakMain ggpj = this.peakList.get(j);
				float posj = ggpj.position;
				if (Math.abs(posj - compPos) < ConstantValue.complementaryDelta) {
					ggpi.setComplementaryPeak(ggpj);
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private boolean isWaterLossPeak(Peak peak0, Peak peakTest) {

		float mass0 = peak0.position * peak0.charge - peak0.charge;
		float mass1 = peakTest.position * peakTest.charge - peakTest.charge;
		if (mass1 > mass0)
			return false;
		if (Math.abs(mass0 - mass1 - ConstantValue.derivativeMass[4]) < ConstantValue.lossDelta)
			return true;
		return false;
	}

	/*
	 * Add a new GAGsPeak to peakList and remain the peakList is ascended by
	 * GAGsPeak.peakPositon.
	 */

	private int addPeakToPeakList(PeakMain addedP) {
		int i = 0;
		for (; i < peakList.size(); i++) {
			PeakMain pi = peakList.get(i);
			if (pi.position > addedP.position + ConstantValue.mergeDelta) {
				peakList.add(i, addedP);
				if (addedP.intensity > this.maxIntensity) {
					this.maxIntensity = addedP.intensity;

				}

				break;
			} else if (Math.abs(pi.position - addedP.position) < ConstantValue.mergeDelta) {
				pi.intensity += addedP.intensity;
				pi.relative += addedP.relative;
				if (pi.intensity > this.maxIntensity) {
					this.maxIntensity = pi.intensity;

				}

				break;

			}
		}
		if (i >= peakList.size()) {
			peakList.add(i, addedP);
			if (addedP.intensity > this.maxIntensity) {
				this.maxIntensity = addedP.intensity;

			}

		}

		return i;
	}

	public float getAllIntensity() {
		float ai = 0;
		for (int i = 0; i < peakList.size(); i++) {
			ai += peakList.get(i).intensity;
		}
		return ai;
	}

	/*
	 * for the peaks of the spectrum,when the intensity of the peak is very low
	 * relative to the max peaks in the window of windThreshold. delete it.
	 */

	public float get_precursor_position() {
		return this.precuPosition;
	}

	public int indexMaxIntensityNear(float mass) {

		float intensity = 0;
		int index = -1;
		for (int k = 2; k < peakList.size(); k++) {
			Peak gp = peakList.get(k);
			if (gp.position > mass - fragmentError
					&& gp.position < mass + fragmentError) {// &&pIA[k][1]>bIn){
				if (gp.intensity > intensity) {
					intensity = gp.intensity;
					index = k;
				}
			}
		}
		return index;

	}

	/*
	 * return the index of the peak nearest to mass; if there is no peak in the
	 * allowable error, return -1;
	 */
	public int indexPosNearest(float pos) {
		float fragmass = pos;
		int index = -1;
		for (int k = 2; k < peakList.size(); k++) {
			Peak gp = peakList.get(k);
			if (gp.position > pos - fragmentError
					&& gp.position < pos + fragmentError) {// &&pIA[k][1]>bIn){
				if (Math.abs(gp.position - pos) < Math.abs(fragmass - pos)) {
					fragmass = gp.position;
					index = k;

				}
			}
		}
		if (fragmass == 0)
			fragmass = pos;

		return index;
	}

	public boolean isContainMdivZ(float MdivZ, float intenTres) {
		for (int k = 2; k < peakList.size(); k++) {
			Peak gp = peakList.get(k);
			if (gp.intensity > intenTres) {
				if (gp.position > MdivZ - fragmentError
						&& gp.position < MdivZ + fragmentError) {// &&pIA[k][1]>bIn){

					return true;

				}
			}
		}
		return false;
	}

	public void print() {

		for (int k = 0; k < this.peakList.size(); k++) {
			PeakMain gp = this.peakList.get(k);
			System.out.print(k + "\t");
			gp.print();
		}

	}

	public static void main(String[] args) throws Exception {
		// Spectrum spec1 = new Spectrum(ConstantValue.spectraDir
		// + "28.1061.49243.2.dta", (float)0.1, (float)0.1, 0);
	}

}