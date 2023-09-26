package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационный класс. Используется для доступа к параметрам из "application.yaml"
 */

@Getter
@Setter
@Component
@ConfigurationProperties("indexing-config")
public class Config
{
    // User agent
    private String userAgent;

    // Referrer
    private String referrer;

    // Размер буфера для сохранения страниц
    private int pageBufferSize;

    // Размер буфера для сохранения лемм
    private int lemmaBufferSize;

    // Размер буфера для сохранения индекса
    private int indexBufferSize;

    // Пороговое значение коэффициента встречаемости леммы на сайте
    private float lemmaOccurrenceLimit;

    // Имя логера
    private String customLoggerName;

}