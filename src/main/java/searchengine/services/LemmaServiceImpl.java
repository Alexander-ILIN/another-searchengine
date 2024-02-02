package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.repository.LemmaNonStandardRepository;
import searchengine.repository.LemmaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * класс, реализующий запуск операций с леммами в БД
 * для запуска операций используются интерфейсы LemmaRepository и LemmaNonStandardRepository
 */
@Service
class LemmaServiceImpl implements LemmaService {
    private final LemmaNonStandardRepository lemmaNonStandardRepository;

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaServiceImpl(LemmaNonStandardRepository lemmaNonStandardRepository, LemmaRepository lemmaRepository) {
        this.lemmaNonStandardRepository = lemmaNonStandardRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public Map<String, Integer> getLemmasByStrings(Collection<String> lemmaStrings, int siteId) {
        return lemmaNonStandardRepository.getLemmasByStrings(lemmaStrings, siteId);
    }

    @Override
    public List<Lemma> findLemmas(Collection<String> lemmaStrings, int siteId) {
        return lemmaNonStandardRepository.findLemmas(lemmaStrings, siteId);
    }

    @Override
    public long count() {
        return lemmaRepository.count();
    }

    @Override
    public long countAllBySiteId(int siteId) {
        return lemmaRepository.countAllBySiteId(siteId);
    }


    @Override
    public Iterable<Lemma> findAllById(Iterable<Integer> ids) {
        return lemmaRepository.findAllById(ids);
    }

    @Override
    public void delete(Lemma lemma) {
        lemmaRepository.delete(lemma);
    }

    @Override
    public Iterable<Lemma> saveAll(Iterable<Lemma> lemmas) {
        return lemmaRepository.saveAll(lemmas);
    }

    @Override
    public void deleteBySiteId(int siteId) {
        lemmaNonStandardRepository.deleteBySiteId(siteId);
    }
}
