package net.csibio.propro.utils;

import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.domain.db.PeptideDO;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nico Wang
 * Time: 2019-07-04 16:24
 */
public class PeptideUtil {


    public static final Pattern unimodPattern = Pattern.compile("([a-z])[\\(]unimod[\\:](\\d*)[\\)]");

    /**
     * 用于解码5/sp|Q9NY65|TBA8_HUMAN/sp|Q6PEY2|TBA3E_HUMAN/sp|Q13748|TBA3C_HUMAN/sp|Q9BQE3|TBA1C_HUMAN/sp|Q71U36|TBA1A_HUMAN
     * 这种类似结构的格式
     *
     * @param proteinLabel 蛋白质编码
     */
    public static Set<String> parseProtein(String proteinLabel) {
        Set<String> proteins = new HashSet<>();
        if (proteinLabel.contains(SymbolConst.LEFT_SLASH)) {
            String[] proteinArray = proteinLabel.split(SymbolConst.LEFT_SLASH);
            for (int i = 1; i < proteinArray.length; i++) {
                proteins.add(proteinArray[i]);
            }
        } else if (proteinLabel.contains("irt")) {
            proteins.add("iRT");
        } else {
            proteins.add(proteinLabel);
        }
        return proteins;
    }

    public static String removeUnimod(String fullName) {
        if (fullName.contains("(")) {
            String[] parts = fullName.replaceAll("\\(", "|(").replaceAll("\\)", "|").split("\\|");
            String sequence = "";
            for (String part : parts) {
                if (part.startsWith("(")) {
                    continue;
                }
                sequence += part;
            }
            return sequence;
        } else {
            return fullName;
        }
    }


    /**
     * 解析出Modification的位置
     *
     * @param peptideDO
     */
    public static void parseModification(PeptideDO peptideDO) {
        //不论是真肽段还是伪肽段,fullUniModPeptideName字段都是真肽段的完整版
        String peptide = peptideDO.getFullName();
        peptide = peptide.toLowerCase();
        HashMap<Integer, String> unimodMap = new HashMap<>();

        while (peptide.contains("(unimod:") && peptide.indexOf("(unimod:") != 0) {
            Matcher matcher = unimodPattern.matcher(peptide);
            if (matcher.find()) {
                unimodMap.put(matcher.start(), matcher.group(2));
                peptide = StringUtils.replaceOnce(peptide, matcher.group(0), matcher.group(1));
            }
        }
//        if (unimodMap.size() > 0) {
        peptideDO.setUnimodMap(unimodMap);
//        }
    }

    /**
     * 解析出Modification的位置
     *
     * @param fullName
     */
    public static HashMap<Integer, String> parseModification(String fullName) {
        //不论是真肽段还是伪肽段,fullUniModPeptideName字段都是真肽段的完整版

        fullName = fullName.toLowerCase();
        HashMap<Integer, String> unimodMap = new HashMap<>();

        while (fullName.contains("(unimod:") && fullName.indexOf("(unimod:") != 0) {
            Matcher matcher = unimodPattern.matcher(fullName);
            if (matcher.find()) {
                unimodMap.put(matcher.start(), matcher.group(2));
                fullName = StringUtils.replaceOnce(fullName, matcher.group(0), matcher.group(1));
            }
        }
        return unimodMap;
    }

}
