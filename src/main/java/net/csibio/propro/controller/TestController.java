package net.csibio.propro.controller;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.LibraryType;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.ProteinDO;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.ProteinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("test")
@Slf4j
public class TestController {

    @Autowired
    ProteinService proteinService;
    @Autowired
    LibraryService libraryService;

    @GetMapping(value = "/lms")
    Result lms() throws IOException {
        File[] proteinFiles = getFiles("dbfile/fasta");
        log.info("正在初始化fasta文件:" + proteinFiles[2].getName());
        FileInputStream inputStream = new FileInputStream(proteinFiles[2]);
        List<ProteinDO> pList = proteinService.importFromLocalFasta(inputStream, proteinFiles[2].getName(), true);
        LibraryDO lib = new LibraryDO("TestForFasta", LibraryType.ANA.getName());
        libraryService.insert(lib);
        proteinService.proteinToPeptide(lib.getId(), pList, 10, 100, "HCD", false);

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
