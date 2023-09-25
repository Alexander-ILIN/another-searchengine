package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InitService
{
    private final SiteService siteService;

    private final SitesList sites;

    private final MappingIndexingService mappingIndexingService;


    @Autowired
    public InitService(SiteService siteService, SitesList sites, MappingIndexingService mappingIndexingService)
    {
        this.siteService = siteService;
        this.sites = sites;
        this.mappingIndexingService = mappingIndexingService;
    }


    /**
     * сравнение списка сайтов, находящися в конфигурационном файле с БД;
     * добавление / удаление информации о сайтах из БД при расхождениях
     */
    public void verifyConfigVsDbSites()
    {
        List<Site> sitesToRemove = new ArrayList<>();

        Map<String, searchengine.config.Site> configSites = sites.getSites().stream().
                collect(Collectors.toMap(k -> UtilService.getUrlWithSlash(k.getUrl()), v -> v));

        Iterable<Site> dbSites = siteService.findAll();

        for(Site curSiteDb : dbSites)
        {
            String curSiteUrl = curSiteDb.getUrl();

            if(configSites.containsKey(curSiteUrl))
            {
                configSites.remove(curSiteUrl);
            }
            else
            {
                sitesToRemove.add(curSiteDb);
            }
        }

        saveAddedSites(configSites.values());
        removeConfigAbsentSites(sitesToRemove);
    }

    /**
     * сохранение в БД добавленных сайтов (присутствующих в конфигурационном файле и отсутствующих в БД)
     * @param newSitesFromConfig сайты, присутствующие в конфигурационном файле и отсутствующие в БД
     */
    private void saveAddedSites(Collection<searchengine.config.Site> newSitesFromConfig)
    {
        List<Site> sitesToSave = new ArrayList<>();

        for(searchengine.config.Site curSite : newSitesFromConfig)
        {
            Site newSite = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null,
                    UtilService.getUrlWithSlash(curSite.getUrl()), curSite.getName());
            sitesToSave.add(newSite);
        }

        siteService.saveAll(sitesToSave);
    }

    /**
     * удаление из БД данных, относящихся к сайтам, присутствующим в БД, но отсутствующим в конфигурационном файле
     * @param sitesToRemove сайты, присутствующие в БД, но отсутствующие в конфигурационном файле
     */
    private void removeConfigAbsentSites(List<Site> sitesToRemove)
    {
        for(Site curSite : sitesToRemove)
        {
            mappingIndexingService.removeSiteData(curSite);
            siteService.delete(curSite);
        }
    }
}