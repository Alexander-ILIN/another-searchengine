package searchengine.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.dto.response.Response;
import searchengine.dto.response.ResponseFail;
import searchengine.dto.response.ResponseSuccess;
import searchengine.dto.ResponseWrapper;
import searchengine.model.Site;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Класс, использующийся для запуска и остановки процесса индексации всех сайтов из конфигурационного файла
 */
@Service
@Log4j2
class IndexingControlServiceImpl implements IndexingControlService
{

    private final SiteService siteService;

    private final LoggingService loggingService;

    private List<Future<?>> indexingFutureList;

    private ExecutorService executor;

    private List<MappingIndexingService> siteProcessorList;


    @Autowired
    public IndexingControlServiceImpl(SiteService siteService, LoggingService loggingService)
    {
        this.siteService = siteService;
        this.loggingService = loggingService;
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
        String logMsg;

        if(null == siteUrl)
        {
            logMsg = "Индексация всех сайтов: запуск";
        }
        else
        {
            logMsg = "Индексация сайта " + siteUrl + " : запуск";
        }

        loggingService.logCustom(logMsg);

        HttpStatus httpStatus;
        Response response;

        try {
            if (!isIndexingInProgress())
            {
                startIndexingSitesProcess(siteUrl);

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
            response = new ResponseFail(false, "Ошибка при выполнении индексации");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Индексация: ошибка", ex);
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        loggingService.logCustom("Индексация: результат = " + response.isResult());

        return responseWrapper;
    }

    /**
     * Запуск процесса индексации выбранного сайта / всех сайтов из конфигурационного файла
     * @param siteUrl ссылка на сайт. Если null, то индексируются все сайты
     */
    private void startIndexingSitesProcess(String siteUrl)
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
    }


    /**
     * Остановка процесса индексации
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если текущая индексация была остановлена;
     * со значением false, если процесс индексации не удалось остановить
     */
    @Override
    public ResponseWrapper stopSitesIndexing()
    {
        loggingService.logCustom("Остановка индексации: запуск");

        Response response;
        HttpStatus httpStatus;

        try
        {
            if (isIndexingInProgress())
            {
                if (stopIndexingSitesProcess())
                {
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
            response = new ResponseFail(false, "Ошибка при остановке индексации");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Остановка индексации: ошибка", ex);
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(httpStatus, response);

        loggingService.logCustom("Остановка индексации: результат = " + response.isResult());

        return responseWrapper;
    }

    /**
     * Запуск остановки процесса индексации сайтов
     * @return false, время ожидания остановки индексации истекло; true, если индексация остановлена
     */
    private boolean stopIndexingSitesProcess()
    {
        int attemptsQty;
        int maxAttempts = 40;

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
            return true;
        }
        else
        {
            return false;
        }
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
        loggingService.logCustom("Индексация страницы " + pageUrl + " : запуск");

        ResponseWrapper responseWrapper;

        if(!isIndexingInProgress())
        {
            Future<Integer> indexingFuture = startSinglePageIndexingProcess(pageUrl);

            responseWrapper = getPageIndexingResponse(indexingFuture);
        }
        else
        {
            Response response = new ResponseFail(false, "Индексация уже запущена");
            HttpStatus httpStatus = HttpStatus.ACCEPTED;

            responseWrapper = new ResponseWrapper(httpStatus, response);
        }

        loggingService.logCustom("Индексация страницы: результат = " + responseWrapper.getResponse().isResult());

        return responseWrapper;
    }

    /**
     * Запуск процесса индексации отдельной страницы
     * @param pageUrl ссылка на страницу
     * @return объект Future, содержащий результат индексации страницы
     */
    private Future<Integer> startSinglePageIndexingProcess(String pageUrl)
    {
        ApplicationContext context = Application.getContext();

        indexingFutureList = new ArrayList<>();
        executor = Executors.newCachedThreadPool();
        MappingIndexingService mappingIndexingService =
                context.getBean(MappingIndexingService.class);

        Callable<Integer> indexingCallable = () -> mappingIndexingService.indexSinglePage(pageUrl);
        Future<Integer> indexingFuture = executor.submit(indexingCallable);
        indexingFutureList.add(indexingFuture);
        executor.shutdown();

        return indexingFuture;
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
                log.error(e);
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
                log.error("Индексация страницы: ошибка", ex);
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