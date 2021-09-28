package net.csibio.propro.constants.enums;

import com.google.common.collect.Lists;
import net.csibio.propro.constants.constant.ResidueType;

import java.util.List;

public enum FragMode {
    CID("CID", Lists.newArrayList(ResidueType.BIon, ResidueType.YIon)),
    HCD("HCD", Lists.newArrayList(ResidueType.BIon, ResidueType.YIon)),
    ETD("ETD", Lists.newArrayList(ResidueType.BIon, ResidueType.YIon, ResidueType.CIon, ResidueType.ZIon));

    String name;

    List<String> ionTypes;

    FragMode(String name, List<String> ionTypes) {
        this.name = name;
        this.ionTypes = ionTypes;
    }

    public String getName() {
        return name;
    }

    public List<String> getIonTypes() {
        return ionTypes;
    }
}
