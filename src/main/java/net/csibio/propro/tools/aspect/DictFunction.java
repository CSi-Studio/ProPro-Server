package net.csibio.propro.tools.aspect;

import net.csibio.propro.annotation.Section;
import net.csibio.propro.domain.db.DictDO;
import net.csibio.propro.domain.vo.DictItem;
import net.csibio.propro.service.DictService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("dictFunction")
public class DictFunction {
    @Autowired
    public static DictService dictService;
    public   static List<DictDO> dictTableInsert(Map<String, Section> map) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        List<DictDO> dictDOList = new ArrayList<>();
        for (String key : map.keySet()) {
            Class aClass = Class.forName(key);
            //Enum格式化
            List<DictItem> list = new ArrayList<>();
            Object[] enumConstants = aClass.getEnumConstants();
            for (int i = 0; i < enumConstants.length; i++) {
                Method[] methods = enumConstants[i].getClass().getMethods();
                Method getDesc = enumConstants[i].getClass().getMethod("get" + map.get(key).value());
                Method getName = enumConstants[i].getClass().getMethod("get" + map.get(key).key());
                Object[] o = enumConstants[i].getClass().getEnumConstants();
                String desc = getDesc.invoke(o[i]).toString();
                String name = getName.invoke(o[i]).toString();
                DictItem dictItem = new DictItem();
                dictItem.setKey(name);
                dictItem.setValue(desc);
                list.add(dictItem);
            }
            DictDO dictDO = new DictDO();
            String baseName = FilenameUtils.getName(key);
            int i = baseName.lastIndexOf(".");
            String substring = baseName.substring(i + 1, baseName.length());
            dictDO.setName(substring);
            dictDO.setItem(list);
            dictDO.setVersion(map.get(key).Version());
            dictDOList.add(dictDO);
//            }else if(map.get(key).type().equals("class")){
//                List<DictItem> list = new ArrayList<>();
//                Class newClass = Class.forName(key);
//                Field[] declaredFields = newClass.getDeclaredFields();
//                for(Field field:declaredFields){
//                    String name = field.getName();
//                    Object o1 = field.get(name);
//                    System.out.println(o1);
//                    DictItem dictItem = new DictItem();
//                    dictItem.setKey(name);
//                    dictItem.setValue(o1.toString());
//                    list.add(dictItem);
//                }
//                DictDO dictDO = new DictDO();
//                String baseName = FilenameUtils.getName(key);
//                int i = baseName.lastIndexOf(".");
//                String substring = baseName.substring(i+1, baseName.length());
//                dictDO.setName(substring);
//                dictDO.setItem(list);
//                dictDOList.add(dictDO);
//            }
        }
            return dictDOList;
    }
}
