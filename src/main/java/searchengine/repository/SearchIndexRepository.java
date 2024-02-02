package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.SearchIndex;

import java.util.List;

/**
 * интерфейс, описывающий CRUD операции с поисковым индексом
 */

public interface SearchIndexRepository extends CrudRepository<SearchIndex, Integer> {
    List<SearchIndex> findByLemmaId(int lemmaId);

    List<SearchIndex> findByPageId(int pageId);
}