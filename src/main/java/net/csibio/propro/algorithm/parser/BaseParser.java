package net.csibio.propro.algorithm.parser;

import net.csibio.aird.bean.Compressor;
import net.csibio.propro.utils.CompressUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public abstract class BaseParser {

    public final Logger logger = LoggerFactory.getLogger(BaseParser.class);

    //默认从Aird文件中读取,编码Order为LITTLE_ENDIAN,精度为小数点后三位
    public float[] getMzValues(byte[] value, Compressor intCompressor) {
        return getMzValues(value, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * get mz values only for aird file
     *
     * @param value
     * @return
     */
    public float[] getMzValues(byte[] value, ByteOrder order) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
        byteBuffer.order(order);

        IntBuffer ints = byteBuffer.asIntBuffer();
        int[] intValues = new int[ints.capacity()];
        for (int i = 0; i < ints.capacity(); i++) {
            intValues[i] = ints.get(i);
        }
        intValues = CompressUtil.decompressForSortedInt(intValues);
        float[] floatValues = new float[intValues.length];
        for (int index = 0; index < intValues.length; index++) {
            floatValues[index] = (float) intValues[index] / 1000;
        }
        byteBuffer.clear();
        return floatValues;
    }

    public float[] getIntValues(byte[] value, Compressor intCompressor) throws Exception {
        if (intCompressor.getMethod().contains("log10")) {
            return getLogedIntValues(value, ByteOrder.LITTLE_ENDIAN);
        } else {
            return getIntValues(value, ByteOrder.LITTLE_ENDIAN);
        }
    }

    /**
     * get mz values only for aird file
     *
     * @param value
     * @return
     */
    public float[] getIntValues(byte[] value, ByteOrder order) throws Exception {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
            byteBuffer.order(order);

            FloatBuffer intensities = byteBuffer.asFloatBuffer();
            float[] intValues = new float[intensities.capacity()];
            for (int i = 0; i < intensities.capacity(); i++) {
                intValues[i] = intensities.get(i);
            }

            byteBuffer.clear();
            return intValues;
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * get mz values only for aird file
     *
     * @param value
     * @return
     */
    public float[] getLogedIntValues(byte[] value, ByteOrder order) throws Exception {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
            byteBuffer.order(order);

            FloatBuffer intensities = byteBuffer.asFloatBuffer();
            float[] intValues = new float[intensities.capacity()];
            for (int i = 0; i < intensities.capacity(); i++) {
                intValues[i] = (float) Math.pow(10, intensities.get(i));
            }

            byteBuffer.clear();
            return intValues;
        } catch (Exception e) {
            throw e;
        }

    }

}
