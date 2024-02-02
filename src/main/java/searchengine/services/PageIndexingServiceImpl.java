package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

import java.util.*;
import java.util.stream.Collectors;

/**
 * класс, использующийся для индексации страниц и сохранения информации о леммах и индксах
 */
@Service
@Scope("prototype")
class PageIndexingServiceImpl implements PageIndexingService {
    private final LemmasProcessingService lemmasProcessingService;

    private final LemmaService lemmaService;

    private final SearchIndexService searchIndexService;

    private final Config config;

    @Autowired
    public PageIndexingServiceImpl(LemmasProcessingService lemmasProcessingService, LemmaService lemmaService,
                                   SearchIndexService searchIndexService, Config config) {
        this.lemmasProcessingService = lemmasProcessingService;
        this.lemmaService = lemmaService;
        this.searchIndexService = searchIndexService;
        this.config = config;
    }

    /**
     * запуск индексации страницы
     *
     * @param page - экземпляр класса Page, который необходимо проиндексировать
     */
    @Override
    public void indexPage(Page page) {
        Map<String, Integer> rankedPageLemmasMap;

        String content = page.getPageContent();
        Document htmlDocument = Jsoup.parse(content);
        int pageId = page.getId();
        int siteId = page.getSiteId();

        rankedPageLemmasMap = createRankedPageLemmasMap(htmlDocument);

        saveLemmasAndIndex(rankedPageLemmasMap, pageId, siteId);
    }

    /**
     * удаление / обновление информации при обновлении отдельной страницы
     * поиск и удаление объектов SearchIndex по pageId;
     * поиск лемм по объектам SearchIndex
     *
     * @param page страница
     */
    @Override
    public void deletePageIndexData(Page page) {
        int pageId = page.getId();

        List<SearchIndex> pageSearchIndexes = searchIndexService.findByPageId(pageId);

        List<Integer> lemmasId = pageSearchIndexes.stream().map(SearchIndex::getLemmaId).collect(Collectors.toList());

        modifyOrRemoveLemmas(lemmasId);

        searchIndexService.deleteByPageId(pageId);
    }

    /**
     * удаление данных, относящихся к сайту, из таблиц: lemma, index
     *
     * @param siteId id сайта, данные о котором необходимо удалить
     */
    @Override
    public void removeSiteIndexData(int siteId) {
        searchIndexService.deleteBySiteId(siteId);
        lemmaService.deleteBySiteId(siteId);
    }

    /**
     * обновление / удаление лемм:
     * если частота встречаемости леммы равна 1, то лемма удаляется из базы данных;
     * если частота встречаемости леммы больше 1, то значение частоты уменьшается на 1
     *
     * @param lemmasId список id лемм, встречающихся на обновляемой странице
     */
    private void modifyOrRemoveLemmas(List<Integer> lemmasId) {
        int bufferSize = config.getLemmaBufferSize();
        List<Lemma> lemmasBuffer = new ArrayList<>();
        int lemmasCounter = 0;

        Iterable<Lemma> affectedLemmas = lemmaService.findAllById(lemmasId);

        if (affectedLemmas == null) {
            return;
        }

        for (Lemma curLemma : affectedLemmas) {
            int frequency = curLemma.getFrequency();

            if (frequency == 1) {
                lemmaService.delete(curLemma);
            } else {
                curLemma.setFrequency(--frequency);
                lemmasBuffer.add(curLemma);
                ++lemmasCounter;
            }
            if (lemmasCounter >= bufferSize) {
                lemmaService.saveAll(lemmasBuffer);
                lemmasBuffer.clear();
                lemmasCounter = 0;
            }
        }
        lemmaService.saveAll(lemmasBuffer);
        lemmasBuffer.clear();
    }

    /**
     * создание Map, содержащего леммы, встречающиеся на странице и их ранги
     *
     * @param htmlDocument
     * @return Map, содержащий леммы, встречающиеся на странице и их ранги
     */
    private Map<String, Integer> createRankedPageLemmasMap(Document htmlDocument) {
        Map<String, Integer> rankedPageLemmasMap = new HashMap<>();

        List<String> currentHtmlElementsTexts = htmlDocument.select("title, body").eachText();

        for (String htmlElementText : currentHtmlElementsTexts) {
            Map<String, Integer> lemmasMap = lemmasProcessingService.getTextLemmasWithFreq(htmlElementText);

            for (Map.Entry<String, Integer> lemmaEntry : lemmasMap.entrySet()) {
                String currentLemma = lemmaEntry.getKey();
                Integer currentRank = lemmaEntry.getValue();
                Integer newRank = rankedPageLemmasMap.
                        computeIfPresent(currentLemma, (key, val) -> val + currentRank);
                if (newRank == null) {
                    rankedPageLemmasMap.put(currentLemma, currentRank);
                }
            }
        }

        return rankedPageLemmasMap;
    }

    /**
     * вычисление ранга леммы для текущего поля
     *
     * @param lemmasMap   Map с леммами слов, встречающихся на странице и их количествами на странице
     * @param fieldWeight релевантность (вес) текущего поля
     * @return Map, содержащий лемму и её ранг для текущего поля
     */
    private Map<String, Float> getRankedLemmasMap(Map<String, Integer> lemmasMap, float fieldWeight) {
        Map<String, Float> rankedLemmasMap = new HashMap<>();
        for (String lemma : lemmasMap.keySet()) {
            float lemmaRank = lemmasMap.get(lemma) * fieldWeight;
            rankedLemmasMap.put(lemma, lemmaRank);
        }
        return rankedLemmasMap;
    }

    /**
     * сохранение в БД лемм и индексов страниц
     *
     * @param rankedPageLemmasMap Map, содержащий леммы, встречающиеся на странице и их ранги
     * @param pageId              ID страницы
     */
    private void saveLemmasAndIndex(Map<String, Integer> rankedPageLemmasMap, int pageId, int siteId) {
        // Profiling
        long start = System.currentTimeMillis();
        Set<SearchIndex> searchIndexSet = new HashSet<>();

        Map<String, Integer> lemmasMap = lemmaService.
                getLemmasByStrings(rankedPageLemmasMap.keySet(), siteId);

        try {

            // Profiling
            long lemmasSaved = System.currentTimeMillis();
            System.out.println("page " + pageId + " lemmas saved " + (lemmasSaved - start) + " ms");


            // Profiling
            long indexPrepared = System.currentTimeMillis();

            searchIndexService.saveIndexes(lemmasMap, rankedPageLemmasMap, pageId);

            // Profiling
            long indexSaved = System.currentTimeMillis();
            System.out.println("page " + pageId + " index saved " + (indexSaved - indexPrepared) + " ms");

        } catch (Exception e) {
            System.out.println(e.getMessage() + " on page ID = " + pageId);
            return;
        }

        System.out.println("Indexes saved for page ID = " + pageId);
    }
}