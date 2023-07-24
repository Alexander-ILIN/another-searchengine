package searchengine.services.db;

import searchengine.model.entity.Lemma;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * интерфейс, позволяющий запускать операции с леммами в БД
 */
public interface LemmaService
{
    /**
     * получение сочетания значение леммы - id леммы
     * @param lemmaStrings коллекция, содержащая значения лемм (String)
     * @param siteId id сайта
     * @return Map: key = значение леммы (String), value = id леммы в БД (Integer)
     */
    Map<String, Integer> getLemmasByStrings (Collection<String> lemmaStrings, int siteId);

    /**
     * поиск объектов лемм в БД по значениям (String) и id сайта (int)
     * @param lemmaStrings значения лемм
     * @param siteId id сайта
     * @return список объектов лемм, удовлетворяющих входным параметрам
     */
    List<Lemma> findLemmas(Collection<String> lemmaStrings, int siteId);

    /**
     * получение количества лемм в БД
     * @return количество лемм в БД
     */
    long count();

    /**
     * получение количества лемм, относящихся к сайту
     * @param siteId id сайта
     * @return количество лемм, относящихся к сайту
     */
    long countAllBySiteId(int siteId);

    /**
     * получение перечня лемм по их идентификаторам
     * @param ids перечень id лемм
     * @return перечень лемм с заданными id
     */
    Iterable<Lemma> findAllById(Iterable<Integer> ids);

    /**
     * удаление леммы из БД
     * @param lemma объект класса Lemma, который нужно удалить
     */
    void delete(Lemma lemma);

    /**
     * сохранение перечня лемм в БД
     * @param lemmas перечень лемм для сохранения в БД
     * @return перечень лемм, сохранённых в БД
     */
    Iterable<Lemma> saveAll(Iterable<Lemma> lemmas);

    /**
     * удаление всех лемм, относящихся к сайту с указанным id
     * @param siteId id сайта
     */
    void deleteBySiteId(int siteId);
}
