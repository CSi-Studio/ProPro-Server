package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.parser.FastaParser;
import net.csibio.propro.constants.enums.LibraryType;
import net.csibio.propro.constants.enums.TaskTemplate;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.ProteinService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.task.LibraryTask;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Api(tags = {"Home"})
@RestController
@RequestMapping("/")
//@Section(name="homecontroller",type = "controller")
public class HomeController {

    @Autowired
    FastaParser fastaParser;
    @Autowired
    ProteinService proteinService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    TaskService taskService;
    @Autowired
    LibraryTask libraryTask;


    @GetMapping(value = "/init")
    Result init() throws IOException {
        initFastas();
        initInsLibrary();

        return Result.OK();
    }


    @GetMapping(value = "/initd")
    Result ok() {
        return Result.OK();
    }

    private void initFastas() throws IOException {
        File[] proteinFiles = getFiles("dbfile/fasta");
        for (int i = 0; i < proteinFiles.length; i++) {
            long start = System.currentTimeMillis();
            log.info("正在初始化fasta文件:" + proteinFiles[i].getName());
            FileInputStream inputStream = new FileInputStream(proteinFiles[i]);
            proteinService.importFromLocalFasta(inputStream, proteinFiles[i].getName(), true);
            log.info(proteinFiles[i].getName() + "初始化完毕,耗时:" + (System.currentTimeMillis() - start));
        }
    }

    private void initInsLibrary() throws IOException {
        File[] libraryFiles = getFiles("dbfile/library/ins");
        for (int j = 0; j < libraryFiles.length; j++) {
            LibraryDO library = new LibraryDO();
            String baseName = FilenameUtils.getBaseName(libraryFiles[j].getName());
            library.setName(baseName);
            library.setType(LibraryType.INS.getName());
            libraryService.insert(library);
            try {
                TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
                taskService.insert(taskDO);
                library.setFilePath(libraryFiles[j].getPath());
                InputStream libFileStream = new FileInputStream(libraryFiles[j]);
                libraryTask.uploadLibraryFile(library, libFileStream, taskDO);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
