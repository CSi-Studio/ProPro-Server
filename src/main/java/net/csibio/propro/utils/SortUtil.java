package net.csibio.propro.utils;

import com.google.common.collect.Ordering;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.domain.bean.data.DataScore;
import net.csibio.propro.domain.bean.score.PeakGroup;
import net.csibio.propro.domain.bean.score.SelectedPeakGroup;
import net.csibio.propro.domain.db.BlockIndexDO;
import net.csibio.propro.domain.db.PeptideDO;

import java.util.Comparator;
import java.util.List;

public class SortUtil {

    public static List<DataScore> sortByPeptideRef(List<DataScore> scores) {
        Ordering<DataScore> ordering = Ordering.from(new Comparator<DataScore>() {
            @Override
            public int compare(DataScore o1, DataScore o2) {
                return o1.getPeptideRef().compareTo(o2.getPeptideRef());
            }
        });

        return ordering.sortedCopy(scores);
    }

    public static List<BlockIndexDO> sortBlockIndexByStartPtr(List<BlockIndexDO> swathList) {
        Ordering<BlockIndexDO> ordering = Ordering.from(new Comparator<BlockIndexDO>() {
            @Override
            public int compare(BlockIndexDO o1, BlockIndexDO o2) {
                return o1.getStartPtr().compareTo(o2.getStartPtr());
            }
        });

        return ordering.sortedCopy(swathList);
    }

    public static List<SelectedPeakGroup> sortByMainScore(List<SelectedPeakGroup> scores, boolean isDesc) {
        Ordering<SelectedPeakGroup> ordering = Ordering.from(new Comparator<SelectedPeakGroup>() {
            @Override
            public int compare(SelectedPeakGroup o1, SelectedPeakGroup o2) {
                try {
                    if (isDesc) {
                        return o2.getTotalScore().compareTo(o1.getTotalScore());
                    } else {
                        return o1.getTotalScore().compareTo(o2.getTotalScore());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        return ordering.sortedCopy(scores);
    }

    public static List<SelectedPeakGroup> sortByFdr(List<SelectedPeakGroup> scores, boolean isDesc) {
        Ordering<SelectedPeakGroup> ordering = Ordering.from(new Comparator<SelectedPeakGroup>() {
            @Override
            public int compare(SelectedPeakGroup o1, SelectedPeakGroup o2) {
                try {
                    if (o1.getFdr() == null) {
                        return 1;
                    }
                    if (o2.getFdr() == null) {
                        return -1;
                    }
                    if (isDesc) {
                        return o2.getFdr().compareTo(o1.getFdr());
                    } else {
                        return o1.getFdr().compareTo(o2.getFdr());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        return ordering.sortedCopy(scores);
    }

    public static List<PeakGroup> sortBySelectedScore(List<PeakGroup> scores, String scoreName, boolean isDesc, List<String> scoreTypes) {
        Ordering<PeakGroup> ordering = Ordering.from(new Comparator<PeakGroup>() {
            @Override
            public int compare(PeakGroup o1, PeakGroup o2) {
                if (isDesc) {
                    return o2.get(scoreName, scoreTypes).compareTo(o1.get(scoreName, scoreTypes));
                } else {
                    try {
                        return o1.get(scoreName, scoreTypes).compareTo(o2.get(scoreName, scoreTypes));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                }
            }
        });

        return ordering.sortedCopy(scores);
    }

    /**
     * @param scores
     * @param isDesc 是否降序排序
     * @return
     */
    public static List<SelectedPeakGroup> sortByPValue(List<SelectedPeakGroup> scores, boolean isDesc) {
        Ordering<SelectedPeakGroup> ordering = Ordering.from(new Comparator<SelectedPeakGroup>() {
            @Override
            public int compare(SelectedPeakGroup o1, SelectedPeakGroup o2) {
                if (isDesc) {
                    return o2.getPValue().compareTo(o1.getPValue());
                } else {
                    return o1.getPValue().compareTo(o2.getPValue());
                }
            }
        });

        return ordering.sortedCopy(scores);
    }

    /**
     * @param peptides
     * @param isDesc   是否降序排序
     * @return
     */
    public static List<PeptideDO> sortByMz(List<PeptideDO> peptides, boolean isDesc) {
        Ordering<PeptideDO> ordering = Ordering.from(new Comparator<PeptideDO>() {
            @Override
            public int compare(PeptideDO o1, PeptideDO o2) {
                if (isDesc) {
                    return o2.getMz().compareTo(o1.getMz());
                } else {
                    return o1.getMz().compareTo(o2.getMz());
                }
            }
        });

        return ordering.sortedCopy(peptides);
    }

    /**
     * @param rangs
     * @param isDesc 是否降序排序
     * @return
     */
    public static List<WindowRange> sortByMzStart(List<WindowRange> rangs, boolean isDesc) {
        Ordering<WindowRange> ordering = Ordering.from(new Comparator<WindowRange>() {
            @Override
            public int compare(WindowRange o1, WindowRange o2) {
                if (isDesc) {
                    return o2.getStart().compareTo(o1.getStart());
                } else {
                    return o1.getStart().compareTo(o2.getStart());
                }
            }
        });

        return ordering.sortedCopy(rangs);
    }

}
