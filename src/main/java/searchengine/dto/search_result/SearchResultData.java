package searchengine.dto.search_result;

import lombok.Getter;
import searchengine.services.SearchResultProcessor;

/**
 * класс, используемый для хранения данных для ответа на поисковый запрос
 */
@Getter
public class SearchResultData {
    private final String site;      // ссылка на сайт, к которому относится страница
    private final String siteName;  // имя сайта, к которому относится страница
    private final String uri;       // ссылка на страницу
    private final String title;     // заголовок страницы
    private final String snippet;   // фрагмент текста, в котором найдены совпадения
    private final float relevance;  // относительная релевантность страницы

    /**
     * конструктор класса
     *
     * @param searchResultProcessor объект класса SearchResultProcessor
     */
    public SearchResultData(SearchResultProcessor searchResultProcessor) {
        this.site = searchResultProcessor.getSiteUrl();
        this.siteName = searchResultProcessor.getSiteName();
        this.uri = searchResultProcessor.getPageUrl();
        this.title = searchResultProcessor.getTitle();
        this.snippet = searchResultProcessor.getSnippet();
        this.relevance = searchResultProcessor.getRelRelevance();
    }
}