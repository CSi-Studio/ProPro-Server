package net.csibio.propro.utils;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.domain.db.DataDO;

import java.util.HashMap;

@Slf4j
public class DataUtil {

    public static void clearOrigin(DataDO data) {
        data.setRtArray(null);
        data.setIntMap(null);
        data.setCutInfoMap(null);
        data.setIonsLow(null);
        data.setIonsHigh(null);
    }

    public static void clearCompressed(DataDO data) {
        data.setRtBytes(null);
        data.setIntMapBytes(null);
        data.setCutInfosFeature(null);
        data.setIonsLowBytes(null);
        data.setIonsHighBytes(null);
    }

    public static void compress(DataDO data) {
        if (data.getRtArray() != null) {
            data.setRtBytes(CompressUtil.compressedToBytes(data.getRtArray()));
        }
        if (data.getIonsLow() != null) {
            data.setIonsLowBytes(CompressUtil.compressedToBytes(data.getIonsLow()));
        }
        if (data.getIonsHigh() != null) {
            data.setIonsHighBytes(CompressUtil.compressedToBytes(data.getIonsHigh()));
        }
        if (data.getIntMap() != null && data.getIntMap().size() > 0) {
            HashMap<String, byte[]> intMap = new HashMap<>();
            data.getIntMap().forEach((key, value) -> {
                if (value != null) {
                    intMap.put(key, CompressUtil.compressedToBytes(value));
                } else {
                    intMap.put(key, null);
                }
            });
            data.setIntMapBytes(intMap);
        }
        data.setCutInfosFeature(FeatureUtil.toString(data.getCutInfoMap()));
        clearOrigin(data);
    }

    public static void decompress(DataDO data) {
        if (data.getRtBytes() != null) {
            data.setRtArray(CompressUtil.transToFloat(data.getRtBytes()));
        }
        if (data.getIonsLowBytes() != null) {
            data.setIonsLow(CompressUtil.transToInt(data.getIonsLowBytes()));
        }
        if (data.getIonsHighBytes() != null) {
            data.setIonsHigh(CompressUtil.transToInt(data.getIonsHighBytes()));
        }
        if (data.getIntMapBytes() != null && data.getIntMapBytes().size() > 0) {
            HashMap<String, float[]> intensityMap = new HashMap<>();
            data.getIntMapBytes().forEach((key, value) -> {
                if (value != null) {
                    intensityMap.put(key, CompressUtil.transToFloat(value));
                } else {
                    intensityMap.put(key, null);
                }
            });
            data.setIntMap(intensityMap);
        }

        if (data.getCutInfosFeature() != null) {
            data.setCutInfoMap(FeatureUtil.toFloatMap(data.getCutInfosFeature()));
        }

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
