package searchengine.dto.response;

import lombok.Getter;

/**
 * Класс ответа в случае неудачи
 */

@Getter
public class ResponseFail extends Response {
    // Описание ошибки
    private final String error;

    public ResponseFail(boolean result, String error) {
        super(result);
        this.error = error;
    }
}
