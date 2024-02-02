package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Поисковый индекс
 */

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "search_index")
public class SearchIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private int id;

    @Column(name = "page_id", nullable = false)
    private int pageId; // Идентификатор страницы

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId; // Идентификатор леммы

    @Column(name = "lemma_rank", nullable = false)
    private float rank; // Ранг леммы на странице


    public SearchIndex(int pageId, int lemmaId, float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
}
