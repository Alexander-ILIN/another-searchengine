package searchengine.model.dao.non_standard_operations;

import searchengine.model.entity.Page;
import java.util.List;

/**
 * интерфейс, описывающий операции со страницами в БД
 * данные операции отсутствуют в интерфейсе CrudRepository
 */
public interface PageNonStandardRepository
{
    /**
     * проверка, какие страницы из перечня уже существуют в БД
     * @param pages перечень страниц
     * @param siteId id сайта
     * @return список страниц, существующих в БД и относящихся к сайту с указанным id
     */
    List<Page> findByPagesAndSiteId(Iterable<Page> pages, int siteId);

    /**
     * поиск страниц по ссылке и id сайта
     * @param pageUrl ссылка на страницу
     * @param siteId id сайта
     * @return список страниц, имеющих указанный url и относящихся к сайту с указанным id
     */
    List<Page> findByUrlAndSiteId(String pageUrl, int siteId);

    /**
     * удаление всех страниц, относящихся к сайту с указанным id
     * @param siteId id сайта
     */
    void deleteBySiteId(int siteId);
}
