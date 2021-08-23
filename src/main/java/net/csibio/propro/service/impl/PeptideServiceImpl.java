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
import net.csibio.propro.domain.bean.peptide.Protein;
import net.csibio.propro.domain.bean.peptide.SimplePeptide;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.PeptideUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        return peptideDAO.countByProteinName(libraryId);
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
//
//    @Override
//    public PeptideDO buildWithPeptideRef(String peptideRef) {
//        List<Integer> chargeTypes = new ArrayList<>();
//        //默认采集所有离子碎片,默认采集[1,precusor charge]区间内的整数
//        if (peptideRef.contains("_")) {
//            int charge = Integer.parseInt(peptideRef.split("_")[1]);
//            for (int i = 1; i <= charge; i++) {
//                chargeTypes.add(i);
//            }
//        } else {
//            chargeTypes.add(1);
//            chargeTypes.add(2);
//        }
//        return buildWithPeptideRef(peptideRef, 3, ResidueType.abcxyz, chargeTypes);
//    }

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
}
