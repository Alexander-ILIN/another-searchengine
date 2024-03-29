package searchengine.services;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.dto.ResponseWrapper;
import searchengine.dto.response.Response;
import searchengine.dto.response.ResponseFail;
import searchengine.dto.response.ResponseSearch;
import searchengine.dto.search_result.SearchResultData;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * класс, реализующий выполнение поискового запроса пользователя
 */
@Service
@Log4j2
class SiteSearchServiceImpl implements SiteSearchService {
    // символы, обрабатываемые на страницах и в запросах
    private static final String ALLOWED_SYMBOLS_REGEX = "[а-яА-Яa-zA-Z0-9]+";
    // разбиение текста на слова, пробелы и остальные знаки
    private static final String REQ_SPLIT_REGEX = "(?<=[^а-яА-Яa-zA-Z0-9])|(?=[^а-яА-Яa-zA-Z0-9])";
    // открывающий тег
    private static final String OP_TAG = "<b>";
    // закрывающий тег
    private static final String CL_TAG = "</b>";
    // количество слов, пробелов и прочих остальных знаков перед первым и после последнего искомого слова в snippet
    private static final int LEADING_AND_TRAILING_ITEMS_QTY = 60;
    private final LemmasProcessingService lemmasProcessingService; // лемматизатор
    private final LemmaService lemmaService; // операции с леммами в БД
    private final SearchIndexService searchIndexService; // операции с индексами страниц в БД
    private final PageService pageService; // операции со страницами в БД
    private final SiteService siteService; // операции с сайтами в БД
    private final Config config; // доступ к параметрам конфигурации
    private final LoggingService loggingService;
    private boolean isMapInitialized = false;

    @Autowired
    public SiteSearchServiceImpl(LemmasProcessingService lemmasProcessingService, LemmaService lemmaService, SearchIndexService searchIndexService,
                                 PageService pageService, SiteService siteService, LoggingService loggingService, Config config) {
        this.lemmasProcessingService = lemmasProcessingService;
        this.lemmaService = lemmaService;
        this.searchIndexService = searchIndexService;
        this.pageService = pageService;
        this.siteService = siteService;
        this.loggingService = loggingService;
        this.config = config;
    }

    /**
     * обработка поискового запроса от пользователя
     *
     * @param queryText       поисковый запрос
     * @param siteUrl         ссылка на сайт, по которому необходимо осуществить поиск
     * @param outputOffset    количество результатов, которые необходимо пропустить
     * @param resultsQtyLimit количество результатов на одной странице
     * @return объект ResponseWrapper: HTTP статус и Response, содержащий результат поиска
     */
    @Override
    public ResponseWrapper searchSites(String queryText, String siteUrl, Integer outputOffset, Integer resultsQtyLimit) {
        logOnSearchStart(queryText, siteUrl);

        Response response;
        HttpStatus httpStatus;

        try {
            if (queryText.isBlank()) {
                response = new ResponseFail(false, "Задан пустой поисковый запрос");
                httpStatus = HttpStatus.BAD_REQUEST;
            } else {
                SearchResultData[] resultData = getSearchResults(queryText, siteUrl, outputOffset, resultsQtyLimit);
                response = new ResponseSearch(true, SearchResultProcessor.getAllSearchResultProcessorList().size(), resultData);
                httpStatus = HttpStatus.OK;
            }
        } catch (IOException ioEx) {
            response = new ResponseFail(false, ioEx.getMessage());
            httpStatus = HttpStatus.BAD_REQUEST;
            log.error("Поиск: ошибка", ioEx);
        } catch (Exception ex) {
            response = new ResponseFail(false, "Не удалось совершить поиск");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Поиск: ошибка", ex);
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        loggingService.logCustom("Поиск: результат = " + response.isResult());

        return responseWrapper;
    }

    /**
     * логирование начала поиска
     *
     * @param queryText поисковый запрос
     * @param siteUrl   ссылка на сайт, по которому необходимо осуществить поиск
     */
    private void logOnSearchStart(String queryText, String siteUrl) {
        String logMsg = "Поиск \"" + queryText + "\" по ";

        if (null == siteUrl) {
            logMsg += "всем сайтам";
        } else {
            logMsg += ("сайту " + siteUrl + " ");
        }
        loggingService.logCustom(logMsg + ": запуск");
    }

    /**
     * пошагово создаются и заполняются объекты SearchResultProcessor
     *
     * @param queryText       строка, содержащая поисковый запрос
     * @param siteUrl         ссылка на сайт, по которому необходимо выполнить поиск
     * @param outputOffset    количество результатов, которые необходимо пропустить
     * @param resultsQtyLimit количество результатов на одной странице
     * @return массив объектов SearchResultData, который впоследствии используется для вывода результата поиска пользователю
     * @throws IOException исключение, если отсутствуют проиндексированные сайты
     */
    public SearchResultData[] getSearchResults(String queryText, String siteUrl, Integer outputOffset, Integer resultsQtyLimit) throws IOException {

        Set<String> lemmasSet = lemmasProcessingService.getTextLemmas(queryText);

        if (outputOffset == 0) {
            SearchResultProcessor.clearResults();

            Set<Site> siteSet;

            if (siteUrl == null) {
                siteSet = getValidSites();
            } else {
                Site singleSite = getSite(siteUrl);

                siteSet = new HashSet<>();
                siteSet.add(singleSite);
            }


            for (Site curSite : siteSet) {
                getSiteSearchResults(lemmasSet, curSite);
            }

            SearchResultProcessor.generateSortedTotalResults();
        }

        SearchResultProcessor.limitResults(outputOffset, resultsQtyLimit);

        addPagesData();

        SearchResultProcessor.setTitles();

        fillInSnippets(lemmasSet);

        SearchResultData[] resultData = SearchResultProcessor.generateResultArray();

        return resultData;
    }

    /**
     * запуск поиска страниц, содержащих леммы из поискового запроса пользователя
     * если количество лемм в запросе больше 1,
     * то леммы содержащиеся более чем на (lemmaOccurrenceLimit * общее количество страниц) страницах будут исключены из поиска
     *
     * @param lemmasSet сет лемм из поискового запроса пользователя
     * @param site      сайт, по которому осуществляется поиск
     */
    private void getSiteSearchResults(Set<String> lemmasSet, Site site) {
        isMapInitialized = false;

        SearchResultProcessor.clearSiteSearchResult();

        List<Lemma> lemmas = lemmaService.findLemmas(lemmasSet, site.getId());

        if (lemmasSet.size() != lemmas.size()) {
            return;
        }

        long pagesQty = pageService.countAllBySiteId(site.getId());

        boolean excludeFrequentLemmas = isExcludeFrequentLemmas(lemmas, pagesQty);

        for (Lemma curLemma : lemmas) {
            if (isSkippedLemma(excludeFrequentLemmas, curLemma, pagesQty)) {
                continue;
            }

            List<SearchIndex> curSearchIndexes = searchIndexService.findByLemmaId(curLemma.getId());

            if (!isMapInitialized) {
                try {
                    initializeSearchResult(site, curSearchIndexes);
                } catch (Exception ex) {
                    log.warn(ex.getMessage());
                }
            } else {
                reduceSearchResult(curSearchIndexes);
            }
        }
        SearchResultProcessor.calculateAbsRelevanceForSitePages();
    }

    /**
     * проверка необходимости пропуска часто встречающихся лемм
     *
     * @param lemmas   список лемм из поискового запроса
     * @param pagesQty количество страниц сайта
     * @return true, если часто встречающиеся леммы необходимо исключать из поискового запроса;
     * false - в противном случае: если в поисковом запросе одна лемма или
     * наименьший коэффициент встречаемости лемм из поискового запроса больше, чем пороговое значение коэффициента встречаемости леммы на сайте
     */
    private boolean isExcludeFrequentLemmas(List<Lemma> lemmas, long pagesQty) {
        boolean excludeFrequentLemmas = false;

        int lemmasQty = lemmas.size();
        float lemmaOccurrenceLimit = config.getLemmaOccurrenceLimit();

        if (lemmasQty > 1) {
            Lemma firstLemma = lemmas.get(0);
            float firstLemmaOccurrence = (float) firstLemma.getFrequency() / (float) pagesQty;

            excludeFrequentLemmas = firstLemmaOccurrence < lemmaOccurrenceLimit;
        }

        return excludeFrequentLemmas;
    }

    /**
     * проверка необходимости исключения конкретной леммы из поискового запроса
     *
     * @param excludeFrequentLemmas исключаются ли из поискового запроса часто встречающихся леммы
     * @param curLemma              лемма
     * @param pagesQty              количество страниц на сайте
     * @return true, если лемму нужно исключить из поискового запроса; false - в противном случае
     */
    private boolean isSkippedLemma(boolean excludeFrequentLemmas, Lemma curLemma, long pagesQty) {
        boolean skipLemma = false;

        float lemmaOccurrenceLimit = config.getLemmaOccurrenceLimit();

        if (excludeFrequentLemmas) {
            float curLemmaOccurrence = (float) curLemma.getFrequency() / (float) pagesQty;

            skipLemma = curLemmaOccurrence >= lemmaOccurrenceLimit;
        }

        return skipLemma;
    }

    /**
     * метод создаёт объекты SearchResultProcessor для самой редкой леммы и устанавливает перемнную isMapInitialized = true
     * выполняется один раз для поискового запроса для каждого сайта, если перемнная isMapInitialized = false
     *
     * @param searchIndexes список объектов SearchIndex, относящийся к заданной лемме
     */
    private void initializeSearchResult(Site site, List<SearchIndex> searchIndexes) {
        for (SearchIndex curSearchIndex : searchIndexes) {
            int curPageId = curSearchIndex.getPageId();

            SearchResultProcessor curSearchResultProcessor = new SearchResultProcessor(site, curPageId, curSearchIndex);
            SearchResultProcessor.getSiteSearchResultMap().put(curPageId, curSearchResultProcessor);
        }

        isMapInitialized = true;
    }

    /**
     * метод исключает страницы из результатов поиска страницы, которые не содержат заданной леммы
     * выполняется для каждой леммы из поискового запроса, кроме первой (самой редкой), при isMapInitialized = true
     *
     * @param searchIndexes список объектов SearchIndex, относящийся к заданной лемме
     */
    private void reduceSearchResult(List<SearchIndex> searchIndexes) {
        Map<Integer, SearchIndex> curIndexMap = new HashMap<>();

        for (SearchIndex curSearchIndex : searchIndexes) {
            curIndexMap.put(curSearchIndex.getPageId(), curSearchIndex);
        }

        Set<Integer> toLeave = new HashSet<>(SearchResultProcessor.getSiteSearchResultMap().keySet());
        toLeave.retainAll(curIndexMap.keySet());

        for (int pageId : SearchResultProcessor.getSiteSearchResultMap().keySet()) {
            if (toLeave.contains(pageId)) {
                SearchResultProcessor curSearchResultProcessor = SearchResultProcessor.getSiteSearchResultMap().get(pageId);
                curSearchResultProcessor.addIndex(curIndexMap.get(pageId));
            }
        }
        SearchResultProcessor.getSiteSearchResultMap().keySet().removeIf(key -> !toLeave.contains(key));
    }

    /**
     * метод заполняет поля snippet для всех объектов SearchResultProcessor
     *
     * @param reqLemmas сет лемм из запроса
     */
    private void fillInSnippets(Set<String> reqLemmas) {
        for (SearchResultProcessor curSearchResultProcessor : SearchResultProcessor.getSearchResultList()) {
            setSnippet(reqLemmas, curSearchResultProcessor);
        }
    }

    /**
     * метод заполняет поле snippet для заданного объекта SearchResultProcessor
     *
     * @param reqLemmas             сет лемм, содержащихся в поисковом запросе
     * @param searchResultProcessor заданный объект SearchResultProcessor
     */
    private void setSnippet(Set<String> reqLemmas, SearchResultProcessor searchResultProcessor) {
        List<String> snippetWords = new LinkedList<>();
        int firstReqWordPos = 0;
        int lastReqWordPos = 0;

        Document htmlDocument = searchResultProcessor.getHtmlDocument();
        List<String> bodies = htmlDocument.select("body").eachText();
        if (bodies.size() == 0) {
            return;
        }

        for (String body : bodies) {
            String[] words = body.split(REQ_SPLIT_REGEX);
            for (String curWord : words) {
                if (isWordSearched(curWord, reqLemmas)) {
                    snippetWords.add(OP_TAG);

                    if (firstReqWordPos == 0) {
                        firstReqWordPos = snippetWords.size() - 1;
                    }

                    snippetWords.add(curWord);
                    snippetWords.add(CL_TAG);
                    lastReqWordPos = snippetWords.size() - 1;
                } else {
                    snippetWords.add(curWord);
                }
            }
        }

        String snippetText = generateSnippetText(snippetWords, firstReqWordPos, lastReqWordPos);
        searchResultProcessor.setSnippet(snippetText);
    }

    /**
     * метод проверяет содержатся ли леммы заданного слова в поисковом запросе
     *
     * @param word      заданное слово
     * @param reqLemmas сет, содержащий леммы поискового запроса
     * @return true, если леммы заданного слова содержатся в поисковом запросе; false, если нет
     */
    private boolean isWordSearched(String word, Set<String> reqLemmas) {

        List<String> wordLemmas = lemmasProcessingService.getWordLemmas(word);

        if (wordLemmas.size() == 0) {
            if (!word.matches(ALLOWED_SYMBOLS_REGEX)) {
                return false;
            } else {
                wordLemmas.add(word);
            }
        }

        for (String curLemma : wordLemmas) {
            if (reqLemmas.contains(curLemma)) {
                return true;
            }
        }
        return false;
    }

    /**
     * метод создаёт snippet
     *
     * @param snippetWords    список, содержащий текст страницы, разбитый на слова, пробелы и знаки препинания
     * @param firstReqWordPos индекс первого вхождения слова из поискового запроса в списке snippetWords
     * @param lastReqWordPos  индекс последнего вхождения слова из поискового запроса в списке snippetWords
     * @return текст snippet
     */
    private String generateSnippetText(List<String> snippetWords, int firstReqWordPos, int lastReqWordPos) {
        int i = firstReqWordPos;
        int counter = 0;

        while (i > 0 && !snippetWords.get(i).equals(".") && counter < LEADING_AND_TRAILING_ITEMS_QTY) {
            --i;
            ++counter;
        }

        firstReqWordPos = i;

        if (snippetWords.get(firstReqWordPos).equals(".")) {
            ++firstReqWordPos;
        }

        i = lastReqWordPos;
        counter = 0;

        while (i < snippetWords.size() - 1 && !snippetWords.get(i).equals(".") && counter < LEADING_AND_TRAILING_ITEMS_QTY) {
            ++i;
            ++counter;
        }

        lastReqWordPos = i;

        StringBuilder snippetText = new StringBuilder();

        for (int j = firstReqWordPos; j <= lastReqWordPos; j++) {
            snippetText.append(snippetWords.get(j));
        }

        return snippetText.toString().trim();
    }

    /**
     * получение сайта из запроса пользователя
     *
     * @param siteUrl ссылка на сайт
     * @return null, сайт не выбран пользователем; объект класса Site, соответствующий выбору пользователя
     * @throws IOException исключение, если выбранный сайт не проиндексирован
     */
    private Site getSite(String siteUrl) throws IOException {
        Site result;
        if (siteUrl == null) {
            result = null;
        } else {
            String urlForSearch = UtilService.getUrlWithSlash(siteUrl);

            List<Site> sites = siteService.findByUrl(urlForSearch);

            if (sites.size() != 1) {
                result = null;
            } else {
                result = sites.get(0);

                if (!result.getStatus().equals(SiteStatus.INDEXED)) {
                    throw new IOException("Выбранный сайт не проиндексирован");
                }
            }
        }
        return result;
    }

    /**
     * получение сета проиндексированных сайтов
     *
     * @return сет, содержащий только проиндексированные сайты
     * @throws IOException исключение, если отсутствуют проиндексированные сайты
     */
    private Set<Site> getValidSites() throws IOException {
        Set<Site> sitesSet = new HashSet<>();
        Iterable<Site> sites = siteService.findAll();

        sites.forEach(sitesSet::add);

        if (sitesSet.isEmpty()) {
            throw new IOException("Список сайтов пуст");
        }

        sitesSet.removeIf(site -> !site.getStatus().equals(SiteStatus.INDEXED));

        if (sitesSet.isEmpty()) {
            throw new IOException("Отсутствуют проиндексированные сайты");
        }

        return sitesSet;
    }

    /**
     * получение объетов Page для добавления данных к объектам SearchResultProcessor после ограничения количества результатов поиска
     *
     * @param searchResultProcessors - список объектов SearchResultProcessor
     * @return Map : Key = id страницы, value = объект Page (страница)
     */
    private Map<Integer, Page> getPagesBySearchResults(List<SearchResultProcessor> searchResultProcessors) {
        Map<Integer, Page> foundPagesMap = new HashMap<>();

        Set<Integer> pageIds = searchResultProcessors.stream().map(searchResultProcessor -> searchResultProcessor.getPageId()).collect(Collectors.toSet());
        Iterable<Page> foundPages = pageService.findAllById(pageIds);

        for (Page curPage : foundPages) {
            foundPagesMap.put(curPage.getId(), curPage);
        }

        return foundPagesMap;
    }

    /**
     * добавление данных к объектам SearchResultProcessor после ограничения количества результатов поиска
     * добавляемые данные:
     * - HTML документ страницы;
     * - ссылка на страницу
     */
    private void addPagesData() {
        List<SearchResultProcessor> searchResultProcessorList = SearchResultProcessor.getSearchResultList();

        Map<Integer, Page> pagesMap = getPagesBySearchResults(searchResultProcessorList);

        for (SearchResultProcessor curSearchResultProcessor : searchResultProcessorList) {
            Page curPage = pagesMap.get(curSearchResultProcessor.getPageId());
            if (null != curPage) {
                Document htmlDocument = Jsoup.parse(curPage.getPageContent());
                curSearchResultProcessor.setHtmlDocument(htmlDocument);
                curSearchResultProcessor.setPageUrl(curPage.getPageUrl());
            }
        }
    }
}