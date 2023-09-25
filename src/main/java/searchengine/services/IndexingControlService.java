package searchengine.services;

import searchengine.dto.ResponseWrapper;

/**
 * Интерфейс, использующийся для запуска и остановки процесса индексации всех сайтов из конфигурационного файла
 */
public interface IndexingControlService
{
    /**
     * Запуск индексации выбранного сайта / всех сайтов из конфигурационного файла
     * @param siteUrl ссылка на сайт. Если null, то индексируются все сайты
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если новый процесс индексации был запущен;
     * false, если ещё не закончен текущий процесс индексации
     */
    ResponseWrapper launchSitesIndexing(String siteUrl);

    /**
     * Остановка процесса индексации
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если текущая индексация была остановлена;
     * false, если процесс индексации не был запущен
     */
    ResponseWrapper stopSitesIndexing();

    /**
     * Метод определяет, есть ли незавершённые задачи в списке задач по индексации сайтов (indexingFutureList)
     * @return true, если в indexingFutureList есть незавершённые задачи; false в обратном случае
     */
    boolean isIndexingInProgress();

    /**
     * Запуск добавления или обновления отдельной страницы
     * @param pageUrl ссылка на страницу
     * @return объект ResponseWrapper: HTTP статус и Response со значением true, если страница была успешно обновлена или добавлена;
     * со значением false, если в процессе произошла ошибка
     */
    ResponseWrapper singlePageIndexing(String pageUrl);

}
