package searchengine.model.dao.standard_crud;


import org.springframework.data.repository.CrudRepository;
import searchengine.model.entity.Page;

/**
 * интерфейс, описывающий CRUD операции с индексированными страницами
 */

public interface PageRepository extends CrudRepository<Page, Integer>
{
    long countAllBySiteId(int siteId);
}