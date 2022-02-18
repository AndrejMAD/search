package main.controller;

import main.entity.Site;
import main.service.SearchResultElement;
import main.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;

    @PostMapping("/search")
    public List<SearchResultElement> search(@RequestParam String siteName, @RequestParam String query) {
        return searchService.search(siteName, query);
    }
}
