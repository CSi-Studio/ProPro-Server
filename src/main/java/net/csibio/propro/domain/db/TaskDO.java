package net.csibio.propro.domain.db;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.constants.enums.TaskStatus;
import net.csibio.propro.constants.enums.TaskTemplate;
import net.csibio.propro.domain.BaseDO;
import net.csibio.propro.domain.bean.task.TaskLog;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:34
 */
@Data
@Slf4j
@Document(collection = "task")
public class TaskDO extends BaseDO {

    @Id
    String id;

    @Indexed
    List<String> runIds;

    @Indexed
    String status;

    String name;

    String taskTemplate;

    @Indexed
    Date createDate;

    Date lastModifiedDate;

    List<TaskLog> logs;

    Long totalCost;

    String features;

    public TaskDO() {
    }

    public TaskDO(TaskTemplate taskTemplate, String taskSuffixName) {
        this.taskTemplate = taskTemplate.getName();
        this.status = TaskStatus.WAITING.getName();
        this.name = taskTemplate.getName() + "-" + taskSuffixName;
    }

    public void addBindingRun(String runId) {
        if (runIds == null) {
            runIds = new ArrayList<>();
        }
        runIds.add(runId);
    }

    public TaskDO addLog(String content) {
        if (logs == null) {
            if (status == null || taskTemplate == null) {
                this.taskTemplate = TaskTemplate.DEFAULT.getName();
                this.status = TaskStatus.WAITING.getName();
                this.name = TaskTemplate.DEFAULT.getName() + "-DEFAULT";
            }
            logs = new ArrayList<>();
            logs.add(new TaskLog("Task Started"));
        }
        TaskLog taskLog = new TaskLog(content);
        log.info(content);
        logs.add(taskLog);

        return this;
    }

    public TaskDO addLog(List<String> contents) {
        if (logs == null) {
            logs = new ArrayList<>();
            logs.add(new TaskLog("Task Started"));
        }
        for (String content : contents) {
            TaskLog taskLog = new TaskLog(content);
            log.info(content);
            logs.add(taskLog);
        }

        return this;
    }

    public TaskDO start() {
        if (logs == null || logs.size() == 0) {
            logs = new ArrayList<>();
            logs.add(new TaskLog("Task Started"));
        }

        return this;
    }

    public Long getStartTime() {
        if (logs == null || logs.size() == 0) {
            return null;
        }
        TaskLog taskLog = logs.get(0);
        return taskLog.getTime().getTime();
    }

    public TaskDO finish(String status) {
        addLog("Task Ended");
        this.status = status;
        totalCost = System.currentTimeMillis() - getStartTime();
        return this;
    }

    public TaskDO finish(String status, String finishLog) {
        addLog(finishLog);
        addLog("Task Ended");
        this.status = status;
        totalCost = System.currentTimeMillis() - getStartTime();

        return this;
    }
}
