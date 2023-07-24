package searchengine.dto.response;

import lombok.Getter;
import searchengine.dto.statistics.Statistics;

/**
 * Класс ответа на запрос статистики по сайтам
 */

@Getter
public class ResponseStatistics extends Response
{
    // Статистические данные
    private final Statistics statistics;

    public ResponseStatistics(boolean result, Statistics statistics)
    {
        super(result);
        this.statistics = statistics;
    }
}
