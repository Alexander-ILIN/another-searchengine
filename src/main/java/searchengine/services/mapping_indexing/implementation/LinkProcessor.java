package searchengine.services.mapping_indexing.implementation;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.services.mapping_indexing.SiteMappingService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * класс, использующийся для запуска ForkJoinTask
 */
class LinkProcessor extends RecursiveAction
{
    private final String url;                     // ссылка на текущую страницу
    private final AuxSiteData auxSiteData;                 // экземпляр класса AuxSiteData. Содержит информацию о сайте, карту которого необходимо создать
    public static final String URL_CHECK_REGEX_1 = ".+((/#)|(\\?)).*";   // проверка наличия в ссылке символов "#" и "?"

    /**
     * конструктор класса
     * @param url ссылка на страницу
     * @param auxSiteData - сайт, к которому относится страница
     */
    public LinkProcessor(String url, AuxSiteData auxSiteData)
    {
        this.url = url;
        this.auxSiteData = auxSiteData;
    }

    /**
     * переопределение метода compute родительского класса RecursiveAction
     */
    @Override
    protected void compute()
    {
        if(auxSiteData.isTerminated())
        {
            return;
        }

        Set<String> allPageLinks; // сет ссылок текущей страницы
        List<LinkProcessor> tasksList = new ArrayList<>(); // список заданий для запуска ForkJoinTask

        try {
                // получение сета ссылок текущей страницы
                allPageLinks = getAllPageLinks(url, auxSiteData);

                for (String currentLink : allPageLinks) {
                    // проверка, использовалась ли уже ссылка для запуска ForkJoinTask
                    if (!auxSiteData.isUrlChecked(currentLink))
                    {
                        auxSiteData.addCheckedUrl(currentLink);
                        LinkProcessor task = new LinkProcessor(currentLink, auxSiteData);
                        tasksList.add(task);
                    }
                }

                ForkJoinTask.invokeAll(tasksList);

                System.out.println("Site #" + auxSiteData.getSiteId() + ": " + auxSiteData.getCheckedUrlsQty() + " pages have been proceeded...");
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * получение информации о странице: статус ответа, содержимое, список ссылок. Запуск записи информации о странице в БД.
     * @param currentUrl - ссылка на страницу
     * @param auxSiteData - сайт, к которому относится страница
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private Set<String> getAllPageLinks (String currentUrl, AuxSiteData auxSiteData) throws Exception
    {
        String rootUrl;
        Elements elementsWithLinks;
        
        Set<String> allPageLinks = new TreeSet<>();
        rootUrl = auxSiteData.getRootUrl();
        
        Document htmlDocument = getAndSavePageData(currentUrl);

        elementsWithLinks = htmlDocument.select("a[href]");

        for (Element element : elementsWithLinks)
        {
            String link = element.attr("href");

            if (!link.matches(URL_CHECK_REGEX_1 ))
            {
                if (link.startsWith(rootUrl))
                {
                    allPageLinks.add(link);
                }
                else if (link.startsWith("/"))
                {
                    allPageLinks.add(rootUrl + link);
                }
            }
        }
            
        return allPageLinks;
    }

    /**
     * получение кода ответа, body страницы и запуск их записи в базу данных
     * @param url ссылка на страницу
     * @return HTML документ страницы
     * @throws Exception
     */
    public Document getAndSavePageData(String url) throws Exception
    {
        Connection.Response response;
        int responseCode;
        String body;
        Document htmlDocument = new Document("");
        String exceptionMessage = null;
        
        try
        {
            String userAgent = this.auxSiteData.getUserAgent();
            String referrer = this.auxSiteData.getReferrer();
            response = Jsoup.connect(url).userAgent(userAgent).referrer(referrer).maxBodySize(0).execute();

            Thread.sleep(500);

            responseCode = response.statusCode();

            htmlDocument = response.parse();

            body = htmlDocument.toString();

        }
        catch (HttpStatusException ex)
        {
            responseCode = ex.getStatusCode();
            body = "";
            exceptionMessage = ex.getMessage();
        }

        String pageUrl;
        if(url.equals(auxSiteData.getRootUrl()))
        {
            pageUrl = "/";
        }
        else
        {
            pageUrl = url.substring(auxSiteData.getRootUrlLen());
        }

        SiteMappingService siteMappingService = this.auxSiteData.getSiteMapper();
        siteMappingService.proceedWithPageData(pageUrl, responseCode, body, auxSiteData.getSiteId());

        if(null != exceptionMessage)
        {
            throw new Exception(exceptionMessage);
        }

        return htmlDocument;
    }
}