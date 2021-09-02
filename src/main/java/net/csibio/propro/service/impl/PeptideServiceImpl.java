package net.csibio.propro.service.impl;

import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.algorithm.formula.FormulaCalculator;
import net.csibio.propro.algorithm.formula.FragmentFactory;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.constant.ResidueType;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.LibraryDAO;
import net.csibio.propro.dao.PeptideDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.Protein;
import net.csibio.propro.domain.bean.peptide.SimplePeptide;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 20:02
 */
@Service("peptideService")
public class PeptideServiceImpl implements PeptideService {

    public final Logger logger = LoggerFactory.getLogger(PeptideServiceImpl.class);

    public static Pattern pattern = Pattern.compile("/\\(.*\\)/");
    @Autowired
    PeptideDAO peptideDAO;
    @Autowired
    LibraryDAO libraryDAO;
    @Autowired
    TaskService taskService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    FormulaCalculator formulaCalculator;
    @Autowired
    LibraryService libraryService;

    @Override
    public List<PeptideDO> getAllByLibraryId(String libraryId) {
        return peptideDAO.getAllByLibraryId(libraryId);
    }

    @Override
    public BaseDAO<PeptideDO, PeptideQuery> getBaseDAO() {
        return peptideDAO;
    }

    @Override
    public void beforeInsert(PeptideDO peptideDO) throws XException {
        if (peptideDO.getPeptideRef() == null) {
            throw new XException(ResultCode.PEPTIDE_REF_CANNOT_BE_EMPTY);
        }
        if (peptideDO.getMz() == null) {
            throw new XException(ResultCode.PEPTIDE_MZ_CANNOT_BE_NULL);
        }
    }

    @Override
    public void beforeUpdate(PeptideDO peptideDO) throws XException {
        if (peptideDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (peptideDO.getPeptideRef() == null) {
            throw new XException(ResultCode.PEPTIDE_REF_CANNOT_BE_EMPTY);
        }
        if (peptideDO.getMz() == null) {
            throw new XException(ResultCode.PEPTIDE_MZ_CANNOT_BE_NULL);
        }
    }

    @Override
    public void beforeRemove(String libraryId) throws XException {
        //Do Nothing
    }

    @Override
    public Result updateDecoyInfos(List<PeptideDO> peptides) {
        peptideDAO.updateDecoyInfos(peptides);
        return Result.OK();
    }

    @Override
    public Result removeAllByLibraryId(String libraryId) {
        try {
            peptideDAO.deleteAllByLibraryId(libraryId);
            return Result.OK();
        } catch (Exception e) {
            return Result.Error(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public Double[] getRTRange(String libraryId) {
        Double[] range = new Double[2];

        PeptideQuery query = new PeptideQuery(libraryId);
        query.setPageSize(1);
        query.setOrderBy(Sort.Direction.ASC);
        query.setSortColumn("rt");
        List<PeptideDO> descList = peptideDAO.getList(query);
        if (descList != null && descList.size() == 1) {
            range[0] = descList.get(0).getRt();
        }
        query.setOrderBy(Sort.Direction.DESC);
        List<PeptideDO> ascList = peptideDAO.getList(query);
        if (ascList != null && ascList.size() == 1) {
            range[1] = ascList.get(0).getRt();
        }
        return range;
    }

    @Override
    public Result<List<Protein>> getProteinList(PeptideQuery query) {
        LibraryDO libraryDO = libraryDAO.getById(query.getLibraryId());
        List<Protein> proteins = peptideDAO.getProteinList(query);
        Result<List<Protein>> result = new Result<>(true);
        result.setData(proteins);
        if (libraryDO.getStatistic().get(StatConst.Protein_Count) != null) {
            result.setTotalNum((long) libraryDO.getStatistic().get(StatConst.Protein_Count));
        }

        result.setPageSize(query.getPageSize());
        return result;
    }

    @Override
    public Long countByProteinName(String libraryId) {
        return peptideDAO.countByProtein(libraryId);
    }

    @Override
    public List<SimplePeptide> buildCoord4Irt(String libraryId, WindowRange mzRange) {
        long start = System.currentTimeMillis();
        PeptideQuery query = new PeptideQuery(libraryId);
        query.setMzStart(mzRange.getStart()).setMzEnd(mzRange.getEnd());
        List<SimplePeptide> targetList = getAll(query, SimplePeptide.class);
        long dbTime = System.currentTimeMillis() - start;
        targetList.parallelStream().forEach(s -> s.setRtRange(-1, 99999));
        logger.info("构建提取EIC的MS2坐标(4Irt),总计" + targetList.size() + "条记录,读取标准库耗时:" + dbTime + "毫秒");
        return targetList;
    }

    @Override
    public List<SimplePeptide> buildCoord(String libraryId, WindowRange mzRange, Double rtWindow, SlopeIntercept si) {
        long start = System.currentTimeMillis();
        PeptideQuery query = new PeptideQuery(libraryId);
        query.setMzStart(mzRange.getStart()).setMzEnd(mzRange.getEnd());
        List<SimplePeptide> targetList = getAll(query, SimplePeptide.class);
        long dbTime = System.currentTimeMillis() - start;

        if (rtWindow != null) {
            for (SimplePeptide simplePeptide : targetList) {
                double iRt = (simplePeptide.getRt() - si.getIntercept()) / si.getSlope();
                simplePeptide.setRtStart(iRt - rtWindow);
                simplePeptide.setRtEnd(iRt + rtWindow);
            }
        } else {
            for (SimplePeptide simplePeptide : targetList) {
                simplePeptide.setRtStart(-1);
                simplePeptide.setRtEnd(99999);
            }
        }

        logger.info("构建提取XIC的MS2坐标,总计" + targetList.size() + "条记录,读取标准库耗时:" + dbTime + "毫秒");
        return targetList;
    }


    @Override
    public PeptideDO buildWithPeptideRef(String peptideRef, int minLength, List<String> ionTypes, List<Integer> chargeTypes) {
        int charge;
        String fullName;

        if (peptideRef.contains("_")) {
            String[] peptideInfos = peptideRef.split("_");
            charge = Integer.parseInt(peptideInfos[1]);
            fullName = peptideInfos[0];
        } else {
            charge = 1;
            fullName = peptideRef;
        }

        PeptideDO peptide = new PeptideDO();
        peptide.setFullName(fullName);
        peptide.setCharge(charge);
        peptide.setSequence(fullName.replaceAll("\\([^)]+\\)", ""));
        HashMap<Integer, String> unimodMap = PeptideUtil.parseModification(fullName);
        peptide.setUnimodMap(unimodMap);
        peptide.setMz(formulaCalculator.getMonoMz(peptide.getSequence(), ResidueType.Full, charge, 0, 0, false, new ArrayList<>(unimodMap.values())));
        peptide.setPeptideRef(peptideRef);
        peptide.setRt(-1d);

        peptide.setFragments(fragmentFactory.buildFragmentMap(peptide, minLength, ionTypes, chargeTypes));
        return peptide;
    }

    @Override
    public Result<Map<String, List<Object>>> getPeptideLink(String libraryId, String proteinName, double range, List<WindowRange> windowRanges) {
        LibraryDO library = libraryService.getById(libraryId);
        Set<String> proteins = library.getProteins();
        proteins.remove(proteinName);
        PeptideQuery peptideQuery = new PeptideQuery();
        peptideQuery.setLibraryId(libraryId);
        peptideQuery.setProtein(proteinName);
        List<PeptideDO> peptideList = getAll(peptideQuery);
        Map<String,Double[]> swath = new HashMap<>();
        for(PeptideDO pp : peptideList){
            for(WindowRange windowRange:windowRanges){
                if(pp.getMz()<=windowRange.getEnd() && pp.getMz()>=windowRange.getStart() ){
                    Double[] dd = new Double[]{windowRange.getStart(),windowRange.getEnd()};
                    swath.put(pp.getPeptideRef(),dd);
                }
            }
        }
        Map<String, List<Object>> finalMap = new HashMap<>();
        finalMap.put("nodes", new ArrayList<>());
        finalMap.put("links", new ArrayList<>());
        finalMap.put("categories", new ArrayList<>());
        int i = 0;
        for (PeptideDO peptide : peptideList) {
            i = i + 1;
            SourceNode sourceNode = new SourceNode();
            String peptideRef = peptide.getPeptideRef();
            sourceNode.setId(peptideRef);
            sourceNode.setValue(new double[]{peptide.getMz(),peptide.getRt()});
            sourceNode.setSymbolSize(24.266666666666666);
            sourceNode.setCategory(0);
            sourceNode.setName(peptideRef);
            sourceNode.setSymbol("circle");
            List<SourceNode> sourceNodes = new ArrayList<>();
            sourceNodes.add(sourceNode);
            List<SourceLinkUtil> sourceLinks = new ArrayList<>();
            Iterator<FragmentInfo> iterator = peptide.getFragments().iterator();
            while (iterator.hasNext()) {
                FragmentInfo fragment = iterator.next();
                SourceNode sourceNodeFragment = new SourceNode();
                sourceNodeFragment.setName(fragment.getCutInfo());
                sourceNodeFragment.setId(peptideRef + "-" + fragment.getCutInfo());
                double[] d = new double[]{fragment.getMz(),peptide.getRt(), fragment.getIntensity()};
                sourceNodeFragment.setValue(d);
                sourceNodeFragment.setCategory(0);
                sourceNodeFragment.setSymbolSize(5.295237333333333);
                sourceNodeFragment.setSymbol("circle");
                sourceNodes.add(sourceNodeFragment);
                SourceLinkUtil sourceLink = new SourceLinkUtil();
                sourceLink.setSource(sourceNodeFragment.getId());
                sourceLink.setTarget(peptideRef);
                sourceLinks.add(sourceLink);
                }
                SourceCategory sourceCategory = new SourceCategory();
                sourceCategory.setName("类目"+i);
                finalMap.get("nodes").addAll(sourceNodes);
                finalMap.get("links").addAll(sourceLinks);
                finalMap.get("categories").add(sourceCategory);
            }
        List<SourceNode> sourceNodeWithOutPet = new ArrayList<>();
        Map<String,double[]> fragMap = new HashMap<>();
        for(Object ss:finalMap.get("nodes")){
            SourceNode tt = (SourceNode) ss;
            if(tt.getSymbolSize()==5.295237333333333){
                sourceNodeWithOutPet.add(tt);
                fragMap.put(tt.getId(),tt.getValue());
            }
        }
        List<Object> intensityList = new ArrayList<>();
        for(int st=0;st<sourceNodeWithOutPet.size();st++) {
            for (int sts = st + 1; sts < sourceNodeWithOutPet.size(); sts++) {
                if (Math.abs(sourceNodeWithOutPet.get(st).getValue()[0] - sourceNodeWithOutPet.get(sts).getValue()[0]) <= range ) {
                    SourceLinkUtil stLink = new SourceLinkUtil();
                    stLink.setSource(sourceNodeWithOutPet.get(sts).getId());
                    stLink.setTarget(sourceNodeWithOutPet.get(st).getId());
                    finalMap.get("links").add(stLink);
                    if(sourceNodeWithOutPet.get(st).getValue().length==3 && sourceNodeWithOutPet.get(sts).getValue().length==3){
                        SourceLinkUtil intensityDesc = new SourceLinkUtil();
                        intensityDesc.setValue(Math.abs(sourceNodeWithOutPet.get(st).getValue()[2]-sourceNodeWithOutPet.get(sts).getValue()[2])/sourceNodeWithOutPet.get(st).getValue()[2]);
                        intensityDesc.setSource(sourceNodeWithOutPet.get(sts).getId());
                        intensityDesc.setTarget(sourceNodeWithOutPet.get(st).getId());
                        intensityList.add(intensityDesc);
                    }
                }
            }
        }
        List<PeptideDO> allByLibraryId = getAllByLibraryId(libraryId);
        allByLibraryId.removeAll(peptideList);
        List<PeptideDO> removeNull = new ArrayList<>();
        for(PeptideDO peptideDO:allByLibraryId){
            if(peptideDO.getProteins()!=null){
                removeNull.add(peptideDO);
            }
        }
        AtomicInteger c= new AtomicInteger();
        removeNull.forEach(peptideDO -> {
            if(c.get()%1000==0){
                logger.info("已经循环"+c.get()+"次");
            }
            Set<FragmentInfo> fragments = peptideDO.getFragments();
            fragments.forEach(fragment ->{
                for(SourceNode sourceNode:sourceNodeWithOutPet){
                    String[] peptideRef = sourceNode.getId().split("-");
                    String s = peptideRef[0];
                    if(peptideDO.getMz()<=swath.get(s)[1] && peptideDO.getMz()>=swath.get(s)[0]){
                        if(Math.abs(sourceNode.getValue()[0]-fragment.getMz())<range){
                            SourceNode node = new SourceNode();
                            SourceNode fNode = new SourceNode();
                            node.setSymbol("triangle");
                            fNode.setSymbol("triangle");
                            node.setCategory(1);
                            fNode.setCategory(1);
                            node.setId(peptideDO.getPeptideRef());
                            fNode.setId(peptideDO.getPeptideRef()+"-"+fragment.getCutInfo());
                            node.setCategory(1);
                            fNode.setCategory(1);
                            node.setValue(new double[]{peptideDO.getMz(),peptideDO.getRt()});
                            fNode.setValue(new double[]{fragment.getMz(),peptideDO.getRt(), fragment.getIntensity()});
                            node.setSymbolSize(20.266666666666666);
                            fNode.setSymbolSize(5.295237333333333);
                            node.setName(peptideDO.getPeptideRef());
                            fNode.setName(fragment.getCutInfo());
                            SourceLinkUtil sourceLinkP = new SourceLinkUtil();
                            sourceLinkP.setSource(node.getId());
                            sourceLinkP.setTarget(fNode.getId());
                            SourceLinkUtil sourceLinkF = new SourceLinkUtil();
                            sourceLinkF.setSource(fNode.getId());
                            sourceLinkF.setTarget(sourceNode.getId());
                            if(fNode.getValue().length==3 && sourceNode.getValue().length==3){
                                SourceLinkUtil intensityDesc = new SourceLinkUtil();
                                intensityDesc.setValue(fNode.getValue()[2]/sourceNode.getValue()[2]);
                                intensityDesc.setSource(fNode.getId());
                                intensityDesc.setTarget(sourceNode.getId());
                                intensityList.add(intensityDesc);
                            }
                            sourceLinkF.setValue(Math.abs(peptideDO.getRt()-sourceNode.getValue()[1]));
                            finalMap.get("nodes").add(node);
                            finalMap.get("nodes").add(fNode);
                            finalMap.get("links").add(sourceLinkP);
                            finalMap.get("links").add(sourceLinkF);
                        }
                    }
                }
            });
            c.getAndIncrement();
        });
        List<Object> nodes = finalMap.get("nodes");
        Set<Object> removeRepeat = new HashSet<>();
        removeRepeat.addAll(nodes);
        finalMap.remove("nodes");
        List<Object> finalList = new ArrayList<>();
        finalList.addAll(removeRepeat);
        finalMap.put("nodes",finalList);
        SourceCategory sourceCategory = new SourceCategory();
        sourceCategory.setName("本蛋白肽段");
        SourceCategory sourceCategory2 = new SourceCategory();
        sourceCategory2.setName("关联肽段");
        finalMap.get("categories").clear();
        finalMap.get("categories").add(sourceCategory);
        finalMap.get("categories").add(sourceCategory2);
        Result result = new Result();
        Map<String,Integer>  linMap= new HashMap<>();
        List<Object> links = finalMap.get("links");
        for(Object object:links) {
            SourceLinkUtil sourceLinkUtil = (SourceLinkUtil) object;
            if (sourceLinkUtil.getTarget().contains("-") && sourceLinkUtil.getSource().contains("-")) {
                if (linMap.containsKey(sourceLinkUtil.getTarget())) {
                    Integer i1 = linMap.get((sourceLinkUtil.getTarget()));
                    linMap.put(sourceLinkUtil.getTarget(), i1 + 1);
                } else {
                    linMap.put(sourceLinkUtil.getTarget(), 1);
                }
            }
        }
        List<Object> list =new ArrayList<>();
        for(String key:linMap.keySet()){
            SourcePeptideLinkNumber sourcePeptideLinkNumber = new SourcePeptideLinkNumber();
            sourcePeptideLinkNumber.setName(key);
            sourcePeptideLinkNumber.setNumber(linMap.get(key));
            list.add(sourcePeptideLinkNumber);
        }
        finalMap.put("count",list);
        finalMap.put("intensity",intensityList);
        result.setData(finalMap);
        return result;
        }
    }

