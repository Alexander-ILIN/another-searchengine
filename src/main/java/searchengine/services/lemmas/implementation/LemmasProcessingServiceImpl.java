package searchengine.services.lemmas.implementation;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.services.lemmas.LemmasProcessingService;
import java.io.IOException;
import java.util.*;

/**
 * Класс использующийся для получения:
 *  1. лемм всех слов из заданного текста и их повторений в тексте;
 *  2. лемм всех слов из заданного текста;
 *  3. лемм заданного слова;
 */
@Service
class LemmasProcessingServiceImpl implements LemmasProcessingService
{
    private LuceneMorphology russianLuceneMorphology;
    private LuceneMorphology englishLuceneMorphology;

    // разбиение текста на слова
    private static final String WORDS_SPLIT_REGEX = "([^а-яА-Яa-zA-Z0-9]+)";

    // символы, обрабатываемые на страницах и в запросах
    private static final String ALLOWED_SYMBOLS_REGEX = "[а-яА-Яa-zA-Z0-9]+";

    // разделитель свойств в классах лемматизаторов
    private static final String PROPERTIES_SPLITTER = "|";

    // части речи английского и русского языков, которые необходимо исключать из списка лемм
    private final String ruConj = PartOfSpeech.CONJUNCTION.getRuDesc();
    private final String enConj = PartOfSpeech.CONJUNCTION.getEnDesc();
    private final String ruInt = PartOfSpeech.INTERJECTION.getRuDesc();
    private final String enInt = PartOfSpeech.INTERJECTION.getEnDesc();
    private final String ruPart = PartOfSpeech.PARTICLE.getRuDesc();
    private final String enPart = PartOfSpeech.PARTICLE.getEnDesc();
    private final String ruPrep = PartOfSpeech.PREPOSITION.getRuDesc();
    private final String enPrep = PartOfSpeech.PREPOSITION.getEnDesc();
    private final String enArt = PartOfSpeech.ARTICLE.getEnDesc();
    private final String ruPron = PartOfSpeech.PRONOUN.getRuDesc();
    private final String enPron = PartOfSpeech.PRONOUN.getEnDesc();


    /**
     * Конструктор класса
     * При создании экземпляра класса создаются экземпляры классов RussianLuceneMorphology и EnglishLuceneMorphology
     */
    public LemmasProcessingServiceImpl()
    {
        try
        {
            russianLuceneMorphology = new RussianLuceneMorphology();
            englishLuceneMorphology = new EnglishLuceneMorphology();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Метод выполняет разделение текста на слова, получение лемм их свойств для каждого слова
     * @param text текст
     * @return Map, содержащий леммы и количества их повторений в тексте
     */
    @Override
    public Map<String, Integer> getTextLemmasWithFreq(String text)
    {
        Map<String, Integer> lemmasMap = new HashMap<>();

        String[] words = text.split(WORDS_SPLIT_REGEX);

        for(String inputWord : words)
        {
            try
            {
                checkAndPutWord(inputWord.toLowerCase(), lemmasMap);
            }
            catch (Exception ex)
            {
                continue;
            }
        }

        return lemmasMap;
    }


    /**
     * Метод выполняет разделение текста на слова, получение лемм их свойств для каждого слова
     * @param text текст
     * @return Set, содержащий леммы, встречающиеся в тексте
     */
    @Override
    public Set<String> getTextLemmas(String text)
    {
        Map<String, Integer> lemmasMap = getTextLemmasWithFreq(text);
        return lemmasMap.keySet();
    }


    /**
     * Метод возвращает список нормальных форм слова на английском или русском языках.
     * Если нормальные формы слова не найдены, то возвращается пустой список
     * @param word - слово
     * @return список нормальных форм слова
     */
    @Override
    public List<String> getWordLemmas(String word)
    {
        List<String> wordForms = new ArrayList<>();

        try
        {
            wordForms = russianLuceneMorphology.getNormalForms(word.toLowerCase());
        }
        catch (Exception ruEx)
        {
            try
            {
                wordForms = englishLuceneMorphology.getNormalForms(word.toLowerCase());
            }
            catch (Exception enEx)
            {
                return wordForms;
            }
        }
        return wordForms;
    }


    /**
     * Метод получает леммы слова и информацию о них и за добавляет леммы в lemmasMap
     * @param word слово
     * @param lemmasMap Map, содержащий леммы и количества их повторений в тексте
     */
    private void checkAndPutWord(String word, Map<String, Integer> lemmasMap)
    {
        List<String> wordLemmas = getWordLemmas(word);

        if(wordLemmas.size() == 0)
        {
            if(word.matches(ALLOWED_SYMBOLS_REGEX))
            {
                putWord(word, lemmasMap);
            }
            return;
        }

        List<String> properties = getProperties(word);

        for(int i = 0; i < properties.size(); i++)
        {
            String property = properties.get(i);

            if(property.endsWith(ruConj) || property.endsWith(enConj) ||
                    property.endsWith(ruInt) || property.endsWith(enInt) ||
                    property.endsWith(ruPart) || property.endsWith(enPart) ||
                    property.endsWith(ruPrep) || property.endsWith(enPrep) ||
                    property.endsWith(enArt))
            {
                continue;
            }

            int splitterPos = property.indexOf(PROPERTIES_SPLITTER);
            String propSubstr = property.substring(splitterPos);
            if(propSubstr.contains(ruPron) || propSubstr.contains(enPron))
            {
                continue;
            }

            putWord(wordLemmas.get(i), lemmasMap);
        }
    }


    /**
     * Метод добавляет лемму слова в lemmasMap (key = лемма, value = количество повторений на странице)
     * @param lemma лемма слова
     * @param lemmasMap Map, содержащий леммы и количества их повторений в тексте
     */
    private void putWord(String lemma, Map<String, Integer> lemmasMap)
    {
        if(lemmasMap.containsKey(lemma))
        {
            Integer wordRepetitions = lemmasMap.get(lemma);
            ++wordRepetitions;
            lemmasMap.put(lemma, wordRepetitions);
        }
        else
        {
            lemmasMap.put(lemma, 1);
        }
    }


    /**
     * Метод возвращает список свойств слова на английском или русском языках
     * @param word слово
     * @return список свойств
     */
    private List<String> getProperties(String word)
    {
        List<String> wordProperties;

        try
        {
            wordProperties = russianLuceneMorphology.getMorphInfo(word.toLowerCase());
        }
        catch (Exception ruEx)
        {
            wordProperties = englishLuceneMorphology.getMorphInfo(word.toLowerCase());
        }

        return wordProperties;
    }




    /**
     * ENUM
     * Части речи английского и русского языков, которые необходимо исключать из списка лемм
     */
    private enum PartOfSpeech {CONJUNCTION("СОЮЗ", "CONJ"), INTERJECTION("МЕЖД", "INT"),
        PARTICLE("ЧАСТ", "PART"), PREPOSITION("ПРЕДЛ", "PREP"),
        PRONOUN("МС", "PN"), ARTICLE("", "ARTICLE");

        private final String ruDesc;
        private final String enDesc;


        PartOfSpeech(String ruDesc, String enDesc)
        {
            this.ruDesc = ruDesc;
            this.enDesc = enDesc;
        }

        private String getRuDesc()
        {
            return ruDesc;
        }

        private String getEnDesc()
        {
            return enDesc;
        }
    }
}