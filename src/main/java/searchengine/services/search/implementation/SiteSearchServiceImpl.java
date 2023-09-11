package searchengine.services.search.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.response.Response;
import searchengine.dto.response.ResponseFail;
import searchengine.dto.response.ResponseSearch;
import searchengine.dto.ResponseWrapper;
import searchengine.dto.search_result.SearchResultData;
import searchengine.services.search.SearchResultService;
import searchengine.services.search.SiteSearchService;
import java.io.IOException;

/**
 * класс, реализующий получение ответа на поисковый запрос от пользователя
 */
@Service
class SiteSearchServiceImpl implements SiteSearchService
{
    SearchResultService searchResultService; // объект, осуществляющий выполнение поискового запроса

    @Autowired
    public SiteSearchServiceImpl(SearchResultService searchResultService)
    {
        this.searchResultService = searchResultService;
    }

    /**
     * обработка поискового запроса от пользователя
     * @param queryText поисковый запрос
     * @param siteUrl ссылка на сайт, по которому необходимо осуществить поиск
     * @param resultsQtyLimit количество результатов, которое необходимо вывести
     * @return объект ResponseWrapper: HTTP статус и Response, содержащий результат поиска
     */
    @Override
    public ResponseWrapper searchSites(String queryText, String siteUrl, int resultsQtyLimit)
    {
        Response response;
        HttpStatus httpStatus;

        try
        {
            if(queryText.isBlank())
            {
                response = new ResponseFail(false, "Задан пустой поисковый запрос");
                httpStatus = HttpStatus.BAD_REQUEST;
            }
            else
            {
                SearchResultData[] resultData = searchResultService.getSearchResults(queryText, siteUrl, resultsQtyLimit);
                response = new ResponseSearch(true, resultData.length, resultData);
                httpStatus = HttpStatus.OK;
            }
        }
        catch (IOException ioEx)
        {
            response = new ResponseFail(false, ioEx.getMessage());
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            response = new ResponseFail(false, "Не удалось совершить поиск");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        return responseWrapper;
    }
}
