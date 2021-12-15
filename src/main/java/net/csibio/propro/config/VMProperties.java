package net.csibio.propro.config;

import com.sun.management.OperatingSystemMXBean;
import net.csibio.propro.utils.RepositoryUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;

@Component("vmProperties")
public class VMProperties {
    static OperatingSystemMXBean osmb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    static long size = osmb.getTotalMemorySize() / 1024 / 1024 / 1024;

    @Autowired
    private Environment env;

    @Value("${repository}")
    private String repository;

    private int multiple = (int) Math.ceil(size / 10.0);

    @PostConstruct
    public void init() {
        System.out.println("Multiple Threads: " + multiple);
        System.out.println("Repository: " + env.getProperty("repository"));
        System.out.println("RAM: " + size + "GB");
        RepositoryUtil.repository = repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getRepository() {
        if (StringUtils.isEmpty(repository)) {
            return "/nas/data";
        }
        return repository;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public int getMultiple() {
        return Math.max(multiple, 1);
    }

}
