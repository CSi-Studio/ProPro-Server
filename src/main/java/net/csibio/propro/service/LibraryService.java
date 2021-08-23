package net.csibio.propro.service;

import net.csibio.propro.domain.Result;
import net.csibio.propro.domain.db.LibraryDO;
import net.csibio.propro.domain.db.TaskDO;
import net.csibio.propro.domain.query.LibraryQuery;

import java.io.InputStream;

public interface LibraryService extends BaseService<LibraryDO, LibraryQuery> {

    /**
     * Clone a library from an existed library
     *
     * @param oriLib       existed library
     * @param newLibName   new library name
     * @param includeDecoy if including all the decoy info from existed library
     * @return the new library object
     */
    Result<LibraryDO> clone(LibraryDO oriLib, String newLibName, Boolean includeDecoy);

    /**
     * 库文件上传,包括parseAndInsert步骤和统计步骤
     *
     * @param library
     * @param libFileStream
     * @param taskDO
     */
    void uploadFile(LibraryDO library, InputStream libFileStream, TaskDO taskDO);

    /**
     * 解析库文件并且将文件中相关信息插入数据库
     *
     * @param library
     * @param in
     * @param taskDO
     * @return
     */
    Result parseAndInsert(LibraryDO library, InputStream in, TaskDO taskDO);

    /**
     * 对某一个库进行数据统计
     *
     * @param library
     */
    void statistic(LibraryDO library);

    /**
     * 清除库下所有伪肽段信息
     *
     * @param library
     * @return
     */
    Result clearDecoys(LibraryDO library);

    /**
     * 为库下所有的肽段生成对应的伪肽段,同时清除已经生成的伪肽段
     *
     * @param library   需要重新生成伪肽段的库
     * @param generator 伪肽段生成算法
     * @return
     */
    Result generateDecoys(LibraryDO library, String generator);
}
