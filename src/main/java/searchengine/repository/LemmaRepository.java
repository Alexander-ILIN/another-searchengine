package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Lemma;

/**
 * интерфейс, описывающий CRUD операции с леммами
 */

public interface LemmaRepository extends CrudRepository<Lemma, Integer>
{
    long countAllBySiteId(int siteId);
}