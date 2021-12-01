package net.csibio.propro.controller;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.query.PageQuery;
import net.csibio.propro.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class XController<T, Q extends PageQuery, S extends BaseService<T, Q>> {

    @Autowired
    S service;

    /**
     * 导出excel
     */
    protected Result exportXls(Q q, T object, Class<T> clazz, String title) {

        Result<List<T>> result = service.getList(q);

        //TODO 李然 导出报表的相关代码
        return Result.OK();
    }


}
