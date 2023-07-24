package searchengine.model.dao.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Config;
import searchengine.model.dao.non_standard_operations.LemmaNonStandardRepository;
import searchengine.model.entity.Lemma;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

/**
 * класс, реализующий операции с леммами в БД
 * данные операции отсутствуют в интерфейсе CrudRepository
 */
@Repository
@Transactional
class LemmaNonStandardRepositoryImpl implements LemmaNonStandardRepository
{
    private EntityManager entityManager;

    private Config config;

    @Autowired
    public LemmaNonStandardRepositoryImpl(EntityManager entityManager, Config config)
    {
        this.entityManager = entityManager;
        this.config = config;
    }

    /**
     * получение сочетания значение леммы - id леммы
     * @param lemmaStrings коллекция, содержащая значения лемм (String)
     * @param siteId id сайта
     * @return Map: key = значение леммы (String), value = id леммы в БД (Integer)
     */
    @Override
    public Map<String, Integer> getLemmasByStrings(Collection<String> lemmaStrings, int siteId)
    {
        Map<String, Integer> lemmasMap = new HashMap<>();

        if(lemmaStrings != null && lemmaStrings.size() != 0)
        {
            Set<String> requestedLemmasStrings = lemmaStrings.stream().collect(Collectors.toSet());
            List<Lemma> savedLemmas = findSaveUpdateLemmasInDB(requestedLemmasStrings, siteId);
            lemmasMap = createLemmasMap(savedLemmas);
        }

        return lemmasMap;
    }


    /**
     * сохранение лемм в БД и получение их списка
     * при сохранении лемм, которые уже существуют в БД обновляется значение частоты, с которой лемма встречается на странице
     * @param lemmaStrings коллекция, содержащая значения лемм (String)
     * @param siteId id сайта, к которому относится лемма
     * @return список объектов лемм, сохранённых в БД
     */
    private List<Lemma> findSaveUpdateLemmasInDB(Collection<String> lemmaStrings, int siteId)
    {
        int bufferSize = config.getLemmaBufferSize();

        List<Lemma> foundResults = new ArrayList<>();

        String condDelimiter = "', ";
        String condPrefix = "'";
        String condSuffix = "";

        String qryDelimiterUpdate = ", 1), (";
        String qryPrefixUpdate = "INSERT INTO lemma (lemma, site_id, frequency) VALUES (";
        String qrySuffixUpdate = ", 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1";

        String qryDelimiterSelect = "', '";
        String qryPrefixSelect = "FROM Lemma WHERE lemma in ('";
        String qrySuffixSelect = "') AND siteId = " + siteId;

        int bufferCounter = 0;
        int totalCounter = 0;

        int lemmasQty = lemmaStrings.size();
        StringJoiner sqlConditionsUpdate = new StringJoiner(qryDelimiterUpdate, qryPrefixUpdate, qrySuffixUpdate);
        StringJoiner sqlConditionsSelect = new StringJoiner(qryDelimiterSelect, qryPrefixSelect, qrySuffixSelect);
        for(String currentLemma : lemmaStrings)
        {
            StringJoiner subCondUpdate = new StringJoiner(condDelimiter, condPrefix, condSuffix);
            subCondUpdate.add(currentLemma);
            subCondUpdate.add(String.valueOf(siteId));

            sqlConditionsUpdate.add(subCondUpdate.toString());
            sqlConditionsSelect.add(currentLemma);
            ++bufferCounter;
            ++totalCounter;

            if (bufferCounter >= bufferSize || totalCounter >= lemmasQty)
            {
                Query updateQuery = entityManager.createNativeQuery(sqlConditionsUpdate.toString());
                Query selectQuery = entityManager.createQuery(sqlConditionsSelect.toString());
                updateQuery.executeUpdate();
                List<Lemma> result =  selectQuery.getResultList();
                foundResults.addAll(result);
                bufferCounter = 0;
                sqlConditionsUpdate = new StringJoiner(qryDelimiterUpdate, qryPrefixUpdate, qrySuffixUpdate);
                sqlConditionsSelect = new StringJoiner(qryDelimiterSelect, qryPrefixSelect, qrySuffixSelect);
            }
        }

        return foundResults;
    }

    /**
     * получение сочетания значение леммы - id леммы из списка объектов лемм
     * @param lemmas список объектов лемм
     * @return Map: key = значение леммы (String), value = id леммы в БД (Integer)
     */
    private Map<String, Integer> createLemmasMap (List<Lemma> lemmas)
    {
        Map<String, Integer> foundLemmasMap = new HashMap<>();

        for (Lemma currentLemma : lemmas)
        {

            foundLemmasMap.put(currentLemma.getLemma(), currentLemma.getId());
        }

        return foundLemmasMap;
    }

    /**
     * Поиск объектов лемм в БД по значениям (String) и id сайта (int)
     * @param lemmaStrings значения лемм
     * @param siteId id сайта
     * @return список объектов лемм, удовлетворяющих входным параметрам
     */
    @Override
    public List<Lemma> findLemmas(Collection<String> lemmaStrings, int siteId)
    {
        StringBuilder sqlQry = new StringBuilder();

        String qryDelimiterSelect = "', '";
        String qryPrefixSelect = "FROM Lemma WHERE lemma in ('";
        String qrySuffixSelect = "')";

        StringJoiner sqlConditionsSelect = new StringJoiner(qryDelimiterSelect, qryPrefixSelect, qrySuffixSelect);
        for(String currentLemma : lemmaStrings)
        {
            sqlConditionsSelect.add(currentLemma);
        }

        sqlQry.append(sqlConditionsSelect.toString());

        if(siteId != -1)
        {
            sqlQry.append(" AND siteId = ");
            sqlQry.append(siteId);
        }

        sqlQry.append(" ORDER BY frequency");

        Query selectQuery = entityManager.createQuery(sqlQry.toString());
        List<Lemma> result =  selectQuery.getResultList();

        return result;
    }

    /**
     * удаление всех лемм, относящихся к сайту с указанным id
     * @param siteId id сайта
     */
    @Override
    public void deleteBySiteId(int siteId)
    {
        StringBuilder sqlQry = new StringBuilder();
        sqlQry.append("DELETE FROM Lemma WHERE siteId = ");
        sqlQry.append(siteId);

        Query deleteQuery = entityManager.createQuery(sqlQry.toString());
        int result =  deleteQuery.executeUpdate();
    }
}
