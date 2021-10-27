package net.csibio.propro.algorithm.irt;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.extract.Extractor;
import net.csibio.propro.algorithm.fitter.LinearFitter;
import net.csibio.propro.algorithm.peak.FeatureExtractor;
import net.csibio.propro.algorithm.score.Scorer;
import net.csibio.propro.algorithm.score.features.RtNormalizerScorer;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.ListPairs;
import net.csibio.propro.domain.bean.common.Pair;
import net.csibio.propro.domain.bean.irt.IrtResult;
import net.csibio.propro.domain.bean.peptide.PeptideCoord;
import net.csibio.propro.domain.bean.score.PeakGroupListWrapper;
import net.csibio.propro.domain.bean.score.ScoreRtPair;
import net.csibio.propro.domain.bean.score.SlopeIntercept;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.ExperimentDO;
import net.csibio.propro.domain.options.AnalyzeParams;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.service.BlockIndexService;
import net.csibio.propro.service.ExperimentService;
import net.csibio.propro.service.PeptideService;
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
    FeatureExtractor featureExtractor;
    @Autowired
    RtNormalizerScorer rtNormalizerScorer;
    @Autowired
    LinearFitter linearFitter;
    @Autowired
    ExperimentService experimentService;

    public abstract List<DataDO> extract(ExperimentDO exp, AnalyzeParams params);

    /**
     * XIC并且求出iRT
     *
     * @param exp
     * @param params
     * @return
     */
    public Result<IrtResult> align(ExperimentDO exp, AnalyzeParams params) {
        try {
            List<DataDO> dataList = extract(exp, params);
            if (dataList == null || dataList.isEmpty()) {
                return Result.Error(ResultCode.DATA_IS_EMPTY);
            }

            Result<IrtResult> result = align(dataList, params);
            if (result.isFailed()) {
                return result;
            }
            log.info("实验" + exp.getName() + "IRT结束:" + result.getData().getSi().getFormula());
            exp.setIrt(result.getData());
            experimentService.update(exp);
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
        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Double> compoundRt = new ArrayList<>();
        double minGroupRt = Double.MAX_VALUE;
        double maxGroupRt = -Double.MAX_VALUE;
        dataList = dataList.stream().sorted(Comparator.comparing(DataDO::getLibRt)).toList();
        for (DataDO data : dataList) {
            PeptideCoord coord = peptideService.getOne(new PeptideQuery(params.getInsLibId(), data.getPeptideRef()), PeptideCoord.class);
            PeakGroupListWrapper peakGroupListWrapper = featureExtractor.getExperimentFeature(data, coord.buildIntensityMap(), params.getMethod().getIrt().getSs());
            if (!peakGroupListWrapper.isFeatureFound()) {
                continue;
            }
            double groupRt = data.getLibRt();
            if (groupRt > maxGroupRt) {
                maxGroupRt = groupRt;
            }
            if (groupRt < minGroupRt) {
                minGroupRt = groupRt;
            }
            List<ScoreRtPair> scoreRtPairs = rtNormalizerScorer.score(peakGroupListWrapper.getList(), peakGroupListWrapper.getNormedIntMap(), groupRt, params);
            scoreRtPairs = scoreRtPairs.stream().sorted(Comparator.comparing(ScoreRtPair::getScore).reversed()).toList();
            if (scoreRtPairs.size() == 0) {
                continue;
            }
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(groupRt);
        }

        List<Pair> pairs = findBestFeature(scoreRtList, compoundRt);
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
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt         get from groupsResult.getModel()
     * @return rt pairs
     */
    protected List<Pair> findBestFeature(List<List<ScoreRtPair>> scoresList, List<Double> rt) {

        List<Pair> pairs = new ArrayList<>();

        for (int i = 0; i < scoresList.size(); i++) {
            List<ScoreRtPair> scores = scoresList.get(i);
            double max = Double.MIN_VALUE;
            //find max scoreForAll's rt
            double expRt = 0d;
            for (int j = 0; j < scores.size(); j++) {
                if (scores.get(j).getScore() > max) {
                    max = scores.get(j).getScore();
                    expRt = scores.get(j).getRealRt();
                }
            }
            if (Constants.ESTIMATE_BEST_PEPTIDES && max < Constants.OVERALL_QUALITY_CUTOFF) {
                continue;
            }
            pairs.add(new Pair(rt.get(i), expRt));
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
