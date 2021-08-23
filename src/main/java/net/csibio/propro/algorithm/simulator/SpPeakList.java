package net.csibio.propro.algorithm.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class SpPeakList {
	public static ArrayList<double[]> getPeakList(String dta_file)
	{
		ArrayList<double[]> peak_list=new ArrayList<double[]>();
		try{
			BufferedReader infile=new BufferedReader(new FileReader(dta_file));
			String rline=new String();
			infile.readLine();
			while((rline=infile.readLine())!=null)
			{
				String[] splits=rline.split("\\s+");
				double[] peak_pair=new double[2];
				peak_pair[0]=Double.parseDouble(splits[0]);
				peak_pair[1]=Double.parseDouble(splits[1]);
				peak_list.add(peak_pair);
			}
			infile.close();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return peak_list;
	}
	public static double[][] getPeakGroup(String dta_file)
	{
		ArrayList<double[]> peak_list=new ArrayList<double[]>();
		peak_list=getPeakList(dta_file);
		double[][] peak_group=new double[peak_list.size()][2];
		int index=0;
		for(double[] iter:peak_list)
		{
			peak_group[index++]=iter;
		}
		return peak_group;
	}
	public static ArrayList<double[]> getPeakList2(String dta_file)
	{
		ArrayList<double[]> peak_list=new ArrayList<double[]>();
		try{
			BufferedReader infile=new BufferedReader(new FileReader(dta_file));
			String rline=new String();
			while((rline=infile.readLine())!=null)
			{
				String[] splits=rline.split("\\s+");
				double[] peak_pair=new double[2];
				peak_pair[0]=Double.parseDouble(splits[0]);
				peak_pair[1]=Double.parseDouble(splits[1]);
				peak_list.add(peak_pair);
			}
			infile.close();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return peak_list;
	}

}
