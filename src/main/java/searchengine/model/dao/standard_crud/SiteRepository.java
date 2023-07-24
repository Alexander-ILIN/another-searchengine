package searchengine.model.dao.standard_crud;


import org.springframework.data.repository.CrudRepository;
import searchengine.model.entity.Site;
import java.util.List;

/**
 * интерфейс, описывающий CRUD операции с сайтами
 */
public interface SiteRepository extends CrudRepository<Site, Integer>
{
    List<Site> findByUrl(String url);
}
