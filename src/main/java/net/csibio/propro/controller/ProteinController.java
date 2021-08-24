package net.csibio.propro.controller;


import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.ProteinDO;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.query.ProteinQuery;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.ProteinService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Api(tags = {"Protein Module"})
@RestController
@RequestMapping("/protein")
@Slf4j

public class ProteinController {

    @Autowired
    ProteinService proteinService;
    @Autowired
    LibraryService libraryService;

    @GetMapping(value = "/list")
    Result<List<ProteinDO>> list(ProteinQuery query) {
        return proteinService.getList(query);
    }

    @PostMapping(value = "/add", headers = "content-type=multipart/form-data")
    Result add(@RequestParam(value = "createTag", required = false) String createTag,
               @RequestParam(value = "reviewed", required = true) Boolean reviewed,
               @RequestParam(value = "createLibrary", required = true) Boolean createLibrary,
               @RequestParam(value = "libraryName", required = false) String libraryName,
               @RequestParam(value = "libFile", required = true) MultipartFile libFile,
               @RequestParam(value = "spModel", required = false) String spModel,
               @RequestParam(value = "isotope", required = false) Boolean isotope,
               @RequestParam(value = "minPepLen", required = false) int minPepLen,
               @RequestParam(value = "maxPepLen", required = false) int maxPepLen) throws IOException {
        LibraryQuery libraryQuery = new LibraryQuery();
        libraryQuery.setName(libraryName);
        if (libraryService.getAll(libraryQuery).size() != 0) {
            return Result.Error("已存在該庫");
        }
        ProteinDO proteinDO = new ProteinDO();
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String format = dateFormat.format(date);
        if (StringUtils.isEmpty(createTag)) {
            createTag = libFile.getOriginalFilename() + SymbolConst.DELIMITER + format;
        }
        proteinDO.setCreateTag(createTag);
        InputStream inputStream = libFile.getInputStream();
        Result<List<ProteinDO>> result = proteinService.importFromFasta(inputStream, libFile.getOriginalFilename(), reviewed, minPepLen, maxPepLen);
        if (result.isFailed()) {
            return result;
        }
        if (createLibrary) {
            if (StringUtils.isEmpty(libraryName)) {
                return Result.Error(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
            }
            LibraryDO libraryDO = new LibraryDO();
            libraryDO.setName(libraryName);
            libraryDO.setFilePath(libFile.getOriginalFilename());
            libraryService.insert(libraryDO);
            List<ProteinDO> data = result.getData();
            proteinService.proteinToPeptide(libraryDO.getId(), data, minPepLen, maxPepLen, spModel, isotope);
        }
        return Result.OK();
    }
}
