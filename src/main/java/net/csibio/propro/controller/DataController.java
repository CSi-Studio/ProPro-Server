package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DataSumDO;
import net.csibio.propro.domain.query.DataSumQuery;
import net.csibio.propro.service.DataService;
import net.csibio.propro.service.DataSumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"Data Module"})
@RestController
@RequestMapping("/data")
@Slf4j
public class DataController {

    @Autowired
    DataService dataService;
    @Autowired
    DataSumService dataSumService;

    @GetMapping(value = "/list")
    Result list(DataSumQuery query, @RequestParam("projectId") String projectId) {
        Result<List<DataSumDO>> result = dataSumService.getList(query, projectId);
        return result;
    }
}
