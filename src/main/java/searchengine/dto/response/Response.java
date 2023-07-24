package searchengine.dto.response;

import lombok.Data;

/**
 * Абстрактный класс ответов на запросы
 */

@Data
public abstract class Response
{
    // Результат
    private final boolean result;

}
