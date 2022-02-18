package main.util;

import org.apache.logging.log4j.util.Strings;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Morph {

    private static final String[] FUNCTION_WORDS_RU = {"МЕЖД", "СОЮЗ", "ПРЕДЛ", "МС", "ЧАСТ", "L С"};

    private LuceneMorphology morphRU;

    public Morph() throws IOException {
        morphRU = new RussianLuceneMorphology();
    }

    public Map<String, Integer> lemmaCount(String text) {
        HashMap<String, Integer> lemmaCount = new HashMap<>();

        if (Strings.isBlank(text)) {
            return lemmaCount;
        }

        text = text.toLowerCase();

        String textRU = text.replaceAll("[^а-я\s\n]*", "").trim();
        if (Strings.isBlank(textRU)) {
            return lemmaCount;
        }

        String[] wordsRU = textRU.split("[\s\n]+");

        for (int i = 0; i < wordsRU.length; i++) {
            if (!isFunctionWordRU(wordsRU[i])) {
                String lemma = morphRU.getNormalForms(wordsRU[i]).get(0);
                Integer count = lemmaCount.getOrDefault(lemma, 0);
                lemmaCount.put(lemma, count + 1);
            }
        }

        return lemmaCount;
    }

    private boolean isFunctionWordRU(String word) {
        List<String> morphInfo = morphRU.getMorphInfo(word);

        for (String info : morphInfo) {
            for (int i = 0; i < FUNCTION_WORDS_RU.length; i++) {
                if (info.contains(FUNCTION_WORDS_RU[i])) {
                    return true;
                }
            }
        }

        return false;
    }
}
