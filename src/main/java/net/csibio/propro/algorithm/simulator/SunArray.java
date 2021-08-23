package net.csibio.propro.algorithm.simulator;
import java.util.ArrayList;

public class SunArray {
	static public void print(float[] b) {
		for (int i = 0; i < b.length; i++) {
			System.out.print(b[i] + "\t");
		}
		System.out.println();
	}
	
	static public void print(double[] b) {
		for (int i = 0; i < b.length; i++) {
			System.out.print(b[i] + "\t");
		}
		System.out.println();
	}

	static public void print(int[] b) {
		for (int i = 0; i < b.length; i++) {
			System.out.print(b[i] + "\t");
		}
		System.out.println();
	}
	static public void print(int[] b,int threshold) {
		for (int i = 0; i < b.length; i++) {
			if(b[i]>threshold)
				System.out.println(i+"\t"+ b[i] );
		}
	}


	static public void print(String[] b) {
		for (int i = 0; i < b.length; i++) {
			System.out.print(b[i] + "\t");
		}
		System.out.println();
	}

	static public void printT(float[][] b) {
		for (int i = 0; i < b[0].length; i++) {

			for (int j = 0; j < b.length; j++) {
				System.out.print(b[j][i] + "\t");
			}
			System.out.println();

		}
		System.out.println();
	}
	static public void print(float[][] b) {
		for (int i = 0; i < b.length; i++) {

			for (int j = 0; j < b[0].length; j++) {
				System.out.print(b[i][j] + "\t");
			}
			System.out.println();

		}
		System.out.println();
	}

	static public void print(int[][] b) {
		for (int i = 0; i < b.length; i++) {

			for (int j = 0; j < b[0].length; j++) {
				System.out.print(b[i][j] + "\t");
			}
			System.out.println();

		}
		System.out.println();
	}

	static public float[][] zero(float[][] b) {
		for (int i = 0; i < b.length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				b[i][j] = 0;
			}
		}
		return b;
	}

	static public float[] zero(float[] b) {
		for (int i = 0; i < b.length; i++) {
			b[i] = 0;
		}
		return b;
	}
	static public int max(float[] b) {
		float maxValue = b[0];
		int maxIndex = 0;
		for (int i = 1; i < b.length; i++) {
			if(b[i]>maxValue){
				maxValue = b[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	static public int getNonZeroNum(float[] b) {
		int num=0;
		for (int i = 0; i < b.length; i++) {
			if(b[i]!=0)
				num++;
		}
		return num;
	}

	static public boolean isZero(int[] a){
		for(int i=0;i<a.length;i++){
			if(a[i]!=0) return false;
		}
		return true;
	}
	static public boolean onlyNoZeroAt(int[] a, int index){
		if(index<0||index>a.length){
			System.err.println("Manipulate.onlyNoZeroAt error");
			return false;
		}
		if(a[index]==0) return false;
		for(int i=0;i<a.length;i++){
			if(i!=index){
				if(a[i]!=0) return false;
			}
		}
		return true;
	}

	static public float[] fillWith(float[] b, float a) {
		for (int i = 0; i < b.length; i++) {
			b[i] = a;
		}
		return b;

	}
	static public float[] nomalizeWith(float[] b,float a) {
		float[] c = new float[b.length];
		for (int i = 0; i < b.length; i++) {
			c[i] = b[i]/a;
		}
		return c;
	}


	static public float[][] fillWith(float[][] b, float a) {
		for (int i = 0; i < b.length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				b[i][j] = a;
			}
		}
		return b;
	}

	// Descending the Array b according to b[0];
	static public float[][] descend(float[][] b) {
		// printArray(b);
		// ****************************************************
		// rerank the matrix according to the b[0];

		for (int i = 1; i < b.length; i++) {
			float[] m = new float[b[0].length];
			for (int p = 0; p < b[0].length; p++) {
				m[p] = b[i][p];
			}
			for (int j = 0; j < i; j++) {
				if (m[0] > b[j][0]) {
					for (int k = i; k > j; k--) {
						for (int q = 0; q < b[0].length; q++) {
							b[k][q] = b[k - 1][q];
						}
					}
					for (int q = 0; q < b[0].length; q++) {
						b[j][q] = m[q];
					}
					break;
				}
			}
		}

		return b;
	}
	static public float[][] descend(float[][] b, int index) {
		// printArray(b);
		// ****************************************************
		// rerank the matrix according to the b[index];

		for (int i = 1; i < b.length; i++) {
			float[] m = new float[b[0].length];
			for (int p = 0; p < b[0].length; p++) {
				m[p] = b[i][p];
			}
			for (int j = 0; j < i; j++) {
				if (m[index] > b[j][index]) {
					for (int k = i; k > j; k--) {
						for (int q = 0; q < b[0].length; q++) {
							b[k][q] = b[k - 1][q];
						}
					}
					for (int q = 0; q < b[0].length; q++) {
						b[j][q] = m[q];
					}
					break;
				}
			}
		}

		return b;
	}
	static public float[][] ascend(float[][] b) {
		// printArray(b);
		// ****************************************************
		// rerank the matrix according to the b[0];

		for (int i = 1; i < b.length; i++) {
			float[] m = new float[b[0].length];
			for (int p = 0; p < b[0].length; p++) {
				m[p] = b[i][p];
			}
			for (int j = 0; j < i; j++) {
				if (m[0] < b[j][0]) {
					for (int k = i; k > j; k--) {
						for (int q = 0; q < b[0].length; q++) {
							b[k][q] = b[k - 1][q];
						}
					}
					for (int q = 0; q < b[0].length; q++) {
						b[j][q] = m[q];
					}
					break;
				}
			}
		}

		return b;
	}
	static public int[] ascend(int[] b) {
		// printArray(b);
		// ****************************************************
		// rerank the matrix according to the b[0];

		for (int i = 1; i < b.length; i++) {
			int m = b[i];
			for (int j = 0; j < i; j++) {
				if (m < b[j]) {
					for (int k = i; k > j; k--) {
						b[k] = b[k - 1];
					}
					b[j] = m;
					break;
				}
			}
		}

		return b;
	}

	static public int[] trim(int[] pathIndex) {

		int i;
		for (i = pathIndex.length - 1; i >= 0; i--) {
			if (pathIndex[i] != 0)
				break;
		}
		int[] trimArray = new int[i + 1];
		for (; i >= 0; i--) {
			trimArray[i] = pathIndex[i];
		}
		return trimArray;
	}
	static public float[] trim(float[] pathIndex) {

		int i;
		for (i = pathIndex.length - 1; i >= 0; i--) {
			if (pathIndex[i] != 0)
				break;
		}
		float[] trimArray = new float[i + 1];
		for (; i >= 0; i--) {
			trimArray[i] = pathIndex[i];
		}
		return trimArray;
	}
	

	static public float[][] trim(float[] d1, float[] d2) {
		int nonzero = d1.length;

		for (int i = 0; i < d1.length; i++) {
			if (d1[i] == 0 && d2[i] == 0)
				nonzero--;
		}
		float[][] trimArray = new float[2][nonzero];

		for (int i = d1.length - 1; i >= 0; i--) {
			if (d1[i] == 0 && d2[i] == 0) {
			} else {
				trimArray[0][nonzero - 1] = d1[i];
				trimArray[1][nonzero - 1] = d2[i];
				nonzero--;
			}
		}

		return trimArray;
	}
	static public float[][] trim(float[][] d) {
		if (d.length == 0)
			return new float[0][2];
		int non_zero_num = 0;
		for (int i = 0; i < d.length; i++) {
			boolean b = false;
			for (int j = 0; j < d[0].length; j++) {
				if (d[i][j] != 0) {
					b = true;
				}
			}
			if (b)
				non_zero_num++;
		}
		float[][] trimArray = new float[non_zero_num][d[0].length];
		int index = 0;
		for (int i = 0; i < d.length; i++) {
			boolean b = false;
			for (int j = 0; j < d[0].length; j++) {
				if (d[i][j] != 0) {
					b = true;
				}
			}
			if (b) {
				for (int j = 0; j < d[0].length; j++) {
					trimArray[index][j] = d[i][j];
				}
				index++;
			}
		}
		return trimArray;
	}

	static public float[][] delZero(float[][] d) {
		/*
		 * for array float[n][2]. float[n][0] contains the position
		 * information; float[n][1] contains the intensity information [2]
		 * could be [3],...[i]; this function is del d[i] whose intensity is 0;
		 */
		if (d.length == 0)
			return new float[0][2];
		int non_zero_num = 0;
		for (int i = 0; i < d.length; i++) {
			boolean b = false;
			for (int j = 1; j < d[0].length; j++) {
				if (d[i][j] != 0) {
					b = true;
				}
			}
			if (b)
				non_zero_num++;
		}
		float[][] trimArray = new float[non_zero_num][d[0].length];
		int index = 0;
		for (int i = 0; i < d.length; i++) {
			boolean b = false;
			for (int j = 1; j < d[0].length; j++) {
				if (d[i][j] != 0) {
					b = true;
				}
			}
			if (b) {
				for (int j = 0; j < d[0].length; j++) {
					trimArray[index][j] = d[i][j];
				}
				index++;
			}
		}
		return trimArray;
	}

	static public float[][] del_small_value_4_intensity_Array(float[][] d,
			float thres) {
		/*
		 * for array float[n][2]. float[n][0] contains the position
		 * information; float[n][1] contains the intensity information [2]
		 * could be [3],...[i]; this function is del d[i] whose intensity is 0;
		 */
		if (d.length == 0 || d[0].length != 2)
			return new float[0][2];
		int val_num = 0;
		for (int i = 0; i < d.length; i++) {
			if (d[i][1] > thres) {
				val_num++;
			}
		}
		float[][] trimArray = new float[val_num][2];
		int index = 0;
		for (int i = 0; i < d.length; i++) {
			if (d[i][1] > thres) {
				for (int j = 0; j < d[0].length; j++) {
					trimArray[index][j] = d[i][j];
				}
				index++;
			}
		}
		return trimArray;
	}
	static public void intensityArrayPositionShift(float[][] intensityArray,
			float beginMass, float shift) {
		for (int i = 0; i < intensityArray.length; i++) {
			if (intensityArray[i][0] >= beginMass - 0.5)
				intensityArray[i][0] += shift;
		}
		// return intensityArray;
	}

	// merge array a and b to one array;
	static public float[][] merge(float[][] a, float[][] b) {
		if (a == null || a.length == 0)
			return b;
		if (b == null || b.length == 0)
			return a;

		if (a[0].length != b[0].length) {

			print(a);
			System.out.println("The dimension does not pair!!! " + a[0].length
					+ "\t" + b[0].length);
			print(b);
			return new float[0][0];
		}
		float[][] m = new float[a.length + b.length][a[0].length];
		int i = 0;
		for (; i < a.length; i++) {
			for (int j = 0; j < a[0].length; j++) {
				m[i][j] = a[i][j];
			}
		}
		for (; i < m.length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				m[i][j] = b[i - a.length][j];
			}
		}
		return m;
	}
	static public int[] merge(int[] a, int[] b) {
		if (a == null || a.length == 0)
			return b;
		if (b == null || b.length == 0)
			return a;

		int[] m = new int[a.length + b.length];
		int i = 0;
		for (; i < a.length; i++) {
			m[i] = a[i];
		}
		for (; i < m.length; i++) {
			m[i] = b[i - a.length];
		}
		return m;
	}

	static public float sum(float[] d) {
		float sum = 0;
		int i = 0;
		for (; i < d.length; i++) {
			sum += d[i];
		}
		return sum;
	}
	static public float sumFisrtN(float[] d, int n) {
		if (n > d.length) {
			System.err
					.println("Error From ArrayManipulate.sumFristN float[] d, int n: n is bigger than the length of d");
			return -1;
		}
		if (n < 0) {
			System.err
					.println("Error From ArrayManipulate.sumFristN float[] d, int n : n is less than 0");
			return -1;

		}
		float sum = 0;
		int i = 0;
		for (; i < n; i++) {
			sum += d[i];
		}
		return sum;
	}
	static public int sumFisrtN(int[] d, int n) {
		if (n > d.length) {
			System.err
					.println("Error From ArrayManipulate.sumFristN(int[] d, int n):n is bigger than the length of d");
			return -1;
		}

		int sum = 0;
		int i = 0;
		for (; i < n; i++) {
			sum += d[i];
		}
		return sum;
	}

	static public int sum(int[] d) {
		int sum = 0;
		int i = 0;
		for (; i < d.length; i++) {
			sum += d[i];
		}
		return sum;
	}


	static public float[] divide(float[] d, float v) {
		int i = 0;
		for (; i < d.length; i++) {
			d[i] = d[i] / v;
		}
		return d;
	}
	static public float[] insertDescend(float[] d, float v) {
		int i = 0;
		for (; i < d.length; i++) {
			if (v > d[i])
				break;
		}
		for (int j = d.length - 1; j > i; j--) {
			d[j] = d[j - 1];
		}
		if (i == d.length)
			i--;
		d[i] = v;
		return d;
	}
	static public float[][] insertDescend(float[][] d, float[] v) {
		if (d[0].length != v.length) {
			System.out.println("Sorry, the format of the input data is wrong!");
			return null;
		}
		int i = 0;
		for (; i < d.length; i++) {
			if (v[0] > d[i][0])
				break;
		}
		for (int j = d.length - 1; j > i; j--) {
			for (int s = 0; s < v.length; s++) {
				d[j][s] = d[j - 1][s];
			}
		}
		if (i < d.length) {
			for (int s = 0; s < v.length; s++) {
				d[i][s] = v[s];
			}
		}

		return d;
	}
	static public float[][] descendArray_insert_for_intensityArray(
			float[][] d, float[] v) {
		if (d[0].length != 2 || v.length != 2) {
			System.out.println("Sorry, the format of the input data is wrong!");
			return null;
		}
		int i = 0;
		for (; i < d.length; i++) {
			if (v[1] > d[i][1])
				break;
		}
		for (int j = d.length - 1; j > i; j--) {
			for (int s = 0; s < v.length; s++) {
				d[j][s] = d[j - 1][s];
			}
		}
		if (i <= d.length - 1) {
			for (int s = 0; s < v.length; s++) {
				d[i][s] = v[s];
			}
		}

		return d;
	}

	static public float[] descend(float[] b) {
		// rerank the vector;

		for (int i = 1; i < b.length; i++) {
			float m = b[i];
			for (int j = 0; j < i; j++) {
				if (m > b[j]) {
					for (int k = i; k > j; k--) {
						b[k] = b[k - 1];
					}
					b[j] = m;
					break;
				}
			}
		}

		return b;
	}

	static public float getSimilarity(float[] d1, float[] d2) {
		float squareD1 = 0;
		float squareD2 = 0;
		for (int i = 0; i < d1.length; i++) {
			squareD1 += d1[i];
			squareD2 += d2[i];
		}
		// float k=squareD2/squareD1;
		float convolution = 0;
		float denominator = (float)(Math.sqrt(squareD1) * Math.sqrt(squareD2));
		for (int i = 0; i < d1.length; i++) {
			convolution += Math.sqrt(d1[i] * d2[i]);
			// denominator +=(k*d1[i]+d2[i])/2;
		}
		return convolution / denominator;

	}
	static public float[][] transpose(float[][] d) {
		if (d == null || d.length == 0)
			return null;
		float[][] tempd = new float[d[0].length][d.length];
		for (int i = 0; i < d.length; i++) {
			for (int j = 0; j < d[0].length; j++) {
				tempd[j][i] = d[i][j];

			}
		}
		return tempd;
	}
	static public double[][] transpose(double[][] d) {
		if (d == null || d.length == 0)
			return null;
		double[][] tempd = new double[d[0].length][d.length];
		for (int i = 0; i < d.length; i++) {
			for (int j = 0; j < d[0].length; j++) {
				tempd[j][i] = d[i][j];

			}
		}
		return tempd;
	}

	static public float[] deleteAt(float[] d, int del_i) {
		if (del_i >= d.length){
			System.err.println("deleteAt() error overflow!");
			return d;
		}
			
		float[] temp = new float[d.length - 1];
		for (int i = 0; i < d.length; i++) {
			if (i < del_i) {
				temp[i] = d[i];
			} else if (i > del_i) {
				temp[i - 1] = d[i];
			}
		}
		return temp;
	}
	static public int[] deleteAt(int[] d, int index) {
		if (index >= d.length)
			return d;
		int[] temp = new int[d.length - 1];
		for (int i = 0; i < d.length; i++) {
			if (i < index) {
				temp[i] = d[i];
			} else if (i > index) {
				temp[i - 1] = d[i];
			}
		}
		return temp;
	}

	static public int[] insertAt(int[] d, int index, int x) {
		if (index > d.length)
			return d;
		int[] temp = new int[d.length + 1];
		for (int i = 0; i < d.length; i++) {
			if (i < index) {
				temp[i] = d[i];
			} else if (i > index) {
				temp[i + 1] = d[i];
			}
		}
		temp[index] = x;
		return temp;
	}

	static public DiscriptiveStatistics statistics(float[] d) {
		/*
		 * float[0]: Ntotal float[1]: mean; float[2]: standard deviation
		 * float[3]: Sum float[4]: minimum float[5]: median float[6]:
		 * maximum float[7]:mean of the three middle value;p
		 */
		float[] temp = new float[8];
		temp[0] = d.length;
		for (int i = 0; i < d.length; i++) {
			temp[1] += d[i];
			temp[2] += d[i] * d[i];
		}
		temp[3] = temp[1];
		temp[1] = temp[1] / temp[0];
		temp[2] = temp[2] - temp[0] * temp[1] * temp[1];
		temp[2] = (float) Math.sqrt(temp[2] / (temp[0] - 1));
		d = descend(d);
		temp[4] = d[d.length - 1];
		temp[5] = d[d.length / 2];
		temp[6] = d[0];
		// for(int i=3;i<)
		temp[7] = d[d.length / 2 - 1] + d[d.length / 2] + d[d.length / 2 + 1];

		return new DiscriptiveStatistics(temp);

	}
	static public float[] clone(float[] a) {
		float[] b = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
		}
		return b;
	}
	static public float[][] clone(float[][] a) {
		float[][] b = new float[a.length][a[0].length];
		for (int i = 0; i < a.length; i++) {
			for(int j=0;j<a[0].length;j++){
				b[i][j] = a[i][j];
			}
		}
		return b;
	}

	static public ArrayList<int[]> combination(int[] a){
		ArrayList<int[]> al = new ArrayList<int[]>();
		al.add(new int[0]);
		for(int i=0;i<a.length;i++){
			ArrayList<int[]> tempal = new ArrayList<int[]>();
			for(int z=0;z<a[i];z++){
				int[] b ={z};
				for(int l=0;l<al.size();l++){
					tempal.add(merge(al.get(l),b));
				}
			}
			al = tempal;
		}
		return al;
	}

	static public int[] clone(int[] a) {
		int[] b = new int[a.length];
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i];
		}
		return b;
	}
	static public int[] reverse(int[] a) {
		int[] b = new int[a.length];
		for (int i = 0; i < a.length; i++) {
			b[b.length-1-i] = a[i];
		}
		return b;
	}
	static public float[] reverse(float[] a) {
		float[] b = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			b[b.length-1-i] = a[i];
		}
		return b;
	}


	static public boolean isSame(int[] a, int[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}
	static public int[] minus(int[] a, int[] b) {
		
		if (a.length != b.length)
			return null;
		int[] c = new int[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i]-b[i];
		}
		return c;
	}
	static public float[] minus(float[] a, float[] b) {
		
		if (a.length != b.length)
			return null;
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i]-b[i];
		}
		return c;
	}
	static public float[] minus(float[] a, float b) {
		
		float[] c = new float[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = a[i]-b;
		}
		return c;
	}



	public static void fillWith(int[][] b, int a) {
		for (int i = 0; i < b.length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				b[i][j] = a;
			}
		}
	}
	public static float getPartialSimilarity(float[] a, float[] b,int beginPosition){
		if(beginPosition<0) return -1;
		if(beginPosition+b.length>a.length) return -2;
		float[] c = new float[b.length];
		for(int i=0;i<c.length;i++){
			c[i]=a[i+beginPosition];
		}
		return getSimilarity(c,b);
		
	}
	public static float[] merge(float[] a, float[] b) {
		if (a == null || a.length == 0)
			return b;
		if (b == null || b.length == 0)
			return a;

		float[] m = new float[a.length + b.length];
		int i = 0;
		for (; i < a.length; i++) {
			m[i] = a[i];
		}
		for (; i < m.length; i++) {
			m[i] = b[i - a.length];
		}
		return m;
	}
	public static float[] deleteFirst(float[] a,int n) {
		float[] c = new float[a.length-n];
		if (n<a.length)
			return new float[0];
		int i = 0;
		for (i=n; i < c.length; i++) {
			c[i] = a[i - n];
		}
		return c;
	}
	public static float cosine(float[] a,float[] b) {
		if(a.length!= b.length)
			return Float.NaN;
		float ab = 0;
		float aLen = 0;
		float bLen = 0;

		for (int i=0; i < b.length; i++) {
			ab += a[i]*b[i];
			aLen += a[i]*a[i];
			bLen += b[i]*b[i];
		}
		return (float) (ab/Math.sqrt(aLen*bLen));
	}
	public static void main(String[] args) throws Exception {
		System.out.println("main???");
		float[] a1= {1,0};
		float[] a2 = {-1,0};
		System.out.println(cosine(a1,a2));
		
	}
	public static int[][] trim(int[][] d) {
		if (d.length == 0)
			return new int[0][2];
		int non_zero_num = 0;
		for (int i = 0; i < d.length; i++) {
			boolean b = false;
			for (int j = 0; j < d[0].length; j++) {
				if (d[i][j] != 0) {
					b = true;
				}
			}
			if (b)
				non_zero_num++;
		}
		int[][] trimArray = new int[non_zero_num][d[0].length];
		int index = 0;
		for (int i = 0; i < d.length; i++) {
			boolean b = false;
			for (int j = 0; j < d[0].length; j++) {
				if (d[i][j] != 0) {
					b = true;
				}
			}
			if (b) {
				for (int j = 0; j < d[0].length; j++) {
					trimArray[index][j] = d[i][j];
				}
				index++;
			}
		}
		return trimArray;
	}
	public static float[][] subArray(float[][] d, int beginIndex,
			int endIndex) {
		if(endIndex<beginIndex){
			return null;
		}
		float[][] trimArray = new float[endIndex-beginIndex][d[0].length];
		for (int i = beginIndex; i < endIndex; i++) {
			for (int j = 0; j < d[0].length; j++) {
				trimArray[i-beginIndex][j] = d[i][j];
			}
		}
		return trimArray;
	}

	public static int[][] subArray(int[][] d, int beginIndex, int endIndex) {
		if(endIndex<beginIndex){
			return null;
		}
		int[][] trimArray = new int[endIndex-beginIndex][d[0].length];
		for (int i = beginIndex; i < endIndex; i++) {
			for (int j = 0; j < d[0].length; j++) {
				trimArray[i-beginIndex][j] = d[i][j];
			}
		}
		return trimArray;
	}
	public static float[] subArray(float[] d, int beginIndex, int endIndex) {
		if(endIndex<beginIndex){
			return null;
		}
		if(endIndex>d.length) return null;
		float[] trimArray = new float[endIndex-beginIndex];
		for (int i = beginIndex; i < endIndex; i++) {
				trimArray[i-beginIndex] = d[i];
		}
		return trimArray;
	}
	
	public static int[] subArray(int[] d, int beginIndex, int endIndex) {
		if(endIndex<beginIndex){
			return null;
		}
		if(endIndex>d.length) return null;
		int[] trimArray = new int[endIndex-beginIndex];
		for (int i = beginIndex; i < endIndex; i++) {
				trimArray[i-beginIndex] = d[i];
		}
		return trimArray;
	}

	/*
	 * get the parameters of the linear regression
	 * Y=aX+b
	 * parameters[0] is the a(slope)
	 * parameters[1] is the b(intercept)
	 * parameters[2] is the 
	 * parameters[3] is the correlation
	 */
	static public DiscriptiveLinearRegression linearRegression(float[] x,float[] y){
		DiscriptiveLinearRegression parameters=
				new DiscriptiveLinearRegression();
		float SYY=0;
		float SXX=0;
		float SXY=0;
		float Ymean=0;
		float Xmean=0;
		for(int i=0;i<y.length;i++){
			Xmean+=x[i]/x.length;
			Ymean+=y[i]/y.length;
		}
		for(int i=0;i<y.length;i++){
			SXX+=Math.pow((x[i]-Xmean), 2);
			SYY+=Math.pow((y[i]-Ymean), 2);
			SXY+=(x[i]-Xmean)*(y[i]-Ymean);
		}
		float RSS = SYY-SXY*SXY/SXX;

		float RSquare= 1-RSS/SYY;
		if(SYY==0) RSquare=0;
		float beta1 = SXY/SXX;
		float beta0 = Ymean-beta1*Xmean;
		//float thetaV2=RSS/(x.length-2);
		
		parameters.slope=beta1;
		parameters.intercept=beta0;
		if(beta1>0){
			parameters.correlation=Math.sqrt(RSquare);
		}
		else parameters.correlation=0-Math.sqrt(RSquare);
		//float tr=parameters[3]/Math.sqrt((1-parameters[3]*parameters[3])/(d1.length-2));
		
		//float confidence=1-Probability.studentT(d1.length-2, tr);
		return parameters;
	}
	
	static public float correlation(float[] d1,float[] d2){
		if(d1.length!=d2.length){
			System.err.println("Correlation error: the dimension is not compatable");
			return Float.NaN;
		}
		float pc=0;
		float sigmaXY= 0;
		float sigmaX = 0;
		float sigmaY = 0;
		float sigmaX2 =0;
		float sigmaY2 =0;
		for(int i=0;i<d1.length;i++){
			sigmaXY+=d1[i]*d2[i];
			sigmaX+=d1[i];
			sigmaY+=d2[i];
			sigmaX2+= d1[i]*d1[i];
			sigmaY2+=d2[i]*d2[i];
			
		}
		pc=sigmaXY-sigmaX*sigmaY/d1.length;
		float den = (float)Math.sqrt((sigmaX2-sigmaX*sigmaX/d1.length)*(sigmaY2-sigmaY*sigmaY/d1.length));
		float r=pc/den;
		if(d1.length<3) return Float.NaN;
		//float tr=r/Math.sqrt((1-r*r)/(d1.length-2));
		return r;
	}
	static public float getInnerProduct(float[] d1, float[] d2){
		
		if (d1.length != d2.length) {
			System.err
					.println("InnerProduct error: the dimension is not compatable");
			return Float.NaN;
		}

		if(d1.length!= d2.length) return Float.NaN;
		
//		SunArray.print(d1);
//		SunArray.print(d2);

		float pc=0;
		for(int i=0;i<d1.length;i++){
			pc += d1[i]*d2[i];
		}
		return pc;

	}
	
	static public float getInnerProduct(float[][] d1, float[][] d2){
		if(d1.length!= d2.length||d1[0].length!=d2[0].length) {
			System.err.println("Get Inner product Error: SunArray.getInnerProduct");
			return Float.NaN;
		}
//		SunArray.print(d1);
//		SunArray.print(d2);
		float pc=0;
		for(int i=0;i<d1.length;i++){
			for(int j =0;j<d1[0].length;j++)
				pc += d1[i][j]*d2[i][j];
		}
		return pc;

	}



}
