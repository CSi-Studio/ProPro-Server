package net.csibio.propro.task;

import net.csibio.propro.constants.enums.LibraryType;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.constants.enums.TaskTemplate;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.utils.FileUtil;
import net.csibio.propro.utils.RepositoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("libraryTask")
public class LibraryTask extends BaseTask {

    @Autowired
    LibraryService libraryService;

    @Async(value = "uploadFileExecutor")
    public void uploadLibraryFile(LibraryDO library, InputStream libFileStream, TaskDO taskDO) {
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        libraryService.uploadFile(library, libFileStream, taskDO);
    }

    /**
     * 用于直接扫描本地仓库指定路径下的库文件,具体的指定路径见RepositoryUtil
     *
     * @throws FileNotFoundException
     * @see RepositoryUtil#getIrtLibraryRepo()
     * @see RepositoryUtil#getAnaLibraryRepo()
     */
    @Async(value = "uploadFileExecutor")
    public void scan() throws FileNotFoundException {
        List<LibraryDO> libList = libraryService.getAll(new LibraryQuery());
        Set<String> libPathSet = libList.stream().map(LibraryDO::getFilePath).collect(Collectors.toSet());
        List<File> libFiles = FileUtil.scanLibraryFiles();
        List<File> irtLibFiles = FileUtil.scanIrtLibraryFiles();
        logger.info("本地库:" + libFiles.size());
        logger.info("本地iRT库:" + irtLibFiles.size());
        for (File file : libFiles) {
            if (!libPathSet.contains(file.getAbsolutePath())) {
                logger.info("扫描到文件:" + file.getAbsolutePath() + ",开始解析");
                LibraryDO library = new LibraryDO(file.getName(), LibraryType.ANA.getName(), file.getAbsolutePath());
                Result<LibraryDO> resultDO = libraryService.insert(library);
                if (resultDO.isFailed()) {
                    logger.error(resultDO.getErrorMessage());
                    continue;
                }
                TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
                taskService.insert(taskDO);
                libraryService.uploadFile(library, new FileInputStream(file), taskDO);
            }
        }
        for (File file : irtLibFiles) {
            if (!libPathSet.contains(file.getAbsolutePath())) {
                LibraryDO library = new LibraryDO(file.getName(), LibraryType.INS.getName(), file.getAbsolutePath());
                Result resultDO = libraryService.insert(library);
                if (resultDO.isFailed()) {
                    logger.error(resultDO.getErrorMessage());
                    continue;
                }
                TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
                taskService.insert(taskDO);
                libraryService.uploadFile(library, new FileInputStream(file), taskDO);
            }
        }
    }
}
