package searchengine.services;

import searchengine.dto.ResponseWrapper;

/**
 * интерфейс, использующийся для выполнения поискового запроса пользователя
 */
public interface SiteSearchService {
    /**
     * обработка поискового запроса от пользователя
     *
     * @param queryText       поисковый запрос
     * @param siteUrl         ссылка на сайт, по которому необходимо осуществить поиск
     * @param outputOffset    количество результатов, которые необходимо пропустить
     * @param resultsQtyLimit количество результатов на одной странице
     * @return объект ResponseWrapper: HTTP статус и Response, содержащий результат поиска
     */
    ResponseWrapper searchSites(String queryText, String siteUrl, Integer outputOffset, Integer resultsQtyLimit);
}
