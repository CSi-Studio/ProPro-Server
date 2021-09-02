package net.csibio.propro.utils;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.db.DataDO;

import java.util.HashMap;

@Slf4j
public class DataUtil {

    public static void clearOrigin(DataDO data) {
        data.setRtArray(null);
        data.setIntensityMap(null);
        data.setCutInfoMap(null);
    }

    public static void clearCompressed(DataDO data) {
        data.setRtsBytes(null);
        data.setIntMapBytes(null);
        data.setCutInfosFeature(null);
    }

    public static void compress(DataDO data) {
        data.setRtsBytes(CompressUtil.compressedToBytes(data.getRtArray()));
        HashMap<String, byte[]> intMap = new HashMap<>();
        data.getIntensityMap().forEach((key, value) -> {
            intMap.put(key, CompressUtil.compressedToBytes(value));
        });
        data.setIntMapBytes(intMap);
        data.setCutInfosFeature(FeatureUtil.toString(data.getCutInfoMap()));
        clearOrigin(data);
    }

    public static void decompress(DataDO data) {
        data.setRtArray(CompressUtil.transToFloat(data.getRtsBytes()));
        HashMap<String, float[]> intensityMap = new HashMap<>();
        data.getIntMapBytes().forEach((key, value) -> {
            intensityMap.put(key, CompressUtil.transToFloat(value));
        });
        data.setIntensityMap(intensityMap);
        data.setCutInfoMap(FeatureUtil.toFloatMap(data.getCutInfosFeature()));
        clearCompressed(data);
    }


    public static String getDataRef(String overviewId, String peptideRef, Boolean decoy) {
        return overviewId + "-" + peptideRef + "-" + decoy;
    }
//    public static void compress(AnalyseDataDO data) {
//        if (data.getRtArray() != null) {
//            data.setRtStart(data.getRtArray()[0]);
//            data.setRtEnd(data.getRtArray()[data.getRtArray().length - 1]);
////            data.setConvRtArray(CompressUtil.zlibCompress(CompressUtil.transToByte(ArrayUtils.toPrimitive(data.getRtArray()))));
//            data.setRtArray(null);
//        }
//        data.setConvIntensityMap(new HashMap<>());
//        for (String cutInfo : data.getIntensityMap().keySet()) {
//            Float[] intensities = data.getIntensityMap().get(cutInfo);
//            if (intensities != null) {
//                data.getConvIntensityMap().put(cutInfo, CompressUtil.zlibCompress(CompressUtil.transToByte(ArrayUtils.toPrimitive(intensities))));
//            } else {
//                data.getConvIntensityMap().put(cutInfo, null);
//            }
//        }
//        data.setIntensityMap(null);
//        data.setCompressed(true);
//    }
//
//
//    public static void decompress(AnalyseDataDO data, List<Float> rtList) {
//        int indexStart = rtList.indexOf(data.getRtStart());
//        int indexEnd = rtList.indexOf(data.getRtEnd());
//
//        Float[] rtArray = new Float[rtList.size()];
//        rtList.toArray(rtArray);
//        data.setRtArray(ArrayUtils.subarray(rtArray, indexStart, indexEnd+1));
//
//        for (String cutInfo : data.getConvIntensityMap().keySet()) {
//            byte[] intensities = data.getConvIntensityMap().get(cutInfo);
//            if (intensities != null) {
//                data.getIntensityMap().put(cutInfo, CompressUtil.transToFloat(CompressUtil.zlibDecompress(intensities)));
//            } else {
//                data.getIntensityMap().put(cutInfo, null);
//            }
//        }
//        data.setConvIntensityMap(null);
//        data.setCompressed(false);
//    }


}
