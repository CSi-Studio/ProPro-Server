package net.csibio.propro.algorithm.irt;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.peak.PeakPicker;
import net.csibio.propro.algorithm.score.scorer.IrtScorer;
import net.csibio.propro.algorithm.score.scorer.Scorer;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.ListPairs;
import net.csibio.propro.domain.bean.common.Pair;
import net.csibio.propro.domain.bean.irt.IrtResult;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.RunService;
import net.csibio.propro.utils.MathUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public abstract class Irt {

    @Autowired
    PeptideService peptideService;
    @Autowired
    Extractor extractor;
    @Autowired
    Scorer scorer;
    @Autowired
    BlockIndexService blockIndexService;
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    IrtScorer irtScorer;
    @Autowired
    LinearFitter linearFitter;
    @Autowired
    RunService runService;

    public abstract List<DataDO> extract(RunDO run, AnalyzeParams params);

    /**
     * XIC并且求出iRT
     *
     * @param run
     * @param params
     * @return
     */
    public Result<IrtResult> align(RunDO run, AnalyzeParams params) {
        try {
            List<DataDO> dataList = extract(run, params);
            if (dataList == null || dataList.isEmpty()) {
                return Result.Error(ResultCode.DATA_IS_EMPTY);
            }

            Result<IrtResult> result = align(dataList, params);
            if (result.isFailed()) {
                return result;
            }
            log.info("实验" + run.getName() + "IRT结束:" + result.getData().getSi().getFormula());
            run.setIrt(result.getData());
            runService.update(run);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.IRT_EXCEPTION);
        }
    }

    /**
     * 从一个数据提取结果列表中求出iRT
     *
     * @param dataList 对于靶点的xic结果
     * @param params   分析参数,其中的Sigma通常为30/8 = 6.25/Spacing通常为0.01
     * @return irt结果
     */
    protected Result<IrtResult> align(List<DataDO> dataList, AnalyzeParams params) throws Exception {
        List<Double> compoundRt = new ArrayList<>();
        double minGroupRt = Double.MAX_VALUE;
        double maxGroupRt = -Double.MAX_VALUE;
        dataList = dataList.stream().sorted(Comparator.comparing(DataDO::getLibRt)).toList();
        List<DataDO> selectedDataList = new ArrayList<>();
        for (DataDO data : dataList) {

            double groupRt = data.getLibRt();
            if (groupRt > maxGroupRt) {
                maxGroupRt = groupRt;
            }
            if (groupRt < minGroupRt) {
                minGroupRt = groupRt;
            }

            data = irtScorer.score(data, params);
            if (data.getPeakGroupList() == null || data.getPeakGroupList().size() == 0) {
                continue;
            }
            selectedDataList.add(data);
            compoundRt.add(groupRt);
        }

        List<Pair> pairs = findBestFeature(selectedDataList, compoundRt);
        double delta = (maxGroupRt - minGroupRt) / 30d;
        List<Pair> pairsCorrected = selectPairs(pairs, delta);

        log.info("choose finish ------------------------");
        IrtResult irtResult = new IrtResult();

        List<Double> x4Selected = new ArrayList<>();
        List<Double> y4Selected = new ArrayList<>();
        List<Double> x4Unselected = new ArrayList<>();
        List<Double> y4Unselected = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            if (pairsCorrected.contains(pairs.get(i))) {
                x4Selected.add(pairs.get(i).left());
                y4Selected.add(pairs.get(i).right());
            } else {
                x4Unselected.add(pairs.get(i).left());
                y4Unselected.add(pairs.get(i).right());
            }
        }

        irtResult.setSelected(new ListPairs(x4Selected, y4Selected));
        irtResult.setUnselected(new ListPairs(x4Unselected, y4Unselected));
        SlopeIntercept slopeIntercept = linearFitter.proproFit(pairsCorrected, delta);
        irtResult.setSi(slopeIntercept);
        irtResult.setLibraryId(params.getInsLibId());
        return Result.OK(irtResult);
    }

    /**
     * get rt pairs for every peptideRef
     *
     * @param dataList peptideRef list of List<PeakGroup>
     * @param rt       get from groupsResult.getModel()
     * @return rt pairs
     */
    protected List<Pair> findBestFeature(List<DataDO> dataList, List<Double> rt) {

        List<Pair> pairs = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            List<PeakGroup> peakGroupList = dataList.get(i).getPeakGroupList();
            double max = Double.MIN_VALUE;
            double runRt = 0d;
            for (int j = 0; j < peakGroupList.size(); j++) {
                if (peakGroupList.get(j).getScores() == null) {
                    continue;
                }
                if (peakGroupList.get(j).getTotalScore() > max) {
                    max = peakGroupList.get(j).getTotalScore();
                    runRt = peakGroupList.get(j).getApexRt();
                }
            }
            if (Constants.ESTIMATE_BEST_PEPTIDES && max < Constants.OVERALL_QUALITY_CUTOFF) {
                continue;
            }
            pairs.add(new Pair(rt.get(i), runRt));
        }
        return pairs;
    }

    /**
     * 选择合适的点作为回归点
     *
     * @param rtPairsList 所有入选的点列表,每一个Pair<Double, Double>都代表该肽段的所有的峰得分
     * @param delta
     * @return
     * @throws Exception
     */
    protected List<Pair> selectPairs(List<Pair> rtPairsList, double delta) throws Exception {
        List<Pair> rtPairsCorrected = new ArrayList<>(rtPairsList);
        preprocessRtPairs(rtPairsCorrected, 50d);
        SlopeIntercept slopeIntercept = linearFitter.huberFit(rtPairsCorrected, delta);
        while (MathUtil.getRsq(rtPairsCorrected) < 0.95 && rtPairsCorrected.size() >= 2) {
            int maxErrorIndex = findMaxErrorIndex(slopeIntercept, rtPairsCorrected);
            rtPairsCorrected.remove(maxErrorIndex);
            slopeIntercept = linearFitter.huberFit(rtPairsCorrected, delta);
        }
        return rtPairsCorrected;
    }

    protected int findMaxErrorIndex(SlopeIntercept si, List<Pair> rtPairs) {
        int maxIndex = 0;
        double maxError = 0d;
        for (int i = 0; i < rtPairs.size(); i++) {
            double tempError = Math.abs(rtPairs.get(i).right() * si.getSlope() + si.getIntercept() - rtPairs.get(i).left());
            if (tempError > maxError) {
                maxError = tempError;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    protected void preprocessRtPairs(List<Pair> rtPairs, double tolerance) {
        try {
            SlopeIntercept initSlopeIntercept = linearFitter.getInitSlopeIntercept(rtPairs);
            for (int i = rtPairs.size() - 1; i >= 0; i--) {
                double tempError = Math.abs(rtPairs.get(i).right() * initSlopeIntercept.getSlope() + initSlopeIntercept.getIntercept() - rtPairs.get(i).left());
                if (tempError > tolerance) {
                    rtPairs.remove(i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
