package net.csibio.propro.loader;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.annotation.Section;
import net.csibio.propro.config.SectionRegister;
import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.domain.query.DictQuery;
import net.csibio.propro.service.DictService;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.tools.aspect.DictFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DictLoader implements ApplicationRunner {

    @Autowired
    DictService dictService;

    @Autowired
    LibraryService libraryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, Section> sectionMap = SectionRegister.getSectionMap();
        List<DictDO> dictDOList = DictFunction.dictTableInsert(sectionMap);
        log.info("Dict扫描开始执行");
        for (DictDO dictDO : dictDOList) {
            String name = dictDO.getName();
            String version = dictDO.getVersion();
            DictQuery dictQuery = new DictQuery();
            dictQuery.setName(name);
            List<DictDO> list = dictService.getList(dictQuery).getData();

            if (list.size() == 0) {
                try {
                    dictService.insert(dictDO);
                } catch (Exception e) {
                    //Do Nothing
                }

            } else {
                if (!list.get(0).getVersion().equals(version)) {
                    DictDO dictDOE = list.get(0);
                    dictDOE.setVersion(dictDO.getVersion());
                    dictDOE.setItem(dictDO.getItem());
                    dictDOE.setName(dictDO.getName());
                    dictService.update(dictDOE);
                }
            }
        }
        log.info("Dict扫描开始完成");
    }
}
