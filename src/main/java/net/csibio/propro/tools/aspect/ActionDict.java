package net.csibio.propro.tools.aspect;

import net.csibio.propro.annotation.Section;
import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ActionDict {
    @Autowired
    public static DictService dictService;
    public static void insert(Map<String, Section> sectionMap) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        List<DictDO> dictDOList = DictFunction.dictTableInsert(sectionMap);
        dictService.insert(dictDOList);

    }
}
