package searchengine.repository;


import org.springframework.data.repository.CrudRepository;
import searchengine.model.Page;

/**
 * интерфейс, описывающий CRUD операции с индексированными страницами
 */

public interface PageRepository extends CrudRepository<Page, Integer> {
    long countAllBySiteId(int siteId);
}