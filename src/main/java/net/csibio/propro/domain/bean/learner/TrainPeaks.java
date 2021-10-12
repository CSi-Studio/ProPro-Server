package net.csibio.propro.domain.bean.learner;

import lombok.Data;
import net.csibio.propro.domain.bean.score.SelectedPeakGroupScore;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 16:00
 */
@Data
public class TrainPeaks {

    List<SelectedPeakGroupScore> bestTargets;

    List<SelectedPeakGroupScore> topDecoys;
}
