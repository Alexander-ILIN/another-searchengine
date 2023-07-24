package searchengine.services.mapping_indexing;

import searchengine.model.entity.Page;

/**
 * интерфейс, использующийся для индексации страниц и сохранения информации о леммах и индксах
 */
public interface PageIndexingService
{
    /**
     * запуск индексации страницы
     * @param page - экземпляр класса Page, который необходимо проиндексировать
     */
    void indexPage(Page page);

    /**
     * удаление / обновление информации при обновлении отдельной страницы
     * @param page страница
     */
    void deletePageIndexData(Page page);

    /**
     * удаление из БД данных, относящихся к сайту
     * @param siteId id сайта, данные о котором необходимо удалить
     */
    void removeSiteIndexData(int siteId);
}
