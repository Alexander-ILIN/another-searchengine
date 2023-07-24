package searchengine.services.mapping_indexing.implementation;

import lombok.Getter;
import searchengine.model.entity.Site;
import searchengine.services.UtilService;
import searchengine.services.mapping_indexing.SiteMappingService;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * класс, описывающий сайт
 */
@Getter
class AuxSiteData
{
    private final String rootUrl;       // ссылка на сайт
    private final int siteId;           // ID сайта в таблице site
    private Set<String> checkedUrls;    // сет ссылок на страницы сайта, пройденные системой обхода страниц
    private final int rootUrlLen;       // длина ссылки на сайт
    private final SiteMappingService siteMapper; // объект, используемый для сохранения страниц и запуска их индексации
    private final String userAgent; // user agent
    private final String referrer; // referrer
    private volatile boolean terminated = false; // статус прерывания процесса индексации

    /**
     * конструктор класса
     * @param site - сайт, для которого необходимо получить карту
     */
    public AuxSiteData(Site site, SiteMappingService siteMapper, String userAgent, String referrer)
    {
        String tempRootUrl = site.getUrl();
        this.siteId = site.getId();
        this.siteMapper = siteMapper;
        this.userAgent = userAgent;
        this.referrer = referrer;

        this.rootUrl = UtilService.getUrlWithoutSlash(tempRootUrl);

        this.rootUrlLen = rootUrl.length();

        checkedUrls = new ConcurrentSkipListSet<>();
    }

    /**
     * проверка, была ли страница уже пройдена системой обхода страниц сайта
     * @param url - ссылка на страницу
     * @return true, если страница была пройдена, false в противном случае
     */
    public boolean isUrlChecked(String url)
    {
        String urlToCheck = UtilService.getUrlWithoutSlash(url);
        return checkedUrls.contains(urlToCheck);
    }

    /**
     * добавление страницы в сет пройденных страниц
     * приведение ссылке к виду без "/"
     * @param url - ссылка на страницу
     */
    public synchronized void addCheckedUrl(String url)
    {
        String urlToAdd = UtilService.getUrlWithoutSlash(url);

        checkedUrls.add(urlToAdd);
    }

    /**
     * получение количества страниц, пройденных системой обхода страниц
     * @return
     */
    public int getCheckedUrlsQty()
    {
        return checkedUrls.size();
    }

    public void terminate()
    {
        terminated = true;
    }

}
