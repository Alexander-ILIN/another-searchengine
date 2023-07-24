package searchengine.services.db.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.dao.standard_crud.SiteRepository;
import searchengine.model.entity.Site;
import searchengine.services.db.SiteService;
import java.util.List;

/**
 * класс, реализующий запуск операций с сайтами в БД
 * для запуска операций используется интерфейс SiteRepository
 */
@Component
class SiteServiceImpl implements SiteService
{
    SiteRepository siteRepository;

    @Autowired
    public SiteServiceImpl(SiteRepository siteRepository)
    {
        this.siteRepository = siteRepository;
    }

    @Override
    public Iterable<Site> saveAll(Iterable<Site> sites)
    {
        return siteRepository.saveAll(sites);
    }

    @Override
    public Iterable<Site> findAll()
    {
        return siteRepository.findAll();
    }

    @Override
    public void delete(Site site)
    {
        siteRepository.delete(site);
    }

    @Override
    public List<Site> findByUrl(String url)
    {
        return siteRepository.findByUrl(url);
    }

    @Override
    public long count()
    {
        return siteRepository.count();
    }

    @Override
    public Site save(Site site)
    {
        return siteRepository.save(site);
    }
}
