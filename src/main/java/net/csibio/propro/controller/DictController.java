package net.csibio.propro.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.domain.query.DictQuery;
import net.csibio.propro.domain.vo.DictItem;
import net.csibio.propro.domain.vo.DictUpdateVO;
import net.csibio.propro.service.DictService;
import net.csibio.propro.tools.aspect.annotation.Dict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Api(tags = {"Dict Module"})
@RestController
@RequestMapping("/dict")
@Slf4j
public class DictController {
    @Autowired
    DictService dictService;

    @GetMapping(value = "/list")
    Result list(DictQuery dictQuery) {
        Result<List<DictDO>> result = dictService.getList(dictQuery);
        return result;
    }

    @PostMapping(value = "/add")
    Result add(@RequestParam(value = "name") String name) {
        DictDO dictDO = new DictDO();
        dictDO.setName(name);
        Result result = dictService.insert(dictDO);
        if (result.isFailed()) {
            return result;
        }
        return Result.OK(dictDO);
    }

    @PostMapping(value = "/addItem")
    Result addItem(@RequestParam(value = "id") String id,
                   @RequestParam(value = "key") String key,
                   @RequestParam(value = "value") String value) {
        DictDO dictDO = dictService.getById(id);
        if(dictDO.getItem()!=null ){
            List<DictItem> items = dictDO.getItem();
            for(DictItem dictItem:items){
                if(dictItem.getKey().equals(key)){
                    return Result.Error("已经存在字典数据");
                }
            }
                DictItem newItem = new DictItem();
                newItem.setKey(key);
                newItem.setValue(value);
                items.add(newItem);
                dictDO.setItem(items);
                dictService.update(dictDO);
                return Result.OK();
        }else{
            DictItem dictItem = new DictItem();
            dictItem.setKey(key);
            dictItem.setValue(value);
            List<DictItem> list = new ArrayList<>();
            list.add(dictItem);
            dictDO.setItem(list);
            dictService.update(dictDO);
            return Result.OK();
        }
    }

    @PostMapping(value = "/update")
    Result update(
            @RequestParam(value = "id",required = true) String id,
            @RequestParam(value = "key",required = false) String key,
            @RequestParam(value = "value",required = false) String value) {
        DictDO dict = dictService.getById(id);
        List<DictItem> items = dict.getItem();
        for(DictItem dictItem:items){
        if(dictItem.getKey().equals(key)){
            dictItem.setValue(value);
        }
        }
        dict.setItem(items);
        return dictService.update(dict);
    }

    @PostMapping(value = "/removeItem")
    Result deleteItem(
            @RequestParam(value = "id",required = true) String id,
            @RequestParam(value = "key",required = false) String key
            ) {
        DictDO dict = dictService.getById(id);
        List<DictItem> items = dict.getItem();
        List<DictItem> newItems = new ArrayList<>();
        for(DictItem dictItem:items){
            if(!dictItem.getKey().equals(key)){
                newItems.add(dictItem);
            }
        }
        dict.setItem(newItems);
        return dictService.update(dict);
    }

    @PostMapping(value = "/remove")
    Result delete(
            @RequestParam(value = "id",required = true) String id
    ) {
        DictDO dict = dictService.getById(id);
       dictService.removeById(id);
        return Result.OK();
    }

    @GetMapping(value = "/getAll")
    Result get(){
        DictQuery dictQuery = new DictQuery();
        List<DictDO> dictAll = dictService.getAll(dictQuery);
        if(dictAll.size()==0){
            return Result.Error("没有此字典");
        }
        return Result.OK(dictAll);
    }



}
