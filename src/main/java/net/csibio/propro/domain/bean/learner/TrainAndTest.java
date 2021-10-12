package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.data.PeptideScore;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-18 23:16
 */
@Data
public class TrainAndTest {
    Double[][] trainData;
    Integer[] trainId;
    Boolean[] trainIsDecoy;
    Double[][] testData;
    Integer[] testId;
    Boolean[] testIsDecoy;

    List<PeptideScore> trainTargets;
    List<PeptideScore> trainDecoys;
    List<PeptideScore> testTargets;
    List<PeptideScore> testDecoys;

    public TrainAndTest() {
    }

    public TrainAndTest(List<PeptideScore> trainTargets, List<PeptideScore> trainDecoys, List<PeptideScore> testTargets, List<PeptideScore> testDecoys) {
        this.trainTargets = trainTargets;
        this.trainDecoys = trainDecoys;
        this.testTargets = testTargets;
        this.testDecoys = testDecoys;
    }
}
