package searchengine.services;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Config;

/**
 * класс, используемый для логирования
 */
@Service
@Log4j2
public class LoggingServiceImpl implements LoggingService
{
    private final Config config;

    @Autowired
    public LoggingServiceImpl(Config config)
    {
        this.config = config;
    }

    /**
     * создание лога с пользовательским уровнем логирования
     * @param logText текст лога
     */
    @Override
    public void logCustom(String logText)
    {
        Level customLevel = Level.getLevel(config.getCustomLoggerName());
        log.log(customLevel, logText);
    }
}
