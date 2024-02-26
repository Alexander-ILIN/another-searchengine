package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import searchengine.dto.search_result.SearchResultData;
import searchengine.model.SearchIndex;
import searchengine.model.Site;

import java.util.*;

/**
 * класс, используемый для генерации и хранения данных для ответа на поисковый запрос
 */

@Setter
@Getter
public class SearchResultProcessor {
    private static List<SearchResultProcessor> allSearchResultProcessorList = new ArrayList<>();          // список всех созданных объектов класса SearchResultProcessor для всех сайтов
    private static List<SearchResultProcessor> searchResultProcessorList = new ArrayList<>();             // список всех объектов класса SearchResultProcessor для отображения на текущей странице
    private static Map<Integer, SearchResultProcessor> siteSearchResultMap = new HashMap<>();    // Map, Key = id страницы, value = объект класса SearchResultProcessor, относящийся к странице для одного сайта
    private static float maxRelevance = 0f;     // максимальная относительная релевантность
    private final int pageId;                   // id страницы, к которой относится данный объект класса SearchResultProcessor
    private List<SearchIndex> searchIndexList;  // список объектов поисковых индексов
    private float absRelevance;                 // абсолютная релевантность страницы
    private float relRelevance;                 // относительная релевантность страницы
    private String title;                       // заголовок страницы
    private String snippet;                     // фрагмент текста, в котором найдены совпадения
    private String siteUrl;                     // ссылка на сайт, к которому относится страница
    private String siteName;                    // имя сайта, к которому относится страница
    private String pageUrl;                     // ссылка на страницу
    private Document htmlDocument;              // содержимое страницы


    /**
     * конструктор класса
     *
     * @param site        сайт
     * @param pageId      ID страницы
     * @param searchIndex поисковый индекс
     */
    public SearchResultProcessor(Site site, int pageId, SearchIndex searchIndex) {
        this.pageId = pageId;
        this.siteUrl = UtilService.getUrlWithoutSlash(site.getUrl());
        this.siteName = site.getName();
        searchIndexList = new ArrayList<>();
        searchIndexList.add(searchIndex);

    }

    /**
     * добавление объекта класса SearchResultProcessor в allSearchResultProcessorList
     *
     * @param searchResultProcessor объект класса SearchResultProcessor
     */
    public static void addSearchResult(SearchResultProcessor searchResultProcessor) {
        allSearchResultProcessorList.add(searchResultProcessor);
    }

    /**
     * получение массива данных для ответа на поисковый запрос
     *
     * @return массив данных для ответа на поисковый запрос
     */
    public static SearchResultData[] generateResultArray() {
        int arraySize = searchResultProcessorList.size();

        SearchResultData[] resultArray = new SearchResultData[arraySize];

        for (int i = 0; i < arraySize; i++) {
            resultArray[i] = new SearchResultData(searchResultProcessorList.get(i));
        }

        return resultArray;
    }

    /**
     * очистка searchResultProcessorList и обнуление максимальной относительной релевантности
     */
    public static void clearResults() {
        allSearchResultProcessorList.clear();
        maxRelevance = 0.0f;
    }

    /**
     * очистка siteSearchResultMap
     */
    public static void clearSiteSearchResult() {
        siteSearchResultMap.clear();
    }

    /**
     * ограничение количества результатов поиска в соответствии с параметром поискового запроса
     *
     * @param resultsQtyLimit
     * @param outputOffset
     */
    public static void limitResults(Integer outputOffset, Integer resultsQtyLimit) {

        int upperBoundaryCalc = outputOffset + resultsQtyLimit;

        int upperBoundary = Math.min(upperBoundaryCalc, allSearchResultProcessorList.size());

        searchResultProcessorList = allSearchResultProcessorList.subList(outputOffset, upperBoundary);
    }

    /**
     * запуск расчёта и заполнения относительных релевантностей страниц для всех объектов SearchResultProcessor
     */
    private static void calculateRelRelevance() {
        allSearchResultProcessorList.stream().forEach(searchResultProcessor ->
                searchResultProcessor.setRelRelevance(searchResultProcessor.getAbsRelevance() / SearchResultProcessor.getMaxRelevance()));
    }

    /**
     * метод запускает расчёт и заполнение и относительных релевантностей страниц для всех объектов SearchResultProcessor
     * после расчёта осуществляется сортировка страниц по относительной релевантности в обратном порядке
     */
    public static void generateSortedTotalResults() {
        calculateRelRelevance();

        allSearchResultProcessorList.sort(Comparator.comparing((SearchResultProcessor searchResultProcessor) ->
                searchResultProcessor.getRelRelevance()).reversed());
    }

    /**
     * метод запускает расчёт и заполнение абсолютных релевантностей страниц для всех объектов SearchResultProcessor
     */
    public static void calculateAbsRelevanceForSitePages() {
        for (SearchResultProcessor curSearchResultProcessor : siteSearchResultMap.values()) {
            float curAbsRelevance = 0f;

            List<SearchIndex> curSearchIndexList = curSearchResultProcessor.getSearchIndexList();

            for (SearchIndex curSearchIndex : curSearchIndexList) {
                curAbsRelevance += curSearchIndex.getRank();
            }

            curSearchResultProcessor.setAbsRelevance(curAbsRelevance);
            addSearchResult(curSearchResultProcessor);
            setMaxRelevance(Float.max(curAbsRelevance, SearchResultProcessor.getMaxRelevance()));
        }
    }

    /**
     * метод заполняет поле title, для всех объектов SearchResultProcessor из searchResultProcessorList
     */
    public static void setTitles() {

        for (SearchResultProcessor searchResultProcessor : searchResultProcessorList) {
            Document htmlDocument = searchResultProcessor.getHtmlDocument();
            List<String> titles = htmlDocument.select("title").eachText();
            String title = (titles.size() == 0) ? "" : titles.get(0);
            searchResultProcessor.setTitle(title);
        }
    }

    public static List<SearchResultProcessor> getAllSearchResultProcessorList() {
        return allSearchResultProcessorList;
    }

    public static List<SearchResultProcessor> getSearchResultList() {
        return searchResultProcessorList;
    }

    public static Map<Integer, SearchResultProcessor> getSiteSearchResultMap() {
        return siteSearchResultMap;
    }

    public static float getMaxRelevance() {
        return maxRelevance;
    }

    public static void setMaxRelevance(float maxRelevance) {
        SearchResultProcessor.maxRelevance = maxRelevance;
    }

    /**
     * добавление объекта поискового индекса
     *
     * @param searchIndex объект поискового индекса
     */
    public void addIndex(SearchIndex searchIndex) {
        searchIndexList.add(searchIndex);
    }
}