package net.csibio.propro.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtil {

    public static void log(String prefix, long start) {
        long delta = System.currentTimeMillis() - start;
        if (delta < 1000) {
            log.info(prefix + ":" + delta + "毫秒");
        } else if (delta > 1000) {
            log.info(prefix + ":" + delta + "秒");
        }
    }
}
