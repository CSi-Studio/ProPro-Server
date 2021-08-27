package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.domain.vo.PeptideUpdateVO;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = {"Peptide Module"})
@RestController
@RequestMapping("/peptide")
@Slf4j
public class PeptideController extends XController<LibraryDO, LibraryQuery, LibraryService> {

    @Autowired
    PeptideService peptideService;

    @GetMapping(value = "/list")
    Result list(PeptideQuery query) {
        Result<List<PeptideDO>> result = peptideService.getList(query);
        return result;
    }

    @PostMapping(value = "/update")
    Result<PeptideDO> update(PeptideUpdateVO peptideUpdateVO) {
        PeptideDO peptide = peptideService.getById(peptideUpdateVO.getId());
        if (peptide == null) {
            return Result.Error(ResultCode.OBJECT_NOT_EXISTED);
        }
        peptide.setMz(peptideUpdateVO.getMz());
        peptide.setRt(peptideUpdateVO.getRt());
        peptide.setProteins(peptideUpdateVO.getProteins());
        peptide.setIsUnique(peptideUpdateVO.getIsUnique());

        return peptideService.update(peptide);
    }

    @GetMapping(value = "/remove")
    Result remove(@RequestParam(value = "peptideId") String peptideId) {
        return peptideService.removeById(peptideId);
    }
}
