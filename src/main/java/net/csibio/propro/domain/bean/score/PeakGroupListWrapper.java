package net.csibio.propro.domain.bean.score;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

/**
 * 一个peptideRef在某一个时间段内检测到的所有峰组的列表
 */
@Data
public class PeakGroupListWrapper {

    /**
     * 时候检测到了峰组
     */
    boolean found;

    /**
     * 已经检测到的峰组
     */
    List<PeakGroup> list;

    /**
     * 归一化的库强度Map,key为cutInfo
     */
    HashMap<String, Double> normIntMap;

    public PeakGroupListWrapper() {
    }

    public PeakGroupListWrapper(boolean isFound) {
        this.found = isFound;
    }

    public PeakGroupListWrapper(List<PeakGroup> list, HashMap<String, Double> normIntMap) {
        this.found = true;
        this.list = list;
        this.normIntMap = normIntMap;
    }
}
