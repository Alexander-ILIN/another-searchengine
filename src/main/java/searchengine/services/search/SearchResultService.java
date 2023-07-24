package searchengine.services.search;

import searchengine.dto.search_result.SearchResultData;
import java.io.IOException;

/**
 * интерфейс, использующийся для выполнения поискового запроса пользователя
 */
public interface SearchResultService
{
    /**
     * пошагово создаются и заполняются объекты SearchResultProcessor
     * @param queryText строка, содержащая поисковый запрос
     * @param siteUrl ссылка на сайт, по которому необходимо выполнить поиск
     * @param resultsQtyLimit количество результатов, которое необходимо вывести
     * @return массив объектов SearchResultData, который впоследствии используется для вывода результата поиска пользователю
     * @throws IOException исключение, если отсутствуют проиндексированные сайты
     */
    SearchResultData[] getSearchResults(String queryText, String siteUrl, int resultsQtyLimit) throws IOException;
}
