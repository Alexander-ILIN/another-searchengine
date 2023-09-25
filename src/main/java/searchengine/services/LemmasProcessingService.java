package searchengine.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Интерфейс использующийся для получения:
 *  1. лемм всех слов из заданного текста и их повторений в тексте;
 *  2. лемм всех слов из заданного текста;
 *  3. лемм заданного слова;
 */
public interface LemmasProcessingService
{
    /**
     * Метод выполняет разделение текста на слова, получение лемм их свойств для каждого слова
     * @param text текст
     * @return Map, содержащий леммы и количества их повторений в тексте
     */
    Map<String, Integer> getTextLemmasWithFreq(String text);

    /**
     * Метод выполняет разделение текста на слова, получение лемм их свойств для каждого слова
     * @param text текст
     * @return Set, содержащий леммы, встречающиеся в тексте
     */
    Set<String> getTextLemmas(String text);

    /**
     * Метод возвращает список лемм слова на английском или русском языках.
     * Если леммы слова не найдены, то возвращается пустой список
     * @param word - слово
     * @return список нормальных форм слова
     */
    List<String> getWordLemmas(String word);
}