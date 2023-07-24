package searchengine.model.dao.standard_crud;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.entity.Lemma;

/**
 * интерфейс, описывающий CRUD операции с леммами
 */

public interface LemmaRepository extends CrudRepository<Lemma, Integer>
{
    long countAllBySiteId(int siteId);
}