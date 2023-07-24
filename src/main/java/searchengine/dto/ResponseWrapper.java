package searchengine.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;
import searchengine.dto.response.Response;

@Data
public class ResponseWrapper
{
    private final HttpStatus httpStatus;

    private final Response response;
}
