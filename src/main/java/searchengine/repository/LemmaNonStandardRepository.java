package searchengine.repository;

import searchengine.model.Lemma;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * интерфейс, описывающий операции с леммами в БД
 * данные операции отсутствуют в интерфейсе CrudRepository
 */
public interface LemmaNonStandardRepository {
    /**
     * получение сочетания значение леммы - id леммы
     *
     * @param lemmaStrings коллекция, содержащая значения лемм (String)
     * @param siteId       id сайта
     * @return Map: key = значение леммы (String), value = id леммы в БД (Integer)
     */
    Map<String, Integer> getLemmasByStrings(Collection<String> lemmaStrings, int siteId);

    /**
     * поиск объектов лемм в БД по значениям (String) и id сайта (int)
     *
     * @param lemmaStrings значения лемм
     * @param siteId       id сайта
     * @return список объектов лемм, удовлетворяющих входным параметрам
     */
    List<Lemma> findLemmas(Collection<String> lemmaStrings, int siteId);

    /**
     * удаление всех лемм, относящихся к сайту с указанным id
     *
     * @param siteId id сайта
     */
    void deleteBySiteId(int siteId);
}
