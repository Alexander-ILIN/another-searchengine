package searchengine.model.dao.standard_crud;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.entity.SearchIndex;
import java.util.List;

/**
 * интерфейс, описывающий CRUD операции с поисковым индексом
 */

public interface SearchIndexRepository extends CrudRepository<SearchIndex, Integer>
{
    List<SearchIndex> findByLemmaId(int lemmaId);

    List<SearchIndex> findByPageId(int pageId);
}