package searchengine.services.db.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.dao.non_standard_operations.PageNonStandardRepository;
import searchengine.model.dao.standard_crud.PageRepository;
import searchengine.model.entity.Page;
import searchengine.services.db.PageService;
import java.util.List;
import java.util.Optional;

/**
 * класс, реализующий запуск операций со страницами в БД
 * для запуска операций используются интерфейсы PageRepository и PageNonStandardRepository
 */
@Component
class PageServiceImpl implements PageService
{
    private PageRepository pageRepository;

    private PageNonStandardRepository pageNonStandardRepository;

    @Autowired
    public PageServiceImpl(PageRepository pageRepository, PageNonStandardRepository pageNonStandardRepository)
    {
        this.pageRepository = pageRepository;
        this.pageNonStandardRepository = pageNonStandardRepository;
    }

    @Override
    public Iterable<Page> saveAll(Iterable<Page> pages)
    {
        return pageRepository.saveAll(pages);
    }


    @Override
    public long count()
    {
        return pageRepository.count();
    }

    @Override
    public List<Page> findByPagesAndSiteId(Iterable<Page> pages, int siteId)
    {
        return pageNonStandardRepository.findByPagesAndSiteId(pages, siteId);
    }

    @Override
    public long countAllBySiteId(int siteId)
    {
        return pageRepository.countAllBySiteId(siteId);

    }

    @Override
    public List<Page> findByUrlAndSiteId(String pageUrl, int siteId)
    {
        return pageNonStandardRepository.findByUrlAndSiteId(pageUrl, siteId);
    }

    @Override
    public void deleteBySiteId(int siteId)
    {
        pageNonStandardRepository.deleteBySiteId(siteId);
    }

    @Override
    public Iterable<Page> findAllById(Iterable<Integer> ids)
    {
        return pageRepository.findAllById(ids);
    }
}
