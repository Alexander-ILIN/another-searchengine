package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.repository.PageNonStandardRepository;
import searchengine.repository.PageRepository;

import java.util.List;

/**
 * класс, реализующий запуск операций со страницами в БД
 * для запуска операций используются интерфейсы PageRepository и PageNonStandardRepository
 */
@Service
class PageServiceImpl implements PageService {
    private final PageRepository pageRepository;

    private final PageNonStandardRepository pageNonStandardRepository;

    @Autowired
    public PageServiceImpl(PageRepository pageRepository, PageNonStandardRepository pageNonStandardRepository) {
        this.pageRepository = pageRepository;
        this.pageNonStandardRepository = pageNonStandardRepository;
    }

    @Override
    public Iterable<Page> saveAll(Iterable<Page> pages) {
        return pageRepository.saveAll(pages);
    }


    @Override
    public long count() {
        return pageRepository.count();
    }

    @Override
    public List<Page> findByPagesAndSiteId(Iterable<Page> pages, int siteId) {
        return pageNonStandardRepository.findByPagesAndSiteId(pages, siteId);
    }

    @Override
    public long countAllBySiteId(int siteId) {
        return pageRepository.countAllBySiteId(siteId);

    }

    @Override
    public List<Page> findByUrlAndSiteId(String pageUrl, int siteId) {
        return pageNonStandardRepository.findByUrlAndSiteId(pageUrl, siteId);
    }

    @Override
    public void deleteBySiteId(int siteId) {
        pageNonStandardRepository.deleteBySiteId(siteId);
    }

    @Override
    public Iterable<Page> findAllById(Iterable<Integer> ids) {
        return pageRepository.findAllById(ids);
    }
}
