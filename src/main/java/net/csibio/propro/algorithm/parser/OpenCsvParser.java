package net.csibio.propro.algorithm.parser;

import com.alibaba.fastjson.JSON;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import net.csibio.propro.algorithm.decoy.generator.ShuffleGenerator;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.parser.model.csv.Transition;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.Annotation;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.PeptideUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.opencsv.ICSVWriter.*;

@Component("openCsvParser")
public class OpenCsvParser extends BaseLibraryParser {

    @Autowired
    TaskService taskService;
    @Autowired
    ShuffleGenerator shuffleGenerator;
    @Autowired
    LibraryService libraryService;
    @Autowired
    FragmentFactory fragmentFactory;

    @Override
    public Result parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {

        Result resultTmp = peptideService.removeAllByLibraryId(library.getId());
        if (resultTmp.isFailed()) {
            logger.error(resultTmp.getErrorMessage());
            return Result.Error(ResultCode.DELETE_ERROR);
        }
        taskDO.addLog("删除旧数据完毕,开始文件解析");
        taskService.update(taskDO);

        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        HeaderColumnNameMappingStrategy<Transition> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(Transition.class);
//            Map<String, String> values = new CSVReaderHeaderAware(reader).readMap();
        char seperator = library.getFileFormat().equals("tsv") ? '\t' : DEFAULT_SEPARATOR;
        char quote = library.getFileFormat().equals("tsv") ? NO_QUOTE_CHARACTER : DEFAULT_QUOTE_CHARACTER;
        List<Transition> transitionList = new ArrayList<>();
        try {
            CsvToBean<Transition> csvToBean = new CsvToBeanBuilder<Transition>(reader)
                    .withQuoteChar(quote)
                    .withSeparator(seperator)
                    .withEscapeChar(NO_ESCAPE_CHARACTER)
                    .withMappingStrategy(strategy)
                    .build();
            transitionList = csvToBean.parse();
        } catch (Exception ex) {
            logger.error("OpenCSV解析错误");
            ex.printStackTrace();
            return Result.Error(ResultCode.PARSE_ERROR);
        } finally {
            try {
                reader.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        if (transitionList.size() == 0) {
            return Result.Error(ResultCode.LINE_IS_EMPTY);
        }
        HashMap<String, PeptideDO> map = new HashMap<>();
        Set<String> proteinSet = new HashSet<>();

        for (Transition transition : transitionList) {
            try {
                if (transition.getDecoy().equals(1)) { //不读取decoy数据
                    continue;
                }
                PeptideDO peptide = new PeptideDO();
                FragmentInfo fi = new FragmentInfo();

                peptide.setLibraryId(library.getId());
                peptide.setMz(transition.getPrecursorMz());
                peptide.setRt(transition.getNormalizedRetentionTime());
                peptide.setProteins(PeptideUtil.parseProtein(transition.getProteinName()));

                fi.setMz(transition.getProductMz());
                fi.setIntensity(transition.getProductIonIntensity());
                if (transition.getAnnotation() != null) {
                    fi.setAnnotations(transition.getAnnotation());
                } else {
                    fi.setAnnotations(transition.getFragmentType()
                            + transition.getFragmentSeriesNumber()
                            + transition.getFragmentLossType()
                            + "^"
                            + transition.getFragmentCharge()
                    );
                }
                if (transition.getFullUniModPeptideName() == null) {
                    String[] transitionGroupId = transition.getTransitionGroupId().split(SymbolConst.UNDERLINE);
                    if (transitionGroupId.length > 2) {
                        peptide.setFullName(transitionGroupId[2]);
                    } else {
                        logger.info("Full Peptide Name cannot be empty");
                        taskDO.addLog("Full Peptide Name cannot be empty!" + JSON.toJSONString(transition));
                        continue;
                    }
                } else {
                    peptide.setFullName(transition.getFullUniModPeptideName());
                }
                peptide.setSequence(PeptideUtil.removeUnimod(peptide.getFullName()));
                peptide.setCharge(transition.getPrecursorCharge());
                peptide.setPeptideRef(peptide.getFullName() + SymbolConst.UNDERLINE + peptide.getCharge());

                Annotation annotation = parseAnnotation(fi.getAnnotations());
                fi.setCharge(annotation.getCharge());
                fi.setCutInfo(annotation.toCutInfo());
                peptide.getFragments().add(fi);

                PeptideUtil.parseModification(peptide);

                proteinSet.addAll(peptide.getProteins());
                addFragment(peptide, map);
            } catch (Exception e) {
                taskDO.addLog("Parse Error:" + JSON.toJSONString(transition));
                logger.info("Parse Error:" + JSON.toJSONString(transition));
            }
        }

        List<PeptideDO> peptideDOList = new ArrayList<>(map.values());
        for (PeptideDO peptideDO : peptideDOList) {
            peptideDO.setFragments(peptideDO.getFragments().stream().sorted(Comparator.comparing(FragmentInfo::getIntensity).reversed()).collect(Collectors.toList()));
            fragmentFactory.calcFingerPrints(peptideDO);
        }
        //在导入Peptide的同时生成伪肽段
        shuffleGenerator.generate(peptideDOList);
        logger.info("准备插入肽段:" + peptideDOList.size() + "条");
        Result<List<PeptideDO>> res = peptideService.insert(peptideDOList);
        logger.info("实际插入肽段:" + res.getData().size() + "条");
        library.setProteins(proteinSet);
        libraryService.update(library);
        taskDO.addLog(res.getData().size() + "条肽段数据插入成功");
        taskService.update(taskDO);
        logger.info(res.getData().size() + "条肽段数据插入成功");
        return Result.OK();
    }

    @Override
    public Result selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO) {
        return null;
    }
}
