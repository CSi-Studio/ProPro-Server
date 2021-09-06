package net.csibio.propro.domain.vo;

import lombok.Data;
import net.csibio.propro.domain.bean.data.BaseData;
import net.csibio.propro.domain.db.DataDO;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.utils.DataUtil;

import java.util.HashMap;
import java.util.Map;

@Data
public class ExpDataVO {

    String expId;

    String overviewId;

    String peptideRef;

    float[] rtArray;  //排序后的rt

    Map<String, float[]> intMap = new HashMap<>();  //key为cutInfo, value为对应的intensity值列表(也即该碎片的光谱图信息)

    Map<String, Float> cutInfoMap; //冗余的peptide切片信息,key为cutInfo,value为mz

    Integer status;

    Double fdr;

    Double qValue;

    //库rt
    Double libRt;

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
            this.peptideRef = data.getPeptideRef();
            this.overviewId = data.getOverviewId();
            this.rtArray = data.getRtArray();
            this.cutInfoMap = data.getCutInfoMap();
            this.intMap = data.getIntMap();
            this.status = data.getStatus();
            this.libRt = data.getLibRt();
        }
        if (dataSum != null) {
            this.fdr = dataSum.getFdr();
            this.qValue = dataSum.getQValue();
            this.status = dataSum.getStatus();
            this.sum = dataSum.getSum();
            this.fragIntFeature = dataSum.getFragIntFeature();
            this.realRt = dataSum.getRealRt();
        }
        return this;
    }


    public ExpDataVO merge(BaseData data, DataSumDO dataSum) {
        if (data != null) {
            this.peptideRef = data.getPeptideRef();
            this.overviewId = data.getOverviewId();
            this.status = data.getStatus();
            this.libRt = data.getLibRt();
        }
        if (dataSum != null) {
            this.fdr = dataSum.getFdr();
            this.qValue = dataSum.getQValue();
            this.status = dataSum.getStatus();
            this.sum = dataSum.getSum();
            this.fragIntFeature = dataSum.getFragIntFeature();
            this.realRt = dataSum.getRealRt();
        }
        return this;
    }
}
