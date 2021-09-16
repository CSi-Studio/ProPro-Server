package net.csibio.propro.service;

import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;

import java.util.List;
import java.util.Set;

public interface SimulateService {

    void predictFragment(LibraryDO library, String spModel, boolean iso);

    /**
     * 预测肽段碎片
     *
     * @param sequence 原有的肽段信息
     * @param spModel  @see SpModelConstant
     * @param iso      是否考虑同位素
     * @param limit    按照intensity从高到低排序以后取强度前limit的碎片
     * @return
     */
    List<FragmentInfo> predictFragment(String sequence, String spModel, boolean iso, int limit);

    /**
     * 预测肽段碎片
     *
     * @param sequence
     * @param spModel
     * @param iso
     * @return
     */
    Set<FragmentInfo> predictFragment(String sequence, String spModel, boolean iso);

    /**
     * 预测肽段碎片,不做限制
     *
     * @param peptide 原有的肽段信息
     * @param spModel @see SpModelConstant
     * @param iso     是否考虑同位素
     * @return
     */
    Set<FragmentInfo> singlePredictFragment(PeptideDO peptide, String spModel, boolean iso);
}
