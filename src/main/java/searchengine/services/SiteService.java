package searchengine.services;

import searchengine.model.Site;

import java.util.List;

/**
 * интерфейс, позволяющий запускать операции с сайтами в БД
 */
public interface SiteService {
    /**
     * сохранение сайтов в БД
     *
     * @param sites перечень сайтов для сохранения
     * @return перечень сайтов, сохранённых в БД
     */
    Iterable<Site> saveAll(Iterable<Site> sites);

    /**
     * получение всех сайтов из БД
     *
     * @return перечень объектов класса Site, содержащихся в БД
     */
    Iterable<Site> findAll();

    /**
     * удаление сайта из БД
     *
     * @param site объект класса Site, который необходимо удалить из БД
     */
    void delete(Site site);

    /**
     * поиск сайта по ссылке
     *
     * @param url ссылка на сайт
     * @return список сайтов, имеющих заданный url
     */
    List<Site> findByUrl(String url);

    /**
     * получение количества сайтов, содержащихся в БД
     *
     * @return количество сайтов в БД
     */
    long count();

    /**
     * сохранение сайта
     *
     * @param site объект класса Site, который необходимо сохранить
     * @return сохранённый объект класса Site
     */
    Site save(Site site);
}
