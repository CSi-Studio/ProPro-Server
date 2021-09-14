package net.csibio.propro.algorithm.simulator;

import java.util.ArrayList;

public class Simulator extends PeptideSpectrumPair {

    float initialIntensity = 10000;

    public Simulator(Peptide pep, Parameter parameter) {
        this.peptide = pep;
        super.parameter = parameter;
        this.addyPeaks();
        this.initialRelative();
    }

    public void initialRelative() {
        float maxI = 0;
        for (int i = 0; i < this.labelPeaks.size(); i++) {
            if (this.labelPeaks.get(i).peak.intensity > maxI) {
                maxI = this.labelPeaks.get(i).peak.intensity;
            }
        }
        for (int i = 0; i < this.labelPeaks.size(); i++) {
            this.labelPeaks.get(i).peak.relative = this.labelPeaks.get(i).peak.intensity
                    * 100 / maxI;
        }
    }

    public void addyPeaks() {
        // array;k
        String skel = peptide.seqAA;
        float inten = this.initialIntensity;

        for (int i = skel.length() - 1; i > 0; i--) {

            Ion yi = peptide.getYion(i, 1);

            ArrayList<Ion> ionList = new ArrayList<Ion>();
            ionList.add(yi);
            /*
             * iso
             */
            ionList.add(peptide.getYion_iso(i, 1));

            ionList.add(peptide.getYion_iso(i, 1));
            
            float[] iso_ratio = peptide.getYion_iso_ratio(i);

            if (Float.isNaN(inten)) {
                System.err.println(peptide.seqAA + "'s " + i + "th Y peak error!");
                return;
            }
            float addedInten = inten;

            if (i == 1) {
                char n0c = peptide.seqAA.charAt(0);
                char n1c = peptide.seqAA.charAt(1);
                int len = peptide.seqAA.length();
                @SuppressWarnings("unused")
                float exP = SimuConst.lAAProbs[SimuConst.getIndex(n0c)]
                        * SimuConst.rAAProbs[SimuConst.getIndex(n1c)];
                if (len < SimuConst.posProbs.length) {
                    exP *= SimuConst.posProbs[len];
                }
                // System.out.println("exP....."+exP);
                // if(exP>0.1){
                if (peptide.seqAA.length() >= 20)
                    addedInten = 0;

            }
            if (i == 2) {
                if (peptide.seqAA.length() > 22) {
                    addedInten = 0;
                }
            }


            PeakLabeled pi = new PeakLabeled(new Peak(yi.getPosition(), 1,
                    addedInten), ionList, iso_ratio);

            this.labelPeaks.add(pi);

            inten = this.getPeakIntensity(skel.length() - i, inten);

        }
    }

    public Float getLnRatio(int pos) {
        if (this.peptide.pepString.contains("#")) {
            return null;
        } else {
        }

        Instance instance = SimuConst.getInstance(this.peptide, pos, 0);

        double ratio = parameter.getlnR(instance);
		/*
		if(ratio==Double.NaN)
		{
			return null;
		}else
		{
			
		}
		*/
        return (float) ratio;
    }

    // compute from Y1, assumpt that the peak of Y1 is existing;
    public float getPeakIntensity(int pos, float referValue) {
        float lnRatio = this.getLnRatio(pos);
        return (float) (referValue * Math.exp(lnRatio));
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("BEGIN IONS\nPEPTIDE="
                + this.peptide.pepString + "\n");
        sb.append("CHARGE=" + this.peptide.charge + "\n");
        sb.append("MH="
                + (this.peptide.getPrecursorMass() - this.peptide.charge + 1)
                + "\n");
        for (int i = this.labelPeaks.size() - 1; i >= 0; i--) {
            PeakLabeled per = labelPeaks.get(i);
            sb.append(per.peak.position + "\t");
            sb.append(per.peak.intensity + "\n");
        }
        sb.append("END IONS" + "\n");
        return sb.toString();


    }

    public String MGF_format(float[][] peak_group, String sp_model, int rank) {
        StringBuffer sb = new StringBuffer("BEGIN IONS\r\n");
        sb.append("TITLE=" + rank + "\r\n");
        sb.append("PEPMASS=" + (this.peptide.getPrecursorMass()
                - this.peptide.charge + 1 - 19) + "\r\n");
        sb.append("CHARGE=" + this.peptide.charge + "\r\n");
        sb.append("SEQ=" + this.peptide.pepString + "\r\n");

        for (int i = 0; i <= peak_group.length - 1; i++) {
            int intens = (int) peak_group[i][1];
            if (intens > 0) {
                sb.append(peak_group[i][0] + "\t");
                sb.append(intens + "\r\n");
            }
        }
        sb.append("END IONS" + "\r\n\r\n");
        return sb.toString();
    }

}
