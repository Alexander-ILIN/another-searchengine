package searchengine.services.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.dto.response.Response;
import searchengine.dto.response.ResponseFail;
import searchengine.dto.response.ResponseStatistics;
import searchengine.dto.ResponseWrapper;
import searchengine.dto.statistics.Statistics;
import searchengine.model.entity.Site;
import searchengine.services.indexing_control.IndexingControlService;
import searchengine.services.db.LemmaService;
import searchengine.services.db.PageService;
import searchengine.services.db.SiteService;

/**
 * класс, использующийся для сбора статистики по индексации сайтов: общей и по каждому сайту
 */
@Component
class StatisticsServiceImpl implements StatisticsService
{
    private final SiteService siteService;

    private final PageService pageService;

    private final LemmaService lemmaService;

    private final IndexingControlService indexingControlService;

    @Autowired
    public StatisticsServiceImpl(SiteService siteService, PageService pageService, LemmaService lemmaService,
                                 IndexingControlService indexingControlService)
    {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexingControlService = indexingControlService;
    }

    /**
     * сбор статистики индексации: общей и по каждому сайту
     * @return объект ResponseWrapper: HTTP статус и Response содержащий в себе объект Statistics
     */
    @Override
    public ResponseWrapper getStatistics()
    {
        Response response;
        HttpStatus httpStatus;
        try {

            long sitesQty = siteService.count();
            long pagesQty = pageService.count();
            long lemmasQty = lemmaService.count();
            boolean isIndexing = indexingControlService.isIndexingInProgress();

            Statistics statistics = new Statistics(sitesQty, pagesQty, lemmasQty, isIndexing);

            Iterable<Site> sites = siteService.findAll();

            for (Site curSite : sites) {
                int siteId = curSite.getId();
                long sitePagesQty = pageService.countAllBySiteId(siteId);
                long siteLemmasQty = lemmaService.countAllBySiteId(siteId);

                statistics.addDetailedInfo(curSite, sitePagesQty, siteLemmasQty);
            }

            response = new ResponseStatistics(true, statistics);
            httpStatus = HttpStatus.OK;
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            response = new ResponseFail(false, "Ошибка при получении статистики");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        return responseWrapper;
    }
}