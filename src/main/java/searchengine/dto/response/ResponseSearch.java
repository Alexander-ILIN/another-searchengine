package searchengine.dto.response;


import lombok.Getter;
import searchengine.dto.search_result.SearchResultData;

/**
 * Класс ответа на поисковый запрос
 */
@Getter
public class ResponseSearch extends Response {
    // Количество результатов
    private final int count;

    // Данные ответа
    private final SearchResultData[] data;

    public ResponseSearch(boolean result, int count, SearchResultData[] data) {
        super(result);
        this.count = count;
        this.data = data;
    }
}
