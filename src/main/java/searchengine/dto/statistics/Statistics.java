package searchengine.dto.statistics;

import lombok.Getter;
import searchengine.model.entity.Site;
import searchengine.model.entity.SiteStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * класс, использующийся для хранения статистики по индексации сайтов: общей и по каждому сайту
 */

@Getter
public class Statistics
{
    private final Total total; // общая статистика по всем сайтам
    private Detailed[] detailed; // массив, содержащий статистику по каждому сайту
    private int detailedCounter = 0; // размер массива, содержащего статистику по каждому сайту

    public Statistics(long sites, long pages, long lemmas, boolean isIndexing)
    {
        this.total = new Total(sites, pages, lemmas, isIndexing);
        this.detailed = new Detailed[(int) sites];
    }

    /**
     * класс, использующийся для хранения статистики по всем сайтам
     */
    public class Total
    {
        private final long sites; // общее количество сайтов
        private final long pages; // общее количество страниц
        private final long lemmas; // общее количество лемм
        private final boolean isIndexing; // происходит ли в данный момент индексация сайтов

        public Total(long sites, long pages, long lemmas, boolean isIndexing)
        {
            this.sites = sites;
            this.pages = pages;
            this.lemmas = lemmas;
            this.isIndexing = isIndexing;
        }
    }

    /**
     * класс, использующийся для хранения статистики по отдельным сайтам
     */
    @Getter
    public class Detailed
    {
        private final String url; // ссылка на сайт
        private final String name; // имя сайта
        private final SiteStatus status; // статус индексации сайта
        private final long statusTime; // время последнего изменения статуса
        private final String error; // последняя возникшая при индексации ошибка
        private final long pages; // количество страниц
        private final long lemmas; // количество лемм

        public Detailed(Site site, long pages, long lemmas)
        {
            this.url = site.getUrl();
            this.name = site.getName();
            this.status = site.getStatus();
            LocalDateTime statusTime = site.getStatusTime();
            ZonedDateTime zonedStatusTime = ZonedDateTime.of(statusTime, ZoneId.systemDefault());
            this.statusTime = zonedStatusTime.toInstant().toEpochMilli();
            this.error = site.getLastError();
            this.pages = pages;
            this.lemmas = lemmas;
        }
    }

    /**
     * добавление информации о сайте
     * @param site объект сайта
     * @param pages количество страниц
     * @param lemmas количество лемм
     */
    public void addDetailedInfo(Site site, long pages, long lemmas)
    {
        if(detailedCounter < detailed.length)
        {
            Detailed detailedData = new Detailed(site, pages, lemmas);
            detailed[detailedCounter] = detailedData;
            ++detailedCounter;
        }
        else
        {
            System.out.println("All sites already added");
        }
    }
}