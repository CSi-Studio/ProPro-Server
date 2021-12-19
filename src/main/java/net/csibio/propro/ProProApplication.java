package net.csibio.propro;

import lombok.extern.slf4j.Slf4j;
import net.csibio.propro.annotation.SectionScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
@EnableAsync
@SectionScan(basePackages = {"net.csibio.propro.constants.enums.*"})
public class ProProApplication {
    public static void main(String[] args)
            throws UnknownHostException, ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InstantiationException {
        ConfigurableApplicationContext application =
                SpringApplication.run(ProProApplication.class, args);
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
//        String path = env.getProperty("server.servlet.context-path");
        log.info(
                "\n----------------------------------------------------------\n\t"
                        + "Application ProPro is running! Access URLs:\n\t"
                        + "Front: \t\thttp://localhost:8000"
                        + "/\n\t"
                        + "Local: \t\thttp://localhost:8080"
                        + "/\n\t"
                        + "Swagger文档: \thttp://"
                        + ip
                        + ":"
                        + port
                        + "/swagger-ui/index.html\n"
                        + "----------------------------------------------------------");
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/index.html");
            factory.addErrorPages(error404Page);
        };
    }
}
