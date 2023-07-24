package searchengine.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Проиндексированная страница
 */

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "page", indexes = @Index(columnList = "path"))
public class Page implements Comparable<Page>
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(name = "path", nullable = false, columnDefinition = "VARCHAR(255)")
    private String pageUrl; // ссылка на текущую страницу

    @Column(name = "code" ,nullable = false)
    private int responseCode; // код ответа

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String pageContent; // контент страницы


    public Page(String pageUrl, int responseCode, String pageContent, int siteId)
    {
        this.pageUrl = pageUrl;
        this.responseCode = responseCode;
        this.pageContent = pageContent;
        this.siteId = siteId;
    }

    @Override
    public int compareTo(Page page)
    {
        return (this.pageUrl).compareTo(page.pageUrl);
    }
}