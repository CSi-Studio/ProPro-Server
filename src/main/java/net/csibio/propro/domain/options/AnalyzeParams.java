package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.domain.bean.method.Method;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.db.TaskDO;

@Data
public class AnalyzeParams {

    TaskDO taskDO;

    String overviewId; //分析概览ID

    /**
     * 是否强制执行Irt,如果是那么即便exp自带了irt结果也会强制重新计算irt并且把新计算的结果赋值给exp
     */
    Boolean forceIrt = false;

    /**
     * 方法包快照
     */
    Method method;

    /**
     * 分析使用的内标库
     */
    String insLibId;

    /**
     * 用于打印日志用
     */
    String insLibName;

    /**
     * 分析使用的标准库
     */
    String anaLibId;

    /**
     * 用于打印日志用
     */
    String anaLibName;

    //上下文备忘录
    String note;

    //用于PRM, <precursor mz, [rt start, rt end]>
//    HashMap<Float, Float[]> rtRangeMap;

    //    HashMap<String, Object> resultMap = new HashMap<>();

    public AnalyzeParams() {
        
    }

    public AnalyzeParams(MethodDO method) {
        this.method = method.toMethod();
    }

    public String getIrtLibraryId() {
        if (method.getIrt().isUseAnaLibForIrt()) {
            return anaLibId;
        } else {
            return insLibId;
        }
    }
}
