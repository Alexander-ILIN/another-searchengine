package searchengine.services.search;

import searchengine.dto.ResponseWrapper;

/**
 * интерфейс, использующийся для выполнения поискового запроса пользователя
 */
public interface SiteSearchService
{
    /**
     * обработка поискового запроса от пользователя
     * @param queryText поисковый запрос
     * @param siteUrl ссылка на сайт, по которому необходимо осуществить поиск
     * @param resultsQtyLimit количество результатов, которое необходимо вывести
     * @return объект ResponseWrapper: HTTP статус и Response, содержащий результат поиска
     */
    ResponseWrapper searchSites(String queryText, String siteUrl, int resultsQtyLimit);
}
