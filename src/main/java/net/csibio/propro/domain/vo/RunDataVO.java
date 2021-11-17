package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.constants.enums.IdentifyStatus;
import net.csibio.propro.domain.bean.data.BaseData;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.utils.DataUtil;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RunDataVO {

    String id;

    String runId;

    String group;

    String alias;

    Boolean decoy;

    String overviewId;

    List<String> proteins;

    String peptideRef;

    List<PeakGroup> peakGroupList;

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
    Double apexRt;

    //距离realRt最近的光谱图rt
    Double selectedRt;

    //Intensity Sum
    Double intensitySum;

    Integer ionsLow;

    //所在overview的最低分数阈值
    Double minTotalScore;


    public RunDataVO() {
    }

    public RunDataVO(String runId) {
        this.runId = runId;
    }

    public RunDataVO(String runId, String overviewId, String peptideRef) {
        this.runId = runId;
        this.peptideRef = peptideRef;
        this.overviewId = overviewId;
    }

    public RunDataVO merge(DataDO data, DataSumDO dataSum) {
        if (data != null) {
            DataUtil.decompress(data);
            BeanUtils.copyProperties(data, this);
        }
        if (dataSum != null) {
            BeanUtils.copyProperties(dataSum, this);
            if (data != null && data.getPeakGroupList() != null && dataSum.getApexRt() != null) {
                this.selectIndex = peakGroupList.stream().map(PeakGroup::getApexRt).toList().indexOf(dataSum.getApexRt());
            }
        } else {
            if (status.equals(IdentifyStatus.WAIT.getCode())) {
                status = IdentifyStatus.NO_PEAK_GROUP_FIND.getCode();
            }
        }
        return this;
    }

    public RunDataVO merge(BaseData data, DataSumDO dataSum) {
        if (data != null) {
            BeanUtils.copyProperties(data, this);
        }
        if (dataSum != null) {
            BeanUtils.copyProperties(dataSum, this);
            if (data != null && data.getPeakGroupList() != null && dataSum.getApexRt() != null) {
                this.selectIndex = peakGroupList.stream().map(PeakGroup::getApexRt).toList().indexOf(dataSum.getApexRt());
            }
        } else {
            if (status.equals(IdentifyStatus.WAIT.getCode())) {
                status = IdentifyStatus.NO_PEAK_GROUP_FIND.getCode();
            }
        }
        return this;
    }
}
