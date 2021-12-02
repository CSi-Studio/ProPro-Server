package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.WindowRange;
import net.csibio.propro.algorithm.decoy.repeatCount.RepeatCount;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskTemplate;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.db.RunDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.vo.LibraryUpdateVO;
import net.csibio.propro.service.*;
import net.csibio.propro.task.LibraryTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = {"Library Module"})
@RestController
@RequestMapping("/api/library")
@Slf4j
public class LibraryController extends XController<LibraryDO, LibraryQuery, LibraryService> {

    @Autowired
    LibraryService libraryService;
    @Autowired
    TaskService taskService;
    @Autowired
    LibraryTask libraryTask;
    @Autowired
    RepeatCount repeatCount;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ProjectService projectService;
    @Autowired
    RunService runService;

    @GetMapping(value = "/list")
    Result list(LibraryQuery query) {
        Result<List<LibraryDO>> result = libraryService.getList(query);
        return result;
    }

    @PostMapping(value = "/update")
    Result<LibraryDO> update(LibraryUpdateVO libraryUpdateVO) {
        LibraryDO library = libraryService.getById(libraryUpdateVO.getId());
        if (library == null) {
            return Result.Error(ResultCode.OBJECT_NOT_EXISTED);
        }

        library.setTags(libraryUpdateVO.getTags());
        library.setDescription(libraryUpdateVO.getDescription());
        library.setOrganismStr(libraryUpdateVO.getOrganism());
        library.setType(libraryUpdateVO.getType());
        return libraryService.update(library);
    }

    @GetMapping(value = "/get")
    Result get(String id) {
        LibraryDO byId = libraryService.getById(id);
        Result result = new Result();
        result.setData(byId);
        return result;
    }

    @GetMapping(value = "/remove")
    Result remove(@RequestParam(value = "libraryIds") String libraryIds) {
        String[] libraryArray = libraryIds.split(SymbolConst.COMMA);
        Result result = new Result();
        List<String> errorList = new ArrayList<>();
        List<String> deletedIds = new ArrayList<>();
        for (String libraryId : libraryArray) {
            Result libraryResult = libraryService.removeById(libraryId);
            if (libraryResult.isSuccess()) {
                deletedIds.add(libraryId);
            } else {
                errorList.add("LibraryId:" + libraryId + "--" + libraryResult.getErrorMessage());
            }
        }
        if (deletedIds.size() != 0) {
            result.setData(deletedIds);
            result.setSuccess(true);
        }
        if (errorList.size() != 0) {
            result.setErrorList(errorList);
        }
        return result;
    }

    @PostMapping(value = "/add", headers = "content-type=multipart/form-data")
    Result add(LibraryUpdateVO libraryUpdateVO) {
        LibraryDO library = new LibraryDO();
        library.setName(libraryUpdateVO.getName());
        library.setDescription(libraryUpdateVO.getDescription());
        library.setType(libraryUpdateVO.getType());
        library.setOrganism(libraryUpdateVO.getOrganism() != null ? Arrays.stream(libraryUpdateVO.getOrganism().toLowerCase().split(SymbolConst.COMMA))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet())
                : null);
        Result result = libraryService.insert(library);
        if (result.isFailed()) {
            return result;
        }

        if (libraryUpdateVO.getLibFile() != null) {
            try {
                TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
                taskService.insert(taskDO);
                library.setFilePath(libraryUpdateVO.getLibFile().getOriginalFilename());
                InputStream libFileStream = libraryUpdateVO.getLibFile().getInputStream();
                libraryTask.uploadLibraryFile(library, libFileStream, taskDO);
            } catch (IOException e) {
                e.printStackTrace();
                return Result.Error(ResultCode.INSERT_ERROR);
            }
        }
        return Result.OK(library);
    }

    @GetMapping(value = "/clone")
    Result clone(
            @RequestParam(value = "id") String sourceLibId,
            @RequestParam(value = "newLibName") String newLibName,
            @RequestParam(value = "includeDecoy", defaultValue = "false") Boolean includeDecoy) {
        if (StringUtils.isEmpty(newLibName)) {
            return Result.Error(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        LibraryQuery libraryQuery = new LibraryQuery();
        libraryQuery.setName(newLibName);
        Result<List<LibraryDO>> list = libraryService.getList(libraryQuery);
        if (list.getData().size() != 0) {
            return Result.Error("已经有同名库存在");
        }
        LibraryDO library = libraryService.getById(sourceLibId);
        if (library == null) {
            return Result.Error(ResultCode.LIBRARY_NOT_EXISTED);
        }
        Result result = libraryService.clone(library, newLibName, includeDecoy);
        return result;
    }

    @GetMapping(value = "/statistic")
    Result statCount(@RequestParam(value = "libraryId") String libraryId) {
        LibraryDO library = libraryService.getById(libraryId);
        if (library == null) {
            return Result.Error(ResultCode.LIBRARY_NOT_EXISTED);
        }
        libraryService.statistic(library);
        return Result.OK(library);
    }

    @GetMapping(value = "/clearDecoys")
    Result clearDecoys(@RequestParam(value = "libraryId") String libraryId) {
        LibraryDO library = libraryService.getById(libraryId);
        if (library == null) {
            return Result.Error(ResultCode.LIBRARY_NOT_EXISTED);
        }
        return libraryService.clearDecoys(library);
    }

    @GetMapping(value = "/generateDecoys")
    Result generateDecoys(
            @RequestParam(value = "libraryId") String libraryId,
            @RequestParam(value = "generator") String generator) {
        LibraryDO library = libraryService.getById(libraryId);
        if (library == null) {
            return Result.Error(ResultCode.LIBRARY_NOT_EXISTED);
        }

        return libraryService.generateDecoys(library, generator);
    }

    @GetMapping(value = "/repeatCount")
    Result repeatCount(@RequestParam(value = "libraryId") String libraryId) {
        String s = repeatCount.repeatCount(libraryId);
        Result result = new Result();
        result.setData(s);
        return result;
    }

    @GetMapping(value = "/getProteins")
    Result getProteins(@RequestParam(value = "projectId") String projectId) {

        ProjectDO project = projectService.getById(projectId);
        LibraryDO library = libraryService.getById(project.getAnaLibId());
        return Result.OK(library.getProteins());
    }

    @GetMapping(value = "/getPeptide")
    Result getPeptide(
            @RequestParam(value = "projectId") String projectId,
            @RequestParam(value = "proteinName") String proteinName,
            @RequestParam(value = "range") double range) {
        ProjectDO project = projectService.getById(projectId);
        List<RunDO> runs = runService.getAllByProjectId(projectId);
        List<WindowRange> windowRanges = runs.get(0).getWindowRanges();
        Result<Map<String, List<Object>>> peptideLink =
                peptideService.getPeptideLink(project.getAnaLibId(), proteinName, range, windowRanges);
        peptideLink.setSuccess(true);
        return peptideLink;
    }
}
