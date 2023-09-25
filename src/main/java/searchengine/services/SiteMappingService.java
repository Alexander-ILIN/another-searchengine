package searchengine.services;

/**
 * интерфейс, используемый для сохранения страниц и запуска их индексации
 */
public interface SiteMappingService
{
    /**
     * создание страниц, добавление в буфер
     * при достижении заданного размера буфера, запуск их сохранения и индексации
     * @param pageUrl ссылка на страницу
     * @param responseCode код http ответа
     * @param pageContent содержание страницы
     * @return true - в случае успеха, false - в случае прерывания процесса индексации
     */
    boolean proceedWithPageData(String pageUrl, int responseCode, String pageContent, int siteId);
}
