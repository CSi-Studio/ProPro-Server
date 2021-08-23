package net.csibio.propro.algorithm.simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

public class DTA {
	public float[][] content = new float[3000][3];

	public DTA() {
		content = new float[0][3];
	}

	public DTA(File dtafile) {
		float maxInt = 0;
		try {
			BufferedReader sq = new BufferedReader(new FileReader(dtafile));

			String a = sq.readLine();
			int index = 0;
			while (a != null) {
				StringTokenizer st;

				if (a.indexOf("\t") > -1) {
					st = new StringTokenizer(a, "\t");
				} else {
					st = new StringTokenizer(a, " ");
				}
				// add the peak to IA;
				// System.out.println(a);
				float pos = Float.parseFloat((String) st.nextToken());

				float intensity = Float.parseFloat((String) st.nextElement());

				content[index][0] = pos;
				content[index++][1] = intensity;
				if (intensity > maxInt)
					maxInt = intensity;
				a = sq.readLine();
			}
			// System.out.println("hello");
			sq.close();
			content = SunArray.subArray(content, 0, index);

			this.initRelative(maxInt);
		} catch (Exception e) {

			System.out.println("Sorry, the type of file " + " isn't correct");
			e.printStackTrace();
		}
	}

	public DTA(String dtafilename) {
		float maxInt = 0;
		try {
			BufferedReader sq = new BufferedReader(new FileReader(dtafilename));

			String a = sq.readLine();
			int index = 0;
			while (a != null) {
				StringTokenizer st;

				if (a.indexOf("\t") > -1) {
					st = new StringTokenizer(a, "\t");
				} else {
					st = new StringTokenizer(a, " ");
				}
				// add the peak to IA;
				// System.out.println(a);
				float pos = Float.parseFloat((String) st.nextToken());

				float intensity = Float.parseFloat((String) st.nextElement());

				content[index][0] = pos;
				content[index++][1] = intensity;
				if (intensity > maxInt)
					maxInt = intensity;
				a = sq.readLine();
			}
			// System.out.println("hello");
			sq.close();
			content = SunArray.subArray(content, 0, index);

			this.initRelative(maxInt);
		} catch (Exception e) {

			System.out.println("Sorry, the type of file " + " isn't correct");
			e.printStackTrace();
		}
	}

	public void initRelative(float maxInt) {
		for (int i = 1; i < content.length; i++) {
			content[i][2] = 100 * content[i][1] / maxInt;
		}

	}

}
