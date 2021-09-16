package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.bean.peptide.PeptideV1;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.query.LibraryQuery;
import net.csibio.propro.domain.query.PeptideQuery;
import net.csibio.propro.domain.vo.PeptideUpdateVO;
import net.csibio.propro.service.LibraryService;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.SimulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Api(tags = {"Peptide Module"})
@RestController
@RequestMapping("/peptide")
@Slf4j
public class PeptideController extends XController<LibraryDO, LibraryQuery, LibraryService> {

    @Autowired
    PeptideService peptideService;
    @Autowired
    SimulateService simulateService;

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

        return peptideService.update(peptide);
    }

    @GetMapping(value = "/remove")
    Result remove(@RequestParam(value = "peptideId") String peptideId) {
        return peptideService.removeById(peptideId);
    }

    @GetMapping(value = "/getPeptideRefs")
    Result getPeptideRefs(@RequestParam(value = "libraryId") String libraryId,
                          @RequestParam(value = "protein") String protein) {
        List<PeptideV1> peptideList = peptideService.getAll(new PeptideQuery(libraryId).setProtein(protein), PeptideV1.class);
        return Result.OK(peptideList);
    }

    @GetMapping(value = "/predict")
    Result predict(@RequestParam(value = "peptideId") String peptideId,
                   @RequestParam(value = "spModel", defaultValue = "CID") String spModel,
                   @RequestParam(value = "isotope", required = false, defaultValue = "false") boolean iso,
                   @RequestParam(value = "limit", required = false, defaultValue = "6") int limit) {
        PeptideDO peptide = peptideService.getById(peptideId);
        if (peptide == null) {
            return Result.Error(ResultCode.OBJECT_NOT_EXISTED);
        }
        if (peptide.getCharge() != 2) {
            return Result.Error(ResultCode.PREDICT_MODEL_ONLY_FOR_TWO_CHARGE_PEPTIDE);
        }
        if (!Objects.equals(spModel, "CID") && !Objects.equals(spModel, "HCD")) {
            return Result.Error(ResultCode.UNSUPPORTED_FRAGMENTATION_MODEL);
        }
        List<FragmentInfo> fragmentInfos = simulateService.predictFragment(peptide.getSequence(), spModel, iso, limit);
        return Result.OK(fragmentInfos);
    }

    @RequestMapping(value = "/updateFragment", method = RequestMethod.POST)
    Result<PeptideDO> updateFragment(@RequestParam(value = "peptideId") String peptideId,
                                     @RequestBody FragmentInfo[] fragments) {
        PeptideDO peptide = peptideService.getById(peptideId);
        if (peptide == null) {
            return Result.Error(ResultCode.OBJECT_NOT_EXISTED);
        }
        for (FragmentInfo fragment : fragments) {
            peptide.getFragments().add(fragment);
        }
        return peptideService.update(peptide);
    }
}
