package searchengine.dto.response;

/**
 * Класс ответа в случае удачного завершения запроса
 */
public class ResponseSuccess extends Response
{
    public ResponseSuccess(boolean result)
    {
        super(result);
    }
}
