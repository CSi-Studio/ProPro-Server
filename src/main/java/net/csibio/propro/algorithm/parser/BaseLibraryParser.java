package net.csibio.propro.algorithm.parser;

import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.Annotation;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.utils.PeptideUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 21:13
 */
public abstract class BaseLibraryParser {

    public final Logger logger = LoggerFactory.getLogger(BaseLibraryParser.class);

    @Autowired
    PeptideService peptideService;

    public abstract Result parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO);

    public abstract Result selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO);

    public Annotation parseAnnotation(String annotations) {
        String[] annotationStrs = annotations.split(",");
        Annotation annotation = new Annotation();

        try {
            String annotationStr = annotationStrs[0];
            if (StringUtils.startsWith(annotationStr, "[")) {
                annotation.setIsBrotherIcon(true);
                annotationStr = annotationStr.replace("[", "");
                annotationStr = annotationStr.replace("]", "");
            }
            String[] forDeviation = annotationStr.split("/");
            if (forDeviation.length > 1) {
                annotation.setDeviation(Double.parseDouble(forDeviation[1]));
            }

            if (forDeviation[0].endsWith("i")) {
                annotation.setIsotope(true);
                forDeviation[0] = forDeviation[0].replace("i", "");
            }

            String[] forCharge = forDeviation[0].split("\\^");
            if (forCharge.length == 2) {
                annotation.setCharge(Integer.parseInt(forCharge[1]));
            } else if (forDeviation[0].contains("(")) {
                String[] msmsCutoff = forDeviation[0].split("\\(");
                annotation.setCharge(Integer.parseInt(msmsCutoff[1].substring(0, 1)));
                forCharge[0] = msmsCutoff[0];
            }
            //默认为负,少数情况下校准值为正
            String nOrP = "-";
            String[] forAdjust;
            if (forCharge[0].contains("+")) {
                nOrP = "+";
                forAdjust = forCharge[0].split("\\+");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            } else if (forCharge[0].contains("-")) {
                forAdjust = forCharge[0].split("-");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            } else {
                forAdjust = forCharge;
            }

            String finalStr = forAdjust[0];
            //第一位必定是字母,代表fragment类型
            annotation.setType(finalStr.substring(0, 1));
            String location = finalStr.substring(1);
            if (!location.isEmpty()) {
                annotation.setLocation(Integer.parseInt(location));
            }
            return annotation;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("解析Annotation错误,Annotation:" + annotations);
            return null;
        }
    }

    /**
     * 从去除的Peptide推断的Protein中删除含有未去除Peptide的Protein
     *
     * @param dropSet   非Unique的Peptide推断得到的ProtSet
     * @param uniqueSet Unique的Peptide推断得到的ProtSet
     * @return 非Unique蛋白的数量
     */
    protected int getDropCount(HashSet<String> dropSet, HashSet<String> uniqueSet) {
        List<String> dropList = new ArrayList<>(dropSet);
        int dropCount = dropList.size();
        for (String prot : dropList) {
            if (uniqueSet.contains(prot)) {
                dropCount--;
            }
        }
        return dropCount;
    }

    protected void addFragment(PeptideDO peptide, HashMap<String, PeptideDO> map) {
        PeptideDO existedPeptide = map.get(peptide.getPeptideRef());
        if (existedPeptide == null) {
            map.put(peptide.getPeptideRef(), peptide);
        } else {
            //去重
            for (FragmentInfo fragment : peptide.getFragments()) {
                if (!existedPeptide.getFragments().contains(fragment)) {
                    existedPeptide.getFragments().add(fragment);
                }
            }
        }
    }

    protected HashMap<String, Integer> parseColumns(String line) {
        String[] columns = null;
        if (line.contains(SymbolConst.TAB)) {
            columns = line.split(SymbolConst.TAB);
        } else {
            columns = line.split(SymbolConst.COMMA);
        }

        HashMap<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnMap.put(StringUtils.deleteWhitespace(columns[i].toLowerCase().replace(SymbolConst.DOUBLE_QUOTA, "")), i);
        }
        return columnMap;
    }

    protected HashSet<String> convertPepToSeq(HashSet<String> selectedPepSet, boolean withCharge) {
        HashSet<String> selectedSeqSet = new HashSet<>();
        for (String pep : selectedPepSet) {
            if (withCharge) {
                selectedSeqSet.add(PeptideUtil.removeUnimod(pep.split("_")[0]));
            } else {
                selectedSeqSet.add(PeptideUtil.removeUnimod(pep));
            }
        }
        return selectedSeqSet;
    }
}
