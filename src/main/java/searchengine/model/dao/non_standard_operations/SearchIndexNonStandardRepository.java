package searchengine.model.dao.non_standard_operations;

import java.util.Map;

/**
 * интерфейс, описывающий операции с индексами страниц в БД
 * данные операции отсутствуют в интерфейсе CrudRepository
 */
public interface SearchIndexNonStandardRepository
{
    /**
     * создание и сохранение индексов в БД
     * @param lemmasMap Map: key = значение леммы (String), value = id леммы в БД (Integer)
     * @param rankedPageLemmasMap Map: key = значение леммы (String), value = ранг леммы на странице (Float)
     * @param pageId id страницы
     */
    void saveIndexes(Map<String, Integer> lemmasMap, Map <String, Integer> rankedPageLemmasMap, Integer pageId);

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
