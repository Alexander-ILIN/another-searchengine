package searchengine.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;

/**
 * класс, используемый для получения карты сайта и запуска индексации
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Log4j2
class MappingIndexingServiceImpl implements MappingIndexingService, SiteMappingService {
    private final PageService pageService;
    private final Config config;
    private final PageIndexingService pageIndexingService;
    private final SiteService siteService;
    private final LoggingService loggingService;
    private final Set<Page> pagesBuffer = new ConcurrentSkipListSet<>();
    private int bufferSize = 0;
    private Site processingSite;

    private AuxSiteData auxSiteData;

    private volatile boolean isTerminated = false;


    @Autowired
    public MappingIndexingServiceImpl(PageService pageService, Config config, PageIndexingService pageIndexingService,
                                      SiteService siteService, LoggingService loggingService) {
        this.pageService = pageService;
        this.config = config;
        this.pageIndexingService = pageIndexingService;
        this.siteService = siteService;
        this.loggingService = loggingService;
    }

    /**
     * Запуск процесса получения всех страниц сайта и их индексации
     */
    @Override
    public void getAndIndexPages(Site site) {
        this.processingSite = site;

        setProcessingSiteStatus(SiteStatus.INDEXING, null);

        LinkProcessor linkProcessor = prepareLinkProcessor(site);

        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(linkProcessor);
        pool.shutdown();

        if (isTerminated) {
            setProcessingSiteStatus(SiteStatus.FAILED, "Индексация была принудительно остановлена");
        } else {
            avoidDuplicatesInBuffer();
            saveAndIndexPages();
            setProcessingSiteStatus(SiteStatus.INDEXED, null);
        }

        loggingService.logCustom("Индексация сайта \"" + site.getName() + "\" завершена или прервана");

    }

    /**
     * Установка переменной isTerminated = true для остановки индексации
     */
    @Override
    public void terminate() {
        isTerminated = true;
    }

    /**
     * Запуск индексации отдельной страницы
     *
     * @param pageUrl ссылка на страницу
     * @return 1 - в случае успеха;
     * 0 - если в БД отсутствует сайт, к которому относится страница;
     * -1 - в случае возникновения других ошибок
     */
    @Override
    public int indexSinglePage(String pageUrl) {
        Site site = checkSite(pageUrl);

        if (site == null) {
            return 0;
        }
        processingSite = site;

        List<Page> foundPages = findPage(pageUrl, site);

        Page pageForReindexing = null;

        if (foundPages.size() > 1) {
            return -1;
        } else if (foundPages.size() == 1) {
            pageForReindexing = foundPages.get(0);
        }

        int indexingResult = startSinglePageIndexingProcess(pageUrl, pageForReindexing, site);

        return indexingResult;
    }

    /**
     * индексация / повторная индексация отдельной страницы;
     *
     * @param pageUrl           ссылка на страницу
     * @param pageForReindexing страница для повторной индексации
     * @param site              сайт, к которому относится страница
     * @return -1 в случае ошибки, 1 в случае успеха
     */
    private int startSinglePageIndexingProcess(String pageUrl, Page pageForReindexing, Site site) {
        setProcessingSiteStatus(SiteStatus.INDEXING, null);

        LinkProcessor linkProcessor = prepareLinkProcessor(site);

        try {
            linkProcessor.getAndSavePageData(pageUrl);
        } catch (Exception ex) {
            log.error(ex);
            setProcessingSiteStatus(SiteStatus.FAILED, ex.getMessage());
            return -1;
        }

        if (null != pageForReindexing) {
            pageIndexingService.deletePageIndexData(pageForReindexing);
            if (!modifyPageInBuffer(pageForReindexing)) {
                setProcessingSiteStatus(SiteStatus.FAILED, "Не удалось получить содержимое страницы");
                return -1;
            }
        }

        saveAndIndexPages();

        setProcessingSiteStatus(SiteStatus.INDEXED, null);

        return 1;
    }

    /**
     * создание страниц, добавление в буфер
     * при достижении заданного размера буфера, запуск их сохранения и индексации
     *
     * @param pageUrl      ссылка на страницу
     * @param responseCode код http ответа
     * @param pageContent  содержание страницы
     * @return true - в случае успеха, false - в случае прерывания процесса индексации
     */
    @Override
    public synchronized boolean proceedWithPageData(String pageUrl, int responseCode,
                                                    String pageContent, int siteId) {
        if (isTerminated) {
            auxSiteData.terminate();
            return false;
        }
        Page page = new Page(pageUrl, responseCode, pageContent, siteId);
        pagesBuffer.add(page);

        if (pagesBuffer.size() >= bufferSize) {
            avoidDuplicatesInBuffer();
            saveAndIndexPages();
        }
        return true;
    }

    /**
     * удаление данных, относящихся к сайту, из таблиц: page, lemma, index
     *
     * @param site сайт, данные о котором необходимо удалить
     */
    @Override
    public void removeSiteData(Site site) {
        int siteId = site.getId();

        pageIndexingService.removeSiteIndexData(siteId);
        pageService.deleteBySiteId(siteId);
    }


    /**
     * сохраниение всех страниц из буфера в БД и запуск их индексации
     */
    private void saveAndIndexPages() {
        // Profiling
        long start = System.currentTimeMillis();

        pageService.saveAll(pagesBuffer);

        // Profiling
        long pagesSaved = System.currentTimeMillis();
        System.out.println("Site #" + processingSite.getId() + ": " + pagesBuffer.size() + " pages saved " + (pagesSaved - start) + " ms");

        for (Page currentPage : pagesBuffer) {
            if (currentPage.getResponseCode() == 200) {
                System.out.println("Site # " + processingSite.getId() + " page # " + currentPage.getId() + " indexing started. Site indexer ==> " + pageIndexingService);
                pageIndexingService.indexPage(currentPage);
                setProcessingSiteStatus(SiteStatus.INDEXING, null);
            }
        }
        pagesBuffer.clear();
    }

    /**
     * создание объекта LinkProcessor
     * Данный объект в дальнейшем используется для создания карты сайта и запуска индексации его страниц,
     * либо для запуска индексации отдельной страницы
     *
     * @param site сайт
     * @return созданный объект LinkProcessor
     */
    private LinkProcessor prepareLinkProcessor(Site site) {
        if (bufferSize == 0) {
            bufferSize = config.getPageBufferSize();
        }
        String userAgent = config.getUserAgent();
        String referrer = config.getReferrer();
        auxSiteData = new AuxSiteData(site, this, userAgent, referrer);
        System.out.println("===========>Mapper: " + this);

        String siteUrl = site.getUrl();

        return new LinkProcessor(siteUrl, auxSiteData);
    }

    /**
     * проверка, относится ли страница к одному из сайтов, указанных в конфигурационном файле
     *
     * @param pageUrl ссылка на страницу
     * @return объект Site, к которому относится страница; null, если таких страниц не найдено
     */
    private Site checkSite(String pageUrl) {
        Iterable<Site> sites = siteService.findAll();

        for (Site site : sites) {
            String siteUrl = site.getUrl();

            if (pageUrl.startsWith(siteUrl)) {
                return site;
            }
        }

        return null;
    }


    /**
     * поиск страницы в базе данных по ссылке и сайту
     *
     * @param pageUrl ссылка на страницу
     * @param site    сайт, к которому страница относится
     * @return список с найденными страницами
     */
    private List<Page> findPage(String pageUrl, Site site) {
        String urlToFind;
        int siteId = site.getId();
        String siteUrl = site.getUrl();

        if (pageUrl.equals(siteUrl)) {
            urlToFind = "/";
        } else {
            urlToFind = pageUrl.substring(siteUrl.length() - 1);
        }

        List<Page> pages = pageService.findByUrlAndSiteId(urlToFind, siteId);

        return pages;
    }

    /**
     * метод используется при обновлении существующей страницы
     * задаётся значение pageId
     *
     * @param oldPage страница, найденная в базе данных
     * @return true при успехе, false при отсутствии обновлённой страницы в буфере
     */
    private boolean modifyPageInBuffer(Page oldPage) {
        if (pagesBuffer.size() != 1) {
            return false;
        }

        Page bufferPage = pagesBuffer.stream().findFirst().get();

        bufferPage.setId(oldPage.getId());

        return true;
    }

    /**
     * задание статуса текущему сайту
     *
     * @param siteStatus статус сайта
     * @param lastError  описание ошибки
     */
    private void setProcessingSiteStatus(SiteStatus siteStatus, String lastError) {
        processingSite.setStatus(siteStatus);
        processingSite.setStatusTime(LocalDateTime.now());
        processingSite.setLastError(lastError);
        siteService.save(processingSite);
    }

    /**
     * проверка наличия в базе данных страниц из буфера (по pageUrl и siteId)
     * в случае, если страницы уже существуют в базе данных, они удаляются из буфера
     */
    private void avoidDuplicatesInBuffer() {
        List<Page> existPages = pageService.findByPagesAndSiteId(pagesBuffer, processingSite.getId());
        pagesBuffer.removeAll(existPages);
    }
}