package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.repository.SiteRepository;
import searchengine.model.Site;

import java.util.List;

/**
 * класс, реализующий запуск операций с сайтами в БД
 * для запуска операций используется интерфейс SiteRepository
 */
@Service
class SiteServiceImpl implements SiteService
{
    private SiteRepository siteRepository;

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
