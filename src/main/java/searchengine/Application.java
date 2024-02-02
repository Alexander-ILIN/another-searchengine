package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import searchengine.services.InitService;

@SpringBootApplication
public class Application {
    private static ApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(Application.class, args);

        InitService initService = context.getBean(InitService.class);
        initService.verifyConfigVsDbSites();
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
