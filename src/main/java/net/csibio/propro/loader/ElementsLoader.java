package net.csibio.propro.loader;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.algorithm.parser.model.chemistry.Element;
import net.csibio.propro.constants.constant.Constants;
import net.csibio.propro.utils.ElementUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class ElementsLoader implements Load {

    String elementsStr = "";
    List<Element> elementList = new ArrayList<>();
    HashMap<String, Element> symbolElementsMap = new HashMap<>();
    Double avgTotal = null;

    public static String H2O = "H:2,O:1";
    public static String H = "H:1";
    public static String OH = "H:1,O:1";
    public static String CHO = "C:1,H:1,O:1";
    public static String NH2 = "N:1,H:2";
    public static String CO = "C:1,O:1";

    @PostConstruct
    public void init() {
        try {

            InputStream stream = getClass().getClassLoader().getResourceAsStream("dbfile/elementsWithWeight.json");
            File file = new File("dbfile/elementsWithWeight.json");
            FileUtils.copyInputStreamToFile(stream, file);

            FileInputStream fis = new FileInputStream(file);
            int fileLength = fis.available();
            byte[] bytes = new byte[fileLength];
            fis.read(bytes);
            elementsStr = new String(bytes, 0, fileLength);
            elementList = JSONObject.parseArray(elementsStr, Element.class);
            for (Element element : elementList) {
                countWeight(element);
                symbolElementsMap.put(element.getSymbol(), element);
            }

            double averageWeightC = getElementBySymbol(Element.C).getAverageWeight();
            double averageWeightH = getElementBySymbol(Element.H).getAverageWeight();
            double averageWeightN = getElementBySymbol(Element.N).getAverageWeight();
            double averageWeightO = getElementBySymbol(Element.O).getAverageWeight();
            double averageWeightS = getElementBySymbol(Element.S).getAverageWeight();
            double averageWeightP = getElementBySymbol(Element.P).getAverageWeight();
            avgTotal = Constants.C * averageWeightC +
                    Constants.H * averageWeightH +
                    Constants.N * averageWeightN +
                    Constants.O * averageWeightO +
                    Constants.S * averageWeightS +
                    Constants.P * averageWeightP;
            log.info("Init Element Database file success!!!");
        } catch (IOException e) {
            log.error("Init Element Database file failed!!!");
            e.printStackTrace();
        }
    }

    public Element getElementBySymbol(String elementSymbol) {
        return getElementsMap().get(elementSymbol);
    }

    public HashMap<String, Element> getElementsMap() {
        return symbolElementsMap;
    }

    public List<Element> getElementList() {
        return elementList;
    }

    public String getJson() {
        return elementsStr;
    }

    //必须符合k:v,k:v的格式
    public double getMonoWeight(String formula) {
        HashMap<String, Integer> elementMap = ElementUtil.getElementMap(formula);
        if (elementMap == null) {
            return 0;
        }
        double monoWeight = 0;
        for (String key : elementMap.keySet()) {
            monoWeight += getElementBySymbol(key).getMonoWeight() * elementMap.get(key);
        }
        return monoWeight;
    }

    //必须符合k:v,k:v的格式
    public double getAverageWeight(String formula) {
        HashMap<String, Integer> elementMap = ElementUtil.getElementMap(formula);
        if (elementMap == null) {
            return 0;
        }
        double averageWeight = 0;
        for (String key : elementMap.keySet()) {
            averageWeight += getElementBySymbol(key).getAverageWeight() * elementMap.get(key);
        }
        return averageWeight;
    }

    public double getAvgTotal() {
        return avgTotal;
    }

    /**
     * 计算平均质量和Mono质量,根据OpenMS源代码中的逻辑,平均质量为"求和(分布率*相对原子质量)",Mono质量为取相对原子质量最小的一个
     *
     * @param element
     */
    private void countWeight(Element element) {
        double smallestWeight = Double.MAX_VALUE;
        Double biggestAbundance = null;
        double biggestAbundanceWeight = 0d;
        double averageWeight = 0;
        for (String iso : element.getIsotopes()) {
            String[] isoArray = iso.split(":");
            double abundance = Double.parseDouble(isoArray[0]);
            double weight = Double.parseDouble(isoArray[1]);

            if (smallestWeight > weight) {
                smallestWeight = weight;
            }

            if (biggestAbundance == null) {
                biggestAbundance = abundance;
                biggestAbundanceWeight = weight;
            } else if (abundance > biggestAbundance) {
                biggestAbundance = abundance;
                biggestAbundanceWeight = weight;
            }

            averageWeight += abundance * weight / 100;
        }

        element.setMaxAbundanceWeight(biggestAbundanceWeight);
        element.setMonoWeight(smallestWeight);
        element.setAverageWeight(averageWeight);

    }
}
