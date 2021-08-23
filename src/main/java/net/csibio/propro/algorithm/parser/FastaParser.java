package net.csibio.propro.algorithm.parser;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Nico Wang Ruimin
 * Time: 2019-02-01 16:18
 * <p>
 * Fasta文件的存储格式为（一行Protein信息，跟着多行该protein的完整序列）
 */
@Component("fastaParser")
public class FastaParser {

    //TODO: params
//    int minPepLen = 0;
//    int minPepLen = 100;

    public Result<HashSet<String>> getUniquePeptide(InputStream in, int minPepLen, int maxPepLen) {
        Result<HashMap<String, HashSet<String>>> protMapResult = parse(in, minPepLen, maxPepLen);
        if (protMapResult.isFailed()) {
            Result<HashSet<String>> Result = new Result(false);
            Result.setErrorMessage(protMapResult.getErrorMessage());
            return Result;
        }
        HashSet<String> uniquePeptides = new HashSet<>();
        HashSet<String> allPeptides = new HashSet<>();
        for (HashSet<String> peptideSet : protMapResult.getData().values()) {
            for (String peptide : peptideSet) {
                if (allPeptides.contains(peptide) && uniquePeptides.contains(peptide)) {
                    //若之前出现过，且在Unique中，移除
                    uniquePeptides.remove(peptide);
                } else {
                    //若之前没出现过，暂且放在Unique中
                    uniquePeptides.add(peptide);
                }
                allPeptides.add(peptide);
            }
        }
        return new Result<HashSet<String>>(true).setData(uniquePeptides);
    }

    public Result<HashMap<String, HashSet<String>>> parse(InputStream in, int minPepLen,
                                                          int maxPepLen) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line = reader.readLine();
            String lastProteinMessage = line;
            //设置初始容量为128，避免重新分配空间造成的性能损失
            StringBuilder lastSequence = new StringBuilder(128);
            HashMap<String, HashSet<String>> proteinPeptideMap = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    //进入下一个protein的分析
                    //此时lastSequence里面存有了当前protein的sequence
                    if (!line.startsWith(">CON")) {

                        HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString(), minPepLen, maxPepLen);
                        proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
                    }
                    //分离为peptide之后，存储新protein的信息，并清空lastSequence
                    lastProteinMessage = line;
                    lastSequence.setLength(0);

                } else {
                    lastSequence.append(line);
                }
            }
            HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString(), minPepLen, maxPepLen);
            proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
            return new Result<HashMap<String, HashSet<String>>>(true).setData(proteinPeptideMap);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.PRM_FILE_FORMAT_NOT_SUPPORTED);
        }
    }

    /**
     * 将K，R作为切分sequence的标志物
     *
     * @param proteinSequence
     * @return
     */
    public HashSet<String> getEnzymeResult(String proteinSequence, int minPepLen, int maxPepLen) {
        String[] result = proteinSequence.replaceAll("K", "K|").replaceAll("R", "R|").split("\\|");
        HashSet<String> peptideSet = new HashSet<>();
        for (String peptide : result) {
            if (peptide.length() >= minPepLen && peptide.length() <= maxPepLen) {
                peptideSet.add(peptide);
            }
        }
        return peptideSet;
    }

    public Result<HashMap<String, String>> parseAll(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = reader.readLine();
            String lastProteinMessage = line;
            //设置初始容量为128，避免重新分配空间造成的性能损失
            StringBuilder lastSequence = new StringBuilder(128);
            HashMap<String, HashSet<String>> proteinPeptideMap = new HashMap<>();
            HashMap<String, String> map = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    //进入下一个protein的分析
                    //此时lastSequence里面存有了当前protein的sequence
                    if (!line.startsWith(">CON")) {
                        int minPepLen = 0;
                        int maxPepLen = 100;
                        HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString(), minPepLen, maxPepLen);
                        proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
                    }
                    //分离为peptide之后，存储新protein的信息，并清空lastSequence
                    lastProteinMessage = line;
                    lastSequence.setLength(0);

                } else {
                    lastSequence.append(line);
                }
                map.put(lastProteinMessage, lastSequence.toString());
            }
            int minPepLen = 0;
            int maxPepLen = 100;
            HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString(), minPepLen, maxPepLen);

            proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
            return new Result<HashMap<String, String>>(true).setData(map);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.PRM_FILE_FORMAT_NOT_SUPPORTED);
        }

    }

    public Result<HashMap<String, String>> parseAllWithInput(InputStream inputStream, int minPepLen, int maxPepLen) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = reader.readLine();
            String lastProteinMessage = line;
            //设置初始容量为128，避免重新分配空间造成的性能损失
            StringBuilder lastSequence = new StringBuilder(128);
            HashMap<String, HashSet<String>> proteinPeptideMap = new HashMap<>();
            HashMap<String, String> map = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    //进入下一个protein的分析
                    //此时lastSequence里面存有了当前protein的sequence
                    if (!line.startsWith(">CON")) {
                        HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString(), minPepLen, maxPepLen);
                        proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
                    }
                    //分离为peptide之后，存储新protein的信息，并清空lastSequence
                    lastProteinMessage = line;
                    lastSequence.setLength(0);

                } else {
                    lastSequence.append(line);
                }
                map.put(lastProteinMessage, lastSequence.toString());
            }
            HashSet<String> enzymedSequence = getEnzymeResult(lastSequence.toString(), minPepLen, maxPepLen);

            proteinPeptideMap.put(lastProteinMessage, enzymedSequence);
            return new Result<HashMap<String, String>>(true).setData(map);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.PRM_FILE_FORMAT_NOT_SUPPORTED);
        }

    }
}
