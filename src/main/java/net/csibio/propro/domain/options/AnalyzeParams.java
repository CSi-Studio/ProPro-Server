package net.csibio.propro.domain.options;

import lombok.Data;
import net.csibio.propro.domain.bean.method.Method;
import net.csibio.propro.domain.db.MethodDO;
import net.csibio.propro.domain.db.OverviewDO;
import net.csibio.propro.domain.db.TaskDO;
import org.springframework.data.annotation.Transient;

@Data
public class AnalyzeParams {

    //不录入数据库中
    @Transient
    TaskDO taskDO;
    @Transient
    String overviewId; //分析概览ID
    @Transient
    String baseOverviewId;  //仅在reselect模式下存在,为reselect之前的分析概览对象id
    @Transient
    OverviewDO baseOverview; //仅在reselect模式下存在,为reselect之前的分析概览对象
    @Transient
    Boolean changeCharge = false;

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

    //重选峰步骤,默认为false,只有在进行重选峰时才会将本字段置为true
    Boolean reselect = false;

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
