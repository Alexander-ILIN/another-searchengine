package searchengine.services;


import searchengine.model.SearchIndex;

import java.util.List;
import java.util.Map;

/**
 * интерфейс, позволяющий запускать операции с индексами страниц в БД
 */
public interface SearchIndexService
{
    /**
     * поиск индексов страниц по id леммы
     * @param lemmaId id леммы
     * @return список объектов SearchIndex, относящихся к лемме с заданным id
     */
    List<SearchIndex> findByLemmaId(int lemmaId);

    /**
     * создание и сохранение индексов в БД
     * @param lemmasMap Map: key = значение леммы (String), value = id леммы в БД (Integer)
     * @param rankedPageLemmasMap Map: key = значение леммы (String), value = ранг леммы на странице (Float)
     * @param pageId id страницы
     */
    void saveIndexes(Map<String, Integer> lemmasMap, Map <String, Integer> rankedPageLemmasMap, Integer pageId);

    /**
     * поиск индексов страниц по id страницы
     * @param pageId id страницы
     * @return списое объектов SearchIndex, относящихся к странице с заданным id
     */
    List<SearchIndex> findByPageId(int pageId);

    /**
     * удаление всех индексов, относящихся к странице
     * @param pageId id страницы
     */
    void deleteByPageId(int pageId);

    /**
     * удаление всех индексов, относящихся к сайту
     * @param siteId id сайта
     */
    void deleteBySiteId(int siteId);
}
