package net.csibio.propro.algorithm.simulator;

import java.text.DecimalFormat;

public class FormatNum {
	public static double DoubleFormat2(double dValue, int decimal_num) {
		DecimalFormat df2;
		switch (decimal_num) {
		case 0:
			df2 = new DecimalFormat("##");
			break;
		case 1:
			df2 = new DecimalFormat("##.0");
			break;
		case 2:
			df2 = new DecimalFormat("##.00");
			break;
		case 3:
			df2 = new DecimalFormat("##.000");
			break;
		case 4:
			df2 = new DecimalFormat("##.0000");
			break;
		case 5:
			df2 = new DecimalFormat("##.00000");
			break;
		default:
			df2 = new DecimalFormat("##.000");
			break;
		}
		return Double.parseDouble(df2.format(dValue));
	}

	public static double DoubleFormat(double dValue, int decimal_num) {

		double formated_value = 0.0;
		double factor = 1;
		switch (decimal_num) {
		case 0:
			factor = 1.0;
			break;
		case 1:
			factor = 10.0;
			break;
		case 2:
			factor = 100.0;
			break;
		case 3:
			factor = 1000.0;
			break;
		case 4:
			factor = 10000.0;
			break;
		case 5:
			factor = 100000.0;
			break;
		}

		formated_value = Math.round(dValue * factor) / factor;
		return formated_value;
	}

}
