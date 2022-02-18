package main.service;

import main.entity.*;
import main.util.Morph;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class SearchService {

    private Morph morph;

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    public SearchService() throws IOException {
        morph = new Morph();
    }

    public List<SearchResultElement> search(String query, String siteUrl) {
        List<SearchResultElement> result = new ArrayList();

        if (Strings.isBlank(query)) {
            return result;
        }

        Set<String> queryLemmaList = morph.lemmaCount(query).keySet();
        if (queryLemmaList.isEmpty()) {
            return result;
        }

        Site site = siteRepository.findByUrl(siteUrl);

        if (site == null) {
            return result;
        }

        List<Lemma> findLemmaList = lemmaRepository.findBySiteAndLemmaInOrderByFrequencyAsc(site, queryLemmaList);
        if (findLemmaList.size() != queryLemmaList.size()) {
            return result;
        }

        List<Page> pageList = findPageByLemmaList(findLemmaList);
        if (pageList.isEmpty()) {
            return result;
        }

        for (Page page : pageList) {
            double absRelevance = indexRepository.sumRankByPageAndLemmaIn(page, findLemmaList);
            SearchResultElement searchResult = buildSearchResultElement(site, page, findLemmaList, absRelevance);

            result.add(searchResult);
        }

        Collections.sort(result, Collections.reverseOrder());

        double maxRelevance = result.get(0).getRelevance();
        for (SearchResultElement e : result) {
            e.setRelevance(e.getRelevance() / maxRelevance);
        }

        return result;
    }

    private SearchResultElement buildSearchResultElement(Site site, Page page, List<Lemma> lemmaList, double absRelevance) {
        SearchResultElement searchResultElement = new SearchResultElement();

        Document doc = Jsoup.parse(page.getContent());
        String title = doc.select("title").get(0).text();

        StringBuilder snippetBuilder = new StringBuilder();
        for (Lemma lemma : lemmaList) {
            snippetBuilder.append("<b>").append(lemma.getLemma()).append("</b>");
        }

        searchResultElement.setSite(site.getUrl());
        searchResultElement.setSiteName(site.getName());
        searchResultElement.setUri(page.getPath());
        searchResultElement.setTitle(title);
        searchResultElement.setSnippet(snippetBuilder.toString());
        searchResultElement.setRelevance(absRelevance);

        return searchResultElement;
    }

    private List<Page> findPageByLemmaList(List<Lemma> findLemmaList) {
        List<Page> pageList = null;

        for (Lemma lemma : findLemmaList) {
            if (pageList == null) {
                pageList = indexRepository.findPageByLemma(lemma);
            } else {
                pageList = indexRepository.findPageByLemmaAndPageIn(lemma, pageList);
            }

            if (pageList.isEmpty()) {
                return pageList;
            }
        }

        return pageList;
    }
}
