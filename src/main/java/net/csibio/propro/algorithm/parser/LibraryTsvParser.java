package net.csibio.propro.algorithm.parser;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.decoy.generator.ShuffleGenerator;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.Annotation;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.PeptideUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 11:07
 */
@Slf4j
@Component("tsvParser")
public class LibraryTsvParser extends BaseLibraryParser {

    @Autowired
    TaskService taskService;
    @Autowired
    ShuffleGenerator shuffleGenerator;

    private static String PrecursorMz = "precursormz";
    private static String ProductMz = "productmz";
    private static String NormalizedRetentionTime = "tr_recalibrated";
    private static String TransitionName = "transition_name";
    private static String TransitionGroupId = "transition_group_id";
    private static String UniprotId = "uniprotid";
    private static String IsDecoy = "decoy";
    private static String ProductIonIntensity = "libraryintensity";
    private static String PeptideSequence = "peptidesequence";
    private static String ProteinName = "proteinname";
    private static String Annotation = "annotation";
    private static String FullUniModPeptideName = "fullunimodpeptidename";
    private static String PrecursorCharge = "precursorcharge";
    private static String Detecting = "detecting_transition";
    private static String Identifying = "identifying_transition";
    private static String Quantifying = "quantifying_transition";

    @Override
    public Result parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {

        Result<List<PeptideDO>> tranResult = new Result<>(true);
        try {
            //开始插入前先清空原有的数据库数据
            Result ResultTmp = peptideService.removeAllByLibraryId(library.getId());
            if (ResultTmp.isFailed()) {
                logger.error(ResultTmp.getErrorMessage());
                return Result.Error(ResultCode.DELETE_ERROR);
            }
            taskDO.addLog("删除旧数据完毕,开始文件解析");
            taskService.update(taskDO);

            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (line == null) {
                return Result.Error(ResultCode.LINE_IS_EMPTY);
            }
            HashMap<String, Integer> columnMap = parseColumns(line);
            HashMap<String, PeptideDO> map = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                Result<PeptideDO> Result = parseTransition(line, columnMap, library);
                if (Result.isFailed()) {
                    if (!Result.getErrorCode().equals(ResultCode.NO_DECOY.getCode())) {
                        tranResult.addErrorMsg(Result.getErrorMessage());
                    }
                    continue;
                }
                PeptideDO peptide = Result.getData();
                addFragment(peptide, map);
            }
            List<PeptideDO> peptideDOList = new ArrayList<>(map.values());
            //在导入Peptide的同时生成伪肽段
            shuffleGenerator.generate(peptideDOList);
            log.info("准备插入肽段:" + peptideDOList.size() + "条");
            Result<List<PeptideDO>> res = peptideService.insert(peptideDOList);
            log.info("实际插入肽段:" + res.getData().size() + "条");
            taskDO.addLog(res.getData().size() + "条肽段数据插入成功");
            taskService.update(taskDO);
            log.info(res.getData().size() + "条肽段数据插入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
    }

    @Override
    public Result selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO) {

        Result<List<PeptideDO>> tranResult = new Result<>(true);
        try {
            //开始插入前先清空原有的数据库数据
            Result ResultTmp = peptideService.removeAllByLibraryId(library.getId());
            if (ResultTmp.isFailed()) {
                log.error(ResultTmp.getErrorMessage());
                return Result.Error(ResultCode.DELETE_ERROR);
            }
            taskDO.addLog("删除旧数据完毕,开始文件解析");
            taskService.update(taskDO);

            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (line == null) {
                return Result.Error(ResultCode.LINE_IS_EMPTY);
            }
            HashMap<String, Integer> columnMap = parseColumns(line);


            HashMap<String, PeptideDO> map = new HashMap<>();

            boolean withCharge = new ArrayList<>(selectedPepSet).get(0).contains("_");
            if (selectBySequence) {
                selectedPepSet = convertPepToSeq(selectedPepSet, withCharge);
            }
            while ((line = reader.readLine()) != null) {
                if (!selectedPepSet.isEmpty() && !isSelectedLine(line, columnMap, selectedPepSet, withCharge, selectBySequence)) {
                    continue;
                }
                Result<PeptideDO> Result = parseTransition(line, columnMap, library);
                if (Result.isFailed()) {
                    if (!Result.getErrorCode().equals(ResultCode.NO_DECOY.getCode())) {
                        tranResult.addErrorMsg(Result.getErrorMessage());
                    }
                    continue;
                }

                PeptideDO peptide = Result.getData();
                addFragment(peptide, map);
                //在导入Peptide的同时生成伪肽段
                shuffleGenerator.generate(peptide);
            }

            //删除命中的部分, 得到未命中的Set
            int selectedCount = selectedPepSet.size();
            if (withCharge) {
                for (PeptideDO peptideDO : map.values()) {
                    selectedPepSet.remove(peptideDO.getPeptideRef());
                }
            } else {
                for (PeptideDO peptideDO : map.values()) {
                    selectedPepSet.remove(peptideDO.getFullName());
                }
            }

            peptideService.insert(new ArrayList<>(map.values()));
            taskDO.addLog(map.size() + "条肽段数据插入成功");
            taskService.update(taskDO);
            log.info(map.size() + "条肽段数据插入成功");
            log.info("在选中的" + selectedCount + "条肽段中, 有" + selectedPepSet.size() + "条没有在库中找到");
            log.info(selectedPepSet.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
    }

    /**
     * 从TSV文件中解析出每一行数据
     *
     * @param line
     * @param columnMap
     * @param library
     * @return
     */
    private Result<PeptideDO> parseTransition(String line, HashMap<String, Integer> columnMap, LibraryDO library) {
        Result<PeptideDO> result = new Result<>(true);
        String[] row = StringUtils.splitByWholeSeparator(line, "\t");
        PeptideDO peptideDO = new PeptideDO();
        boolean isDecoy = !row[columnMap.get(IsDecoy)].equals("0");
        if (isDecoy) {
            return Result.Error(ResultCode.NO_DECOY);
        }
        FragmentInfo fi = new FragmentInfo();

        peptideDO.setLibraryId(library.getId());
        peptideDO.setMz(Double.parseDouble(row[columnMap.get(PrecursorMz)]));
        fi.setMz(Double.parseDouble(row[columnMap.get(ProductMz)]));
        peptideDO.setRt(Double.parseDouble(row[columnMap.get(NormalizedRetentionTime)]));

        fi.setIntensity(Double.parseDouble(row[columnMap.get(ProductIonIntensity)]));
        peptideDO.setSequence(row[columnMap.get(PeptideSequence)]);
        peptideDO.setProtein(row[columnMap.get(ProteinName)]);
        if (peptideDO.getProtein().toLowerCase().contains("irt")) {
            peptideDO.setProtein("iRT");
        }

        String annotations = row[columnMap.get(Annotation)].replaceAll("\"", "");
        fi.setAnnotations(annotations);
        String fullName = row[columnMap.get(FullUniModPeptideName)];//no target sequence
        String[] transitionGroupId = row[columnMap.get(TransitionGroupId)].split("_");
        if (fullName == null) {
            if (transitionGroupId.length > 2) {
                peptideDO.setFullName(transitionGroupId[2]);
            } else {
                logger.info("Full Peptide Name cannot be empty");
            }
        }
        peptideDO.setFullName(fullName);
        peptideDO.setSequence(PeptideUtil.removeUnimod(peptideDO.getFullName()));
        try {
            peptideDO.setCharge(Integer.parseInt(row[columnMap.get(PrecursorCharge)]));
        } catch (Exception e) {
            log.error("Line插入错误(PrecursorCharge未知):" + line + ";");
            log.error(e.getMessage());
        }
        peptideDO.setPeptideRef(peptideDO.getFullName() + "_" + peptideDO.getCharge());
        try {
            Annotation annotation = parseAnnotation(fi.getAnnotations());
            fi.setCharge(annotation.getCharge());
            fi.setCutInfo(annotation.toCutInfo());
            peptideDO.getFragments().add(fi);
            result.setData(peptideDO);
        } catch (Exception e) {
            result.setSuccess(false);
            logger.error("Line插入错误(Sequence未知):" + line + ";");
            result.setErrorMessage("Line插入错误(Sequence未知):" + line + ";");
            logger.error(peptideDO.getLibraryId() + ":" + fi.getAnnotations(), e);
            return result;
        }

        PeptideUtil.parseModification(peptideDO);

        return result;
    }

    private boolean isSelectedLine(String line, HashMap<String, Integer> columnMap, HashSet<String> peptideSet, boolean withCharge, boolean selectBySequence) {
        String[] row = line.split("\t");
        String fullName = row[columnMap.get(FullUniModPeptideName)];
        String charge = row[columnMap.get(PrecursorCharge)];
        if (selectBySequence) {
            String sequence = row[columnMap.get(PeptideSequence)];
            return peptideSet.contains(sequence);
        }
        if (withCharge) {
            return peptideSet.contains(fullName + "_" + charge);
        } else {
            return peptideSet.contains(fullName);
        }
    }

}
