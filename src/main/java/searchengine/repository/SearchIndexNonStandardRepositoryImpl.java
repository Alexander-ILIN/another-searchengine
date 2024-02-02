package searchengine.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Config;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Map;
import java.util.StringJoiner;

/**
 * класс, реализующий операции с индексами страниц в БД
 * данные операции отсутствуют в интерфейсе CrudRepository
 */

@Transactional
@Repository
class SearchIndexNonStandardRepositoryImpl implements SearchIndexNonStandardRepository {
    private final EntityManager entityManager;

    private final Config config;

    @Autowired
    public SearchIndexNonStandardRepositoryImpl(EntityManager entityManager, Config config) {
        this.entityManager = entityManager;
        this.config = config;
    }

    /**
     * создание и сохранение индексов в БД
     *
     * @param lemmasMap           Map: key = значение леммы (String), value = id леммы в БД (Integer)
     * @param rankedPageLemmasMap Map: key = значение леммы (String), value = ранг леммы на странице (Float)
     * @param pageId              id страницы
     */
    @Override
    public void saveIndexes(Map<String, Integer> lemmasMap, Map<String, Integer> rankedPageLemmasMap, Integer pageId) {
        int bufferSize = config.getIndexBufferSize();

        String qryDelimiterInsert = "), (";
        String qryPrefixInsert = "INSERT INTO search_index (page_id, lemma_id, lemma_rank) VALUES (";
        String qrySuffixInsert = ")";

        int bufferCounter = 0;
        int totalCounter = 0;

        int lemmasQty = rankedPageLemmasMap.size();
        StringJoiner sqlConditionsInsert = new StringJoiner(qryDelimiterInsert, qryPrefixInsert, qrySuffixInsert);

        for (String currentLemmaStr : rankedPageLemmasMap.keySet()) {
            StringJoiner curValues = new StringJoiner(", ");

            Integer currentLemmaId = lemmasMap.get(currentLemmaStr);
            Integer currentLemmaRank = rankedPageLemmasMap.get(currentLemmaStr);

            curValues.add(pageId.toString());
            curValues.add(currentLemmaId.toString());
            curValues.add(currentLemmaRank.toString());

            sqlConditionsInsert.add(curValues.toString());

            ++bufferCounter;
            ++totalCounter;

            if (bufferCounter >= bufferSize || totalCounter >= lemmasQty) {
                Query insertQuery = entityManager.createNativeQuery(sqlConditionsInsert.toString());
                insertQuery.executeUpdate();
                bufferCounter = 0;
                sqlConditionsInsert = new StringJoiner(qryDelimiterInsert, qryPrefixInsert, qrySuffixInsert);
            }
        }
    }

    /**
     * удаление всех индексов, относящихся к странице
     *
     * @param pageId id страницы
     */
    @Override
    public void deleteByPageId(int pageId) {
        StringBuilder sqlQry = new StringBuilder();
        sqlQry.append("DELETE FROM SearchIndex WHERE pageId = ");
        sqlQry.append(pageId);

        Query deleteQuery = entityManager.createQuery(sqlQry.toString());
        int result = deleteQuery.executeUpdate();
    }

    /**
     * удаление всех индексов, относящихся к сайту
     *
     * @param siteId id сайта
     */
    @Override
    public void deleteBySiteId(int siteId) {
        StringBuilder sqlQry = new StringBuilder();
        sqlQry.append("DELETE FROM SearchIndex WHERE pageId IN (SELECT id FROM Page WHERE site_id = ");
        sqlQry.append(siteId);
        sqlQry.append(")");

        Query deleteQuery = entityManager.createQuery(sqlQry.toString());
        int result = deleteQuery.executeUpdate();
    }

}
