package searchengine.model.dao.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Config;
import searchengine.model.dao.non_standard_operations.PageNonStandardRepository;
import searchengine.model.entity.Page;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.StringJoiner;

/**
 * класс, реализующий операции со страницами в БД
 * данные операции отсутствуют в интерфейсе CrudRepository
 */
@Transactional
@Repository
class PageNonStandardRepositoryImpl implements PageNonStandardRepository
{
    private EntityManager entityManager;

    private Config config;

    @Autowired
    public PageNonStandardRepositoryImpl(EntityManager entityManager, Config config)
    {
        this.entityManager = entityManager;
        this.config = config;
    }

    /**
     * проверка, какие страницы из перечня уже существуют в БД
     * @param pages перечень страниц
     * @param siteId id сайта
     * @return список страниц, существующих в БД и относящихся к сайту с указанным id
     */
    @Override
    public List<Page> findByPagesAndSiteId(Iterable<Page> pages, int siteId)
    {
        StringBuilder sqlQry = new StringBuilder();

        String condDelimiter = "', '";
        String condPrefix = "('";
        String condSuffix = "')";

        StringJoiner qryCond = new StringJoiner(condDelimiter, condPrefix, condSuffix);

        for(Page curPage : pages)
        {
            qryCond.add(curPage.getPageUrl());
        }

        sqlQry.append("FROM Page WHERE pageUrl IN ");
        sqlQry.append(qryCond.toString());
        sqlQry.append(" AND siteId = ");
        sqlQry.append(siteId);

        Query selectQuery = entityManager.createQuery(sqlQry.toString());
        List<Page> result =  selectQuery.getResultList();

        return result;
    }

    /**
     * поиск страниц по ссылке и id сайта
     * @param pageUrl ссылка на страницу
     * @param siteId id сайта
     * @return список страниц, имеющих указанный url и относящихся к сайту с указанным id
     */
    @Override
    public List<Page> findByUrlAndSiteId(String pageUrl, int siteId)
    {
        StringBuilder sqlQry = new StringBuilder();
        sqlQry.append("FROM Page WHERE pageUrl = '");
        sqlQry.append(pageUrl);
        sqlQry.append("' AND siteId = ");
        sqlQry.append(siteId);

        Query selectQuery = entityManager.createQuery(sqlQry.toString());
        List<Page> result =  selectQuery.getResultList();

        return result;
    }

    /**
     * удаление всех страниц, относящихся к сайту с указанным id
     * @param siteId id сайта
     */
    @Override
    public void deleteBySiteId(int siteId)
    {
        StringBuilder sqlQry = new StringBuilder();
        sqlQry.append("DELETE FROM Page WHERE siteId = ");
        sqlQry.append(siteId);

        Query deleteQuery = entityManager.createQuery(sqlQry.toString());
        int result =  deleteQuery.executeUpdate();
    }
}
