package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.algorithm.score.ScoreType;
import net.csibio.propro.domain.bean.data.BaseData;
import net.csibio.propro.domain.bean.score.PeakGroupScores;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.utils.DataUtil;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExpDataVO {

    String id;

    String expId;

    String label;

    Boolean decoy;

    String overviewId;

    List<String> proteins;

    String peptideRef;

    List<PeakGroupScores> scoreList;

    int selectIndex;

    float[] rtArray;  //排序后的rt

    Map<String, float[]> intMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)

    Map<String, Float> cutInfoMap; //冗余的peptide切片信息,key为cutInfo,value为mz

    Integer status;

    Double fdr;

    Double qValue;

    //库rt
    Double irt;

    //最终鉴定的时间
    Double realRt;

    //Intensity Sum
    Double sum;

    //最终的定量值
    String fragIntFeature;

    public ExpDataVO() {
    }

    public ExpDataVO(String expId) {
        this.expId = expId;
    }

    public ExpDataVO(String expId, String overviewId, String peptideRef) {
        this.expId = expId;
        this.peptideRef = peptideRef;
        this.overviewId = overviewId;
    }

    public ExpDataVO merge(DataDO data, DataSumDO dataSum) {
        if (data != null) {
            DataUtil.decompress(data);
            BeanUtils.copyProperties(data, this);
        }
        if (dataSum != null) {
            BeanUtils.copyProperties(dataSum, this);
            if (data != null && data.getScoreList() != null && dataSum.getRealRt() != null) {
                this.selectIndex = scoreList.stream().map(PeakGroupScores::getRt).toList().indexOf(dataSum.getRealRt());
                data.getScoreList().get(selectIndex).put(ScoreType.WeightedTotalScore, dataSum.getTotalScore(), ScoreType.getAllTypesName());
            }
        }
        return this;
    }

    public ExpDataVO merge(BaseData data, DataSumDO dataSum) {
        if (data != null) {
            BeanUtils.copyProperties(data, this);
        }
        if (dataSum != null) {
            BeanUtils.copyProperties(dataSum, this);
            if (data != null && data.getScoreList() != null && dataSum.getRealRt() != null) {
                this.selectIndex = scoreList.stream().map(PeakGroupScores::getRt).toList().indexOf(dataSum.getRealRt());
                data.getScoreList().get(selectIndex).put(ScoreType.WeightedTotalScore, dataSum.getTotalScore(), ScoreType.getAllTypesName());
            }
        }
        return this;
    }
}
