package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Леммы, встречающиеся в текстах
 */

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma", uniqueConstraints = {@UniqueConstraint(columnNames = {"lemma", "site_id"})},
        indexes = @Index(columnList = "lemma"))
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(name = "lemma", nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma; // нормальная форма слова

    @Column(name = "frequency", nullable = false)
    private int frequency; // количество страниц, на которых слово встречается хотя бы один раз


    public Lemma(String lemma, Integer frequency) {
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
