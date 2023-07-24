package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.Response;
import searchengine.dto.ResponseWrapper;
import searchengine.services.indexing_control.IndexingControlService;
import searchengine.services.search.SiteSearchService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingControlService indexingControlService;

    private final SiteSearchService siteSearchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingControlService indexingControlService,
                         SiteSearchService siteSearchService)
    {
        this.statisticsService = statisticsService;
        this.indexingControlService = indexingControlService;
        this.siteSearchService = siteSearchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<Response> statistics()
    {
        ResponseWrapper responseWrapper = statisticsService.getStatistics();

        return ResponseEntity.status(responseWrapper.getHttpStatus()).body(responseWrapper.getResponse());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing(@RequestParam(required = false, name = "site") String siteUrl)
    {
        ResponseWrapper responseWrapper = indexingControlService.launchSitesIndexing(siteUrl);

        return ResponseEntity.status(responseWrapper.getHttpStatus()).body(responseWrapper.getResponse());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing()
    {
        ResponseWrapper responseWrapper = indexingControlService.stopSitesIndexing();

        return ResponseEntity.status(responseWrapper.getHttpStatus()).body(responseWrapper.getResponse());
    }

    @GetMapping(value = "/search")
    public ResponseEntity<Response> search(@RequestParam(name = "query") String queryText,
                                           @RequestParam(required = false, name = "site") String siteUrl,
                                           @RequestParam(name = "offset") int outputOffset,
                                           @RequestParam(name = "limit") int resultsQtyLimit)
    {
        ResponseWrapper responseWrapper = siteSearchService.searchSites(queryText, siteUrl, resultsQtyLimit);

        return ResponseEntity.status(responseWrapper.getHttpStatus()).body(responseWrapper.getResponse());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam(name = "url") String pageUrl)
    {
        ResponseWrapper responseWrapper = indexingControlService.singlePageIndexing(pageUrl);

        return ResponseEntity.status(responseWrapper.getHttpStatus()).body(responseWrapper.getResponse());
    }

}
