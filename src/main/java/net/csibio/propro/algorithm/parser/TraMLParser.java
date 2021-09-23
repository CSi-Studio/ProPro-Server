package net.csibio.propro.algorithm.parser;

import net.csibio.propro.algorithm.decoy.generator.ShuffleGenerator;
import net.csibio.propro.algorithm.parser.model.traml.*;
import net.csibio.propro.algorithm.parser.xml.AirXStream;
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

import java.io.File;
import java.io.InputStream;
import java.util.*;

@Component("traMLParser")
public class TraMLParser extends BaseLibraryParser {

    @Autowired
    AirXStream airXStream;
    @Autowired
    TaskService taskService;
    @Autowired
    ShuffleGenerator shuffleGenerator;
    @Autowired
    LibraryService libraryService;

    public Class<?>[] classes = new Class[]{
            Compound.class, CompoundList.class, Configuration.class, Contact.class, Cv.class, CvParam.class,
            Evidence.class, Instrument.class, IntermediateProduct.class, Interpretation.class, Modification.class,
            Peptide.class, Precursor.class, Prediction.class, Product.class, Protein.class, ProteinRef.class,
            Publication.class, RetentionTime.class, Software.class, SourceFile.class, Target.class, TargetList.class,
            TraML.class, Transition.class, UserParam.class, ValidationStatus.class
    };

    private void prepare() {
        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
    }

    public TraML parse(File file) {
        prepare();
        TraML traML = new TraML();
        airXStream.fromXML(file, traML);
        return traML;
    }

    public String parse(TraML traML) {
        prepare();
        return airXStream.toXML(traML);
    }

    public TraML parse(InputStream ins) {
        prepare();
        TraML traML = new TraML();
        airXStream.fromXML(ins, traML);
        return traML;
    }

    public HashMap<String, Peptide> makePeptideMap(List<Peptide> peptideList) {
        HashMap<String, Peptide> peptideMap = new HashMap<>();
        for (Peptide peptide : peptideList) {
            peptideMap.put(peptide.getId(), peptide);
        }
        return peptideMap;
    }

    public Result<PeptideDO> parseTransition(Transition transition, HashMap<String, Peptide> peptideMap, LibraryDO library) {
        Result<PeptideDO> result = new Result<>(true);
        PeptideDO peptideDO = new PeptideDO();
        peptideDO.setDisable(false);
        peptideDO.setLibraryId(library.getId());

        FragmentInfo fi = new FragmentInfo();

        // parse transition attribution
        boolean isDecoy = transition.getPeptideRef().toLowerCase().contains("decoy");
        //不处理伪肽段信息
        if (isDecoy) {
            return Result.Error(ResultCode.NO_DECOY);
        }
        // parse transition cvparams
        List<CvParam> listCvParams = transition.getCvParams();
        for (CvParam cvParam : listCvParams) {
            if (cvParam.getName().equals("product ion intensity")) {
                fi.setIntensity(Double.valueOf(cvParam.getValue()));
            } else if (cvParam.getName().equals("decoy SRM transition")) {
                return Result.Error(ResultCode.NO_DECOY);
            }
        }
        // parse transition userparam
        List<UserParam> listUserParams = transition.getUserParams();
        if (listUserParams != null) {
            for (UserParam userParam : listUserParams) {
                if (userParam.getName().equals("annotation")) {
                    fi.setAnnotations(userParam.getValue());
                }
            }
        }

        // parse precursor
        listCvParams = transition.getPrecursor().getCvParams();
        if (listCvParams != null) {
            for (CvParam cvParam : listCvParams) {
                if (cvParam.getName().equals("isolation window target m/z")) {
                    peptideDO.setMz(Double.valueOf(cvParam.getValue()));
                }
                //charge state以Peptide部分带电量为准
                if (cvParam.getName().equals("charge state")) {
                    peptideDO.setCharge(Integer.valueOf(cvParam.getValue()));
                }
            }
        }

        // parse product cvParams
        listCvParams = transition.getProduct().getCvParams();
        if (listCvParams != null) {
            for (CvParam cvParam : listCvParams) {
                if (cvParam.getName().equals("isolation window target m/z")) {
                    fi.setMz(Double.valueOf(cvParam.getValue()));
                }
                if (cvParam.getName().equals("charge state")) {
                    fi.setCharge(Integer.valueOf(cvParam.getValue()));
                }
            }
        }

        // parse rt, sequence, full name, protein name from peptideMap
        Peptide peptide = peptideMap.get(transition.getPeptideRef());
        String rt = peptide.getRetentionTimeList().get(0).getCvParams().get(0).getValue();
        peptideDO.setRt(Double.valueOf(rt));
        peptideDO.setSequence(peptide.getSequence());
        peptideDO.setProteins(PeptideUtil.parseProtein(peptide.getProteinRefList().get(0).getRef()));
        peptideDO.setFullName(peptide.getUserParams().get(0).getValue());
        for (CvParam cvParam : peptide.getCvParams()) {
            if (cvParam.getName().equals("charge state")) {
                peptideDO.setCharge(Integer.valueOf(cvParam.getValue()));
                peptideDO.setPeptideRef(peptideDO.getFullName() + "_" + peptideDO.getCharge());
            }
        }

        // parse annotations
        try {
            Annotation annotation = parseAnnotation(fi.getAnnotations());
            fi.setCutInfo(annotation.toCutInfo());
            fi.setCharge(annotation.getCharge());
            peptideDO.getFragments().add(fi);
            result.setData(peptideDO);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Line插入错误(Sequence未知)");
            logger.error(peptideDO.getLibraryId() + ":" + fi.getAnnotations(), e);
            return result;
        }

        PeptideUtil.parseModification(peptideDO);
        return result;
    }

    @Override
    public Result parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {
        TraML traML = parse(in);

        HashMap<String, Peptide> peptideMap = makePeptideMap(traML.getCompoundList().getPeptideList());
        Result<List<PeptideDO>> tranResult = new Result<>(true);

        try {
            //开始插入前先清空原有的数据库数据
            Result ResultTmp = peptideService.removeAllByLibraryId(library.getId());
            logger.info("删除旧数据完毕");

            if (ResultTmp.isFailed()) {
                logger.error(ResultTmp.getErrorMessage());
                return Result.Error(ResultCode.DELETE_ERROR);
            }

            HashMap<String, PeptideDO> map = new HashMap<>();

            for (Transition transition : traML.getTransitionList()) {
                Result<PeptideDO> Result = parseTransition(transition, peptideMap, library);
                if (Result.isFailed()) {
                    if (!Result.getErrorCode().equals(ResultCode.NO_DECOY.getCode())) {
                        tranResult.addErrorMsg(Result.getErrorMessage());
                    }
                    continue;
                }
                PeptideDO peptide = Result.getData();
                addFragment(peptide, map);
            }

            for (PeptideDO peptide : map.values()) {
                shuffleGenerator.generate(peptide);
            }

            List<PeptideDO> peptideList = new ArrayList<>(map.values());
            peptideService.insert(peptideList);
            Set<String> proteins = new HashSet<>();

            peptideList.forEach(peptide -> proteins.addAll(peptide.getProteins()));
            library.setProteins(proteins);
            libraryService.update(library);

            taskDO.addLog(map.size() + "条肽段数据插入成功");

            taskService.update(taskDO);
            logger.info(map.size() + "条肽段数据插入成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
    }

    @Override
    public Result selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO) {
        TraML traML = parse(in);
        HashMap<String, Peptide> peptideMap = makePeptideMap(traML.getCompoundList().getPeptideList());
        Result<List<PeptideDO>> tranResult = new Result<>(true);
        int selectedCount = selectedPepSet.size();
        try {
            //开始插入前先清空原有的数据库数据
            Result ResultTmp = peptideService.removeAllByLibraryId(library.getId());
            logger.info("删除旧数据完毕");

            if (ResultTmp.isFailed()) {
                logger.error(ResultTmp.getErrorMessage());
                return Result.Error(ResultCode.DELETE_ERROR);
            }

            boolean withCharge = new ArrayList<>(selectedPepSet).get(0).contains("_");
            if (selectBySequence) {
                selectedPepSet = convertPepToSeq(selectedPepSet, withCharge);
            }
            HashMap<String, PeptideDO> map = new HashMap<>();
            for (Transition transition : traML.getTransitionList()) {
                if (!selectedPepSet.isEmpty() && !isSelectedPep(transition, peptideMap, selectedPepSet, withCharge, selectBySequence)) {
                    continue;
                }
                Result<PeptideDO> Result = parseTransition(transition, peptideMap, library);

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
            for (PeptideDO peptideDO : map.values()) {
                selectedPepSet.remove(peptideDO.getPeptideRef());
            }
            ArrayList<PeptideDO> peptides = new ArrayList<PeptideDO>(map.values());

            peptideService.insert(peptides);
            tranResult.setData(peptides);
            taskDO.addLog(peptides.size() + "条数据插入成功");
            taskService.update(taskDO);
            logger.info(map.size() + "条肽段数据插入成功");
            logger.info("在选中的" + selectedCount + "条肽段中, 有" + selectedPepSet.size() + "条没有在库中找到");
            logger.info(selectedPepSet.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
    }


    private boolean isSelectedPep(Transition transition, HashMap<String, Peptide> peptideMap, HashSet<String> selectedPepSet, boolean withCharge, boolean selectBySequence) {
        Peptide peptide = peptideMap.get(transition.getPeptideRef());
        String fullName = peptide.getUserParams().get(0).getValue();
        if (selectBySequence) {
            String sequence = PeptideUtil.removeUnimod(fullName);
            return selectedPepSet.contains(sequence);
        }
        if (withCharge) {
            String charge = "";
            for (CvParam cvParam : peptide.getCvParams()) {
                if (cvParam.getName().equals("charge state")) {
                    charge = cvParam.getValue();
                }
            }
            return selectedPepSet.contains(fullName + "_" + charge);
        } else {
            return selectedPepSet.contains(fullName);
        }
    }


}
