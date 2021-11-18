package net.csibio.propro.algorithm.decoy.generator;

import net.csibio.propro.algorithm.decoy.BaseGenerator;
import net.csibio.propro.algorithm.parser.LibraryTsvParser;
import net.csibio.propro.algorithm.parser.model.chemistry.AminoAcid;
import net.csibio.propro.domain.bean.peptide.Annotation;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.utils.TransitionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component("replaceGenerator")
public class ReplaceGenerator extends BaseGenerator {

    public final Logger logger = LoggerFactory.getLogger(ReplaceGenerator.class);

    public static final String NAME = "replace";

    @Autowired
    LibraryTsvParser libraryTsvParser;

    String origin = "GAVLIFMPWSCTYHKRQEND";
    String target = "LLLVVLLLLTSSSSLLNDQE";

    @Override
    protected void generate(PeptideDO peptide) {
        String sequence = peptide.getSequence();
        HashMap<Integer, String> unimodMap = peptide.getUnimodMap();
        List<AminoAcid> aminoAcids = fragmentFactory.parseAminoAcid(sequence, unimodMap);

        AminoAcid b = aminoAcids.get(0);
        AminoAcid y = aminoAcids.get(aminoAcids.size() - 1);

        b.setName(target.charAt(origin.indexOf(b.getName())) + "");
        y.setName(target.charAt(origin.indexOf(y.getName())) + "");
        
        for (FragmentInfo targetFi : peptide.getFragments()) {
            FragmentInfo decoyFi = new FragmentInfo();
            decoyFi.setCutInfo(targetFi.getCutInfo());
            decoyFi.setIntensity(targetFi.getIntensity());
            decoyFi.setCharge(targetFi.getCharge());
            decoyFi.setAnnotations(targetFi.getAnnotations());
            Annotation oneAnno = libraryTsvParser.parseAnnotation(targetFi.getAnnotations());
            List<String> unimodIds = new ArrayList<>();
            List<AminoAcid> acids = null;
            try {
                acids = fragmentFactory.getFragmentSequence(aminoAcids, oneAnno.getType(), oneAnno.getLocation());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(peptide.getFullName());
                return;
            }
            for (AminoAcid aminoAcid : acids) {
                if (aminoAcid.getModId() != null) {
                    unimodIds.add(aminoAcid.getModId());
                }
            }

            double productMz = formulaCalculator.getMonoMz(
                    TransitionUtil.toSequence(acids, false),
                    oneAnno.getType(),
                    oneAnno.getCharge(),
                    oneAnno.getAdjust(),
                    oneAnno.getDeviation(),
                    oneAnno.isIsotope(),
                    unimodIds
            );

            decoyFi.setMz(productMz);
            peptide.getDecoyFragments().add(decoyFi);
        }

        peptide.setDecoySequence(TransitionUtil.toSequence(aminoAcids, false));
        peptide.setDecoyUnimodMap(unimodMap);
    }
}
