package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.SearchIndex;
import searchengine.repository.SearchIndexNonStandardRepository;
import searchengine.repository.SearchIndexRepository;

import java.util.List;
import java.util.Map;

/**
 * класс, реализующий запуск операций с индексами страниц в БД
 * для запуска операций используются интерфейсы SearchIndexRepository и SearchIndexNonStandardRepository
 */
@Service
class SearchIndexServiceImpl implements SearchIndexService {
    private final SearchIndexRepository searchIndexRepository;

    private final SearchIndexNonStandardRepository searchIndexNonStandardRepository;

    @Autowired
    public SearchIndexServiceImpl(SearchIndexRepository searchIndexRepository, SearchIndexNonStandardRepository searchIndexNonStandardRepository) {
        this.searchIndexRepository = searchIndexRepository;
        this.searchIndexNonStandardRepository = searchIndexNonStandardRepository;
    }

    @Override
    public List<SearchIndex> findByLemmaId(int lemmaId) {
        return searchIndexRepository.findByLemmaId(lemmaId);
    }

    @Override
    public void saveIndexes(Map<String, Integer> lemmasMap, Map<String, Integer> rankedPageLemmasMap, Integer pageId) {
        searchIndexNonStandardRepository.saveIndexes(lemmasMap, rankedPageLemmasMap, pageId);
    }

    @Override
    public List<SearchIndex> findByPageId(int pageId) {
        return searchIndexRepository.findByPageId(pageId);
    }

    @Override
    public void deleteByPageId(int pageId) {
        searchIndexNonStandardRepository.deleteByPageId(pageId);
    }


    @Override
    public void deleteBySiteId(int siteId) {
        searchIndexNonStandardRepository.deleteBySiteId(siteId);
    }
}
