package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.data.PeptideScores;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-18 23:16
 */
@Data
public class TrainData {

    List<PeptideScores> targets;
    List<PeptideScores> decoys;

    public TrainData() {
    }

    public TrainData(List<PeptideScores> targets, List<PeptideScores> decoys) {
        this.targets = targets;
        this.decoys = decoys;
    }
}
