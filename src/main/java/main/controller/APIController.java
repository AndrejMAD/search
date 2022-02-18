package main.controller;

import main.entity.LemmaRepository;
import main.entity.PageRepository;
import main.entity.Site;
import main.entity.SiteRepository;
import main.service.ScannerService;
import main.service.SearchResultElement;
import main.service.SearchService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class APIController {

    private static final String KEY_RESULT = "result";
    private static final String KEY_ERROR = "error";

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private ScannerService scannerService;
    @Autowired
    private SearchService searchService;

    public APIController() {
    }

    @GetMapping("/api/search")
    public Map<String, Object> search(@RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        HashMap<String, Object> result = new HashMap<>();

        if (Strings.isBlank(query)) {
            result.put(KEY_RESULT, Boolean.FALSE);
            result.put(KEY_ERROR, "Задан пустой поисковый запрос");
            return result;
        }

        if (site == null) {
//            TODO:
            result.put(KEY_RESULT, Boolean.FALSE);
            result.put(KEY_ERROR, "Поиск по всем сайтам находится в разработке.");
            return result;
        } else {
            List<SearchResultElement> searchResult = searchService.search(query, site);

            result.put(KEY_RESULT, Boolean.TRUE);
            result.put("count", 100);
            result.put("data", searchResult);
        }

        return result;
    }

    /**
     * Запуск полной индексации — GET /api/startIndexing
     * <p>
     * Метод запускает полную индексацию всех сайтов или полную переиндексацию, если они уже проиндексированы.
     * Если в настоящий момент индексация или переиндексация уже запущена, метод возвращает соответствующее сообщение об ошибке.
     * <p>
     * Параметры:
     * Метод без параметров
     * <p>
     * Формат ответа в случае успеха:
     * {
     * 'result': true
     * }
     * <p>
     * Формат ответа в случае ошибки:
     * {
     * 'result': false,
     * 'error': "Индексация уже запущена"
     * }
     */
    @GetMapping("/api/startIndexing")
    public Map<String, Object> startIndexing() {
        HashMap<String, Object> result = new HashMap<>();

        try {
            if (scannerService.isIndexing()) {
                result.put(KEY_RESULT, Boolean.FALSE);
                result.put(KEY_ERROR, "Индексация уже запущена");
                return result;
            }

            scannerService.scanSiteAll();

            result.put(KEY_RESULT, Boolean.TRUE);
            return result;
        } catch (Exception e) {
            e.printStackTrace();

            result.put(KEY_RESULT, Boolean.FALSE);
            result.put(KEY_ERROR, e.getMessage());
        }

        return result;
    }

    /**
     * Остановка текущей индексации — GET /api/stopIndexing
     * <p>
     * Метод останавливает текущий процесс индексации (переиндексации). Если в настоящий момент индексация или переиндексация не происходит, метод возвращает соответствующее сообщение об ошибке.
     * <p>
     * Параметры:
     * Метод без параметров.
     * <p>
     * Формат ответа в случае успеха:
     * {
     * 'result': true
     * }
     * <p>
     * Формат ответа в случае ошибки:
     * {
     * 'result': false,
     * 'error': "Индексация не запущена"
     * }
     */
    @GetMapping("/api/stopIndexing")
    public Map<String, Object> stopIndexing() {
        HashMap<String, Object> result = new HashMap<>();

        if (scannerService.isIndexing()) {

            scannerService.stop();

            result.put(KEY_RESULT, Boolean.TRUE);
        } else {
            result.put(KEY_RESULT, Boolean.FALSE);
            result.put(KEY_ERROR, "Индексация не запущена");
        }

        return result;
    }

    /**
     * Добавление или обновление отдельной страницы — POST /api/indexPage
     * <p>
     * Метод добавляет в индекс или обновляет отдельную страницу, адрес которой передан в параметре.
     * Если адрес страницы передан неверно, метод должен вернуть соответствующую ошибку.
     * <p>
     * Параметры:
     * url — адрес страницы, которую нужно переиндексировать.
     * <p>
     * Формат ответа в случае успеха:
     * {
     * 'result': true
     * }
     * <p>
     * Формат ответа в случае ошибки:
     * {
     * 'result': false,
     * 'error': "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"
     * }
     */
    @PostMapping("/api/indexPage")
    public Map<String, Object> indexPage(@RequestParam String url) {
        HashMap<String, Object> result = new HashMap<>();
        try {
            scannerService.scanPage(url);
            result.put(KEY_RESULT, Boolean.TRUE);
        } catch (MalformedURLException | IllegalArgumentException e) {
            e.printStackTrace();
            result.put(KEY_RESULT, Boolean.FALSE);
            result.put(KEY_ERROR, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле.");
        }

        return result;
    }

    /**
     * Статистика — GET /api/statistics
     * <p>
     * Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и самого движка.
     * <p>
     * Параметры:
     * Метод без параметров.
     * <p>
     * Формат ответа:
     * {
     * 'result': true,
     * 'statistics': {
     * "total": {
     * "sites": 10,
     * "pages": 436423,
     * "lemmas": 5127891,
     * "isIndexing": true
     * },
     * "detailed": [
     * {
     * "url": "http://www.site.com",
     * "name": "Имя сайта",
     * "status": "INDEXED",
     * "statusTime": 1600160357,
     * "error": "Ошибка индексации: главная страница сайта недоступна",
     * "pages": 5764,
     * "lemmas": 321115
     * },
     * ...
     * ]
     * }
     */
    @GetMapping("/api/statistics")
    public Map<String, Object> statistics() {
        HashMap<String, Object> result = new HashMap<>();
        HashMap<String, Object> statistics = new HashMap<>();
        HashMap<String, Object> total = new HashMap<>();
        List<Map<String, Object>> detailed = new ArrayList<>();

        result.put(KEY_RESULT, Boolean.TRUE);
        result.put("statistics", statistics);
        statistics.put("total", total);

        total.put("sites", siteRepository.count());
        total.put("pages", pageRepository.count());
        total.put("lemmas", lemmaRepository.count());
        total.put("isIndexing", scannerService.isIndexing());

        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            HashMap<String, Object> detailedItem = new HashMap<>();

            detailedItem.put("url", site.getUrl());
            detailedItem.put("name", site.getName());
            detailedItem.put("status", site.getStatus());
            detailedItem.put("statusTime", site.getStatusTime());
            detailedItem.put("error", site.getLastError());
            detailedItem.put("pages", pageRepository.countBySite(site));
            detailedItem.put("lemmas", lemmaRepository.countBySite(site));

            detailed.add(detailedItem);
        }

        statistics.put("detailed", detailed);

        return result;
    }
}
