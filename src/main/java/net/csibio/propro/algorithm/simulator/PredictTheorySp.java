package net.csibio.propro.algorithm.simulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class PredictTheorySp {

	public double[][] getSimuPeakGroup(float[][] peak_group) {
		double[][] Re_group = new double[peak_group.length][2];
		for (int i = 0; i < peak_group.length; i++) {
			Re_group[i][0] = (double) peak_group[i][0];
			Re_group[i][1] = (double) peak_group[i][1];
		}
		return Re_group;

	}

	public double[][] predict_single_pep(String pepStr, String sp_model,
			int iso_num) {
		if (pepStr.contains("#")) {
			int index = pepStr.indexOf("#");
			pepStr = pepStr.substring(0, index) + pepStr.substring(index + 1);
		}
		Peptide peptide = new Peptide(pepStr, 2);

		if (sp_model.equals("HCD")) {
			staticValue.parameter = new Parameters1();
		} else {
			staticValue.parameter = new Parameters2();
		}
		Simulator simu = new Simulator(peptide);

		float[][] peak_group;

		if (iso_num > 0) {
			peak_group = simu.getYisoList();

		} else {
			peak_group = simu.getYList();
		}
		return getSimuPeakGroup(peak_group);
	}

	@SuppressWarnings("resource")
	public void predict(String pep_file, String out_file, String sp_model,
			int iso_num) {
		ArrayList<String> pep_list = new ArrayList<String>();
		if (sp_model.equals("HCD")) {
			staticValue.parameter = new Parameters1();
		} else {
			staticValue.parameter = new Parameters2();
		}

		try {
			BufferedReader infile = new BufferedReader(new FileReader(pep_file));
			String rline = new String();
			while ((rline = infile.readLine()) != null) {
				pep_list.add(rline.trim());
			}

			int rank = 0;
			int fence = -1;
			for (String iter_pep : pep_list) {
				rank++;
				int process = 48*rank/pep_list.size();
				if(process != fence) {
					fence = process;
				}
				BufferedWriter outfile = new BufferedWriter(new FileWriter(
						out_file, true));

				Peptide peptide = new Peptide(iter_pep, 2);
				Simulator simu = new Simulator(peptide);

				float[][] peak_group;

				if (iso_num > 0) {
					peak_group = simu.getYisoList();
					
				} else {
					peak_group = simu.getYList();
				}

				outfile.write(simu.MGF_format(peak_group, sp_model, rank));
				outfile.flush();
				outfile.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		Peptide pep = new Peptide
				("EHGEPLFSSHM(+15.99)LDLSEETDEENISTC(+57.02)VK",2);
		staticValue.parameter = new Parameters2();
		Simulator simu = new Simulator(pep);

		float[][] peak_group = simu.getYList();
		System.out.println();
	}
}
