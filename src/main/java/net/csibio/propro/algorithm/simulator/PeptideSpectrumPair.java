package net.csibio.propro.algorithm.simulator;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class PeptideSpectrumPair {
    public Peptide peptide;
    public Spectrum spectrum;
    public Parameter parameter;
    float fragmentError;
    float precusorError;
    float intensityThres;
    public ArrayList<PeakLabeled> labelPeaks = new ArrayList<PeakLabeled>();

    DecimalFormat df = new DecimalFormat("###.###");

    public PeptideSpectrumPair() {

    }

    public PeptideSpectrumPair(Peptide pep) {
        this.fragmentError = ConstantValue.posDelta;
        this.precusorError = ConstantValue.precusorDelta;
        this.intensityThres = ConstantValue.intensityTres;
        this.peptide = pep;
        ArrayList<PeakLabeled> tempPeaks = new Simulator(pep, parameter).labelPeaks;
        for (int i = 0; i < tempPeaks.size(); i++) {
            this.addPeakList(tempPeaks.get(i));
        }
    }

    public PeptideSpectrumPair(Peptide pep, Spectrum spec) {
        peptide = pep;
        this.spectrum = spec;
        this.fragmentError = spec.fragmentError;
        this.precusorError = spec.precusorError;
        this.intensityThres = spec.intensityThres;
        for (int i = 0; i < spec.peakList.size(); i++) {
            PeakMain pmi = spec.peakList.get(i);
            addPeakList(new PeakLabeled(pmi, new ArrayList<Ion>()));
            for (int s = 0; s < pmi.isoList.size(); s++) {
                addPeakList(new PeakLabeled(pmi.isoList.get(s),
                        new ArrayList<Ion>()));
            }

        }
        this.labelPeaks();
        // this.print();
    }

    public float[][] getYList() {

        float[][] yArray = new float[peptide.seqAA.length() - 1][3];

        for (int i = 1; i < this.peptide.seqAA.length(); i++) {
            Ion yi = this.peptide.getYion(i, 1);
            yArray[i - 1][0] = yi.getPosition();
        }

        for (int i = 0; i < labelPeaks.size(); i++) {
            PeakLabeled lmp = labelPeaks.get(i);
            for (int j = 0; j < lmp.ionList.size(); j++) {
                Ion ion = lmp.ionList.get(j);
                if (ion.mainType == ConstantValue.ionTypes.Y
                        && ion.deriType == ConstantValue.derivativeTypes.main) {

                    if (lmp.peak.intensity > yArray[ion.seqAA.length() - 1][1]) {
                        yArray[ion.seqAA.length() - 1][0] = Float.parseFloat(df
                                .format(lmp.peak.position));
                        yArray[ion.seqAA.length() - 1][1] = Float.parseFloat(df
                                .format(lmp.peak.intensity));
                        yArray[ion.seqAA.length() - 1][2] = Float.parseFloat(df
                                .format(lmp.peak.relative));
                    }
                }
            }
        }
        return yArray;
    }

    public float[][] getBList() {

        float[][] bArray = new float[peptide.seqAA.length() - 1][3];

        for (int i = 1; i < this.peptide.seqAA.length(); i++) {
            Ion bi = this.peptide.getBion(i, 1);
            bArray[i - 1][0] = bi.getPosition();
        }

        for (int i = 0; i < labelPeaks.size(); i++) {
            PeakLabeled lmp = labelPeaks.get(i);
            for (int j = 0; j < lmp.ionList.size(); j++) {
                Ion ion = lmp.ionList.get(j);
                if (ion.mainType == ConstantValue.ionTypes.B
                        && ion.deriType == ConstantValue.derivativeTypes.main) {

                    if (lmp.peak.intensity > bArray[ion.seqAA.length() - 1][1]) {
                        bArray[ion.seqAA.length() - 1][0] = lmp.peak.position;
                        bArray[ion.seqAA.length() - 1][1] = lmp.peak.intensity;
                        bArray[ion.seqAA.length() - 1][2] = lmp.peak.relative;
                    }
                }
            }
        }
        return bArray;
    }

    public float[][] getYisoList() {
        float[][] yisoArray = new float[3 * (peptide.seqAA.length() - 1)][3];
        // for(int i=1;i<this.peptide.seqAA.length();i++){
        // Ion yi = this.peptide.getYion(i, 1);
        // yi.deriLossNum = 2;
        // yi.deriType = ConstantValue.derivativeTypes.iso;
        // yisoArray[i-1][0] = yi.getPosition();
        //
        // }
        for (int i = 0; i < labelPeaks.size(); i++) {
            PeakLabeled lmp = labelPeaks.get(i);
            for (int j = 0; j < lmp.ionList.size(); j++) {
                Ion ion = lmp.ionList.get(j);
                {
                    yisoArray[(ion.seqAA.length() - 1) * 3 + j][0] = Float
                            .parseFloat(df.format(lmp.peak.position + j));
                    yisoArray[(ion.seqAA.length() - 1) * 3 + j][1] = Float
                            .parseFloat(df.format(lmp.peak.intensity
                                    * lmp.iso_ratio[j] / lmp.iso_ratio[0]));
                    yisoArray[(ion.seqAA.length() - 1) * 3 + j][2] = Float
                            .parseFloat(df.format(lmp.peak.relative));
                }
            }
        }
        return yisoArray;
    }

    /*
     * public float[][] getLnYisoList(int isoNum){
     *
     * float[][] yisoArray = this.getYisoList(isoNum);
     *
     * for(int i = 0;i<yisoArray.length;i++){
     *
     * yisoArray[i][1] = (float)Math.log(yisoArray[i][1]+1); } return yisoArray;
     * }
     */
    public float[][] getLnYList() {

        float[][] yArray = this.getYList();

        for (int i = 0; i < yArray.length; i++) {
            yArray[i][1] = (float) Math.log(yArray[i][1] + 1);
        }
        for (int i = 0; i < yArray.length; i++) {
            yArray[i][2] = (float) Math.log(yArray[i][2] + 1);

        }

        return yArray;
    }

    private void addPeakList(PeakLabeled lmp) {
        int i = 0;
        for (; i < this.labelPeaks.size(); i++) {
            if (lmp.peak.position < labelPeaks.get(i).peak.position) {
                this.labelPeaks.add(i, lmp);
                break;
            }
        }
        if (i == this.labelPeaks.size()) {
            this.labelPeaks.add(lmp);
        }

    }

    public Peptide get_peptide() {
        return this.peptide;

    }

    /*
     * Cluster all the derivative peaks to the main Peaks, B,Y peaks;
     */

    private void labelPeaks() {

        for (int i = 1; i < peptide.seqAA.length(); i++) {
            for (int c = 1; c < peptide.charge; c++) {
                Ion bion = this.peptide.getBion(i, c);
                Ion yion = this.peptide.getYion(i, c);
                this.addLabel2PeakList(bion);
                this.addLabel2PeakList(yion);
                // System.out.println(i+"\t"+bion.getPosition()+"\t"+yion.getPosition());

            }

        }

    }

    private void addLabel2PeakList(Ion byion) {

        float mass = byion.getMass();

        for (int i = 0; i < this.labelPeaks.size(); i++) {
            PeakLabeled lpi = this.labelPeaks.get(i);
            float lpimass = lpi.peak.getMass();
            if (Math.abs(lpimass - mass) < ConstantValue.posDelta) {
                lpi.ionList.add(byion);
            }
            for (int t = 1; t < 4; t++) {
                if (Math.abs(lpimass
                        - (mass + t * ConstantValue.derivativeMass[1])) < ConstantValue.posDelta) {
                    Ion deriIon = new Ion(byion.seqAA, byion.massAA,
                            byion.charge, byion.mainType,
                            ConstantValue.derivativeTypes.iso, t);
                    lpi.ionList.add(deriIon);
                }
            }
            for (int t = 1; t < 2; t++) {
                if (Math.abs(lpimass
                        - (mass + t * ConstantValue.derivativeMass[2])) < ConstantValue.posDelta) {
                    Ion deriIon = new Ion(byion.seqAA, byion.massAA,
                            byion.charge, byion.mainType,
                            ConstantValue.derivativeTypes.NH3, t);
                    lpi.ionList.add(deriIon);
                }
            }

            for (int t = 1; t < 2; t++) {

                if (Math.abs(lpimass
                        - (mass + t * ConstantValue.derivativeMass[3])) < ConstantValue.posDelta) {
                    Ion deriIon = new Ion(byion.seqAA, byion.massAA,
                            byion.charge, byion.mainType,
                            ConstantValue.derivativeTypes.H2O, t);

                    lpi.ionList.add(deriIon);
                }
            }
            for (int t = 1; t < 2; t++) {
                if (Math.abs(lpimass
                        - (mass + t * ConstantValue.derivativeMass[4])) < ConstantValue.posDelta) {
                    Ion deriIon = new Ion(byion.seqAA, byion.massAA,
                            byion.charge, byion.mainType,
                            ConstantValue.derivativeTypes.CO2, t);
                    lpi.ionList.add(deriIon);
                }
            }
        }

    }

    public void print() {
        for (int i = 0; i < this.labelPeaks.size(); i++) {
            PeakLabeled lmpi = labelPeaks.get(i);
            System.out.println(i + "\t" + lmpi.peak.position + "\t"
                    + lmpi.peak.intensity);
            for (int j = 0; j < lmpi.ionList.size(); j++) {
                Ion ion = lmpi.ionList.get(j);
                ion.print("\t\t");
            }
        }
    }
//
//    public static void main(String[] args) {
//        SimuConst.parameter = new Parameters1();
//        String sequence = "AEIVQLDLGNLPEGALALEK";
//        Peptide peptide = new Peptide(sequence, 2);
//        Simulator simu = new Simulator(peptide);
//        float[][] peak_group = simu.getYisoList();
//        System.out.println();
//    }

}
