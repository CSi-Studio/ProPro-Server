package net.csibio.propro.service;

import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.ProjectDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.ProjectQuery;

import java.io.File;
import java.util.List;

public interface ProjectService extends BaseService<ProjectDO, ProjectQuery> {

    /**
     * 扫描仓库下所有的未被录入数据库的项目名称
     *
     * @return
     */
    List<String> listUnloadProjects();

    /**
     * 扫描某一个项目下所有的实验文件列表
     *
     * @param projectName
     * @return
     */
    List<File> scanExpFilesByProjectName(String projectName);

    /**
     * 扫描某一个指定项目下的所有Aird文件入库
     *
     * @param project
     */
    Result<TaskDO> scan(ProjectDO project);

    /**
     * 扫描仓库下所有的项目,如果某一个文件夹对应的项目名称不存在,则自动创建项目并且进行Aird文件扫描
     */
    void scanAll();


}
