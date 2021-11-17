package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.data.DataScore;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-18 23:16
 */
@Data
public class TrainData {

    List<DataScore> targets;
    List<DataScore> decoys;

    public TrainData() {
    }

    public TrainData(List<DataScore> targets, List<DataScore> decoys) {
        this.targets = targets;
        this.decoys = decoys;
    }
}
