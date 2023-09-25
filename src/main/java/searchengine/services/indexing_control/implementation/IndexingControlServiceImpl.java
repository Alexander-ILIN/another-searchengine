package searchengine.services.indexing_control.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.dto.response.Response;
import searchengine.dto.response.ResponseFail;
import searchengine.dto.response.ResponseSuccess;
import searchengine.dto.ResponseWrapper;
import searchengine.model.entity.Site;
import searchengine.services.UtilService;
import searchengine.services.indexing_control.IndexingControlService;
import searchengine.services.db.SiteService;
import searchengine.services.mapping_indexing.MappingIndexingService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Класс, использующийся для запуска и остановки процесса индексации всех сайтов из конфигурационного файла
 */
@Service
class IndexingControlServiceImpl implements IndexingControlService
{

    private SiteService siteService;

    private List<Future<?>> indexingFutureList;

    private ExecutorService executor;

    private List<MappingIndexingService> siteProcessorList;

    @Autowired
    public IndexingControlServiceImpl(SiteService siteService)
    {
        this.siteService = siteService;
    }

    /**
     * Запуск индексации выбранного сайта / всех сайтов из конфигурационного файла
     * @param siteUrl ссылка на сайт. Если null, то индексируются все сайты
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если новый процесс индексации был запущен;
     * со значением false, если ещё не закончен текущий процесс индексации
     */
    @Override
    public ResponseWrapper launchSitesIndexing(String siteUrl)
    {
        HttpStatus httpStatus;
        Response response;

        try {
            if (!isIndexingInProgress())
            {
                indexingFutureList = new ArrayList<>();
                siteProcessorList = new ArrayList<>();
                executor = Executors.newCachedThreadPool();

                Iterable<Site> sites = getSitesForIndexing(siteUrl);

                ApplicationContext context = Application.getContext();

                for (Site curSite : sites)
                {
                    MappingIndexingService mappingIndexingService =
                            context.getBean(MappingIndexingService.class);

                    mappingIndexingService.removeSiteData(curSite);

                    siteProcessorList.add(mappingIndexingService);

                    Runnable indexingRunnable = () -> mappingIndexingService.getAndIndexPages(curSite);

                    Future<?> indexingFuture = executor.submit(indexingRunnable);

                    indexingFutureList.add(indexingFuture);
                }

                executor.shutdown();

                response = new ResponseSuccess(true);
                httpStatus = HttpStatus.OK;

            }
            else
            {
                response = new ResponseFail(false, "Индексация уже запущена");
                httpStatus = HttpStatus.ACCEPTED;
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            response = new ResponseFail(false, "Ошибка при выполнении индексации");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        return responseWrapper;
    }

    /**
     * Остановка процесса индексации
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если текущая индексация была остановлена;
     * со значением false, если процесс индексации не удалось остановить
     */
    @Override
    public ResponseWrapper stopSitesIndexing()
    {
        Response response;
        HttpStatus httpStatus;
        int attemptsQty = 0;
        int maxAttempts = 40;

        try
        {
            if (isIndexingInProgress())
            {
                //Profiling
                long start = System.currentTimeMillis();

                for (MappingIndexingService siteProcessor : siteProcessorList)
                {
                    siteProcessor.terminate();
                }

                attemptsQty = waitForCompletion(500, maxAttempts);

                if (attemptsQty < maxAttempts)
                {
                    //Profiling
                    long end = System.currentTimeMillis();
                    System.out.println("Terminated " + (end - start) + " ms");

                    response = new ResponseSuccess(true);
                    httpStatus = HttpStatus.OK;
                }
                else
                {
                    response = new ResponseFail(false, "Не удалось остановить индексацию");
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                }
            }
            else
            {
                response = new ResponseFail(false, "Индексация не запущена");
                httpStatus = HttpStatus.ACCEPTED;
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            response = new ResponseFail(false, "Ошибка при остановке индексации");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        return responseWrapper;
    }

    /**
     * Метод определяет, есть ли незавершённые задачи в списке задач по индексации сайтов (indexingFutureList)
     * @return true, если в indexingFutureList есть незавершённые задачи; false в обратном случае
     */
    @Override
    public boolean isIndexingInProgress()
    {
        if(indexingFutureList == null)
        {
            return false;
        }

        long activeTasksQty = indexingFutureList.stream().filter(future -> !future.isDone()).count();

        return activeTasksQty > 0;
    }

    /**
     * Запуск добавления или обновления отдельной страницы
     * @param pageUrl ссылка на страницу
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если страница была успешно обновлена или добавлена;
     * со значением false, если в процессе произошла ошибка
     */
    @Override
    public ResponseWrapper singlePageIndexing(String pageUrl)
    {
        ResponseWrapper responseWrapper;

        ApplicationContext context = Application.getContext();

        if(!isIndexingInProgress())
        {
            indexingFutureList = new ArrayList<>();
            executor = Executors.newCachedThreadPool();
            MappingIndexingService mappingIndexingService =
                context.getBean(MappingIndexingService.class);

            Callable<Integer> indexingCallable = () -> mappingIndexingService.indexSinglePage(pageUrl);
            Future<Integer> indexingFuture = executor.submit(indexingCallable);
            indexingFutureList.add(indexingFuture);
            executor.shutdown();
            responseWrapper = getPageIndexingResponse(indexingFuture);
        }
        else
        {
            Response response = new ResponseFail(false, "Индексация уже запущена");
            HttpStatus httpStatus = HttpStatus.ACCEPTED;

            responseWrapper = new ResponseWrapper(httpStatus, response);
        }

        return responseWrapper;
    }

    /**
     * Проверка, осуществляется ли процесс индексации и приостановка главного потока
     * @param sleepTime время в мс
     * @param maxAttempts максимальное количество остановок
     * @return количество остановок потока
     */
    private int waitForCompletion(long sleepTime, int maxAttempts)
    {
        int attemptsQty = 0;
        while(isIndexingInProgress() && attemptsQty < maxAttempts)
        {
            try
            {
                Thread.sleep(sleepTime);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            ++attemptsQty;
        }
        return attemptsQty;
    }

    /**
     * Получение результата добавления или обновления отдельной страницы и создание объекта Response
     * @param indexingFuture объект Future
     * @return Response со значением true, если страница была успешно обновлена или добавлена;
     * со значением false, если в процессе произошла ошибка
     */
    private ResponseWrapper getPageIndexingResponse(Future<Integer> indexingFuture)
    {
        int maxAttempts = 150;
        Response response = new ResponseFail(false, "Не удалось завершить индексацию страницы");
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        int attemptsQty = waitForCompletion(2000, maxAttempts);

        if(attemptsQty < maxAttempts)
        {
            try
            {
                int result = indexingFuture.get();

                if(result == 1)
                {
                    response = new ResponseSuccess(true);
                    httpStatus = HttpStatus.OK;
                }
                else if(result == 0)
                {
                    response = new ResponseFail(false,
                            "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
                    httpStatus = HttpStatus.BAD_REQUEST;
                }
            }
            catch (Exception ex)
            {
                System.out.println(ex.getMessage());
            }
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        return responseWrapper;
    }

    /**
     * Получение объекта / объектов Site по ссылке
     * @param siteUrl ссылка на сайт
     * @return объект / объекты Site. Если входящая ссылка = null, то возвращаются все сайты из БД
     */
    private Iterable<Site> getSitesForIndexing(String siteUrl)
    {
        Iterable<Site> sites;
        if(siteUrl == null)
        {
            sites = siteService.findAll();
        }
        else
        {
            String urlForSearch = UtilService.getUrlWithSlash(siteUrl);

            sites = siteService.findByUrl(urlForSearch);
        }
        return sites;
    }
}