package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.data.PeptideScore;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-18 23:16
 */
@Data
public class TrainData {

    List<PeptideScore> targets;
    List<PeptideScore> decoys;

    public TrainData() {
    }

    public TrainData(List<PeptideScore> targets, List<PeptideScore> decoys) {
        this.targets = targets;
        this.decoys = decoys;
    }
}
