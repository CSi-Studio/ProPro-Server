package net.csibio.propro.controller;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("test")
@Slf4j
public class TestController {

    @Autowired
    ProteinService proteinService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    OverviewService overviewService;
    @Autowired
    DataSumService dataSumService;

    @GetMapping(value = "/lms")
    Result lms() {
//        List<IdName> libIdNames = libraryService.getAll(new LibraryQuery(), IdName.class);

        HashMap<String, Object> update = new HashMap<>();
        update.put("disable", false);
        peptideService.updateAll(new HashMap<>(), update);
        return Result.OK();
    }

    @GetMapping(value = "/checkFragmentMz")
    Result checkFragmentMz() {
        long count = peptideService.count(new PeptideQuery());
        log.info("Total Peptides:" + count);
        long batch = count / 2000 + 1;
        PeptideQuery query = new PeptideQuery();
        query.setPageSize(2000);
        for (int i = 0; i < batch; i++) {
            query.setPageNo(i + 1);
            Result<List<PeptideDO>> pepListRes = peptideService.getList(query);
            if (pepListRes.isFailed()) {
                log.error(pepListRes.getErrorMessage());
            }
            List<PeptideDO> pepList = pepListRes.getData();
            if (pepList.size() == 0) {
                break;
            }
            pepList.forEach(pep -> {
                pep.getDecoyFragments().forEach(fragmentInfo -> {
                    if (fragmentInfo.getMz() == null) {
                        log.info(pep.getLibraryId() + "-" + pep.getPeptideRef() + "有问题");
                    }
                });
            });
            log.info("已扫描" + i + "/" + batch);
        }

        return Result.OK();
    }


    public static File[] getFiles(String path) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(path);
        File file = classPathResource.getFile();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    getFiles(files[i].getPath());
                } else {
                }
            }
            return files;
        } else {
            File[] files = new File[0];
            return files;
        }
    }
}
