package net.csibio.propro.domain.bean.data;

import lombok.Data;
import net.csibio.propro.domain.bean.score.PeakGroupScores;

import java.util.List;

/**
 * 某一个肽段的打分结果,包含了其中所有的Peak峰组的打分结果
 */
@Data
public class PeptideScores {

    String id;

    List<String> proteins;

    //肽段名称_带电量,例如:SLMLSYN(UniMod:7)AITHLPAGIFR_3
    String peptideRef;
    //是否是伪肽段
    Boolean decoy = false;
    //所有峰组的打分情况
    List<PeakGroupScores> scoreList;
}
