package net.csibio.propro;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.annotation.SectionScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
@SectionScan(basePackages = {"net.csibio.propro.constants.enums.*"})
public class ProProApplication {
    public static void main(String[] args) throws UnknownHostException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        ConfigurableApplicationContext application = SpringApplication.run(ProProApplication.class, args);
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String path = env.getProperty("server.servlet.context-path");
        log.info("\n----------------------------------------------------------\n\t" +
                "Application ProPro is running! Access URLs:\n\t" +
                "Front: \t\thttp://localhost:8000" + path + "/\n\t" +
                "Local: \t\thttp://localhost:8080" + path + "/\n\t" +
                "Swagger文档: \thttp://" + ip + ":" + port + path + "/swagger-ui/index.html\n" +
                "----------------------------------------------------------");

    }
}
