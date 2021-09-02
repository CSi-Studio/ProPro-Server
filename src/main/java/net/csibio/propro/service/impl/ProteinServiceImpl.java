package net.csibio.propro.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.formula.FormulaCalculator;
import net.csibio.propro.algorithm.parser.FastaParser;
import net.csibio.propro.constants.constant.ResidueType;
import net.csibio.propro.constants.constant.SymbolConst;
import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.dao.BaseDAO;
import net.csibio.propro.dao.ProteinDAO;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.bean.peptide.FragmentInfo;
import net.csibio.propro.domain.db.PeptideDO;
import net.csibio.propro.domain.db.ProteinDO;
import net.csibio.propro.domain.query.ProteinQuery;
import net.csibio.propro.exceptions.XException;
import net.csibio.propro.service.PeptideService;
import net.csibio.propro.service.ProteinService;
import net.csibio.propro.service.SimulateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service("ProteinService")
public class ProteinServiceImpl implements ProteinService {

    @Autowired
    ProteinDAO proteinDAO;
    @Autowired
    FastaParser fastaParser;
    @Autowired
    SimulateService simulateService;
    @Autowired
    FormulaCalculator formulaCalculator;
    @Autowired
    PeptideService peptideService;

    @Override
    public BaseDAO<ProteinDO, ProteinQuery> getBaseDAO() {
        return proteinDAO;
    }

    @Override
    public void beforeInsert(ProteinDO proteinDO) throws XException {
        if (proteinDO.getIdentifier() == null) {
            throw new XException(ResultCode.PROTEIN_IDENTIFIER_CANNOT_BE_NULL);
        }
    }

    @Override
    public void beforeUpdate(ProteinDO proteinDO) throws XException {
        if (proteinDO.getId() == null) {
            throw new XException(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (proteinDO.getIdentifier() == null) {
            throw new XException(ResultCode.PROTEIN_IDENTIFIER_CANNOT_BE_NULL);
        }
    }

    @Override
    public void beforeRemove(String id) throws XException {
        //Do Nothing
    }

    @Override
    public List<ProteinDO> importFromLocalFasta(InputStream inputStream, String fileName, boolean review) {
        Result<HashMap<String, String>> data = fastaParser.parseAll(inputStream);
        HashMap<String, String> map = data.getData();
        List<ProteinDO> proteinDOList = new ArrayList<>();
        for (String key : map.keySet()) {
            ProteinDO proteinDO = new ProteinDO();
            StringBuilder keyBuilder = new StringBuilder(key);
            keyBuilder.delete(0, 1);
            String firstLine = keyBuilder.toString();
            String newString = keyBuilder.toString();
            String[] s = firstLine.split(SymbolConst.SPACE);
            String newS = StringUtils.substringAfter(firstLine, s[0]);
            String name = StringUtils.substringBefore(newS, "OS=");
            String identifier = StringUtils.substringBefore(firstLine, "OS=");
            proteinDO.setIdentifier(identifier);
            String os = StringUtils.substringBetween(firstLine, "OS=", "OX=");
            String gn = StringUtils.substringBetween(newString, "GN=", "PE=");
            proteinDO.setGene(gn);
            proteinDO.setOrganism(os);
            proteinDO.setIdentifier(s[0]);
            proteinDO.setReviewed(review);
            String substringName = name.substring(1, name.length() - 1);
            List<String> nameList = new ArrayList<>();
            nameList.add(substringName);
            proteinDO.setNames(nameList);
            proteinDO.setSequence(map.get(key));
            proteinDO.setCreateTag(fileName);
            proteinDOList.add(proteinDO);
        }
        insert(proteinDOList);

        return proteinDOList;
    }

    @Override
    public Result<List<ProteinDO>> importFromFasta(InputStream inputStream, String fileName, boolean review, int min, int max) {
        HashMap<String, String> map = fastaParser.parseAllWithInput(inputStream, min, max).getData();

        List<ProteinDO> proteinDOList = new ArrayList<>();
        for (String key : map.keySet()) {
            ProteinDO proteinDO = new ProteinDO();
            StringBuilder keyBuilder = new StringBuilder(key);
            keyBuilder.delete(0, 1);
            String firstLine = keyBuilder.toString();
            String newString = keyBuilder.toString();
            String[] s = firstLine.split(" ");
            String newS = StringUtils.substringAfter(firstLine, s[0]);
            String name = StringUtils.substringBefore(newS, "OS=");
            String identifier = StringUtils.substringBefore(firstLine, "OS=");
            proteinDO.setIdentifier(identifier);
            String os = StringUtils.substringBetween(firstLine, "OS=", "OX=");
            String gn = StringUtils.substringBetween(newString, "GN=", "PE=");
            proteinDO.setGene(gn);
            proteinDO.setOrganism(os);
            proteinDO.setIdentifier(s[0]);
            proteinDO.setReviewed(review);
            String substringName = name.substring(1, name.length() - 1);
            List<String> nameList = new ArrayList<>();
            nameList.add(substringName);
            proteinDO.setNames(nameList);
            proteinDO.setSequence(map.get(key));
            proteinDO.setCreateTag(fileName);
            proteinDOList.add(proteinDO);
        }
        ProteinQuery proteinQuery = new ProteinQuery();
        List<ProteinDO> removeList = new ArrayList<>();
        for (ProteinDO proteinDO : proteinDOList) {
            proteinQuery.setIdentifier(proteinDO.getIdentifier());
            List<ProteinDO> all = getAll(proteinQuery);
            if (all.size() != 0) {
                removeList.add(proteinDO);
            }
        }
        proteinDOList.removeAll(removeList);
        return insert(proteinDOList);
//        return Result.OK(proteinDOList);
    }

    @Override
    public void proteinToPeptide(String libraryId, List<ProteinDO> proteinList, int min, int max, String spModel, Boolean isotope) {
        List<PeptideDO> peptideList = new ArrayList<>();
        HashSet<String> set = new HashSet<String>();
        List<String> pList = new ArrayList<>();
        Map<String, String> pPMap = new HashMap<>();
        for (ProteinDO protein : proteinList) {
            String sequence = protein.getSequence();
            HashSet<String> enzymeResult = fastaParser.getEnzymeResult(sequence, min, max);
            Iterator<String> iterator = enzymeResult.iterator();
            while (iterator.hasNext()) {
                String peptide = iterator.next();
                set.add(peptide);
                pList.add(peptide);
                if (pPMap.containsKey(peptide)) {
                    continue;
                } else {
                    pPMap.put(peptide, protein.getIdentifier());
                }
            }
        }
        log.info("蛋白数目:" + proteinList.size());
        log.info("总计肽段数目:" + pList.size());
        long startX = System.currentTimeMillis();
        Set<String> uniquePep = new HashSet<>();
        Set<String> ignorePep = new HashSet<>();
        for (int i = 0; i < pList.size(); i++) {
            String p = pList.get(i);
            if (uniquePep.contains(p)) {
                uniquePep.remove(p);
                ignorePep.add(p);
            } else if (!ignorePep.contains(p)) {
                uniquePep.add(p);
            }
        }
        uniquePep.forEach(item -> {
            for (int i = 1; i < 4; i++) {
                PeptideDO peptide = new PeptideDO();
                peptide.setSequence(item);
                peptide.setLibraryId(libraryId);
                double monoMz = formulaCalculator.getMonoMz(item, ResidueType.Full, i, 0, 0, false, null);
                peptide.setMz(monoMz);
                peptide.setPeptideRef(peptide + "_" + i);
                peptide.setCharge(i);
                List<FragmentInfo> fragmentInfos = simulateService.singlePredictFragment(peptide, spModel, isotope);
                peptide.setFragments(fragmentInfos);
                Set<String> proteinSet = new HashSet<>();
                proteinSet.add(pPMap.get(item));
                peptide.setProteins(proteinSet);
                peptideList.add(peptide);
            }
        });
        log.info(uniquePep.size() + "个唯一肽段");
        System.out.println("Time Cost:" + (System.currentTimeMillis() - startX));
        Result<List<PeptideDO>> insert = peptideService.insert(peptideList);
    }
}
