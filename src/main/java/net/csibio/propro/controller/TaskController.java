package net.csibio.propro.controller;

import net.csibio.propro.constants.enums.ResultCode;
import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.TaskQuery;
import net.csibio.propro.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** Created by Nico Wang Time: 2019-12-03 14:26 */
@RestController
@RequestMapping("/api/task/")
public class TaskController {

  @Autowired TaskService taskService;

  @PostMapping(value = "/remove")
  Result remove(@RequestParam(value = "idList", required = true) List<String> idList) {
    Result result = new Result();
    List<String> errorList = new ArrayList<>();
    List<String> deletedIds = new ArrayList<>();
    for (String taskId : idList) {
      Result taskResult = taskService.removeById(taskId);
      if (taskResult.isSuccess()) {
        deletedIds.add(taskId);
      } else {
        errorList.add("TaskId:" + taskId + "--" + taskResult.getErrorMessage());
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

  @GetMapping("/list")
  Result<List<TaskDO>> list(TaskQuery taskQuery) {
    taskQuery.setSortColumn("createDate");
    taskQuery.setOrderBy(Sort.Direction.DESC);
    Result<List<TaskDO>> taskList = taskService.getList(taskQuery);
    return taskList;
  }

  @GetMapping(value = "/detail")
  Result<TaskDO> detail(@RequestParam(value = "id") String id) {
    TaskDO task = taskService.getById(id);
    if (task == null) {
      return Result.Error(ResultCode.TASK_NOT_EXISTED);
    }

    return Result.OK(task);
  }
}
