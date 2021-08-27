package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.decoy.generator.NicoGenerator;
import net.csibio.propro.algorithm.decoy.generator.ShuffleGenerator;
import net.csibio.propro.algorithm.parser.*;
import net.csibio.propro.algorithm.stat.LibraryStat;
import net.csibio.propro.algorithm.stat.StatConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.LibraryDAO;
import net.csibio.propro.dao.PeptideDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.common.IdName;
import net.csibio.propro.domain.bean.peptide.PeptideS1;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.TaskService;
import net.csibio.propro.utils.FileUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service("libraryService")
public class LibraryServiceImpl implements LibraryService {

    int errorListNumberLimit = 10;

    @Autowired
    LibraryDAO libraryDAO;
    @Autowired
    LibraryStat libraryStat;
    @Autowired
    PeptideService peptideService;
    @Autowired
    PeptideDAO peptideDAO;
    @Autowired
    LibraryTsvParser tsvParser;
    @Autowired
    TraMLParser traMLParser;
    @Autowired
    FastTraMLParser fastTraMLParser;
    @Autowired
    MsmsParser msmsParser;
    @Autowired
    TaskService taskService;
    @Autowired
    FastaParser fastaParser;
    @Autowired
    NicoGenerator nicoGenerator;
    @Autowired
    ShuffleGenerator shuffleGenerator;

    @Override
    public BaseDAO<LibraryDO, LibraryQuery> getBaseDAO() {
        return libraryDAO;
    }

    @Override
    public void beforeInsert(LibraryDO libraryDO) throws XException {
        if (libraryDO.getName() == null) {
            throw new XException(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        libraryDO.setCreateDate(new Date());
        libraryDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeUpdate(LibraryDO libraryDO) throws XException {
        if (libraryDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (libraryDO.getName() == null) {
            throw new XException(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        libraryDO.setLastModifiedDate(new Date());
    }

    @Override
    public void beforeRemove(String id) throws XException {
        try {
            peptideService.removeAllByLibraryId(id);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Result<LibraryDO> clone(LibraryDO oriLib, String newLibName, Boolean includeDecoy) {
        //TODO 李然 根据一个旧库,一个新库名和是否克隆伪肽段 克隆一个新库,包含库内所有的肽段信息
        List<PeptideDO> allByLibraryId = peptideService.getAllByLibraryId(oriLib.getId());
        LibraryDO libraryDO = new LibraryDO();
        libraryDO.setDescription(oriLib.getDescription());
        libraryDO.setGenerator(oriLib.getGenerator());
        libraryDO.setName(newLibName);
        libraryDO.setFilePath(oriLib.getFilePath());
        libraryDO.setOrganism(oriLib.getOrganism());
        libraryDO.setType(oriLib.getType());
        libraryDO.setStatistic(oriLib.getStatistic());
        insert(libraryDO);
        List<PeptideDO> peptideDOList = new ArrayList<>();
        if (includeDecoy) {
            for (PeptideDO peptideDO : allByLibraryId) {
                PeptideDO newPeptide = new PeptideDO();
                BeanUtils.copyProperties(peptideDO, newPeptide);
                peptideDOList.add(newPeptide);
            }
        } else {
            for (PeptideDO peptideDO : allByLibraryId) {
                PeptideDO newPeptide = new PeptideDO();
                BeanUtils.copyProperties(peptideDO, newPeptide);
                newPeptide.clearDecoy();
                peptideDOList.add(newPeptide);
            }
        }
        peptideService.insert(peptideDOList);
        log.info("库克隆完毕");
        return Result.OK();
    }

    @Override
    public void uploadFile(LibraryDO library, InputStream libFileStream, TaskDO taskDO) {
        //先Parse文件,再作数据库的操作
        Result result = parseAndInsert(library, libFileStream, taskDO);
        if (result.getErrorList() != null) {
            if (result.getErrorList().size() > errorListNumberLimit) {
                taskDO.addLog("解析错误,错误的条数过多,这边只显示" + errorListNumberLimit + "条错误信息");
                taskDO.addLog(result.getErrorList().subList(0, errorListNumberLimit));
            } else {
                taskDO.addLog(result.getErrorList());
            }
        }

        if (result.isFailed()) {
            taskDO.addLog(result.getErrorMessage());
            taskDO.finish(TaskStatus.FAILED.getName());
        }

        /**
         * 如果全部存储成功,开始统计蛋白质数目,肽段数目和Transition数目
         */
        taskDO.addLog("开始统计蛋白质数目,肽段数目和Transition数目");
        taskService.update(taskDO);


        statistic(library);
        taskDO.finish(TaskStatus.SUCCESS.getName(), "统计完毕");
        taskService.update(taskDO);

    }

    @Override
    public Result parseAndInsert(LibraryDO library, InputStream libFileStream, TaskDO taskDO) {

        Result result = null;

        String filePath = library.getFilePath();
        if (filePath.toLowerCase().endsWith("tsv") || filePath.toLowerCase().endsWith("csv")) {
            result = tsvParser.parseAndInsert(libFileStream, library, taskDO);
        } else if (filePath.toLowerCase().endsWith("traml")) {
            result = traMLParser.parseAndInsert(libFileStream, library, taskDO);
        } else if (filePath.toLowerCase().endsWith("txt")) {
            result = msmsParser.parseAndInsert(libFileStream, library, taskDO);
        } else {
            return Result.Error(ResultCode.INPUT_FILE_TYPE_MUST_BE_TSV_OR_TRAML);
        }

        FileUtil.close(libFileStream);
        library.setGenerator(ShuffleGenerator.NAME);
        return result;
    }

    @Override
    public void statistic(LibraryDO library) {
        Map<String, Object> statistic = new HashMap<>();
        List<PeptideS1> peptideList = peptideService.getAll(new PeptideQuery(library.getId()), PeptideS1.class);
        statistic.put(StatConst.Protein_Count, libraryStat.proteinCount(library));
        statistic.put(StatConst.Peptide_Count, libraryStat.peptideCount(library));
        statistic.put(StatConst.Fragment_Count, libraryStat.fragmentCount(peptideList));
        statistic.put(StatConst.Peptide_Dist_On_Mz_5, libraryStat.mzDistList(peptideList, 20));
        statistic.put(StatConst.Peptide_Dist_On_RT_5, libraryStat.rtDistList(peptideList, 20));
        library.setStatistic(statistic);
        libraryDAO.update(library);
        log.info("统计完成");
    }

    @Override
    public Result clearDecoys(LibraryDO library) {
        List<PeptideDO> allByLibraryId = peptideService.getAllByLibraryId(library.getId());
        for (PeptideDO peptideDO : allByLibraryId) {
            peptideDO.clearDecoy();
            peptideService.update(peptideDO);
        }

        log.info("伪肽段清除完毕");
        return Result.OK("成功清除");
    }

    @Override
    public Result generateDecoys(LibraryDO library, String generator) {
        List<PeptideDO> peptideList = peptideService.getAllByLibraryId(library.getId());
        switch (generator) {
            case NicoGenerator.NAME:
                nicoGenerator.generate(peptideList);
                break;
            case ShuffleGenerator.NAME:
                shuffleGenerator.generate(peptideList);
                break;
            default:
                generator = ShuffleGenerator.NAME;
                shuffleGenerator.generate(peptideList);
        }
        peptideService.updateDecoyInfos(peptideList);
        library.setGenerator(generator);
        update(library);
        return Result.OK();
    }

    @Cacheable(cacheNames = "libraryGetId", key = "#id")
    @Override
    public LibraryDO getById(String id) {
        try {
            log.info("执行getById方法");
            return getBaseDAO().getById(id);
        } catch (Exception e) {
            return null;
        }
    }

    @CacheEvict(cacheNames = "libraryGetId", key = "#libraryDO.id")
    @Override
    public Result<LibraryDO> update(LibraryDO libraryDO) {
        try {
            beforeUpdate(libraryDO);
            getBaseDAO().update(libraryDO);
            return Result.OK(libraryDO);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    @CacheEvict(allEntries = true)
    @Override
    public Result<List<LibraryDO>> update(List<LibraryDO> libraryDOS) {
        try {
            for (LibraryDO t : libraryDOS) {
                beforeUpdate(t);
            }
            getBaseDAO().update(libraryDOS);
            return Result.OK(libraryDOS);
        } catch (XException xe) {
            return Result.Error(xe.getResultCode());
        } catch (Exception e) {
            return Result.Error(ResultCode.UPDATE_ERROR);
        }
    }

    //    @Cacheable(cacheNames = "LibraryGetAll", key = "#libraryQuery.getName()")


    @Override
    public List<LibraryDO> getAll(LibraryQuery libraryQuery) {
        log.info("执行getById方法");
        return getBaseDAO().getAll(libraryQuery);
    }

    @CacheEvict(cacheNames = "libraryGetId", key = "#id")
    @Override
    public Result removeById(String id) {
        if (id == null || id.isEmpty()) {
            return Result.Error(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            beforeRemove(id);
            getBaseDAO().removeById(id);
            return Result.OK();
        } catch (Exception e) {
            return Result.Error(ResultCode.DELETE_ERROR);
        }
    }

    private void simulateSlowService() {
        try {
            long time = 3000L;
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Result remove(LibraryQuery query) {
        List<IdName> idNameList = getAll(query, IdName.class);
        List<String> errorList = new ArrayList<>();
        idNameList.forEach(idName -> {
            Result res = removeById(idName.id());
            if (res.isFailed()) {
                errorList.add("Remove Library Failed:" + res.getErrorMessage());
            }
        });
        if (errorList.size() > 0) {
            return Result.Error(ResultCode.DELETE_ERROR, errorList);
        }
        return Result.OK();
    }
}
