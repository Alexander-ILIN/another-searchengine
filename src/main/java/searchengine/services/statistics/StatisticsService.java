package searchengine.services.statistics;


import searchengine.dto.ResponseWrapper;

/**
 * Интерфейс, использующийся для сбора статистики по индексации сайтов
 */
public interface StatisticsService
{
    /**
     * Сбор статистики индексации: общей и по каждому сайту
     * @return объект ResponseWrapper: HTTP статус и Response содержащий в себе объект Statistics
     */
    ResponseWrapper getStatistics();
}
